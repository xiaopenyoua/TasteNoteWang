package com.weiji.wang.core.storage

import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.MediaEntity
import com.weiji.wang.core.data.db.RecipeEntity
import com.weiji.wang.core.data.db.SourceEntity
import com.weiji.wang.core.data.db.StepEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeSnapshotTest {

    private val recipe = RecipeEntity(
        id = "01ABC",
        title = "孜然土豆牛肉",
        subtitle = "@西关焖烧男",
        coverPath = "recipes/01ABC/cover.webp",
        servings = 2,
        totalMinutes = 5,
        notes = "牛肉要逆纹切",
        createdAt = 1000,
        updatedAt = 2000,
    )
    private val source = SourceEntity(
        id = "s1",
        recipeId = "01ABC",
        platform = "DOUYIN",
        originalUrl = "https://v.douyin.com/x/",
        canonicalUrl = "https://www.douyin.com/video/123",
        authorName = "西关焖烧男",
        textBody = "食材：牛肉 400克",
        importedAt = 1000,
        fetchStatus = "PARTIAL",
    )
    private val ingredients = listOf(
        IngredientEntity("i1", "01ABC", 0, null, "牛肉", 400.0, "克", "牛肉：400克", true),
        IngredientEntity("i2", "01ABC", 1, null, "土豆", 300.0, "克", "土豆：300克", true),
    )
    private val steps = listOf(
        StepEntity("st1", "01ABC", 0, "切块", 60, null, 0),
        StepEntity("st2", "01ABC", 1, "下锅翻炒", null, "recipes/01ABC/step-01.webp", 12000),
    )
    private val media = listOf(
        MediaEntity(
            "m1", "01ABC", "COVER", "recipes/01ABC/cover.webp", "image/webp",
            1080, 720, null, 12345, "sha", "https://p3.douyinpic.com/a", true, 1000,
        ),
    )

    @Test
    fun `json round trip keeps all fields`() {
        val json = RecipeSnapshot.toJson(recipe, source, ingredients, steps, media, exportedAt = 999)
        val parsed = RecipeSnapshot.parse(json)!!
        assertEquals(RecipeSnapshot.VERSION, parsed.version)
        assertEquals("孜然土豆牛肉", parsed.recipe.title)
        assertEquals(2, parsed.recipe.servings)
        assertEquals("DOUYIN", parsed.source?.platform)
        assertEquals("西关焖烧男", parsed.source?.authorName)
        assertEquals(2, parsed.ingredients.size)
        assertEquals(400.0, parsed.ingredients[0].amount!!, 0.001)
        assertEquals(2, parsed.steps.size)
        assertEquals(60, parsed.steps[0].durationSeconds)
        assertEquals("recipes/01ABC/step-01.webp", parsed.steps[1].mediaPath)
        assertEquals(1, parsed.media.size)
        assertEquals("sha", parsed.media[0].sha256)
    }

    @Test
    fun `markdown is human readable and includes attribution`() {
        val json = RecipeSnapshot.toJson(recipe, source, ingredients, steps, media)
        val markdown = RecipeSnapshot.toMarkdown(RecipeSnapshot.parse(json)!!)
        assertTrue(markdown.contains("# 孜然土豆牛肉"))
        assertTrue(markdown.contains("## 食材"))
        assertTrue(markdown.contains("## 步骤"))
        assertTrue(markdown.contains("牛肉"))
        assertTrue(markdown.contains("400克"))
        assertTrue(markdown.contains("https://v.douyin.com/x/"))
        assertTrue(markdown.contains("西关焖烧男"))
    }

    @Test
    fun `invalid json returns null`() {
        assertEquals(null, RecipeSnapshot.parse("not json"))
    }

    @Test
    fun `snapshot without source still works`() {
        val json = RecipeSnapshot.toJson(recipe, null, emptyList(), emptyList(), emptyList())
        val parsed = RecipeSnapshot.parse(json)
        assertNotNull(parsed)
        assertEquals(null, parsed!!.source)
    }

    @Test
    fun `step id and position round trip for step media`() {
        val stepMedia = listOf(
            MediaEntity(
                id = "m2",
                recipeId = "01ABC",
                kind = "STEP",
                relativePath = "recipes/01ABC/step-01-01.webp",
                mime = "image/webp",
                createdAt = 1000,
                stepId = "st1",
                position = 2,
            ),
        )
        val json = RecipeSnapshot.toJson(recipe, source, ingredients, steps, stepMedia, exportedAt = 999)
        val parsed = RecipeSnapshot.parse(json)!!

        assertEquals(1, parsed.media.size)
        assertEquals("st1", parsed.media[0].stepId)
        assertEquals(2, parsed.media[0].position)
        assertEquals(1, parsed.steps[1].position)
        assertEquals("recipes/01ABC/step-01.webp", parsed.steps[1].mediaPath)
    }
}
