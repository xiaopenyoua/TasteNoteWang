package com.weiji.wang

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ImportLocalInstrumentedTest {

    private val app: WeijiApplication
        get() = ApplicationProvider.getApplicationContext()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun png(name: String, color: Int): File {
        val file = File(context.cacheDir, name)
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    @Test
    fun importsSharedImagesAsRecipe() = runBlocking {
        val container = app.container
        withContext(Dispatchers.IO) { container.database.clearAllTables() }
        val first = png("share-1.png", Color.RED)
        val second = png("share-2.png", Color.GREEN)

        val result = container.importer.importLocal(
            listOf(Uri.fromFile(first), Uri.fromFile(second)),
            "奶奶的红烧肉",
        )

        assertTrue(result.recipeId.isNotBlank())
        val details = withContext(Dispatchers.IO) { container.recipes.details(result.recipeId) }
        assertNotNull(details)
        assertEquals("奶奶的红烧肉", details!!.recipe.title)
        assertEquals(2, details.media.size)
        assertTrue(details.media.any { it.kind == "COVER" })
        assertTrue(details.media.any { it.kind == "PHOTO" })
        details.media.forEach { media ->
            assertTrue(container.files.resolve(media.relativePath).exists())
        }
        assertTrue(container.files.recipeDir(result.recipeId).resolve("recipe.json").exists())
    }

    @Test
    fun importsSingleImageAndFallsBackToFileName() = runBlocking {
        val container = app.container
        withContext(Dispatchers.IO) { container.database.clearAllTables() }
        val file = png("share-single.png", Color.BLUE)
        val result = container.importer.importLocal(listOf(Uri.fromFile(file)), "")
        val details = withContext(Dispatchers.IO) { container.recipes.details(result.recipeId) }
        assertNotNull(details)
        assertEquals(1, details!!.media.size)
        assertEquals("COVER", details.media.first().kind)
        assertTrue(details.recipe.title.isNotBlank())
    }
}
