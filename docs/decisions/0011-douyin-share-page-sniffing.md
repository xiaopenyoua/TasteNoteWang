---
doc: docs/decisions/0011-douyin-share-page-sniffing
title: 抖音收录改为移动分享页 WebView 三路嗅探，扫码登录走桌面版原生二维码
status: accepted
date: 2026-11-04
owner: wang
supersedes:
  - docs/decisions/0006-douyin-fallback
superseded-by: []
---

# 0011. 抖音收录与登录的第二次修正

## 背景

0006 的方案是「JSON-LD 兜底 + WebView 加载主站 `www.douyin.com/video/{id}`，注入钩子读页面自己签名的 detail 响应，15 秒超时」。2026-11-04 用真实分享链接与接口复测，发现三个前提已经变了：

1. **分享页不再内联播放地址**。`v.douyin.com` 302 到 `www.iesdouyin.com/share/video/{id}/?...`；页面的 `window._ROUTER_DATA` 只剩 `itemId` 与 AB 参数，没有 `video.play_addr`。播放地址由页面自己的安全 SDK 请求 `/web/api/v2/aweme/iteminfo/` 后渲染。
2. **匿名直连仍然不可行**。OkHttp 请求 iteminfo 返回 200/0 字节；`/aweme/v1/web/aweme/detail/` 返回 `403 Blocked by ArgusSecurityPlugin Uifid Not Found`；桌面主站是 JS 反爬壳；Googlebot/Baiduspider 的 SEO 页只有标题、封面、正文，没有直链。
3. **主站 WebView 不是最佳入口**。移动 UA 打开 `www.douyin.com/video/{id}` 常常只拿到 SEO 封面；分享页才是为匿名浏览设计的移动页面。旧实现 15 秒超时也短于分享页 detail 请求的实际耗时。

登录侧同样有一次实测：抖音移动版登录页只有验证码/密码，**扫码入口只在桌面版**；桌面登录页在 900px 以上视口才渲染二维码 `<img>`（390/768 宽不渲染）。用短信验证码会新建手机设备会话，可能二次扫码并让抖音 App 掉线；扫码登录是 Web 会话，不会顶掉 App。

## 决定

采用「移动分享页优先 + 三路嗅探 + 错误页切旧路径 + 桌面扫码登录」：

1. **短链解析**：先跟随 `v.douyin.com` 到带 `did/iid/share_sign/ts` 的分享页。`webViewUrl` 优先原样使用这个签名地址；没有再构造干净的 `share/video/{id}/` 或 `share/slides/{id}/`。
2. **WebView**：加载分享页（移动 UA），在 document-start 注入 XHR/fetch 钩子；每 2.5 秒探针一次，同时读：
   - 钩子缓存的 iteminfo/detail 响应体（`item_list` 或 `aweme_detail`）；
   - `performance.getEntriesByType('resource')` 里的直连 CDN（`douyinvod` / `byteicdn` / `video/tos` / `.mp4`）；
   - DOM `<video>.currentSrc`。
   探针先 `muted + play()` 静默触发一次 media 请求，再读 resource timing。
3. **解析**：`DouyinJsonParser` 递归解包 `item_list` / `aweme_detail` / 二次包裹；ID 必须与链接一致；视频只认 CDN 直链，不把 `/aweme/v1/play/` 跳转口当候选；只有直链没有 JSON 时用 SEO 标题/作者/封面合成 `VIDEO`。
4. **兜底**：分享页渲染出「抱歉出错了 / 请尝试在抖音内观看 / 作品不存在」时，自动改载一次 `www.douyin.com/{video|note}/{id}`；两轮都拿不到 `VIDEO` 才在 45 秒后保存 `PARTIAL`（标题、封面、文字），文案给「重试 / 去登录扫码 / 只保留链接」。
5. **下载**：视频请求补 `Referer: https://www.douyin.com/`、`Origin`、`Accept`；`RemoteMedia.headers` 覆盖默认头；403/404/416/5xx 与「200 但是 HTML / 0 字节」都给可执行的原因。
6. **登录**：抖音登录页 UA 切桌面版，在 1024dp 宽的不可见 WebView 里跑（900px 以上才有二维码），从 DOM 取 `data:image/png;base64` 二维码原图，用原生 240dp 面板展示；保存到相册时放大 4 倍、只存二维码本身。扫码不顶掉抖音 App；短信验证码路径降级为「改用网页登录」。
7. **不做的**：不破解 `a_bogus`/Argus，不内嵌 Python/第三方解析服务，不替换页面响应，不批量抓取。

## 后果

- 正面：视频直链由分享页自己签名，应用不碰签名；只有直链时也能合成 `VIDEO`；图文与失败场景都有可读结果；扫码登录同一台手机可完成，且不会让抖音 App 掉线。
- 负面：匿名成功率仍取决于分享页风控与地区；`share_sign` 有时效，解析慢会失效并走旧路径兜底（大概率只剩 SEO 内容）；45 秒探针窗口对慢网络偏紧；1024dp 的隐藏 WebView 增加一点内存。
- 触发重审：分享页也拿不到直链且旧路径同样失败成为常态；或抖音提供官方开放接口；或桌面登录页不再提供二维码。

## 验证

- 2026-11-04 接口实测记录见 [`../research/2026-11-04-douyin-import-probe.md`](../research/2026-11-04-douyin-import-probe.md)。
- 单测：`DouyinJsonParserTest` 13 条（item_list、bit_rate、图文、ID 不匹配、二次包裹、直链合成、非视频 URL）。
- 编译：`:app:compileDebugKotlin`、`:app:assembleDebug` 通过；`:app:testDebugUnitTest --tests "com.weiji.wang.core.import.*"` 44/44 通过。
