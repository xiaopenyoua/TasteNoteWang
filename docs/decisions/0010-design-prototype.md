---
doc: docs/decisions/0010-design-prototype
title: 增加 design/prototype 交互原型目录，以已实现代码为唯一基准
status: accepted
date: 2026-11-02
owner: wang
supersedes: []
superseded-by: []
---

# 0010. 增加 design/prototype 交互原型目录

## 背景

应用已经实现并测试通过，但 `design/` 下只有文字规格，没有可视设计稿。更麻烦的是，现有 `screen-specs.md` 与 `visual-language.md` 写的是早期设想，和代码有多处不一致：

- 写了按压缩放、8dp 上移、220ms spring、大标题收缩、数字滚动，代码里没有。
- 写了首页搜索/设置按钮、长按快捷菜单、排序菜单、食材勾选、分享等，代码里没有。
- 色值/字号/圆角有若干近似值，与 `Color.kt`、`Type.kt`、`Theme.kt` 不完全一致。

需要补一套**和已实现 UI 与动画一模一样**的设计稿，作为设计侧的可视记录，而不是再画一版理想稿。

## 选项

| 选项 | 做法 | 代价 |
|---|---|---|
| A. 只有 Markdown 规格 | 写 `implemented-ui-spec.md` | 看不到界面，无法快速核对视觉 |
| B. HTML 交互原型 + 规格 + 截图 | `design/prototype/ui.html`、`implemented-ui-spec.md`、`design/exports/*.png` | 多一个目录；原型需要随代码更新 |
| C. 只出截图 | 从模拟器截屏放 `design/exports/` | 不能交互，状态/动效无法完整表达 |

## 决定

采用 **B**：

1. 新增 `design/prototype/`，只允许放可交互设计稿（`.html`）与说明（`.md`），不允许放代码、构建配置、图片批次。
2. 原型 `design/prototype/ui.html` 单文件、无外部依赖，覆盖手机/平板、浅色/深色、全部已实现路由与状态。
3. 新增 `design/product/implemented-ui-spec.md`，逐屏记录真实尺寸、状态、交互、动画与代码位置，并列出“规划中、未实现”的条目。
4. 从原型导出关键屏截图到 `design/exports/`，作为静态留痕。
5. 明确优先级：**代码是唯一事实来源**。设计稿与代码冲突时以代码为准；改 UI 必须同时更新代码、`implemented-ui-spec.md`、`design/prototype/ui.html`。
6. 原型只用代码里真实存在的动效：NavHost 默认 700ms 淡入淡出、HorizontalPager、Material 3 ripple/switch/progress；不添加按压缩放、位移、呼吸、发光等未实现效果。

## 后果

- 正面：设计侧第一次有与实现一致的可视稿；状态与动效可以逐屏核对；文字规格与代码的偏差被显式记录。
- 负面：原型是派生品，代码变更后需要同步维护；HTML 不能替代真机截图，字体渲染会因平台略有差异。
- 触发重审：原型维护成本超过收益，改为直接从真机自动截图；或设计稿需要承载多版本时，再引入版本目录。
