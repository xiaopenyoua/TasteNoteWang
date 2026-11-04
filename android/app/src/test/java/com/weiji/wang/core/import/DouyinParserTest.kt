package com.weiji.wang.core.import

import com.weiji.wang.core.model.FetchStatus
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
    fun `prefers direct cdn over signed play redirect`() {
        val json = """
            {"aweme_detail":{
              "aweme_id":"111",
              "desc":"视频",
              "video":{
                "play_addr":{"url_list":[
                  "https://www.douyin.com/aweme/v1/play/?video_id=v1",
                  "https://v26-webf.douyinvod.com/abc/video/tos/cn/x/?mime_type=video_mp4"
                ]}
              }
            }}
        """.trimIndent()
        val content = DouyinJsonParser.parse(json, link())!!
        val video = content.media.first { it.kind == MediaKind.VIDEO }
        assertTrue(video.url.contains("douyinvod.com"))
        assertFalse(video.url.contains("/aweme/v1/play/"))
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
    fun `rejects detail that belongs to another video`() {
        val json = """
            {"aweme_detail":{
              "aweme_id":"999",
              "desc":"相关推荐",
              "video":{"play_addr":{"url_list":["https://v3-web.douyinvod.com/other.mp4"]}}
            }}
        """.trimIndent()
        assertEquals(null, DouyinJsonParser.parse(json, link("111")))
    }

    @Test
    fun `returns null for unrelated json`() {
        assertEquals(null, DouyinJsonParser.parse("""{"status_code":0}""", link()))
    }

    @Test
    fun `iteminfo item_list with bit_rate picks direct cdn`() {
        // 2026-11-04 实测结构：分享页 /web/api/v2/aweme/iteminfo/ 返回 item_list，
        // play_addr 里混着需要页面签名的 /aweme/v1/play/ 跳转口和直连 CDN。
        val json = """
            {"status_code":0,"item_list":[{
              "aweme_id":"7703148118140800266",
              "desc":"香迷糊的孜然土豆牛肉\n食材：牛里脊 400克",
              "author":{"uid":"123456","nickname":"Kami","sec_uid":"MS4wLjABAAAA"},
              "video":{
                "duration":23000,
                "play_addr":{"uri":"v0200fg10000abcd","url_list":[
                  "https://www.iesdouyin.com/aweme/v1/play/?video_id=v0200fg10000abcd&ratio=720p&line=0",
                  "https://v3-web.douyinvod.com/abc/video/tos/cn/tos-cn-ve-15/xyz/?a=1&mime_type=video_mp4"
                ]},
                "bit_rate":[{
                  "gear_name":"normal_1080_0",
                  "bit_rate":2083000,
                  "play_addr":{"url_list":[
                    "https://v26-web.douyinvod.com/def/video/tos/cn/tos-cn-ve-15/uvw/?mime_type=video_mp4"
                  ]}
                }],
                "cover":{"url_list":[
                  "https://p3-sign.douyinpic.com/tos-cn-i-dy/cover~tplv-dy-aweme-images:q75.webp?x=1"
                ]},
                "width":1080,"height":1920
              }
            }]}
        """.trimIndent()
        val content = DouyinJsonParser.parse(json, link("7703148118140800266"))!!
        val video = content.media.single { it.kind == MediaKind.VIDEO }
        assertTrue(video.url.contains("douyinvod.com"))
        assertFalse(video.url.contains("/aweme/v1/play/"))
        assertEquals(23_000L, content.durationMs)
        assertEquals("香迷糊的孜然土豆牛肉", content.title)
        assertEquals("Kami", content.author)
        assertEquals("123456", content.authorId)
        assertEquals("https://www.douyin.com/video/7703148118140800266", content.canonicalUrl)
        assertEquals("https://p3-sign.douyinpic.com/tos-cn-i-dy/cover~tplv-dy-aweme-images:q75.webp?x=1", content.coverUrl)
        // 下载头必须带 Referer/Origin，否则 CDN 403（2026-11-04 实测）
        assertEquals("https://www.douyin.com/", video.headers["Referer"])
        assertEquals("https://www.douyin.com", video.headers["Origin"])
    }

    @Test
    fun `iteminfo image post becomes step media without video`() {
        val json = """
            {"status_code":0,"item_list":[{
              "aweme_id":"7649657395216845711",
              "desc":"今日菜单：孜然土豆炒肉、茄汁豆腐",
              "author":{"nickname":"大地雷"},
              "images":[
                {"url_list":["https://p3-sign.douyinpic.com/tos-cn-i-dy/a~tplv-dy-aweme-images:q75.webp"]},
                {"url_list":["https://p9-sign.douyinpic.com/tos-cn-i-dy/b~tplv-dy-aweme-images:q75.webp"]}
              ]
            }]}
        """.trimIndent()
        val content = DouyinJsonParser.parse(json, link("7649657395216845711"))!!
        assertEquals(2, content.media.count { it.kind == MediaKind.STEP })
        assertFalse(content.media.any { it.kind == MediaKind.VIDEO })
        assertEquals("https://www.douyin.com/note/7649657395216845711", content.canonicalUrl)
        assertEquals("https://p3-sign.douyinpic.com/tos-cn-i-dy/a~tplv-dy-aweme-images:q75.webp", content.coverUrl)
    }

    @Test
    fun `item without video or images returns null`() {
        val json = """
            {"status_code":0,"item_list":[{
              "aweme_id":"111","desc":"只有文字，没有可下载媒体"
            }]}
        """.trimIndent()
        assertEquals(null, DouyinJsonParser.parse(json, link("111")))
    }

    @Test
    fun `item_list id mismatch is rejected`() {
        val json = """
            {"status_code":0,"item_list":[{
              "aweme_id":"999",
              "desc":"页面预取的相关推荐",
              "video":{"play_addr":{"url_list":["https://v3-web.douyinvod.com/other.mp4"]}}
            }]}
        """.trimIndent()
        assertEquals(null, DouyinJsonParser.parse(json, link("111")))
        // 链接里没有 id 时（短链未展开）只能认第一条
        assertNotNull(DouyinJsonParser.parse(json, link(null)))
    }

    @Test
    fun `double wrapped aweme detail is unwrapped`() {
        val json = """
            {"aweme_detail":{"aweme_detail":{
              "aweme_id":"111",
              "desc":"套两层的 detail",
              "video":{"play_addr":{"url_list":["https://v26-web.douyinvod.com/x/video/tos/cn/y.mp4"]}}
            }}}
        """.trimIndent()
        val content = DouyinJsonParser.parse(json, link("111"))!!
        assertEquals("套两层的 detail", content.title)
        assertTrue(content.media.first { it.kind == MediaKind.VIDEO }.url.contains("douyinvod"))
    }

    @Test
    fun `synthesizes video media from sniffed url and seo metadata`() {
        // 只有 performance/DOM 里捞到的直链、没有任何 JSON：标题/作者/封面用 SEO 结果补，duration 不猜。
        val seo = ExtractedContent(
            platform = Platform.DOUYIN,
            canonicalUrl = "https://www.douyin.com/video/111",
            contentId = "111",
            title = "比烧烤还好吃！孜然土豆牛肉",
            author = "西关焖烧男",
            textBody = "食材：牛里脊 400克",
            coverUrl = "https://p3-sign.douyinpic.com/cover~tplv-dy-aweme-images:q75.webp",
            media = listOf(
                RemoteMedia(
                    kind = MediaKind.COVER,
                    url = "https://p3-sign.douyinpic.com/cover~tplv-dy-aweme-images:q75.webp",
                    mime = "image/jpeg",
                ),
            ),
            fetchStatus = FetchStatus.PARTIAL,
            requiresWebView = true,
        )
        val content = DouyinJsonParser.synthesize(
            listOf(
                "https://www.iesdouyin.com/aweme/v1/play/?video_id=v0200fg10000abcd",
                "https://v3-web.douyinvod.com/abc/video/tos/cn/tos-cn-ve-15/xyz/?mime_type=video_mp4",
            ),
            seo = seo,
            link = link("111"),
            pageUrl = "https://www.iesdouyin.com/share/video/111/",
        )!!
        val video = content.media.single { it.kind == MediaKind.VIDEO }
        assertTrue(video.url.contains("douyinvod.com"))
        assertFalse(video.url.contains("/aweme/v1/play/"))
        assertEquals(null, content.durationMs)
        assertEquals("比烧烤还好吃！孜然土豆牛肉", content.title)
        assertEquals("西关焖烧男", content.author)
        assertEquals(seo.coverUrl, content.coverUrl)
        assertEquals(FetchStatus.OK, content.fetchStatus)
        assertFalse(content.requiresWebView)
        assertEquals("https://www.douyin.com/video/111", content.canonicalUrl)
    }

    @Test
    fun `synthesize ignores non video urls`() {
        assertEquals(
            null,
            DouyinJsonParser.synthesize(
                listOf("https://p3-sign.douyinpic.com/a~tplv-dy-aweme-images:q75.webp"),
                seo = null,
                link = link("111"),
                pageUrl = "https://www.iesdouyin.com/share/video/111/",
            ),
        )
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
