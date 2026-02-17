# 🎙️ 录音机 App

一款简洁美观的 Android 录音机应用，支持高质量录音、播放和管理功能。

## ✨ 功能特点

- 🎙️ **高质量录音** - AAC 编码，音质清晰
- 🎨 **精美界面** - 渐变背景、浮动操作按钮
- ⏱️ **实时计时** - 录音时长显示
- 📂 **自动保存** - 保存到应用私有目录
- ▶️ **播放功能** - 内置播放器

## 📱 系统要求

- **最低 Android 版本**: Android 7.0 (API 24)
- **权限需求**: 录音、存储权限

## 🚀 快速开始

### 方法一：下载预编译 APK

在 [GitHub Releases](../../releases) 页面下载最新版本的 APK 文件，直接安装即可。

### 方法二：自行编译

#### 使用 Android Studio

1. 克隆或下载本项目
2. 用 Android Studio 打开项目
3. 等待 Gradle 同步完成
4. 连接手机（开启 USB 调试）
5. 点击运行按钮 ▶️

#### 使用命令行

```bash
# 克隆项目
git clone https://github.com/您的用户名/AudioRecorder.git
cd AudioRecorder

# 构建 Debug APK
./gradlew assembleDebug

# 安装到手机
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📁 项目结构

```
app/
├── src/main/
│   ├── java/com/example/audiorecorder/
│   │   └── MainActivity.kt       # 主活动代码
│   ├── res/
│   │   ├── layout/
│   │   │   └── activity_main.xml  # 界面布局
│   │   ├── values/
│   │   │   ├── colors.xml        # 颜色定义
│   │   │   ├── strings.xml       # 文本定义
│   │   │   └── themes.xml        # 主题样式
│   │   └── drawable/             # 图标和背景
│   └── AndroidManifest.xml       # 应用配置
└── build.gradle                  # 模块构建配置
```

## 🔒 权限说明

本应用需要以下权限：

- `RECORD_AUDIO` - 录制音频
- `WRITE_EXTERNAL_STORAGE` - 保存录音文件
- `READ_EXTERNAL_STORAGE` - 读取录音文件

所有权限仅在应用运行时申请，不会收集或上传任何用户数据。

## 📝 开源协议

本项目采用 [MIT 许可证](LICENSE) 开源。

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📧 联系方式

如有任何问题或建议，请通过以下方式联系：

- GitHub Issues: [提交 Issue](../../issues)

---

Made with ❤️ by OpenClaw
