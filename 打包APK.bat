@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo   艾宾浩斯记忆助手 - APK打包脚本
echo ========================================

REM 设置环境变量（使用已安装的 Android Studio SDK）
set "JAVA_HOME=C:\Program Files\Amazon Corretto\jdk17.0.19_10"
set "ANDROID_HOME=C:\Users\ycxqs\AppData\Local\Android\Sdk"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\emulator;%PATH%"

REM 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

echo.
echo [1/6] 验证环境...
java -version 2>&1 | findstr "version"
echo.
echo [2/6] 检查 Android SDK...
dir "%ANDROID_HOME%\platform-tools\adb.exe" >nul 2>&1 && echo   ✓ platform-tools
dir "%ANDROID_HOME%\platforms" >nul 2>&1 && echo   ✓ platforms
dir "%ANDROID_HOME%\build-tools" >nul 2>&1 && echo   ✓ build-tools

echo.
echo [3/6] 安装 Capacitor 依赖...
call npm install

echo.
echo [4/6] 初始化 Capacitor...
call npx cap init MemoryHelper com.memory.app --web-dir=dist --skip-overwrite

echo.
echo [5/6] 添加 Android 平台并同步...
call npx cap add android
call npx cap sync android

echo.
echo [6/6] 构建 APK（这可能需要几分钟）...
cd android
call gradlew assembleDebug

echo.
echo ========================================
echo   完成！
echo ========================================
set "APK_PATH=%SCRIPT_DIR%android\app\build\outputs\apk\debug\app-debug.apk"
if exist "%APK_PATH%" (
    echo.
    echo   ✓ APK 构建成功！
    echo.
    echo   位置: %APK_PATH%
    echo.
    copy /Y "%APK_PATH%" "%SCRIPT_DIR%MemoryHelper.apk" >nul
    echo   ✓ 已复制到: %SCRIPT_DIR%MemoryHelper.apk
) else (
    echo   [警告] 请检查上方构建日志
)
echo.
pause
