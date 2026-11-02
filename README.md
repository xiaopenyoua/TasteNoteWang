# 味记・王（TasteNote Wang）

> 把抖音、小红书上看到的美食教程，收进一个真正会做饭的菜谱库。

**状态**：MVP 可运行（2026-11-02）· Android 手机 / 平板 · 仅侧载自用

## 这是什么

一个 Android 手机 + 平板上的**个人菜谱应用**。核心不是"收藏"，而是"收藏之后真的做出来"：

- **收录**：从抖音、小红书、B站通过系统分享或复制链接进来，支持视频 / 图文 / 图文+视频
- **整理**：拆成可读的步骤与食材清单，保留原链接与出处，媒体优先取无水印源
- **使用**：烹饪模式大字、防误触、屏幕常亮，食材按份数换算
- **沉淀**：记录每次实际烹饪的调整，形成自己的版本

## 仓库结构

三个内容目录，一个方向：`docs/`（为什么）→ `design/`（做成什么样）→ `android/`（怎么实现，产物在 `android/app/build/`）。

```
TasteNoteWang/
├── README.md            # 项目门面（本文件）
├── AGENTS.md            # 目录契约：唯一的归属判据 + 可执行规则
├── CLAUDE.md            # Claude 入口（只含 @AGENTS.md）
│
├── design/              # 设计侧：产品与界面
│   ├── product/         #   PRD、信息架构、页面规格、视觉语言
│   ├── wireframes/      #   低保真线框
│   ├── exports/         #   视觉稿导出与切图
│   └── source/          #   设计源文件（gitignore）
│
├── android/             # 开发侧：Gradle 工程根
│   ├── app/             #   Kotlin 源码、资源、测试、Room schema
│   │   └── build/       #   构建产物（gitignore）：APK 在 outputs/apk/
│   ├── docs/            #   架构、数据、存储、导入、性能、测试
│   ├── tools/           #   目录契约自检脚本
│   └── build.gradle.kts / settings.gradle.kts
│
├── docs/                # 判断依据：为什么做、参考了什么
│   ├── research/        #   市场调研、技术调研、证据附录
│   ├── decisions/       #   ADR
│   └── conventions/     #   写作规范、ADR 指南
│
└── local/               # 本地私有：抓包、调试 JSON（整体 gitignore）
```

规则全文见 [`AGENTS.md`](AGENTS.md)；为什么这样分见 [`docs/decisions/0008-dir-contract.md`](docs/decisions/0008-dir-contract.md)。

## 导航

| 你想看 | 去哪 |
|---|---|
| 东西放哪里、文件怎么命名 | [`AGENTS.md`](AGENTS.md) |
| 设计侧总索引 | [`design/README.md`](design/README.md) |
| 产品需求与界面规格 | [`design/product/`](design/product/) |
| 判断依据总索引 | [`docs/README.md`](docs/README.md) |
| 市场调研 | [`docs/research/2026-10-30-market-research.md`](docs/research/2026-10-30-market-research.md) |
| 调研证据与来源 | [`docs/research/2026-10-30-market-research-evidence.md`](docs/research/2026-10-30-market-research-evidence.md) |
| 技术方案调研 | [`docs/research/2026-10-30-tech-research.md`](docs/research/2026-10-30-tech-research.md) |
| 为什么做某个决定 | [`docs/decisions/`](docs/decisions/) |
| 技术架构与实现文档 | [`android/docs/`](android/docs/) |
| 测试方案与样本 | [`android/docs/test-plan.md`](android/docs/test-plan.md) / [`test-data.md`](android/docs/test-data.md) |
| 手机/平板试用安装包 | `android/app/build/outputs/apk/debug/app-debug.apk` |

## 开发

```bash
cd android

gradle :app:assembleDebug             # 构建调试包
gradle :app:testDebugUnitTest         # JVM 单元测试
gradle :app:connectedDebugAndroidTest # 仪器化 UI 测试（需连接设备/模拟器）
```

技术栈：Kotlin 2.0 + Jetpack Compose + Material 3 + Room + OkHttp + Media3 + Coil，单模块，本地优先，手机/平板自适应。详见 [`android/docs/architecture.md`](android/docs/architecture.md)。

## 手机 / 平板试用

构建后的安装包就在 Gradle 的原生产物目录：

```
android/app/build/outputs/apk/debug/app-debug.apk
```

```bash
# USB 安装
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

也可以把 APK 传到手机，用文件管理器打开安装（首次需允许“安装未知应用”）。

- 包名：`com.weiji.wang.debug`
- 支持：Android 8.0+ 手机 / 平板（不支持电视）
- 这是 **debug 包**，与正式包数据不互通；卸载会删除本机数据，换机前先导出备份
- 数据目录：`/sdcard/Android/data/com.weiji.wang.debug/files/weiji/`
- 注意：`gradle clean` 会清空整个 `android/app/build/`，需要保留安装包时先复制出去

改完结构自检：

```bash
bash android/tools/verify-layout.sh
```

## 当前进度

- [x] 目录契约与可执行自检（`AGENTS.md` + `verify-layout.sh`）
- [x] 市场调研（7 维度 + 证据附录 + 对抗审查）
- [x] 技术方案调研（三平台实测 + GitHub 方案对比）
- [x] 产品设计（PRD / 信息架构 / 页面规格 / Apple 风格视觉）
- [x] 数据与文件存储设计（九条铁律 + 备份格式）
- [x] Android 应用（收录 / 解析 / 下载 / 详情 / 烹饪 / 采购 / 备份）
- [x] 测试（60 单元 + 12 真机 UI + 5 条真实链接）
- [x] 手机 / 平板试用包（`android/app/build/outputs/apk/debug/`）
- [ ] P1：WorkManager 化导入、后台计时、OCR、WebDAV

## 已验证的平台能力（2026-10-30）

| 平台 | 图文 | 视频 | 无水印 |
|---|---|---|---|
| 小红书 | ✅ 原图 + 正文 | ✅ 本地播放 | ✅ `ci.xiaohongshu.com/{fileId}` / 原视频流 |
| B站 | — | ✅ 720p 单文件 | ✅ 平台本身 |
| 抖音 | ✅ 原图 + 正文 | ⚠️ 标题/封面/食材；视频需登录 | ✅ `play_addr`（登录后） |
