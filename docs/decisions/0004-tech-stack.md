---
doc: docs/decisions/0004-tech-stack
title: 技术栈与工程结构：Kotlin + Compose + Room，单模块，手写 DI
status: accepted
date: 2026-10-30
owner: wang
supersedes: []
superseded-by: []
---

# 0004. 技术栈与工程结构

## 背景

应用要在 Android 手机/平板上完成：分享接收、WebView 解析、大文件下载、本地数据库、视频播放、自适应 UI。仅侧载自用，团队规模为 1 人。

## 选项

| 选项 | 优点 | 缺点 |
|---|---|---|
| Kotlin + Compose + Room，单模块，手写 DI | 平台能力零阻力；启动快；构建简单；无注解处理运行时 | 模块边界靠约定；代码量增大后需拆分 |
| Kotlin + 多模块 + Hilt | 更清晰的边界；大团队友好 | 构建变慢；Hilt/KSP 增加复杂度；个人项目收益低 |
| Flutter / React Native | 跨平台 | 分享接收/MediaStore/WebView/Media3 都要平台通道；包体与性能成本 |

## 决定

采用 **Kotlin 2.0 + Jetpack Compose + Room + 手写 `AppContainer`，单 `:app` 模块**。

- 依赖方向靠包结构与评审保证：`feature → ui → core`，core 不反向依赖。
- 网络 OkHttp；JSON kotlinx.serialization；图片 Coil；视频 Media3；HTML Jsoup。
- 不引入 Hilt/Koin、不引入广告/统计/崩溃 SDK。
- `compileSdk 34 / minSdk 26 / targetSdk 34`。

## 后果

- 正面：冷启动路径短；调试简单；依赖少；包体可控；任何平台能力可直接调用。
- 负面：模块边界不是编译期强制；未来拆模块需要一次重构（包结构已按模块划分，成本可控）。
- 触发重审：代码超过约 3 万行、多人协作、或需要 iOS 版本。
