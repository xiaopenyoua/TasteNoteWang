---
doc: android/docs/architecture
title: 技术架构
status: draft
date: 2026-10-30
owner: wang
related:
  - android/docs/data-model
  - android/docs/data-storage
  - android/docs/import-pipeline
  - docs/decisions/0004-tech-stack
---

# 技术架构

> 单模块 Android 应用，本地优先，无服务端。所有解析与下载在设备上完成。

## 1. 技术栈（已落地）

| 层 | 选型 | 版本 | 理由 |
|---|---|---|---|
| 语言 | Kotlin | 2.0.21 | 协程、密封类、与 Compose 一致 |
| UI | Jetpack Compose + Material 3 | BOM 2024.06.00 | 声明式、平板自适应、动效可控 |
| 数据库 | Room (KSP) | 2.6.1 | 单一事实源、Flow、事务 |
| 偏好 | DataStore Preferences | 1.1.1 | 主题/下载策略 |
| 网络 | OkHttp | 4.12.0 | 成熟、拦截器、Range |
| JSON | kotlinx.serialization | 1.7.1 | 多态/快照、无反射 |
| 图片 | Coil | 2.6.0 | Compose 原生、磁盘缓存 |
| 视频 | Media3 ExoPlayer + PlayerView | 1.3.1 | 本地播放、控制器 |
| 后台 | WorkManager（已声明，导入暂用 ViewModel 协程） | 2.9.0 | 后续任务化 |
| 加密 | Security Crypto (EncryptedSharedPreferences) | 1.1.0-alpha06 | Cookie 本地加密 |
| HTML | Jsoup | 1.17.2 | JSON-LD、错误页识别 |
| DI | 手写 `AppContainer` | — | 零反射、启动快、依赖少 |

不选 Flutter/RN：分享接收、MediaStore、Media3 都要写平台通道；不选 Hilt：个人应用规模下编译与启动成本不划算。

## 2. 包结构（单模块）

```
com.weiji.wang
├── WeijiApplication          # 创建 AppContainer
├── MainActivity              # 单 Activity + Compose
├── capture/                  # 分享接收 Activity、下载 Service 占位
├── core/
│   ├── model/                # Platform / Ulid / IngredientScaler / TextParser
│   ├── data/
│   │   ├── db/               # Entity / DAO / Database
│   │   ├── prefs/            # SettingsRepository
│   │   └── RecipeRepository  # 菜谱/购物/烹饪记录
│   ├── import/               # 链接解析、提取器、下载器、JSON 解析
│   ├── storage/              # FileStore / Snapshot / Backup / 视频抽帧
│   └── AppContainer
├── ui/
│   ├── theme/                # Apple 风格色彩/字阶/形状
│   ├── components/           # 通用组件
│   ├── LocalContainer        # CompositionLocal
│   └── WeijiApp              # 自适应 Scaffold + NavHost
└── feature/
    ├── home / recipes / recipe / cook / capture / shopping / settings / login / edit
```

依赖方向：`feature → ui/components → core`；`core.import → core.storage → core.data.db`；**禁止反向依赖**（core 不引用 feature/UI）。

## 3. 运行时数据流

```
Compose UI
  ↓ collectAsStateWithLifecycle
ViewModel (StateFlow / mutableStateOf)
  ↓
RecipeRepository ── Room Flow ── SQLite
  ↑                         ↑
ImportCoordinator      FileStore（recipe 文件夹）
  ↓
PlatformExtractor (Douyin/Xhs/Bilibili/Generic)
  ↓
OkHttp / WebView（页面自身签名）
  ↓
MediaDownloader（Range + .part + rename）
```

- UI 只读本地 Flow，不做网络请求。
- 导入是唯一的写网络路径，由用户主动触发。
- 媒体文件与 DB 双写：**先 DB 事务，再写快照**；媒体先落盘再写 MediaEntity。崩溃最多产生孤儿文件，不会产生半条菜谱。
- 快照 `recipe.json` 是派生数据，可由 DB 重建；备份导出以 DB 为准（见 03）。

## 4. 线程模型

| 工作 | 调度 |
|---|---|
| WebView 创建/JS 执行 | Main |
| OkHttp 请求/下载 | `Dispatchers.IO`（`MediaDownloader`、`HttpExt`） |
| Room 读写 | Room 自己的 executor；事务用 `withTransaction` |
| 导入编排 | `viewModelScope`（主线程协程，内部切 IO） |
| 图片解码 | Coil 自己的 dispatcher |
| 视频播放 | ExoPlayer 内部线程，UI 只 attach |

## 5. 自适应布局

- `LocalConfiguration.screenWidthDp >= 600` 判定宽屏。
- 手机：底部 NavigationBar + FAB；平板：NavigationRail + FAB。
- 菜谱网格：宽度 ≥840dp 固定 3 列，否则 2 列。
- 烹饪模式全屏沉浸：隐藏导航、屏幕常亮、音量键翻步（`CookKeys` 桥接 Activity）。
- 不锁竖屏；`configChanges` 覆盖旋转/尺寸变化，Compose 自动重排。
- 全屏视频（`VideoFullscreenDialog`）：Compose Dialog 的内容测量默认只到“可用区域”（不含系统栏），
  所以窗口设 `MATCH_PARENT` + `FLAG_LAYOUT_IN_SCREEN` + `setFitInsetsTypes(0)` + 刘海 `ALWAYS`，
  内容用 `currentWindowMetrics` 的整屏像素 `requiredSize` 撑开；`PlayerView` 用 `RESIZE_MODE_FIT`
  保持原始比例完整显示，视频外的黑底铺满整屏。手势由 `GesturePlayerView` 在 `dispatchTouchEvent` 统一判定：单击用
  `onSingleTapConfirmed` 切控件、双击只切播放/暂停，进度条与按钮矩形内的触摸原样透传。
- 外接键盘/焦点导航：自定义按钮、卡片、FAB 使用 `focusRing` 提供焦点描边。

## 6. 错误与降级

| 失败 | 处理 |
|---|---|
| 网络/超时 | 保留链接与已抓文字；草稿可重试 |
| 需登录 | 状态 `NEEDS_LOGIN`，引导 WebView 登录 |
| 风控 | 熔断当前导入，提示稍后或手动保存导入 |
| 媒体下载失败 | 保留已成功媒体；`PARTIAL` |
| WebView 渲染崩溃 | `runCatching` + 45s 超时 + 保留 JSON-LD 内容 |
| 空间不足 | 下载前检查，跳过并提示 |

## 7. 已知限制与后续

- 导入任务在 ViewModel 协程中执行，进程被杀会中断；草稿与已落盘媒体保留，可重试。后续迁移到 WorkManager + 前台通知。
- 计时器目前仅前台运行；后台响铃需前台 Service/AlarmManager（P1）。
- OCR/ASR 未接入；图文解析以文本与图片步骤为主（P2）。
- 单模块构建便于快速迭代；代码量继续增长后可拆 `:core`/`:feature`。

## 8. 构建与发布

- `compileSdk 34 / minSdk 26 / targetSdk 34`；仅侧载，不依赖商店政策。
- `debug` 后缀 `.debug`；`release` 开启 R8 与资源压缩。
- 构建命令：`cd android && gradle :app:assembleDebug`；测试：`gradle :app:testDebugUnitTest`、`:app:connectedDebugAndroidTest`。
