@echo off
setlocal enabledelayedexpansion

echo ========================================
echo   Android SDK 安装脚本
echo ========================================

REM 设置环境变量
set "JAVA_HOME=C:\Program Files\Amazon Corretto\jdk17.0.19_10"
set "ANDROID_HOME=C:\Android"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools;%PATH%"

echo.
echo [1/4] 检查 Java...
java -version

echo.
echo [2/4] 安装 platform-tools...
call "%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools"

echo.
echo [3/4] 安装 platforms;android-34...
call "%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat" "platforms;android-34"

echo.
echo [4/4] 安装 build-tools;34.0.0...
call "%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat" "build-tools;34.0.0"

echo.
echo ========================================
echo   SDK 安装完成！
echo ========================================

REM 设置持久化环境变量
setx JAVA_HOME "%JAVA_HOME%" /M
setx ANDROID_HOME "%ANDROID_HOME%" /M
setx PATH "%PATH%" /M

echo.
echo 环境变量已设置。
echo 请重启命令提示符后运行 打包APK.bat
pause
