---
doc: docs/research/2026-10-30-tech-research
title: 技术方案调研报告（抖音 / 小红书 / B站）
status: accepted
date: 2026-10-30
owner: wang
related:
  - docs/decisions/0003-media-import
  - android/docs/import-pipeline
---

# 技术方案调研报告

> 目标：在 Android 端侧解析抖音、小红书、B站的分享链接，下载视频/图片，优先取无水印源，并解析文字。
> 方法：平台页面实测（2026-10-30，host 与 Pixel 7 模拟器）、GitHub 仓库与源码核查、真实链接回归。
> 结论先行：**自写 Kotlin 提取器 + WebView 页面内签名 + JSON-LD 兜底**；不内嵌 Python、不破解签名、不做 AI 抹水印。

## 1. 三平台实测矩阵

| 平台 | 内容 | 链接 | 可用路径 | 无水印 | 登录 | 本机结果 |
|---|---|---|---|---|---|---|
| 抖音 | 视频 | `v.douyin.com/{code}` → `iesdouyin` 19 位 ID | WebView 主站 + 页面自身 detail；JSON-LD 兜底 | `play_addr` 无水印；`download_addr` 带抖音号水印 | 匿名 detail 常 200/0 字节，视频需登录 | 标题/作者/封面/食材成功；视频需登录 |
| 抖音 | 图文 | `/share/note/{id}` | JSON-LD `article` | `images[].url_list` 无水印；`download_url_list` 带水印 | 不需要 | 4 张原图 + 正文成功 |
| 小红书 | 图文/视频 | `xhslink.cn` → `xiaohongshu.com/discovery/item/{id}?xsec_token=...` | 页面 `__INITIAL_STATE__` | 图片 `https://ci.xiaohongshu.com/{fileId}` 原图；视频 `media.stream.h264[0].masterUrl` | 多数公开笔记不需要；风控码 300031/300015 需登录 | 图文 5 张原图成功；视频 19.6 MB 成功 |
| B站 | 视频 | `b23.tv` → `BV...` | `x/web-interface/view` + `x/player/playurl` | 平台通常无水印 | 720p 不需要 | 720p 单文件 36 MB 成功 |

## 2. 关键字段与 URL 规则

### 2.1 抖音

- 短链 302 一次即可拿到 19 位 `aweme_id`；`share_sign` 不是长期凭证。
- 详情接口：`https://www.douyin.com/aweme/v1/web/aweme/detail/?aweme_id=...`
  - 服务端校验 `a_bogus` / `msToken` / `uifid` / `x-secsdk-web-signature` / `x-tt-argus`；
  - 匿名直连实测 403 `Blocked by ArgusSecurityPlugin`；WebView 页面自身请求也可能返回 200/0 字节。
- **无水印判定**：`video.play_addr`（实测 1080×1920，无 logo）vs `video.download_addr`（实测 720×1280，右下角抖音号水印）；图集 `images[].url_list` vs `download_url_list`。`has_watermark` 字段不可信。
- CDN 需要 `Referer: https://www.douyin.com/` + 浏览器 UA，支持 Range，URL 约 3 小时过期。
- `/aweme/v1/play/?video_id={play_addr.uri}&improve_bitrate=1` 可拿更高码率，但仍需 `uri`。
- JSON-LD 兜底：`Googlebot` UA 打开 `www.douyin.com/{video|note}/{id}`：
  - 视频 `VideoObject.name` 常包含完整食材（如“食材：牛里脊 400克、土豆 300克…”），`description`、`thumbnailUrl`、`duration`、`creator`；
  - 图文 `article.articleBody` + `image[]`，可直接生成菜谱。
  - 这是单条、用户主动的降级通道，不做批量。

### 2.2 小红书

- `xhslink.cn` / `xhslink.com` 短链会过期；解析到最终 URL 时必须保留 `xsec_token`。
- 笔记页 `window.__INITIAL_STATE__` 中 `noteData.data.noteData`：
  - `title`、`desc`、`user.nickName`、`tagList[].name`；
  - `imageList[].fileId` → `https://ci.xiaohongshu.com/{fileId}` 为无水印原图（已下载目视确认）；
  - `video.media.stream.h264[0].masterUrl` 为视频流，`backupUrls` 备用；HTTP → HTTPS 后可用。
- 风控：匿名可能返回登录页/300031/300015；需要用户在应用内 WebView 登录一次，Cookie 加密保存在本机。

### 2.3 B站

- `b23.tv` → `BV...`；`x/web-interface/view` 提供标题、UP 主、封面、时长、cid。
- `x/player/playurl?qn=64&fnval=1` 返回 `durl` 单文件 mp4（720p，音视频已封装），无需合并音视频。
- **CDN 对移动 UA 返回 403，对桌面 UA 返回 206**（2026-10-30 实测）；API 与媒体请求统一使用桌面 UA + `Referer: https://www.bilibili.com/`。
- 更高清晰度需要登录/大会员；本项目只取匿名 720p，不碰付费内容。

## 3. GitHub 方案对比（采集于 2026-10-30）

| 项目 | 平台 | 语言/许可 | 做法 | 结论 |
|---|---|---|---|---|
| `Johnserf-Seed/f2` | 抖音 | Python / Apache-2.0 | 纯 Python 本地签名（a_bogus/X-Bogus） | 最佳算法与端点参考 |
| `Evil0ctal/Douyin_TikTok_Download_API` | 抖音/TikTok | Python / Apache-2.0 | 本地签名 + 端点表 + 风控记录 | 端点与净流字段参考 |
| `JoeanAmier/TikTokDownloader` | 抖音 | Python / GPL-3.0 | 终端 + WebAPI，签名需用户自备 | 流程参考，代码不可拷入 |
| `nilaoda/BBDown` | B站 | C# / MIT | playurl + DASH 合并 | B站合并策略参考 |
| `iawia002/lux` | 多平台 | Go / MIT | 内嵌混淆 sign.js | 抖音支持已落后 |
| `yt-dlp/yt-dlp` | 多平台 | Python / Unlicense | 抖音无签名，依赖新鲜 Cookie | 不作为主方案 |
| `soimort/you-get` | 多平台 | Python | 旧 iteminfo 接口 | 抖音已失效 |
| `NanmiCoder/MediaCrawler` | 多平台 | Python / 非商业许可 | Playwright 浏览器自动化 | 许可与 Android 均不合适 |
| `DYDownloader` / `dyparse` | 抖音 | Java/Kotlin / GPL-3.0 | Android 原生下载器 | 只看架构，不拷代码 |

**选型结论**：

1. 不内嵌 Python（Chaquopy）、不跑伴生服务；包体、启动、维护都不划算。
2. 自写 Kotlin 提取器；B站与小红书用公开页面/接口；抖音用 WebView 让页面自己签名，应用只读结果。
3. 抖音增加 JSON-LD 兜底，保证图文可用、视频至少拿到标题/封面/食材。
4. 不实现 a_bogus/Argus 签名破解；平台改版时适配器失效是可接受的降级。

## 4. 与产品决策的对应

- [`ADR 0003`](../decisions/0003-media-import.md)：做端侧解析与下载、优先无水印源、不做 AI 抹水印。
- 本报告的 JSON-LD 兜底是 0003 的补充：**匿名、单条、用户主动**，不改变"不批量、不破解"的红线。
- 媒体存储与命名见 [`android/docs/data-storage.md`](../../android/docs/data-storage.md)。

## 5. 未验证与风险

- 抖音匿名视频在部分网络/账号下可能成功；本机未登录时稳定复现 200/0 字节。登录后的成功率需真实账号验证。
- 小红书 `xsec_token` 过期、风控策略变化会导致解析失败；登录态可缓解但不保证。
- B站桌面 UA 是当前必需条件，平台可能调整。
- JSON-LD 依赖爬虫 UA，属于 ToS 灰区；仅用于单条用户主动导入，平台收紧即失效。
- 所有媒体 URL 有时效；下载必须"解析后立即下载"，不能缓存 URL 跨天使用。

## 6. 参考

- 真实测试链接与预期结果：[`android/docs/test-data.md`](../../android/docs/test-data.md)
- 收录管线设计：[`android/docs/import-pipeline.md`](../../android/docs/import-pipeline.md)
- GitHub 仓库：`github.com/Johnserf-Seed/f2`、`github.com/Evil0ctal/Douyin_TikTok_Download_API`、`github.com/nilaoda/BBDown`、`github.com/iawia002/lux`、`github.com/yt-dlp/yt-dlp`
