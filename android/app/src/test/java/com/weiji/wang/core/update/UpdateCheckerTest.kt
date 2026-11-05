package com.weiji.wang.core.update

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class UpdateCheckerTest {

    private val releaseJson = """
        {
          "tag_name": "v1.0.0",
          "name": "味记 v1.0.0",
          "body": "首个正式版：抖音视频与图文、在线更新。",
          "html_url": "https://github.com/xiaopenyoua/TasteNoteWang/releases/tag/v1.0.0",
          "assets": [
            {"name": "weiji-1.0.0.apk", "size": 12345,
             "browser_download_url": "https://github.com/xiaopenyoua/TasteNoteWang/releases/download/v1.0.0/weiji-1.0.0.apk",
             "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}
          ]
        }
    """.trimIndent()

    @Test
    fun `parses latest release and apk asset`() {
        val info = UpdateChecker().parseRelease(releaseJson)!!
        assertEquals("1.0.0", info.version)
        assertEquals("v1.0.0", info.tag)
        assertEquals(12_345L, info.apkSize)
        assertTrue(info.apkUrl.endsWith("weiji-1.0.0.apk"))
        assertEquals(64, info.sha256!!.length)
        assertTrue(info.notes.contains("在线更新"))
    }

    @Test
    fun `release without apk is ignored`() {
        assertNull(UpdateChecker().parseRelease("""{"tag_name":"v1.0.0","assets":[]}"""))
        assertNull(UpdateChecker().parseRelease("not json"))
    }

    @Test
    fun `version comparison handles v prefix and debug suffix`() {
        assertEquals("1.0.0", UpdateChecker.normalizeVersion("v1.0.0-debug"))
        assertTrue(UpdateChecker.isNewer("1.0.1", "1.0.0"))
        assertTrue(UpdateChecker.isNewer("1.1.0", "1.0.9"))
        assertTrue(UpdateChecker.isNewer("1.0.0", "1.0.0-debug"))
        assertFalse(UpdateChecker.isNewer("0.9.9", "1.0.0"))
        assertTrue(UpdateChecker.isNewer("2.0.0", "1.9.9"))
        assertTrue(UpdateChecker.isNewer("1.0.0", "1.0.0-beta"))
    }

    @Test
    fun `download verifies size and sha`() = runBlocking {
        val server = MockWebServer()
        val payload = "fake apk bytes".toByteArray()
        server.enqueue(MockResponse().setBody(String(payload)))
        server.start()
        val sha = MessageDigest.getInstance("SHA-256").digest(payload)
            .joinToString("") { "%02x".format(it) }
        val dir = File(System.getProperty("java.io.tmpdir"), "weiji-update-test-${System.nanoTime()}")
        try {
            val info = UpdateInfo(
                version = "1.0.1",
                tag = "v1.0.1",
                notes = "",
                pageUrl = "",
                apkUrl = server.url("/weiji-1.0.1.apk").toString(),
                apkSize = payload.size.toLong(),
                sha256 = sha,
            )
            var last = 0
            val file = UpdateChecker(OkHttpClient()).download(info, dir) { last = it }
            assertEquals(payload.size.toLong(), file.length())
            assertEquals(100, last)
            assertTrue(file.name.endsWith(".apk"))
        } finally {
            server.shutdown()
            dir.deleteRecursively()
        }
    }
}
