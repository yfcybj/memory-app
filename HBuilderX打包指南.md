# HBuilderX 打包 APK 详细指南

## 📋 准备工作

### 1. 下载安装 HBuilderX

1. 访问官网：https://www.dcloud.io/hbuilderx.html
2. 下载 **Windows 正式版**
3. 解压到任意位置（建议 D:\HBuilderX）
4. 双击 `HBuilderX.exe` 启动

### 2. 检查项目文件

确认 `memory_app` 文件夹包含以下文件：
- ✅ index.html（主页面）
- ✅ manifest.json（应用配置）
- ✅ sw.js（离线支持）
- ✅ icon.svg（图标）

---

## 🚀 打包步骤

### 第一步：创建/导入项目

**方法 A：直接打开文件夹**
1. HBuilderX 菜单：文件 → 打开目录
2. 选择 `memory_app` 文件夹
3. 点击"确定"

**方法 B：新建 5+App 项目**
1. 文件 → 新建 → 项目
2. 选择 **5+App**
3. 填写项目名称：记忆助手
4. 点击创建
5. 删除自动生成的文件
6. 将 `memory_app` 中的文件复制到项目目录

### 第二步：检查 manifest.json

1. 在项目管理器中，双击 `manifest.json`
2. 确认以下配置：
   - 应用名称：艾宾浩斯记忆助手
   - Appkey：留空或点击"云获取"
   - 图标设置：上传 512x512 PNG 图标

### 第三步：配置应用图标

1. 在 manifest.json 编辑界面，点击"图标设置"
2. 上传 512x512 的 PNG 图片作为应用图标
3. 自动生成各尺寸图标

### 第四步：云打包

1. 菜单：发行 → 原生App-云打包
2. 选择打包平台：**Android**
3. 证书选项：
   - **使用云端证书**（推荐，无需配置）
   - 或使用本地证书（需要自己生成）
4. 确认应用信息无误
5. 点击"打包"

### 第五步：等待完成

1. 首次打包需要登录 DCloud 账号
2. 等待 1-5 分钟（根据服务器负载）
3. 打包成功后会弹出下载提示

### 第六步：下载 APK

1. 点击"打开目录"查看生成的 APK 文件
2. 文件名类似：`记忆助手_1.0.0.apk`
3. 可以通过以下方式安装到手机：
   - 数据线传输
   - 扫码下载
   - 局域网共享

---

## 🔧 常见问题

### Q1: 提示"Appkey 不能为空"
**解决方法**：
1. 点击"云获取"按钮
2. 登录 DCloud 账号
3. 获取 Appkey

### Q2: 打包失败，提示证书错误
**解决方法**：
- 选择"使用云端证书"（会自动生成）
- 不要手动填写证书信息

### Q3: 手机无法安装 APK
**解决方法**：
1. 检查手机设置 → 安全 → 开启"未知来源安装"
2. 确认 APK 版本与手机 Android 版本兼容

### Q4: 如何生成自定义图标
**解决方法**：
1. 使用在线工具：https://www.toolscat.com/appicon
2. 上传一张 1024x1024 的图片
3. 下载生成的各尺寸图标
4. 在 manifest.json 中上传

### Q5: 应用名称显示乱码
**解决方法**：
- manifest.json 使用纯 ASCII 字符
- 应用名称改为：Memory Helper

---

## 📱 安装测试

### 使用模拟器测试
1. 下载安装 Android Studio
2. 创建虚拟设备（推荐 Pixel 4）
3. 启动模拟器
4. 将 APK 拖入模拟器窗口安装

### 使用真机测试
1. 手机开启开发者模式
2. USB 连接电脑
3. 运行命令安装：
```bash
adb install -r memory_app.apk
```

---

## ✅ 验收清单

- [ ] HBuilderX 已安装并能正常运行
- [ ] 项目成功导入 HBuilderX
- [ ] manifest.json 配置正确
- [ ] 图标已设置
- [ ] 云打包成功完成
- [ ] APK 文件已生成
- [ ] 在手机上成功安装
- [ ] 应用能正常启动和使用

---

## 📞 获取帮助

- HBuilderX 官方文档：https://www.dcloud.io/docs/
- DCloud 社区：https://ask.dcloud.io/
- 问题反馈：https://github.com/dcloudio/uni-app/issues
