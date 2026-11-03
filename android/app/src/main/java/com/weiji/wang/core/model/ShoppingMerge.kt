package com.weiji.wang.core.model

import com.weiji.wang.core.data.db.ShoppingItemEntity
import kotlin.math.abs

/**
 * 采购清单的读取时合并：输入原始采购行，输出展示行，不改数据库。
 * 只做保守换算（大蒜 10 瓣 ≈ 1 头；勺/汤匙视为同一单位），其余单位不猜测。
 */
data class MergedShoppingItem(
    val name: String,
    val unit: String,
    val amount: Double?,
    val display: String,
    val checked: Boolean,
    val category: String?,
    val sources: List<ShoppingItemEntity>,
)

fun mergeShoppingItems(items: List<ShoppingItemEntity>): List<MergedShoppingItem> {
    val groups = LinkedHashMap<Pair<String, String>, MutableList<ShoppingItemEntity>>()
    items.forEach { item ->
        val key = normalizeName(item.name) to normalizeUnit(item.unit)
        groups.getOrPut(key) { mutableListOf() }.add(item)
    }
    return groups.map { (key, sources) ->
        val name = key.first
        val unit = key.second
        val numeric = sources.mapNotNull { it.amount }
        val total = numeric.takeIf { it.isNotEmpty() }?.sum()
        MergedShoppingItem(
            name = name,
            unit = unit,
            amount = total,
            display = displayFor(name, unit, total),
            checked = sources.all { it.checked },
            category = sources.firstNotNullOfOrNull { it.category },
            sources = sources,
        )
    }
}

private val nameAliases = mapOf(
    "蒜" to "大蒜",
    "大蒜" to "大蒜",
    "蒜瓣" to "大蒜",
    "葱" to "小葱",
    "小葱" to "小葱",
    "鸡蛋" to "鸡蛋",
    "蛋" to "鸡蛋",
    "生抽" to "生抽",
    "酱油" to "生抽",
)

private fun normalizeName(raw: String): String {
    val name = raw.trim()
    return nameAliases[name] ?: name
}

private fun normalizeUnit(raw: String?): String = when (val unit = raw?.trim().orEmpty()) {
    "勺", "汤匙" -> "勺"
    else -> unit
}

private fun displayFor(name: String, unit: String, amount: Double?): String {
    if (amount == null) return "适量"
    if (name == "大蒜" && unit == "瓣") {
        if (amount < 10) return "${formatAmount(amount)} 瓣"
        val heads = (amount / 10).toInt()
        val rest = amount - heads * 10
        return if (abs(rest) < 1e-9) {
            "$heads 头"
        } else {
            "$heads 头 + ${formatAmount(rest)} 瓣"
        }
    }
    val text = formatAmount(amount)
    return if (unit.isBlank()) text else "$text$unit"
}

private fun formatAmount(value: Double): String = IngredientScaler.format(value)
