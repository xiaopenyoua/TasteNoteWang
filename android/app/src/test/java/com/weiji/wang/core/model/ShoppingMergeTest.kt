package com.weiji.wang.core.model

import com.weiji.wang.core.data.db.ShoppingItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingMergeTest {

    private var nextId = 0

    private fun item(
        name: String,
        amount: Double?,
        unit: String?,
        checked: Boolean = false,
        category: String? = null,
    ) = ShoppingItemEntity(
        id = "item-${nextId++}",
        listId = "list",
        name = name,
        amount = amount,
        unit = unit,
        category = category,
        checked = checked,
        recipeId = null,
        createdAt = nextId.toLong(),
        sourceDate = null,
    )

    @Test
    fun `same name and unit are summed`() {
        val merged = mergeShoppingItems(
            listOf(
                item("牛肉", 400.0, "克"),
                item("牛肉", 300.0, "克"),
            ),
        )
        assertEquals(1, merged.size)
        assertEquals("牛肉", merged[0].name)
        assertEquals("克", merged[0].unit)
        assertEquals(700.0, merged[0].amount!!, 0.001)
        assertEquals("700克", merged[0].display)
        assertEquals(2, merged[0].sources.size)
    }

    @Test
    fun `names are normalized before grouping`() {
        val merged = mergeShoppingItems(
            listOf(
                item("蒜", 5.0, "瓣"),
                item("大蒜", 5.0, "瓣"),
            ),
        )
        assertEquals(1, merged.size)
        assertEquals("大蒜", merged[0].name)
        assertEquals(10.0, merged[0].amount!!, 0.001)
        assertEquals("1 头", merged[0].display)
    }

    @Test
    fun `ten garlic cloves display as one head`() {
        val merged = mergeShoppingItems(
            listOf(
                item("大蒜", 6.0, "瓣"),
                item("大蒜", 4.0, "瓣"),
            ),
        )
        assertEquals(1, merged.size)
        assertEquals(10.0, merged[0].amount!!, 0.001)
        assertEquals("1 头", merged[0].display)

        val leftovers = mergeShoppingItems(
            listOf(
                item("大蒜", 6.0, "瓣"),
                item("大蒜", 7.0, "瓣"),
            ),
        )
        assertEquals("1 头 + 3 瓣", leftovers[0].display)
    }

    @Test
    fun `different units are not merged`() {
        val merged = mergeShoppingItems(
            listOf(
                item("牛肉", 400.0, "克"),
                item("牛肉", 1.0, "份"),
            ),
        )
        assertEquals(2, merged.size)
        assertTrue(merged.any { it.unit == "克" && it.amount == 400.0 })
        assertTrue(merged.any { it.unit == "份" && it.amount == 1.0 })
    }

    @Test
    fun `spoon aliases merge as one unit`() {
        val merged = mergeShoppingItems(
            listOf(
                item("生抽", 2.0, "勺"),
                item("生抽", 1.0, "汤匙"),
            ),
        )
        assertEquals(1, merged.size)
        assertEquals("勺", merged[0].unit)
        assertEquals("3勺", merged[0].display)
    }

    @Test
    fun `partially checked group stays unchecked`() {
        val merged = mergeShoppingItems(
            listOf(
                item("土豆", 300.0, "克", checked = true),
                item("土豆", 200.0, "克", checked = false),
            ),
        )
        assertEquals(1, merged.size)
        assertFalse(merged[0].checked)
    }

    @Test
    fun `fully checked group is checked`() {
        val merged = mergeShoppingItems(
            listOf(
                item("土豆", 300.0, "克", checked = true),
                item("土豆", 200.0, "克", checked = true),
            ),
        )
        assertEquals(1, merged.size)
        assertTrue(merged[0].checked)
    }

    @Test
    fun `missing amount stays vague`() {
        val merged = mergeShoppingItems(listOf(item("盐", null, null)))
        assertEquals(1, merged.size)
        assertEquals(null, merged[0].amount)
        assertEquals("适量", merged[0].display)
    }
}
