---
doc: docs/decisions/0009-apk-location
title: 取消 dist 交付暂存区，APK 留在 android/app/build
status: accepted
date: 2026-11-02
owner: wang
supersedes: []
superseded-by: []
---

# 0009. 取消 dist 交付暂存区

## 背景

ADR 0008 §6 为「试用 APK 留在项目目录内」设了 `dist/`。实际运行后暴露出三个问题：

1. **重复字节。** `dist/weiji-wang-0.1.0-debug.apk` 与 `android/app/build/outputs/apk/debug/app-debug.apk` 的 SHA-256 完全相同（`f148fff4…`），只是同一份文件的两个名字。
2. **人工同步会过期。** `dist/` 不在 `gradle clean` 的管理范围内，重新构建后很容易留下旧包；要靠人记得覆盖。
3. **多一个顶层目录。** 与「三个内容目录」的契约目标相悖，且 `dist/` 的归属判据只是「用户要求」。

## 选项

| 选项 | 做法 | 代价 |
|---|---|---|
| A. 保留 `dist/`，加同步脚本 | 交付路径稳定 | 多一个需要维护的写通道；仍是第二份字节 |
| B. 取消 `dist/`，直接使用 Gradle 原生产物目录 | 一个字节只有一份；无需同步 | `gradle clean` 会删除 APK |
| C. 取消 `dist/`，APK 放 `local/` | 顶层更干净 | `local/` 语义是「可随时丢失」，不适合作为交付位 |

## 决定

采用 **B**：

1. 删除 `dist/`（含其 `README.md` 与 APK）。
2. 试用包路径固定为 `android/app/build/outputs/apk/debug/app-debug.apk`，不复制到仓库其他位置。
3. 安装说明并入根 `README.md` 的「手机 / 平板试用」小节。
4. 同步修改：`AGENTS.md` 的 §0 / §1 / R1 / R8 / §3 / §5 / §6，以及 `android/tools/verify-layout.sh` 第 1、7 项。
5. `.gitignore` 删除 `dist/*.apk`、`dist/*.aab`；`android/app/build/` 已在忽略列表内。

## 后果

- 正面：一个字节只有一份；不会留下旧包；顶层只剩 `android/`、`design/`、`docs/` 三个内容目录加 `local/`。
- 负面：`gradle clean` 会连 APK 一起清掉，需要保留时先复制到项目外；debug 包不随源码入库。
- 触发重审：需要长期保留多个历史 APK，或开始产出正式签名发布包。
