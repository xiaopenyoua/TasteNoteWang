---
doc: docs/decisions/0006-douyin-fallback
title: 抖音收录：WebView 页面内签名 + JSON-LD 匿名兜底，不破解签名
status: superseded
date: 2026-10-30
owner: wang
supersedes: []
superseded-by:
  - docs/decisions/0011-douyin-share-page-sniffing
---

# 0006. 抖音收录策略

## 背景

抖音 detail 接口受 `a_bogus` / `msToken` / `uifid` / `x-secsdk-web-signature` / `x-tt-argus` 保护；匿名直连 403，WebView 页面自身请求也可能返回 200/0 字节。纯静态 HTML 已不含媒体字段。

## 选项

| 选项 | 能力 | 风险/成本 |
|---|---|---|
| A. 破解签名（Kotlin 重写 a_bogus/Argus） | 成功率最高 | 持续军备竞赛；触碰 ADR 0003 红线 |
| B. 内嵌 Python（Chaquopy）跑 f2/Evil0ctal | 复用成熟实现 | 包体/启动/ABI 成本高；仍依赖签名 |
| C. WebView 让页面自己签名 + 读结果 | 不碰签名；随页面升级 | 匿名可能被风控；渲染进程可能崩 |
| D. JSON-LD 爬虫 UA 兜底 | 图文完整；视频给标题/封面/食材 | ToS 灰区；平台可收紧 |

## 决定

采用 **C + D 组合**：

1. 先请求 JSON-LD（`Googlebot` UA，单条、用户主动）：图文直接导入；视频生成 `PARTIAL`（标题/封面/食材）。
2. 视频再开 WebView（桌面 UA）加载主站，注入 XHR/fetch 钩子，读取页面自身签名的 detail 响应；解析 `play_addr`（无水印）。
3. `shouldInterceptRequest` 只做后台重放，**不替换页面响应**，避免破坏页面加载。
4. 15 秒无结果 / 渲染崩溃 / detail 200+0 → 用 JSON-LD 内容保存为 `PARTIAL`，提示登录后重试。
5. 不实现签名破解、不内嵌 Python、不做 AI 抹水印。

## 后果

- 正面：图文匿名可用；视频在登录后有机会完整；任何失败都有可读菜谱。
- 负面：抖音视频在匿名状态下大概率只有部分内容；JSON-LD 属灰区，需保持"单条、不批量"。
- 触发重审：平台提供官方开放接口；或登录后视频成功率仍低于可接受阈值（需真实账号数据）。
