---
doc: design/README
title: 设计区索引
status: accepted
date: 2026-11-02
owner: wang
related:
  - AGENTS
  - docs/decisions/0008-dir-contract
---

# design/ — 设计侧

这里放**产品怎么呈现**：产品规格、页面规格、视觉语言、线框与视觉稿。
归属判据不在本文件，在仓库根的 [`AGENTS.md`](../AGENTS.md)；两者冲突时以 `AGENTS.md` 为准。

## 四个并列的段

```
design/
  product/       产品与界面规格：PRD、信息架构、页面规格、视觉语言
  prototype/     可交互设计稿：与已实现 UI/动画逐屏对齐的单文件 HTML
  wireframes/    低保真线框
  exports/       视觉稿导出与切图
  source/        设计源文件（Figma/Sketch/PSD，gitignore）
```

| 段 | 回答 | 只进 | 永远不进 |
|---|---|---|---|
| `product/` | 做给谁、有哪些页面、每个页面怎么交互 | 语义名 `.md` | 实现细节、代码、调研正文 |
| `prototype/` | 已实现界面（`ui.html`）与待确认提案（`ui-proposal.html`） | 自包含 `.html`、说明 `.md` | 代码工程、构建配置 |
| `wireframes/` | 布局骨架长什么样 | `.png` / `.jpg` / `.pdf` | 高保真稿、源文件 |
| `exports/` | 定稿视觉长什么样 | 原型导出的 `phone-*.png` / `proposal-*.png` 及后续切图 | `res/` 字节副本、未引用批次图 |
| `source/` | 稿子的可编辑源 | Figma / Sketch / PSD | 任何需要入库的内容 |

## 命名

1. 全小写 ASCII，kebab-case，语义名：`screen-specs.md`、`visual-language.md`。
2. **不写编号前缀**（编号只属于 ADR 与日期快照，见 `AGENTS.md` R4）。
3. **不写 `v2` `final` `new` `old`**（R5）。
4. 图片用 `<屏名>.png` 或 `<用途>-<尺寸>.png`，不用 `01-` 排序。

## 阅读顺序

1. [`product/prd.md`](product/prd.md) —— 产品要解决什么、边界在哪。
2. [`product/information-architecture.md`](product/information-architecture.md) —— 有哪些页面、怎么走。
3. [`product/screen-specs.md`](product/screen-specs.md) —— 每屏的元素、状态、文案。
4. [`product/visual-language.md`](product/visual-language.md) —— 颜色、字阶、形状、间距。
5. [`prototype/ui.html`](prototype/ui.html) —— 已实现界面与动效的可交互设计稿。
6. [`product/implemented-ui-spec.md`](product/implemented-ui-spec.md) —— 设计稿与代码的逐屏对照。
7. [`product/ui-change-proposal.md`](product/ui-change-proposal.md) —— 待确认的 UI 变更提案（详情/编辑/媒体/备注/登录/采购合并与编辑/每日菜单历史/搜索/存储/数据迁移）。
8. [`prototype/ui-proposal.html`](prototype/ui-proposal.html) —— 提案的可视稿。

## 与开发区的边界

- 本区**不放** `.kt`、`.kts`、`build.gradle*`、`res/` 形状目录。
- 代码在 [`android/app/`](../android/app/)；本区只描述它，不复制它。
- 实现文档在 [`android/docs/`](../android/docs/)，不在本区。
- 依据（调研、ADR）在 [`docs/`](../docs/)，不在本区。
