---
doc: docs/decisions/README
title: 决策记录索引
status: accepted
date: 2026-10-30
owner: wang
related:
  - docs/conventions/adr-guide
---

# 决策记录索引

> 规则见 [`../conventions/adr-guide.md`](../conventions/adr-guide.md)。
> `accepted` 的 ADR 不再编辑；变更用新 ADR 取代，编号原地保留，不移动、不归档。

| 编号 | 决策 | 状态 |
|---|---|---|
| [0001](0001-capture-strategy.md) | 链接建卡 + 用户导入官方保存的媒体，不下载、不去水印 | superseded by 0003 |
| [0002](0002-distribution.md) | 仅侧载自用 | accepted |
| [0003](0003-media-import.md) | 端侧解析三平台、下载视频/图片、优先无水印源 | accepted |
| [0004](0004-tech-stack.md) | Kotlin + Compose + Room，单模块，手写 DI | accepted |
| [0005](0005-storage-layout.md) | DB 为事实源 + 一条菜谱一个文件夹 + 派生快照 | accepted |
| [0006](0006-douyin-fallback.md) | 抖音 WebView 页面内签名 + JSON-LD 兜底，不破解签名 | superseded by 0011 |
| [0007](0007-repo-layout.md) | 仓库顶层按设计区 / 开发区 / 交付区 / 本地私有区四分 | superseded by 0008 |
| [0008](0008-dir-contract.md) | 目录契约：三内容目录 + 单一归属判据 + 可执行自检 | accepted（§6 由 0009 修订） |
| [0009](0009-apk-location.md) | 取消 dist 交付暂存区，APK 留在 android/app/build | accepted |
| [0010](0010-design-prototype.md) | 增加 design/prototype 交互原型目录，以已实现代码为唯一基准 | accepted |
| [0011](0011-douyin-share-page-sniffing.md) | 抖音改移动分享页三路嗅探；扫码登录走桌面版原生二维码 | accepted |
