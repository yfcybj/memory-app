@echo off
chcp 65001 >nul
echo ========================================
echo   艾宾浩斯记忆助手 - APK 打包脚本
echo ========================================
echo.

cd /d "%~dp0"

REM 检查 Node.js
echo [检查] Node.js...
"C:\Program Files\nodejs\node.exe" --version
if errorlevel 1 (
    echo [错误] Node.js 未安装！
    echo 请先安装 Node.js: https://nodejs.org/
    pause
    exit /b 1
)
echo.

REM 检查 Capacitor
echo [检查] Capacitor CLI...
if not exist "node_modules\@capacitor\cli" (
    echo [安装] 正在安装 Capacitor...
    "C:\Program Files\nodejs\npm.cmd" install @capacitor/core@5 @capacitor/cli@5
) else (
    echo [OK] Capacitor 已安装
)
echo.

REM 初始化 Capacitor
echo [初始化] Capacitor...
"C:\Program Files\nodejs\npx.cmd" cap init MemoryHelper com.memory.app --web-dir=. --skip-overwrite
echo.

REM 添加 Android 平台
echo [检查] Android 平台...
if not exist "android" (
    echo [安装] 正在添加 Android 平台...
    "C:\Program Files\nodejs\npm.cmd" install @capacitor/android@5
    "C:\Program Files\nodejs\npx.cmd" cap add android
) else (
    echo [OK] Android 平台已存在
)
echo.

REM 构建 Web 应用
echo [构建] Web 应用...
if exist "dist" rmdir /s /q dist
"C:\Program Files\nodejs\npm.cmd" run build 2>nul
if errorlevel 1 (
    echo [提示] 没有 build 脚本，使用默认构建方式
    if not exist "dist" mkdir dist
    copy index.html dist\ >nul
    copy manifest.json dist\ >nul
    copy sw.js dist\ >nul
)
echo.

REM 同步到 Android
echo [同步] 正在同步到 Android...
"C:\Program Files\nodejs\npx.cmd" cap sync android
echo.

echo ========================================
echo   打包步骤已完成！
echo ========================================
echo.
echo 接下来需要：
echo 1. 下载安装 Android Studio: https://developer.android.com/studio
echo 2. 安装 Android SDK 和 Build Tools
echo 3. 在 Android Studio 中打开 android 文件夹
echo 4. Build -> Build Bundle(s) / APK(s) -> Build APK(s)
echo.
echo 或者直接使用命令行构建：
echo cd android
echo .\gradlew assembleDebug
echo.
pause
