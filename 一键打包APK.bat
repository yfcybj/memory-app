@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo   艾宾浩斯记忆助手 - APK打包脚本
echo ========================================

REM 设置环境变量
set "JAVA_HOME=C:\Program Files\Amazon Corretto\jdk17.0.19_10"
set "ANDROID_HOME=C:\Android"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools;%PATH%"

REM 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

echo.
echo [1/8] 验证环境...
java -version 2>&1 | findstr "version"
if errorlevel 1 (
    echo [错误] Java 未安装！请先运行 安装SDK.bat
    pause
    exit /b 1
)

echo.
echo [2/8] 验证 Android SDK...
if not exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    echo [错误] Android SDK 未安装！请先运行 安装SDK.bat
    pause
    exit /b 1
)

echo.
echo [3/8] 安装 Node.js 依赖...
call npm install

echo.
echo [4/8] 构建 Web 应用...
call npm run build

echo.
echo [5/8] 同步到 Android 项目...
call npx cap sync android

echo.
echo [6/8] 复制图标和配置...
if exist "android\app\src\main\res\mipmap-xxxhdpi" (
    copy /Y "icon.png" "android\app\src\main\res\mipmap-xxxhdpi\ic_launcher.png" 2>nul
)
if exist "icon.png" (
    copy /Y "icon.png" "android\app\src\main\res\drawable\splash.png" 2>nul
)

echo.
echo [7/8] 构建 APK（这可能需要几分钟）...
cd android
call gradlew assembleDebug

echo.
echo [8/8] 检查 APK 输出...
set "APK_PATH=%SCRIPT_DIR%android\app\build\outputs\apk\debug\app-debug.apk"
if exist "%APK_PATH%" (
    echo.
    echo ========================================
    echo   ✓ APK 构建成功！
    echo ========================================
    echo.
    echo APK 位置: %APK_PATH%
    echo.
    echo 正在复制到项目根目录...
    copy /Y "%APK_PATH%" "%SCRIPT_DIR%MemoryHelper.apk"
    echo.
    echo ✓ APK 已复制到: %SCRIPT_DIR%MemoryHelper.apk
    echo.
) else (
    echo.
    echo [警告] APK 未找到，请检查构建日志
    echo 尝试查找: android\app\build\outputs\apk
    dir /s /b android\app\build\outputs\apk\*.apk 2>nul
)

echo.
echo ========================================
echo   完成！
echo ========================================
pause
