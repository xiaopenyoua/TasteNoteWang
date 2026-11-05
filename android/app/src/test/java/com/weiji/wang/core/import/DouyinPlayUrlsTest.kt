package com.weiji.wang.core.import

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 2026-11-05 模拟器实测抓到的真实跳转口：
 * `https://m.douyin.com/aweme/v1/playwm/?video_id=v2700fgi0000dblf92fog65v0ma1chq0&ratio=720p&line=0`
 * 改写后 `https://www.douyin.com/aweme/v1/play/…` 302 到 douyinvod，下载得到 720x1280 H.264。
 */
class DouyinPlayUrlsTest {

    private val captured =
        "https://m.douyin.com/aweme/v1/playwm/?video_id=v2700fgi0000dblf92fog65v0ma1chq0&ratio=720p&line=0"

    @Test
    fun `playwm becomes no watermark play on www host`() {
        assertEquals(
            "https://www.douyin.com/aweme/v1/play/?video_id=v2700fgi0000dblf92fog65v0ma1chq0&ratio=720p&line=0",
            DouyinPlayUrls.noWatermark(captured),
        )
    }

    @Test
    fun `iesdouyin share host is normalized too`() {
        assertEquals(
            "https://www.douyin.com/aweme/v1/play/?video_id=abc&ratio=720p&line=0",
            DouyinPlayUrls.noWatermark(
                "https://www.iesdouyin.com/aweme/v1/play/?video_id=abc&ratio=720p&line=0",
            ),
        )
    }

    @Test
    fun `already canonical play url is unchanged`() {
        val url = "https://www.douyin.com/aweme/v1/play/?video_id=abc&ratio=1080p&line=0"
        assertEquals(url, DouyinPlayUrls.noWatermark(url))
    }

    @Test
    fun `video id is read from the query`() {
        assertEquals("v2700fgi0000dblf92fog65v0ma1chq0", DouyinPlayUrls.videoId(captured))
        assertNull(DouyinPlayUrls.videoId("https://www.douyin.com/video/7704239147967475810"))
    }

    @Test
    fun `video id builds a playable redirect`() {
        assertEquals(
            "https://www.douyin.com/aweme/v1/play/?video_id=abc&ratio=720p&line=0",
            DouyinPlayUrls.fromVideoId("abc"),
        )
    }

    @Test
    fun `only cdn hosts count as downloadable`() {
        assertTrue(DouyinPlayUrls.isCdn("https://v13-cold.douyinvod.com/x/video/tos/cn/y/?a=1128"))
        assertTrue(DouyinPlayUrls.isCdn("https://v3.byteicdn.com/x.mp4"))
        assertFalse(DouyinPlayUrls.isCdn("https://www.douyin.com/aweme/v1/play/?video_id=abc"))
    }
}
