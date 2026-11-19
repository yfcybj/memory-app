@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

echo ========================================
echo   艾宾浩斯记忆助手 - 全自动 APK 打包
echo ========================================
echo.

REM 检查管理员权限
net session >nul 2>&1
if not errorlevel 1 (
    echo [OK] 管理员权限
) else (
    echo [提示] 建议以管理员身份运行以避免权限问题
)

REM 设置工作目录
cd /d "%~dp0"

REM 1. 检查并安装 Node.js
echo [步骤 1/6] 检查 Node.js...
"C:\Program Files\nodejs\node.exe" --version >nul 2>&1
if errorlevel 1 (
    echo [错误] Node.js 未安装！
    echo 请先安装 Node.js: https://nodejs.org/
    pause
    exit /b 1
) else (
    echo [OK] Node.js 已安装
)
echo.

REM 2. 安装 Capacitor
echo [步骤 2/6] 安装 Capacitor...
"C:\Program Files\nodejs\npm.cmd" install @capacitor/core@5 @capacitor/cli@5 @capacitor/android@5 --save
if errorlevel 1 (
    echo [警告] Capacitor 安装可能有问题，继续尝试...
)
echo.

REM 3. 初始化 Capacitor
echo [步骤 3/6] 初始化 Capacitor...
if exist "capacitor.config.json" (
    echo [跳过] 已存在 capacitor.config.json
) else (
    "C:\Program Files\nodejs\npx.cmd" cap init MemoryHelper com.memory.app --web-dir=. --skip-overwrite
)
echo.

REM 4. 添加 Android 平台
echo [步骤 4/6] 添加 Android 平台...
if exist "android" (
    echo [跳过] Android 平台已存在
) else (
    "C:\Program Files\nodejs\npx.cmd" cap add android
)
echo.

REM 5. 构建 Web 应用
echo [步骤 5/6] 构建 Web 应用...
if not exist "dist" mkdir dist
copy index.html dist\ >nul
copy manifest.json dist\ >nul
copy sw.js dist\ >nul
echo [OK] Web 应用已构建到 dist 目录
echo.

REM 6. 同步到 Android
echo [步骤 6/6] 同步到 Android...
"C:\Program Files\nodejs\npx.cmd" cap sync android
echo.

echo ========================================
echo   Capacitor 配置完成！
echo ========================================
echo.
echo 项目已准备好构建 APK。
echo.

REM 检查 Android SDK
echo [检查] Android SDK...
if exist "%ANDROID_HOME%" (
    echo [OK] ANDROID_HOME: %ANDROID_HOME%
    echo.
    echo 正在构建 APK...
    cd android
    .\gradlew assembleDebug
    if errorlevel 1 (
        echo.
        echo [错误] Gradle 构建失败
        echo 请确保已安装 Android Studio 和 Android SDK
    ) else (
        echo.
        echo ========================================
        echo   APK 构建成功！
        echo ========================================
        echo.
        for /r %%i in (*.apk) do echo APK: %%i
        echo.
    )
) else (
    echo [警告] 未检测到 Android SDK
    echo.
    echo 需要安装 Android SDK 才能构建 APK
    echo.
    echo 请选择以下方式之一：
    echo.
    echo 方式 1: 下载 Android Studio（推荐）
    echo   https://developer.android.com/studio
    echo.
    echo 方式 2: 使用命令行工具
    echo   1) 下载 command line tools:
    echo      https://developer.android.com/studio#command-line-tools-only
    echo   2) 解压到 C:\Android\cmdline-tools
    echo   3) 运行: sdkmanager "platform-tools" "platforms;android-33" "build-tools;33.0.0"
    echo   4) 设置环境变量 ANDROID_HOME=C:\Android
    echo.
)
echo.
pause
