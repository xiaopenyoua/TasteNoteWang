package com.weiji.wang.core.import

import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XhsWebViewParserTest {

    private val link = ParsedLink(
        platform = Platform.XIAOHONGSHU,
        url = "https://xhslink.cn/o/6QDLJhk8FvQ",
        contentId = "6ad9c67a0000000032016926",
        shareText = "分享",
        titleHint = null,
        authorHint = null,
    )

    private val html = """
        <html><head><script>
        window.__INITIAL_STATE__ = {"noteData":{"data":{"noteData":{
          "noteId":"6ad9c67a0000000032016926",
          "type":"video",
          "title":"港式牛腩煲",
          "desc":"天气转凉，炖一锅牛腩。\n食材：牛腩 500克",
          "user":{"nickName":"Keling","userId":"u1"},
          "tagList":[{"name":"家常菜"},{"name":"炖菜"}],
          "imageList":[
            {"fileId":"file1","width":1000,"height":1332,"infoList":[{"imageScene":"H5_DTL","url":"https://sns-webpic.xhscdn.com/1!h5_1080jpg"}]},
            {"fileId":"file2","width":1088,"height":1448,"infoList":[{"imageScene":"H5_DTL","url":"https://sns-webpic.xhscdn.com/2!h5_1080jpg"}]}
          ],
          "video":{"capa":{"duration":148},"media":{"stream":{"h264":[{
            "masterUrl":"http://sns-video.xhscdn.com/v.mp4?sign=x",
            "backupUrls":["http://sns-bak.xhscdn.com/v.mp4"],
            "width":720,"height":1280,"duration":148,"size":19653461
          }]}}}
        }}}};
        </script></head><body></body></html>
    """.trimIndent()

    @Test
    fun `parses note metadata`() {
        val content = XhsWebViewParser.parse(html, link.url, link)!!
        assertEquals(Platform.XIAOHONGSHU, content.platform)
        assertEquals("港式牛腩煲", content.title)
        assertEquals("Keling", content.author)
        assertEquals("u1", content.authorId)
        assertTrue(content.tags.contains("家常菜"))
        assertTrue(content.textBody.contains("牛腩"))
    }

    @Test
    fun `uses watermark free original image url`() {
        val content = XhsWebViewParser.parse(html, link.url, link)!!
        val images = content.media.filter { it.kind == MediaKind.STEP }
        assertEquals(2, images.size)
        assertEquals("https://ci.xiaohongshu.com/file1", images[0].url)
        assertEquals("https://ci.xiaohongshu.com/file2", images[1].url)
        assertTrue(images.all { it.watermarkFree })
        assertEquals(1000, images[0].width)
        assertEquals("https://ci.xiaohongshu.com/file1", content.coverUrl)
    }

    @Test
    fun `parses video stream and upgrades to https`() {
        val content = XhsWebViewParser.parse(html, link.url, link)!!
        val video = content.media.first { it.kind == MediaKind.VIDEO }
        assertTrue(video.url.startsWith("https://"))
        assertTrue(video.url.contains("v.mp4"))
        assertEquals(148_000L, video.durationMs)
        assertEquals(19_653_461L, video.sizeBytes)
        assertEquals(720, video.width)
    }

    @Test
    fun `fetches without webview`() {
        val content = XhsWebViewParser.parse(html, link.url, link)!!
        assertFalse(content.requiresWebView)
        assertNotNull(content.media.firstOrNull())
    }

    @Test
    fun `returns null when initial state missing`() {
        assertEquals(null, XhsWebViewParser.parse("<html></html>", link.url, link))
    }
}
