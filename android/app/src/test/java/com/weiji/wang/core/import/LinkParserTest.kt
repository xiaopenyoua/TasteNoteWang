package com.weiji.wang.core.import

import com.weiji.wang.core.model.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkParserTest {

    private val douyinText =
        "王强:3.07 复制打开抖音，看看【西关焖烧男的作品】比烧烤还好吃！直接香迷糊的孜然土豆牛肉 食材：牛里脊或牛排肉：400克、土豆：300克 https://v.douyin.com/auY2beN-dPo/ :8pm R@X.zt 01/11 pQx:/"

    private val douyinNoteText =
        "王强:2.05 复制打开抖音，看看【大地雷💣的图文作品】想吃谁做的，就@他吧！手到擒来👌。今日菜单 https://v.douyin.com/tJRlSFqmvnU/ Qkc:/ :2pm 10/22 e@o.Du"

    private val xhsText =
        "王强:天冷吃一口🥘港式牛腩煲 | 暖乎乎巨好吃😋 天气转凉，... https://xhslink.cn/o/6QDLJhk8FvQ 【小红书】里有答案，快去围观~"

    private val biliText =
        "王强:【记住这些万能炒菜公式，做菜怎么都不会难吃！-哔哩哔哩】 https://b23.tv/8xMyJNn"

    @Test
    fun `parses douyin share text`() {
        val link = LinkParser.parse(douyinText)!!
        assertEquals(Platform.DOUYIN, link.platform)
        assertEquals("https://v.douyin.com/auY2beN-dPo/", link.url)
        assertEquals("西关焖烧男", link.authorHint)
        assertTrue(link.titleHint!!.startsWith("比烧烤还好吃"))
        assertNull(link.contentId) // 短链不含 ID，需要展开
    }

    @Test
    fun `parses douyin image note share text`() {
        val link = LinkParser.parse(douyinNoteText)!!
        assertEquals(Platform.DOUYIN, link.platform)
        assertEquals("大地雷💣", link.authorHint)
        assertTrue(link.titleHint!!.startsWith("想吃谁做的"))
    }

    @Test
    fun `parses xiaohongshu share text`() {
        val link = LinkParser.parse(xhsText)!!
        assertEquals(Platform.XIAOHONGSHU, link.platform)
        assertEquals("https://xhslink.cn/o/6QDLJhk8FvQ", link.url)
        assertTrue(link.titleHint!!.startsWith("天冷吃一口"))
    }

    @Test
    fun `parses bilibili share text`() {
        val link = LinkParser.parse(biliText)!!
        assertEquals(Platform.BILIBILI, link.platform)
        assertEquals("https://b23.tv/8xMyJNn", link.url)
        assertEquals("记住这些万能炒菜公式，做菜怎么都不会难吃！", link.titleHint)
    }

    @Test
    fun `url only text does not leave scheme fragments`() {
        val link = LinkParser.parse("https://b23.tv/8xMyJNn")!!
        assertEquals(Platform.BILIBILI, link.platform)
        assertNull(link.titleHint)
        assertFalse(LinkParser.cleanShareNoise("https://b23.tv/8xMyJNn").contains("//b23"))
        assertEquals("", LinkParser.cleanShareNoise("https://b23.tv/8xMyJNn"))
    }

    @Test
    fun `detects full urls and content ids`() {
        val douyin = LinkParser.parse("https://www.douyin.com/video/7673495764546424100")!!
        assertEquals("7673495764546424100", douyin.contentId)

        val xhs = LinkParser.parse("https://www.xiaohongshu.com/explore/6ad9c67a0000000032016926?xsec_token=x")!!
        assertEquals("6ad9c67a0000000032016926", xhs.contentId)

        val bili = LinkParser.parse("https://www.bilibili.com/video/BV1d9uyz4EBo")!!
        assertEquals("BV1d9uyz4EBo", bili.contentId)
    }

    @Test
    fun `unknown link is generic`() {
        val link = LinkParser.parse("https://example.com/a/b")!!
        assertEquals(Platform.OTHER, link.platform)
    }

    @Test
    fun `no url returns null`() {
        assertNull(LinkParser.parse("今晚吃什么"))
    }

    @Test
    fun `cleans sender prefix and trailing password fragments`() {
        val cleaned = LinkParser.cleanShareNoise(douyinText)
        assertFalse(cleaned.contains("王强"))
        assertFalse(cleaned.contains("http"))
        assertTrue(cleaned.contains("西关焖烧男"))
    }

    @Test
    fun `extract urls from noisy text`() {
        val urls = LinkParser.extractUrls("看这个 https://v.douyin.com/abc/ ，还有 https://b23.tv/xyz")
        assertEquals(2, urls.size)
        assertNotNull(urls.firstOrNull { it.contains("douyin") })
    }
}
