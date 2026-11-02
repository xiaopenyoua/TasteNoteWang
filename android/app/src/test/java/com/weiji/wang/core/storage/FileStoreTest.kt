package com.weiji.wang.core.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FileStoreTest {

    private lateinit var store: FileStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = FileStore(context)
    }

    @Test
    fun `save stream writes atomically and returns hash`() {
        val bytes = "hello weiji".toByteArray()
        val saved = store.saveStream("recipe1", "data.bin", ByteArrayInputStream(bytes), "application/octet-stream")
        assertEquals(bytes.size.toLong(), saved.sizeBytes)
        assertEquals(64, saved.sha256.length)
        assertTrue(saved.relativePath.startsWith("recipes/recipe1/"))
        assertTrue(store.exists(saved.relativePath))
        assertTrue(store.resolve(saved.relativePath).readBytes().contentEquals(bytes))
        assertFalse(store.recipeDir("recipe1").resolve("data.bin.part").exists())
    }

    @Test
    fun `text snapshot is atomic and readable`() {
        val file = store.writeTextAtomic("recipe2", "recipe.json", """{"a":1}""")
        assertTrue(file.exists())
        assertEquals("""{"a":1}""", file.readText())
        assertFalse(store.recipeDir("recipe2").resolve("recipe.json.part").exists())
    }

    @Test
    fun `delete recipe removes whole folder`() {
        store.saveStream("recipe3", "video.mp4", ByteArrayInputStream(ByteArray(16)), "video/mp4")
        assertTrue(store.recipeDir("recipe3").exists())
        store.deleteRecipe("recipe3")
        assertFalse(store.recipeDir("recipe3").exists())
    }

    @Test
    fun `total and free bytes are sane`() {
        store.saveStream("recipe4", "a.bin", ByteArrayInputStream(ByteArray(1024)), "application/octet-stream")
        assertTrue(store.recipeBytes("recipe4") >= 1024)
        assertTrue(store.totalBytes() >= 1024)
        assertTrue(store.freeBytes() >= 0)
    }

    @Test
    fun `resolve is rooted and relative paths survive`() {
        val saved = store.saveStream("recipe5", "x.bin", ByteArrayInputStream(byteArrayOf(1)), "application/octet-stream")
        assertEquals(store.root, store.resolve(saved.relativePath).parentFile.parentFile.parentFile)
    }
}
