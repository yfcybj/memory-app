
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
