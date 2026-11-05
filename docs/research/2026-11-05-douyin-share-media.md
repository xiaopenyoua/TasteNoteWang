---
doc: docs/research/2026-11-05-douyin-share-media
title: 抖音分享链接的视频与图文下载（2026-11-05 快照）
status: accepted
date: 2026-11-05
owner: wang
---

# 抖音分享链接的视频与图文下载

> 目标：两条真实分享链接，视频要拿到可播放的 mp4，图文要拿到原图；在 Android 模拟器上验收。
> 本文是 2026-11-05 当天的事实快照，只增不改；结论落到 `android/docs/import-pipeline.md` §4.3。

## 1. 结论

1. **不需要伪造 `a_bogus`。** 分享页播放器自己会发 `…/aweme/v1/playwm/?video_id=…`，它是 302 跳转口；
   跟随重定向就得到 `douyinvod.com` 的 CDN 直链。把 `playwm` 换成 `play`、`m.douyin.com` 换成
   `www.douyin.com`，同一条 `video_id` 返回无水印版本。
2. **JSON 里没有 `play_addr`。** 2026-11-05 的 reflow 接口
   `/aweme/v1/web/aweme/detail/` 只返回 `aweme_list[0].video.cover/duration/bit_rate:null`，
   所以任何"解析 JSON 取 play_addr"的实现都会退化成只保存封面。
3. **图文不需要 WebView。** `www.douyin.com/note/{id}` 的 `application/ld+json` `article.image[]`
   直接给出 17 张原图；测试链接一次导入 17 张步骤图 + 1 张封面。
4. **`playwm` 只作为跳转口。** 直接把它当最终文件下载会拿到带水印版本；先 302 再下载、并优先
   `play`，才是无水印。

## 2. GitHub 方案调查

### 2.1 Evil0ctal / Douyin_TikTok_Download_API

- 仓库：<https://github.com/Evil0ctal/Douyin_TikTok_Download_API>
- 快照：21,158 stars，`pushed_at` 2026-11-02T07:43:14Z（GitHub API，2026-11-05 读取）。
- 关键文件：
  - `src/dtk/platforms/douyin/endpoints.py`：`POST_DETAIL = https://www.douyin.com/aweme/v1/web/aweme/detail/`
  - `src/dtk/platforms/douyin/params.py`：`content_detail_params()` 只加 `aweme_id`，其余是
    `device_platform=webapp&aid=6383&channel=channel_pc_web&version_code=290100…` 的浏览器指纹；
    注释明确 `msToken`、`a_bogus`、`X-Bogus` 由签名层追加。
  - `src/dtk/signing/native/abogus.py`：纯 Python 的 A-Bogus 生成器（SM3 + 自定义 RC4 + 50 个字段），
    文件头说明从 `bdms.js` 逆出；另有 `sm3.py`。
- 现场验证：用该实现按 `content_detail_params()` 拼 query、生成 `a_bogus`，请求
  `https://www.douyin.com/aweme/v1/web/aweme/detail/?…&a_bogus=…`，返回
  **403 `Blocked by ArgusSecurityPlugin Uifid Not Found`**。签名本身结构校验通过，缺的是浏览器身份
  （UIFID/msToken/Cookie 池）。
- 判断：在 Android 本地复刻 35KB 的签名算法 + 身份池，维护成本高、随 `bdms.js` 更新即失效；
  对一个"用户主动分享一条链接"的菜谱应用不划算。

### 2.2 分享页自己的播放请求（采用）

- 分享页 `iesdouyin.com/share/video/{id}` 的 `_ROUTER_DATA` 只剩 `itemId` 与 AB 参数，
  页面由 React 客户端渲染，播放器请求：
  `https://m.douyin.com/aweme/v1/playwm/?video_id=v2700fgi0000dblf92fog65v0ma1chq0&ratio=720p&line=0`
- `curl -I` 结果（2026-11-05）：
  - `playwm` → `302`，`location: https://v13-cold.douyinvod.com/.../video/tos/cn/...`
  - `www.douyin.com/aweme/v1/play/` → `302`，同 CDN，无水印
  - `m.douyin.com/aweme/v1/play/`、`ratio=1080p` → `404`（回退 720p）
- 因此 Android 侧只需 WebView 被动捕获 `play(wm)` 请求（或 DOM `video.currentSrc`），
  再用 OkHttp 跟随 302；不需要 `a_bogus`、不需要 Cookie 池、不需要替换页面响应。

## 3. 现场证据

两条用户提供的分享文案，均在 `emulator-5558`（Android 16，`sdk_google_atv64_amati_arm64`）实测：

| 链接 | 结果 | 落盘 |
|---|---|---|
| `https://v.douyin.com/7ESUV_w26M0/`（视频，恐怖上演） | 标题「恐怖上演」；VIDEO + COVER | `video.mp4` 5,805,777 B；`ffprobe`：h264 720×1280、10.634739 s、4.37 Mbps；`cover.webp` 42,064 B |
| `https://v.douyin.com/wrCuEvuKwVk/`（图文，长城汽车哈弗猛龙） | 标题「长城汽车哈弗猛龙HEV/PHEV。」；17 STEP + COVER | `step-01.webp`…`step-17.webp`，`cover.webp` |

数据库（Room `media` 表）：

- 视频：`VIDEO=1, COVER=1`
- 图文：`STEP=17, COVER=1`

复现命令：`bash android/tools/douyin-e2e.sh`（见 `android/docs/import-pipeline.md` §4.3）。

## 4. 已知限制

- 非中国区网络/风控会让分享页渲染「抱歉出错了」；此时仍可走 `www.douyin.com` 的 JSON-LD 拿图文，
  视频则需要页面能加载播放器。本次测试网络可正常加载分享页。
- `video_id` 来自播放器请求；如果页面在无播放器的情况下只渲染静态封面，仍会退回部分完成。
- CDN 直链带 `x-expires`，必须解析后立即下载；过期需重新解析。
- 本次未验证：登录态、私密作品、直播、合集。

## 5. 参考

- <https://github.com/Evil0ctal/Douyin_TikTok_Download_API>
- <https://github.com/Evil0ctal/Douyin_TikTok_Download_API/blob/main/src/dtk/platforms/douyin/endpoints.py>
- <https://github.com/Evil0ctal/Douyin_TikTok_Download_API/blob/main/src/dtk/signing/native/abogus.py>
- 实现：`android/app/src/main/java/com/weiji/wang/core/import/DouyinPlayUrls.kt`、
  `android/app/src/main/java/com/weiji/wang/feature/capture/CaptureScreen.kt`
