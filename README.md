# 艾宾浩斯记忆助手

基于艾宾浩斯遗忘曲线的间隔重复记忆应用，支持古诗词、单词、成语等内容的录入、复习与统计。
同一份代码可运行在浏览器（PWA）和安卓 APP（Capacitor）上。

## 功能

- 按 1 / 2 / 4 / 7 / 15 / 30 / 60 天自动安排复习
- 学习、添加、管理、统计、备份、设置六个页面
- 数据保存在本机（LocalStorage），支持 JSON 导出 / 导入 / 复制
- AI 生成原文、释义、赏析或记忆口诀（设置页可切换服务商）
- 安卓端支持从 GitHub Releases 检查并安装更新

## 网页版运行

浏览器不允许直接从本地文件调用第三方 AI 接口（跨域），必须通过自带的小型服务器访问：

```bash
python start_server.py
```

然后浏览器打开 <http://localhost:8080>。

`start_server.py` 只做两件事：托管静态文件、把 `/api/ai` 请求转发给 AI 服务商。
不要直接双击 `index.html`，否则 AI 功能会提示网络被拦截。

## AI 配置

仓库里**不包含任何 API Key**。首次使用请进入「设置 → AI 配置」：

| 项目 | 说明 |
|---|---|
| AI 服务商 | 智谱 GLM / DeepSeek / OpenAI / 自定义（OpenAI 兼容） |
| API Key | 你自己的 Key，仅保存在本机 localStorage |
| 模型 | 切换服务商会自动带出常用模型，可手改 |
| 接口地址 | OpenAI 兼容的 `/chat/completions` 地址 |

填完点「测试连接」验证，再点「保存」。

服务端也可以改用环境变量提供 Key：`AI_API_KEY=xxx python start_server.py`。

## 打包安卓 APP

需要 Node.js、JDK 17、Android SDK（compileSdk 33）。

```bash
npm install
npx cap sync android
cd android && ./gradlew assembleDebug
```

产物：`android/app/build/outputs/apk/debug/app-debug.apk`

在 `android/app/build.gradle` 里维护版本号：

```gradle
versionCode 2
versionName "1.1.0"
```

`versionName` 必须与 GitHub Release 的 tag 一致，应用内更新靠它比较版本。

## 应用内更新

APP 的「设置 → 软件更新」从 GitHub Releases 读取最新版本：

1. 把 `versionCode + 1`、`versionName` 改为要发布的 tag（如 `1.1.1`）
2. 构建 APK
3. 在 GitHub 创建 Release，tag 填 `v1.1.1`，**把 APK 作为附件上传**
4. APP 内填仓库 `用户名/仓库名` → 检查更新 → 立即更新

安装流程走系统 `DownloadManager` + `FileProvider` + 安装器，首次需要在系统设置里允许「安装未知应用」。

## 目录结构

```
index.html                     单文件应用（页面 + 逻辑）
dist/                          Capacitor 使用的 web 目录
sw.js                          Service Worker（离线缓存）
start_server.py                本地静态服务器 + AI 代理
android/                       安卓工程（含自定义原生插件）
  app/src/main/java/com/memory/app/
    MainActivity.java          注册插件
    AppUpdaterPlugin.java      下载 APK 并安装
    AiProxyPlugin.java         APP 内转发 AI 请求
    ReviewNotificationPlugin.java  复习提醒通知
```

## 许可

MIT
