---
doc: docs/research/2026-11-04-douyin-import-probe
title: 抖音分享页 / 登录页接口实测（2026-11-04）
status: accepted
date: 2026-11-04
owner: wang
related:
  - docs/decisions/0011-douyin-share-page-sniffing
  - android/docs/import-pipeline
---

# 抖音分享页 / 登录页接口实测（2026-11-04）

> 环境：macOS + curl + Playwright Chromium（非中国 IP）。所有请求都由用户主动提交的单条分享链接触发，不做批量、不遍历。
> 签名、`share_sign`、`msToken`、cookie 值一律不记录；只记录结构、状态码与字段名。

## 1. 短链与分享页

样本（用户提供的分享文案）：

```
https://v.douyin.com/UKunH4a_CNY/
```

- 移动 UA GET：`302` → `https://www.iesdouyin.com/share/video/7703148118140800266/?region=CN&mid=...&did=...&iid=...&share_sign=...&ts=...`
- 分享页 `200 / 35KB`（带签名参数）；去掉参数后 `200 / 32.5KB`。
- `window._ROUTER_DATA` 的 `loaderData["video_(id)/page"]` 只剩：
  `itemId`、`renderInSSR`、`serverToken`、`abParams`、`commonContext` 等；
  **没有 `video.play_addr`、没有 `url_list`、没有 `bit_rate`**。
- 分享页 JS 会调用 `/web/api/v2/aweme/iteminfo/`，参数含 `item_ids`、`aweme_type`、`reflow_id`、`share_scene`、`share_token`、`scene_from`，由页面安全 SDK 签名。
- 2026-11-04 在非中国 IP 的无头 Chromium 里，带签名与不带签名的分享页都渲染「抱歉出错了 / 请尝试在抖音内观看」，没有 `<video>`、没有 iteminfo 请求。**这是地区/风控限制，不能据此判定中国区真机行为**。

## 2. 匿名接口

| 请求 | 结果 |
|---|---|
| `GET https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids={id}` | `200`，0 字节 |
| `GET https://www.douyin.com/aweme/v1/web/aweme/detail/?...&aweme_id={id}` | `403 Blocked by ArgusSecurityPlugin Uifid Not Found` |
| `GET https://www.douyin.com/video/{id}`（桌面 Chrome UA） | `200`，7 万字节 JS 反爬壳，无正文、无视频地址 |
| 同上（Googlebot / Baiduspider UA） | `200`，约 100KB SEO 页：标题、作者、封面、正文、评论；**无 `play_addr`、无 `.mp4`、无 `douyinvod`** |

结论：匿名直连拿不到播放地址；只能在 WebView 里让分享页自己发签名请求，应用被动读取响应/资源/播放器状态。

## 3. 登录页：移动版 vs 桌面版

`https://www.douyin.com/login_page?service=https%3A%2F%2Fwww.douyin.com%2F`

| UA | 渲染出的登录方式 |
|---|---|
| Android 移动 UA（390px） | 只有「验证码登录 / 密码登录」 |
| 桌面 Chrome UA（1280px） | 「扫码登录」+ 二维码 + 「验证码登录 / 密码登录」 |

桌面版在不同视口下的二维码：

| 视口 | `#douyin_login_comp_scan_code img` |
|---|---|
| 390 | 0 个 |
| 768 | 0 个 |
| 900 | 1 个，`src` 为 `data:image/png;base64,...`，长度约 1070 |
| 1024 / 1280 | 1 个，同上 |

二维码元素结构：

```html
<div id="douyin_login_comp_scan_code">
  <div id="animate_qrcode_container">
    <img aria-label="二维码" src="data:image/png;base64,...">
  </div>
</div>
```

页面自身还会请求：

- `GET /passport/web/get_qrcode/`：返回 `qrcode`（base64 PNG，约 128×128）、`token`、`expire_time`、`qrcode_index_url`；
- `POST /passport/web/check_qrconnect/`：轮询扫码状态，参数含 `a_bogus`、`msToken`、`fp`、`verifyFp`、`p_no` 等，由页面 SDK 生成。

推论：

1. 扫码入口只在桌面版，登录页 UA 必须与解析 UA 分开。
2. 二维码原图可以直接从 DOM 取，不需要截整屏；二维码是否渲染取决于**视口宽度 ≥ 900**，所以隐藏 WebView 必须给一个 ≥ 900px 的布局宽度（实现用 1024dp + `requiredWidth`）。
3. 轮询接口有签名，应用不重放；让页面自己轮询、应用只读 `CookieManager`。

## 4. 对实现的约束

- 分享页 URL 必须保留短链 302 的签名参数；干净 URL 只作兜底。
- WebView 嗅探至少要有三路：XHR/fetch 响应体、`performance` 资源、`video.currentSrc`；并先静默 `play()` 触发 media 请求。
- 视频直链只认 CDN 直连（`douyinvod` / `byteicdn` / `video/tos` / `.mp4`）；`/aweme/v1/play/` 是签名跳转口，不能当直链。
- 下载必须带 `Referer` / `Origin`（抖音 CDN 缺了会 403）。
- 分享页错误页要能识别并切 `www.douyin.com/{video|note}/{id}` 再试一次。
- 扫码登录：桌面版 UA + 宽视口 + DOM 二维码 + 保存原图到相册；不要引导短信验证码。

## 5. 未验证 / 下次真机要看

- 中国区真机上分享页是否能正常播放并返回 iteminfo；非中国 IP 的「抱歉出错了」是否只是地区限制。
- `share_sign` 的有效期与失败后的旧路径成功率。
- 抖音扫一扫能否识别我们放大 4 倍后的二维码（预期可以，需真机确认）。
- 扫码登录是否确实不顶掉抖音 App 的登录态（设计推断，需真机确认）。
