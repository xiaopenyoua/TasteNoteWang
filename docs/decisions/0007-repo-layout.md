---
doc: docs/decisions/0007-repo-layout
title: 仓库顶层按"设计区 / 开发区 / 交付区 / 本地私有区"四分
status: superseded
date: 2026-11-02
owner: wang
supersedes: []
superseded-by:
  - docs/decisions/0008-dir-contract
---

# 0007. 仓库顶层布局：设计与开发分开

> 已被 [0008](0008-dir-contract.md) 取代。本文是 2026-11-02 当时的分区方案，正文路径按当时可用来读。

## 背景

MVP 完成后仓库根目录同时出现：`docs/`、空的 `design/`、`android/app/`、`dist/`、`dyres/`（平台抓包 JSON）、Gradle 构建产物与工具日志。问题是：

1. `docs/` 与 `design/` 语义重叠，新内容不知道放哪。
2. 抓包、调试 JSON 直接堆在根目录，看起来像源码的一部分。
3. "设计"（调研/需求/架构/测试方案）与"开发"（可编译代码）没有一条一眼可见的边界。
4. `.gitignore` 有重复项，且漏掉根目录 `local.properties` 与本地私有目录。

## 选项

| 选项 | 做法 | 代价 |
|---|---|---|
| A. 保持 `docs/`，只清理杂项 | 改动最小 | 设计与开发仍混在根目录，问题 3 未解决 |
| B. `docs/` 改名 `design/`，并把视觉资产纳入同一区 | 设计区自洽；doc id 不变（仍相对设计区根） | 需要改 README/CLAUDE/少量文档内路径 |
| C. 再把 Gradle 工程移入 `dev/` | 两区完全对称 | 所有构建命令、IDE 打开路径、`local.properties` 全变；收益低 |

## 决定

采用 **B**，并把仓库顶层固定为四个区：

- `design/` —— 设计区：调研、产品、架构、决策、测试方案、视觉资产。
- `android/app/` + 根目录 Gradle 文件 —— 开发区：可编译代码（Gradle 根保持在仓库根，遵循 Android 平台约定）。
- `dist/` —— 交付区：试用 APK 与安装说明（APK 不入库）。
- `local/` —— 本地私有区：抓包、调试 JSON、临时导出（整体 gitignore）。

同时：

1. `docs/` 整体迁入 `design/`，文档 ID（`30-tech/01-architecture` 等）保持不变，只把引用里的 `docs/` 前缀改为 `design/`。
2. 根目录 `dyres/` 的抖音抓包移入 `local/captures/douyin/`，不再混在源码层。
3. 空的顶层 `design/` 占位取消，视觉资产归入 `design/`，源文件归入 `design/source/`。
4. 重写 `.gitignore`：按"构建产物 / 本地私有 / 工具产物 / 用户数据"分组，补 `local.properties`、`local/`，删除重复项。
5. 宪法 `00-conventions/01-file-organization.md` 同步更新，并新增"设计区不得放可编译代码、开发区不得放设计文档"的硬边界。

## 后果

- 正面：根目录只剩 4 个语义明确的区 + 3 个门面文件；新内容有唯一落点；设计与开发边界一眼可见。
- 负面：少量文档与 README 的路径需要一次性迁移；聊天记录里的旧路径（`docs/...`）不再有效。
- 触发重审：出现第二份可独立构建的产物（如服务端、脚本包），需要为它单开开发区；或设计区文档超过约 100 份、需要再分层。
