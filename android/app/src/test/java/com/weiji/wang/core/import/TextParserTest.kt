package com.weiji.wang.core.import

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextParserTest {

    @Test
    fun `parses xiaohongshu inline sauce line`() {
        val body = """
            没想到牛肉可以这么做，嘎嘎下饭！
            酱汁：1勺生抽➕1勺蚝油➕1勺淀粉➕适量水搅拌均匀即可
            教程在图片上，快去试试吧
            #美食教程[话题]# #超级下饭的家常菜[话题]# #家常菜
        """.trimIndent()
        val parsed = TextParser.parse("牛肉土豆", body)
        val names = parsed.ingredients.map { it.name }
        assertTrue("应拆出生抽: $names", names.any { it.contains("生抽") })
        assertTrue("应拆出蚝油: $names", names.any { it.contains("蚝油") })
        assertTrue("应拆出淀粉: $names", names.any { it.contains("淀粉") })
        assertTrue("应拆出水: $names", names.any { it == "水" })
        assertTrue("不应把话题标签当步骤: ${parsed.steps}", parsed.steps.none { it.contains("#") })
        assertTrue(parsed.ingredients.first { it.name.contains("生抽") }.amount == 1.0)
    }

    @Test
    fun `parses douyin ingredient block`() {
        val body = """
            食材
            牛里脊或牛排肉：400克
            土豆：300克
            大蒜：约4瓣
            小葱：15克
            白芝麻：5克
            孜然粉：5克
        """.trimIndent()
        val parsed = TextParser.parse("孜然土豆牛肉", body)
        val map = parsed.ingredients.associate { it.name to (it.amount to it.unit) }
        assertEquals(400.0 to "克", map["牛里脊或牛排肉"])
        assertEquals(300.0 to "克", map["土豆"])
        assertEquals(4.0 to "瓣", map["大蒜"])
        assertEquals(5.0 to "克", map["孜然粉"])
    }

    @Test
    fun `parses numbered steps`() {
        val body = """
            步骤
            1. 牛肉切片
            2. 加生抽腌制 10 分钟
            3. 下锅大火翻炒
        """.trimIndent()
        val parsed = TextParser.parse("炒牛肉", body)
        assertEquals(3, parsed.steps.size)
        assertEquals("牛肉切片", parsed.steps[0])
        assertEquals("下锅大火翻炒", parsed.steps[2])
    }

    @Test
    fun `detects servings range`() {
        val parsed = TextParser.parse("汤", "食材：排骨 500克\n步骤：1. 炖煮\n适合 2-3 人")
        assertEquals(3, parsed.servings)
    }

    @Test
    fun `parses vague ingredients`() {
        val water = TextParser.parseIngredient(null, "适量水")!!
        assertEquals("水", water.name)
        assertTrue(water.vague)
        assertNull(water.amount)

        val salt = TextParser.parseIngredient(null, "少许盐")!!
        assertEquals("盐", salt.name)
        assertTrue(salt.vague)
    }

    @Test
    fun `parses amount first form`() {
        val item = TextParser.parseIngredient(null, "2勺生抽")!!
        assertEquals("生抽", item.name)
        assertEquals(2.0, item.amount)
        assertEquals("勺", item.unit)
    }

    @Test
    fun `parses range amount as average`() {
        val item = TextParser.parseIngredient(null, "2-3勺生抽")!!
        assertEquals(2.5, item.amount)
        assertEquals("勺", item.unit)
    }

    @Test
    fun `no steps means no fabricated paragraphs`() {
        val parsed = TextParser.parse("标题", "今天天气不错，适合出去走走。\n这道菜真的很好吃。")
        assertEquals(0, parsed.steps.size)
        assertTrue(parsed.ingredients.isEmpty())
    }

    @Test
    fun `strips hashtags and mentions`() {
        val parsed = TextParser.parse("标题", "@小王 今天做这个 #美食[话题]# #家常菜")
        assertTrue(!parsed.notes.contains("#"))
        assertTrue(parsed.steps.none { it.contains("#") })
    }
}
