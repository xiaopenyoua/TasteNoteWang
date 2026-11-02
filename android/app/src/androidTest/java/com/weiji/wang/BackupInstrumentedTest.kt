package com.weiji.wang

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.RecipeEntity
import com.weiji.wang.core.data.db.SourceEntity
import com.weiji.wang.core.data.db.StepEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupInstrumentedTest {

    private val app: WeijiApplication
        get() = ApplicationProvider.getApplicationContext()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val recipeId = "01BACKUP0000000000000000001"

    @Test
    fun exportAndRestoreRoundTrip() = runBlocking {
        val container = app.container
        withContext(Dispatchers.IO) {
            container.database.clearAllTables()
            val now = System.currentTimeMillis()
            val recipe = RecipeEntity(
                id = recipeId,
                title = "备份测试菜",
                servings = 3,
                createdAt = now,
                updatedAt = now,
                sourceId = "src1",
            )
            val source = SourceEntity(
                id = "src1",
                recipeId = recipeId,
                platform = "XIAOHONGSHU",
                originalUrl = "https://xhslink.cn/o/test",
                authorName = "作者",
                textBody = "食材：土豆 2个",
                importedAt = now,
                fetchStatus = "OK",
            )
            val ingredient = IngredientEntity(
                "i1", recipeId, 0, null, "土豆", 2.0, "个", "土豆：2个", true,
            )
            val step = StepEntity("s1", recipeId, 0, "切块", null, null, null)
            container.recipes.saveRecipe(recipe, listOf(ingredient), listOf(step), source, emptyList())
            // 写一个媒体文件，验证媒体是否随 zip 往返
            container.files.writeTextAtomic(recipeId, "note.txt", "hello media")
        }

        val zip = File(context.cacheDir, "backup-test.zip").apply { if (exists()) delete() }
        val uri = Uri.fromFile(zip)
        val exported = container.backup.exportTo(uri, includeMedia = true)
        assertEquals(1, exported.recipes)
        assertTrue(zip.length() > 0)

        withContext(Dispatchers.IO) {
            container.database.clearAllTables()
            container.files.deleteRecipe(recipeId)
        }
        val restored = container.backup.restoreFrom(uri)
        assertEquals(1, restored.imported)
        assertEquals(0, restored.failed)

        val details = withContext(Dispatchers.IO) { container.recipes.details(recipeId) }
        assertEquals("备份测试菜", details?.recipe?.title)
        assertEquals(1, details?.ingredients?.size)
        assertEquals(1, details?.steps?.size)
        assertTrue(container.files.resolve("recipes/$recipeId/note.txt").exists())
    }

    @Test
    fun restoreSkipsSameOrOlderRecipe() = runBlocking {
        val container = app.container
        val zip = File(context.cacheDir, "backup-test-2.zip").apply { if (exists()) delete() }
        val now = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            container.database.clearAllTables()
            val recipe = RecipeEntity(
                id = "01BACKUP0000000000000000002",
                title = "新版本",
                createdAt = now,
                updatedAt = now + 10_000,
            )
            container.recipes.saveRecipe(recipe, emptyList(), emptyList(), null, emptyList())
        }
        // 导出一个没有内容的空备份，恢复时应跳过或不失败
        val exported = container.backup.exportTo(Uri.fromFile(zip), includeMedia = false)
        assertEquals(1, exported.recipes)
        val result = container.backup.restoreFrom(Uri.fromFile(zip))
        assertEquals(1, result.skipped)
    }
}
