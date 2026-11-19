# 艾宾浩斯记忆助手 - APK 打包完整指南

## 当前状态

✅ **已完成**
- Node.js 已安装 (v20.11.0)
- Capacitor 配置文件已创建
- Web 应用已构建到 `dist` 目录
- 所有打包脚本已准备

⏳ **待完成**
- Android SDK 安装（构建 APK 必需）
- APK 实际构建

---

## 方案一：安装 Android SDK 后一键打包（推荐）

### 第一步：安装 Android Studio

1. 访问 https://developer.android.com/studio/download
2. 下载 Windows 版本
3. 安装时**务必勾选** "Android SDK" 选项
4. 安装完成启动一次 Android Studio

### 第二步：运行一键打包脚本

1. 打开 `memory_app` 文件夹
2. 右键点击 `一键打包APK.bat`
3. 选择 **"以管理员身份运行"**
4. 等待脚本自动完成

### 第三步：获取 APK 文件

构建成功后，APK 文件位于：
```
memory_app/android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 方案二：手动命令行打包

### 步骤 1：安装 Android SDK

下载 command-line tools：
https://developer.android.com/studio#command-line-tools-only

解压到 `C:\Android\cmdline-tools`

运行安装命令（以管理员身份）：
```batch
set ANDROID_HOME=C:\Android
C:\Android\cmdline-tools\bin\sdkmanager.bat "platform-tools" "platforms;android-33" "build-tools;33.0.0"
```

### 步骤 2：配置环境变量

系统属性 → 环境变量 → Path 添加：
```
C:\Android\cmdline-tools\bin
C:\Android\platform-tools
```

### 步骤 3：运行打包命令

```batch
cd memory_app
npm install
npx cap add android
npx cap sync android
cd android
gradlew assembleDebug
```

APK 文件位于：
```
memory_app/android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 方案三：使用 HBuilderX（最简单）

### 第一步：下载 HBuilderX
访问 https://www.dcloud.io/hbuilderx.html

### 第二步：打开项目
1. 启动 HBuilderX
2. 文件 → 打开目录 → 选择 `memory_app`

### 第三步：云打包
1. 菜单：发行 → 原生App-云打包
2. 选择 **Android**
3. 选择 **使用云端证书**
4. 点击"打包"

无需安装 Android SDK，HBuilderX 会自动处理。

---

## 方案四：使用在线打包平台

### APPCan
1. 访问 https://www.appcan.cn/
2. 注册账号
3. 上传 `memory_app` 文件夹
4. 在线打包

### DCloud
1. 访问 https://dev.dcloud.net.cn/
2. 注册账号
3. 使用 HBuilderX 云打包功能

---

## 安装 APK 到手机

### 方法 1：数据线传输
1. 用数据线连接手机和电脑
2. 将 APK 文件复制到手机
3. 在手机上点击 APK 文件安装

### 方法 2：局域网传输
1. 将 APK 上传到网盘
2. 在手机上下载

### 方法 3：扫码安装
1. 在电脑上运行 `start_server.py`
2. 手机访问电脑 IP:8080
3. 下载并安装

---

## 安装注意事项

### 手机设置
- 开启"未知来源安装"权限
- 部分手机需要关闭"禁止恶意应用安装"提示

### APK 签名
- 使用云端证书打包的 APK 只能用于测试
- 正式发布需要申请签名证书

---

## 文件结构说明

```
memory_app/
├── dist/                    # Web 应用构建目录
│   ├── index.html
│   ├── manifest.json
│   └── sw.js
├── android/                 # Android 项目（待生成）
├── node_modules/            # npm 依赖
├── package.json              # npm 配置
├── capacitor.config.json     # Capacitor 配置
├── 一键打包APK.bat          # Windows 一键打包脚本
├── start_server.py          # 本地服务器脚本
└── ...
```

---

## 常见问题

### Q: 打包脚本闪退？
以管理员身份运行批处理文件

### Q: 提示"node 不是内部命令"？
重新安装 Node.js，确保添加到系统 PATH

### Q: 提示"Gradle 构建失败"？
确保 Android SDK 正确安装且环境变量已设置

### Q: 手机无法安装 APK？
检查手机安全设置，允许安装未知来源应用

---

## 技术支持

- HBuilderX 文档：https://www.dcloud.io/docs/
- Capacitor 文档：https://capacitorjs.com/
- Android 开发者官网：https://developer.android.com/
