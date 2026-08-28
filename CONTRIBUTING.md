# 贡献指南

感谢你对豆袋（CoffeeBeanTracker）的关注。这是一个面向咖啡爱好者的开源库存管理应用，欢迎任何形式的贡献。

## 项目结构

```
app/src/main/java/com/coffee/beantracker/
├── data/      # Room 实体与 DAO
├── bridge/    # 豆袋互联接口（ContentProvider）
├── utils/     # 工具类
└── *.kt       # 各页面 Activity
```

## 构建

```bash
# Android Debug APK
./gradlew assembleDebug
```

环境：JDK 17+、Android SDK 34。

## 提交规范

- 一个提交做一件事，提交信息说清「做了什么」和「为什么」
- 涉及版本升级时，同时更新 `app/build.gradle` 里的 `versionCode`（自增 1）与 `versionName`
- 不要提交任何密钥、密码、本地路径：`keystore.properties`、`local.properties`、`*.jks` 均已在 `.gitignore` 中

## 签名与隐私

- 自构建无需签名：复制 `keystore.properties.example` 为 `keystore.properties` 填入自己的 keystore；不配置则 release 构建自动降级为 debug 签名
- 本应用为纯本地应用，不收集、不上传任何数据，详见内置《隐私政策》

## 姐妹项目

- [烤豆 RoastCurve](https://github.com/MDx-MoJe/RoastCurve)：烘焙曲线监控与自动跟随，与本 App 通过 ContentProvider 互联。互联协议变更需两端同步发版。

## 许可

本项目采用 [Apache License 2.0](LICENSE)。提交即视为同意以该协议授权你的贡献。
