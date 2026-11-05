package com.weiji.wang.core.import

/**
 * 抖音播放跳转口的纯函数部分，方便单测；真正的 302 跟随在 WebView 探针里做。
 *
 * 2026-11-05 实测（两台分享页，视频/图文各一）：
 * - 分享页 reflow 接口 `aweme/v1/web/aweme/detail/` 只回 `video.cover/duration/bit_rate:null`，
 *   **没有 `play_addr`**，所以 JSON 解析永远拿不到视频直链；
 * - 页面播放器实际请求 `…/aweme/v1/playwm/?video_id=…`，302 到 `douyinvod.com`；
 * - 把 `playwm` 换成 `play`、`m.douyin.com` 换成 `www.douyin.com`，同样 302，但拿到的是无水印版本。
 *
 * 这里只做地址改写；网络请求留在调用方（WebView 探针）。
 */
object DouyinPlayUrls {

    private val videoIdRegex = Regex("""[?&]video_id=([^&]+)""")

    /** 从 play/playwm 跳转口里取 `video_id`；没有就返回 null。 */
    fun videoId(url: String): String? =
        videoIdRegex.find(url)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }

    /**
     * 把播放器请求的跳转口改写成无水印、可跟随的地址。
     * 已经是 `www.douyin.com/aweme/v1/play/` 的原样返回。
     */
    fun noWatermark(url: String): String {
        val play = url.replace("/playwm/", "/play/")
        return play
            .replace("://m.douyin.com/", "://www.douyin.com/")
            .replace("://www.iesdouyin.com/", "://www.douyin.com/")
    }

    /** 只拿到 `video_id` 时构造无水印跳转口；分享页的 `ratio` 默认 720p 最稳。 */
    fun fromVideoId(videoId: String, ratio: String = "720p"): String =
        "https://www.douyin.com/aweme/v1/play/?video_id=$videoId&ratio=$ratio&line=0"

    /** 最终 302 落点是否是可下载的 CDN 直链。 */
    fun isCdn(url: String): Boolean =
        listOf("douyinvod", "byteicdn", "video/tos")
            .any { url.contains(it, ignoreCase = true) }
}
