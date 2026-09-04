# 豆袋 CoffeeBeanTracker

[English](README_EN.md) | 简体中文

[![License](https://img.shields.io/badge/code%20license-Apache%202.0-blue)](LICENSE) [![Trademark](https://img.shields.io/badge/name%20%26%20logo-%E4%B8%8D%E5%9C%A8%E6%8E%88%E6%9D%83%E8%8C%83%E5%9B%B4-important)](NOTICE.txt) [![Platform](https://img.shields.io/badge/platform-Android-brightgreen)](#) [![Kotlin](https://img.shields.io/badge/Kotlin-orange)](#)

咖啡豆库存与冲煮记录 App：生豆批次管理、熟豆库存、做一杯扣账、烘焙消耗联动，纯本地运行。

> 姐妹应用 [烤豆 RoastCurve](https://github.com/MDx-MoJe/RoastCurve)（[Gitee](https://gitee.com/MDx-MoJe/roast-curve)）：烘焙曲线实时监控与自动跟随，两 App 通过系统级接口互联。

```
豆袋 CoffeeBeanTracker → 管豆子（库存、记录、杯测）
烤豆 RoastCurve       → 管曲线（监控、设计、导出）
```

## 功能

- **生豆批次**：产地、处理法、海拔、采收年份、剩余克重，出库入库全程留痕
- **熟豆库存**：烘焙日期、养豆天数、最佳赏味期提醒，库存同名自动累加
- **做一杯**：按冲煮方式（手冲 / 意式等）扣减熟豆，流水可查
- **烘焙联动**：与烤豆 App 互联，一炉烘完自动「扣生豆 + 熟豆入库」，幂等安全
- **杯测记录**：风味标签与评分，帮助建立自己的味觉档案
- **数据备份**：一键导出 / 导入，数据完全掌握在自己手里
- **中英双语**：设置页一键切换语言（跟随系统 / 中文 / English），即时生效

纯本地应用：不联网、不收集任何数据、无广告、无内购。

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 | Kotlin |
| UI | Android View 体系 + ViewBinding |
| 数据库 | Room（SQLite） |
| 架构 | 单模块 + Repository |
| 互联 | ContentProvider（跨 App 幂等扣账接口） |

## 模块结构

```
CoffeeBeanTracker/
├── app/
│   └── src/main/java/com/coffee/beantracker/
│       ├── data/          # Room 实体与 DAO（CoffeeBean、GreenBean、DeductRecord…）
│       ├── bridge/        # 豆袋互联接口（BeanBridgeProvider，供烤豆调用）
│       ├── utils/         # 工具（日期、风味标签、隐私协议管理…）
│       └── *.kt           # 各页面 Activity
└── design/                # 应用图标设计源文件
```

## 下载

不想自己构建？直接到 [Releases](releases/) 下载 APK 安装（GitHub 与 Gitee 同步发布，国内推荐 Gitee：gitee.com/MDx-MoJe/coffee-bean-tracker/releases）。

## 构建

```bash
# Android Debug APK（无需签名配置，开箱即用）
./gradlew assembleDebug
# 产物: app/build/outputs/apk/debug/CoffeeBeanTracker-v<版本号>.apk
```

环境要求：JDK 17+、Android SDK 34。

> **自构建说明**：debug 构建直接可用。如需构建 release，复制
> `keystore.properties.example` 为 `keystore.properties` 并填入你自己的
> keystore 信息；不配置则 release 构建自动降级为 debug 签名。

## 与烤豆互联（可选）

两 App 通过 Android ContentProvider 互联，互相独立安装、互不依赖：
装了烤豆并烘焙出豆时，烤豆会调用本 App 的接口自动扣生豆、入熟豆；
未安装烤豆时豆袋一切功能正常，互联层静默待命。

## 开源协议与版权

**代码授权**：本项目采用 [Apache License 2.0](LICENSE) 协议开源，可自由使用、学习、修改与再分发。

**不在授权范围**：应用名称「豆袋 / CoffeeBeanTracker」、应用图标、官方签名证书不受许可证保护，其权利归 MDx 所有。任何 fork（分叉再发布）必须改名换图标，并在显著位置注明来源。详见 [NOTICE.txt](NOTICE.txt)。

**自构建说明**：开源用户自构建的版本功能完整、不受任何限制。

## 支持开发者

豆袋永久免费、开源、无广告。如果你喜欢它，欢迎给仓库点个 Star ⭐，或到姐妹应用烤豆的 [爱发电](https://afdian.com/a/RoastCurve) 支持开发者。详见 [SPONSOR.md](SPONSOR.md)。

Copyright © 2026 MDx
