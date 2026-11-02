package com.weiji.wang.core.import

import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DouyinJsonLdParserTest {

    private fun link(id: String? = "7673495764546424100") = ParsedLink(
        platform = Platform.DOUYIN,
        url = "https://v.douyin.com/auY2beN-dPo/",
        contentId = id,
        shareText = "分享",
        titleHint = null,
        authorHint = null,
    )

    @Test
    fun `video json ld gives title ingredients cover and duration`() {
        val html = """
            <html><head>
            <script type="application/ld+json">
            {"@context":"https://schema.org","@type":"VideoObject",
             "name":"比烧烤还好吃！直接香迷糊的孜然土豆牛肉 食材：牛里脊或牛排肉：400克、土豆：300克、孜然粉：5克 #美食 - 抖音",
             "description":"做法很简单",
             "thumbnailUrl":["https://p3-sign.douyinpic.com/cover.jpeg"],
             "duration":"PT0H0M23S",
             "creator":{"name":"西关焖烧男"}}
            </script></head><body></body></html>
        """.trimIndent()
        val content = DouyinJsonLdParser.parse(html, link(), "video")!!
        assertEquals("比烧烤还好吃！直接香迷糊的孜然土豆牛肉", content.title)
        assertEquals("西关焖烧男", content.author)
        assertTrue(content.textBody.contains("食材"))
        assertTrue(content.textBody.contains("400克"))
        assertTrue(content.textBody.contains("土豆"))
        assertEquals(23_000L, content.durationMs)
        assertEquals(MediaKind.COVER, content.media.first().kind)
        assertTrue(content.requiresWebView)
        assertEquals("https://p3-sign.douyinpic.com/cover.jpeg", content.coverUrl)
    }

    @Test
    fun `article json ld gives body and original images`() {
        val html = """
            <html><head>
            <script type="application/ld+json">
            {"@context":"https://schema.org","@type":"article",
             "headline":"想吃谁做的，就@他吧！",
             "articleBody":"今日菜单：孜然土豆炒肉、茄汁豆腐。\n第一步：切土豆。",
             "description":"20 元一桌",
             "image":["https://p3-pc-sign.douyinpic.com/a.jpeg","https://p9-pc-sign.douyinpic.com/b.jpeg"],
             "author":{"name":"大地雷"},
             "keywords":"家常菜,下饭"}
            </script></head><body></body></html>
        """.trimIndent()
        val content = DouyinJsonLdParser.parse(html, link("7649657395216845711"), "note")!!
        assertEquals("想吃谁做的，就@他吧！", content.title)
        assertEquals("大地雷", content.author)
        assertTrue(content.textBody.contains("茄汁豆腐"))
        assertEquals(2, content.media.size)
        assertEquals("https://p3-pc-sign.douyinpic.com/a.jpeg", content.coverUrl)
        assertFalse(content.requiresWebView)
        assertTrue(content.tags.contains("家常菜"))
    }

    @Test
    fun `watermarked image variants are filtered`() {
        val html = """
            <html><head>
            <script type="application/ld+json">
            {"@type":"article","headline":"测试",
             "image":["https://p3.douyinpic.com/a~tplv-dy-water-v2.jpeg","https://p3.douyinpic.com/b~tplv-dy-aweme-images.jpg"]}
            </script></head><body></body></html>
        """.trimIndent()
        val content = DouyinJsonLdParser.parse(html, link(), "note")!!
        assertEquals(1, content.media.size)
        assertTrue(content.media.first().url.contains("aweme-images"))
    }

    @Test
    fun `no json ld returns null`() {
        assertEquals(null, DouyinJsonLdParser.parse("<html><body>空</body></html>", link(), "video"))
    }
}

class DouyinJsonParserTest {

    private fun link(id: String? = "111") = ParsedLink(
        platform = Platform.DOUYIN,
        url = "https://v.douyin.com/x/",
        contentId = id,
        shareText = "分享",
        titleHint = null,
        authorHint = null,
    )

    @Test
    fun `prefers play_addr over download_addr`() {
        val json = """
            {"aweme_detail":{
              "aweme_id":"111",
              "desc":"孜然土豆牛肉\n香迷糊了",
              "author":{"nickname":"西关焖烧男","uid":"u1"},
              "video":{
                "duration":23000,
                "play_addr":{"url_list":["https://v3-web.douyinvod.com/play.mp4"]},
                "download_addr":{"url_list":["https://v3-web.douyinvod.com/watermark.mp4"]},
                "cover":{"url_list":["https://p3.douyinpic.com/cover.jpeg"]}
              }
            }}
        """.trimIndent()
        val content = DouyinJsonParser.parse(json, link())!!
        assertEquals("孜然土豆牛肉", content.title)
        assertEquals("西关焖烧男", content.author)
        assertEquals(1, content.media.count { it.kind == MediaKind.VIDEO })
        assertTrue(content.media.first { it.kind == MediaKind.VIDEO }.url.contains("play.mp4"))
        assertFalse(content.media.any { it.url.contains("watermark") })
        assertEquals(23_000L, content.durationMs)
    }

    @Test
    fun `selects matching aweme id from list`() {
        val json = """
            {"aweme_list":[
              {"aweme_id":"000","desc":"别的视频","video":{"play_addr":{"url_list":["https://x/other.mp4"]}}},
              {"aweme_id":"111","desc":"目标视频","video":{"play_addr":{"url_list":["https://x/target.mp4"]}}}
            ]}
        """.trimIndent()
        val content = DouyinJsonParser.parse(json, link("111"))!!
        assertEquals("目标视频", content.title)
        assertTrue(content.media.first().url.contains("target.mp4"))
    }

    @Test
    fun `returns null for unrelated json`() {
        assertEquals(null, DouyinJsonParser.parse("""{"status_code":0}""", link()))
    }

    @Test
    fun `includes carousel images`() {
        val json = """
            {"aweme_detail":{
              "aweme_id":"111","desc":"图文",
              "images":[{"url_list":["https://p3.douyinpic.com/a~tplv-dy-aweme-images.webp"]}]
            }}
        """.trimIndent()
        val content = DouyinJsonParser.parse(json, link())!!
        assertEquals(1, content.media.count { it.kind == MediaKind.STEP })
        assertNotNull(content.coverUrl)
    }
}
