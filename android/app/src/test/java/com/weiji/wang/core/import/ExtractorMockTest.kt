package com.weiji.wang.core.import

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.weiji.wang.core.import.extractor.BilibiliExtractor
import com.weiji.wang.core.import.extractor.DouyinExtractor
import com.weiji.wang.core.import.extractor.XhsExtractor
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Platform
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExtractorMockTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var cookies: CookieStore

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val original = request.url
                val local = server.url(
                    original.encodedPath + (original.encodedQuery?.let { "?$it" } ?: ""),
                )
                chain.proceed(request.newBuilder().url(local).build())
            }
            .build()
        cookies = CookieStore(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun link(platform: Platform, url: String, id: String? = null) = ParsedLink(
        platform = platform,
        url = url,
        contentId = id,
        shareText = "分享 $url",
        titleHint = null,
        authorHint = null,
    )

    @Test
    fun `bilibili extractor resolves short link and selects durl`() = runTest {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path.startsWith("/8xMyJNn") -> MockResponse()
                        .setResponseCode(302)
                        .setHeader("Location", "https://www.bilibili.com/video/BV1d9uyz4EBo")
                    path.startsWith("/video/BV1d9uyz4EBo") -> MockResponse().setResponseCode(200)
                    path.startsWith("/x/web-interface/view") -> MockResponse().setBody(
                        """{"code":0,"data":{"bvid":"BV1d9uyz4EBo","title":"测试B站菜","owner":{"name":"UP主","mid":1},"desc":"做法","pic":"https://i2.hdslb.com/cover.jpg","duration":351,"tname":"美食","pages":[{"cid":123,"part":"正片"}]}}""",
                    )
                    path.startsWith("/x/player/playurl") -> MockResponse().setBody(
                        """{"code":0,"data":{"quality":64,"format":"mp4720","durl":[{"url":"https://upos-sz-mirrorcoso1.bilivideo.com/v.mp4","size":123,"length":351}]}}""",
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val content = BilibiliExtractor(client).extract(link(Platform.BILIBILI, "https://b23.tv/8xMyJNn"))
        assertEquals("测试B站菜", content.title)
        assertEquals("UP主", content.author)
        assertEquals("BV1d9uyz4EBo", content.contentId)
        assertEquals(1, content.media.count { it.kind == MediaKind.VIDEO })
        assertTrue(content.media.first { it.kind == MediaKind.VIDEO }.url.endsWith("v.mp4"))
        assertEquals(HttpClients.DESKTOP_UA, content.media.first { it.kind == MediaKind.VIDEO }.headers["User-Agent"])
    }

    @Test
    fun `xiaohongshu extractor resolves short link and parses note`() = runTest {
        val html = """
            <html><head><script>
            window.__INITIAL_STATE__ = {"noteData":{"data":{"noteData":{
              "noteId":"6ad9c67a0000000032016926","type":"normal","title":"港式牛腩煲",
              "desc":"食材：牛腩 500克","user":{"nickName":"Keling","userId":"u1"},
              "tagList":[{"name":"家常菜"}],
              "imageList":[{"fileId":"file1","width":1000,"height":1332}]
            }}}};
            </script></head><body></body></html>
        """.trimIndent()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path.startsWith("/o/6QDLJhk8FvQ") -> MockResponse()
                        .setResponseCode(302)
                        .setHeader(
                            "Location",
                            "https://www.xiaohongshu.com/discovery/item/6ad9c67a0000000032016926?xsec_token=x",
                        )
                    path.startsWith("/discovery/item/") -> MockResponse().setBody(html)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val content = XhsExtractor(client, cookies).extract(
            link(
                Platform.XIAOHONGSHU,
                "https://www.xiaohongshu.com/discovery/item/6ad9c67a0000000032016926?xsec_token=x",
            ),
        )
        assertEquals("港式牛腩煲", content.title)
        assertEquals("Keling", content.author)
        assertEquals("https://ci.xiaohongshu.com/file1", content.media.first().url)
        assertTrue(content.tags.contains("家常菜"))
    }

    @Test
    fun `douyin extractor uses json ld fallback and asks for webview`() = runTest {
        val jsonLd = """
            <html><head><script type="application/ld+json">
            {"@type":"VideoObject",
             "name":"比烧烤还好吃！孜然土豆牛肉 食材：牛里脊：400克、土豆：300克 - 抖音",
             "description":"做法",
             "thumbnailUrl":["https://p3-sign.douyinpic.com/cover.jpeg"],
             "duration":"PT0H0M23S",
             "creator":{"name":"西关焖烧男"}}
            </script></head><body></body></html>
        """.trimIndent()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path.startsWith("/auY2beN-dPo") -> MockResponse()
                        .setResponseCode(302)
                        .setHeader(
                            "Location",
                            "https://www.iesdouyin.com/share/video/7673495764546424100/",
                        )
                    path.startsWith("/share/video/") -> MockResponse().setBody("<html><title>抖音</title></html>")
                    path.startsWith("/video/7673495764546424100") -> MockResponse().setBody(jsonLd)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val content = DouyinExtractor(client, cookies).extract(
            link(Platform.DOUYIN, "https://v.douyin.com/auY2beN-dPo/"),
        )
        assertEquals("比烧烤还好吃！孜然土豆牛肉", content.title)
        assertEquals("西关焖烧男", content.author)
        assertTrue(content.textBody.contains("400克"))
        assertEquals(MediaKind.COVER, content.media.first().kind)
        assertTrue(content.requiresWebView)
        assertEquals(23_000L, content.durationMs)
    }
}
