package com.weiji.wang.core.model

import kotlin.math.roundToInt

/**
 * 份量换算。规则：
 * - 有明确数量与单位的食材按比例缩放；
 * - “适量 / 少许 / 若干”不缩放，原样保留；
 * - 范围（2-3 勺）按区间线性缩放，保留区间形态；
 * - 结果做“厨房可读”取整：>=100 取 5 的倍数，10-100 取整，1-10 保留一位小数。
 */
object IngredientScaler {

    private val vagueWords = listOf("适量", "少许", "若干", "随意", "按需", "一点", "少量")

    fun isVague(raw: String): Boolean = vagueWords.any { raw.contains(it) }

    fun scale(amount: Double?, baseServings: Int, targetServings: Int): Double? {
        if (amount == null) return null
        if (baseServings <= 0 || targetServings <= 0) return amount
        if (baseServings == targetServings) return amount
        return amount * targetServings / baseServings
    }

    fun format(value: Double, unit: String? = null): String {
        val rounded = kitchenRound(value)
        val text = when {
            rounded >= 100 -> trimZero(rounded.roundToInt().toDouble())
            rounded >= 10 -> trimZero(rounded.roundToInt().toDouble())
            else -> trimZero((rounded * 10).roundToInt() / 10.0)
        }
        return if (unit.isNullOrBlank()) text else "$text$unit"
    }

    fun kitchenRound(value: Double): Double = when {
        value >= 100 -> (value / 5).roundToInt() * 5.0
        value >= 10 -> value.roundToInt().toDouble()
        else -> (value * 10).roundToInt() / 10.0
    }

    private fun trimZero(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

    /** 把 “2-3” / “2~3” 的区间按份量缩放，保留区间。 */
    fun scaleRange(raw: String, baseServings: Int, targetServings: Int): String? {
        val match = Regex("""^\s*(\d+(?:\.\d+)?)\s*[-~～至]\s*(\d+(?:\.\d+)?)\s*(.*)$""")
            .find(raw) ?: return null
        val a = match.groupValues[1].toDoubleOrNull() ?: return null
        val b = match.groupValues[2].toDoubleOrNull() ?: return null
        val unit = match.groupValues[3].trim()
        val sa = scale(a, baseServings, targetServings) ?: return null
        val sb = scale(b, baseServings, targetServings) ?: return null
        return "${format(sa)}-${format(sb)}$unit"
    }
}
