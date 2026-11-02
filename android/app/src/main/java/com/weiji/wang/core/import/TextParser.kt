package com.weiji.wang.core.import

data class ParsedIngredient(
    val name: String,
    val amount: Double? = null,
    val unit: String? = null,
    val rawText: String,
    val group: String? = null,
    val vague: Boolean = false,
)

data class ParsedRecipe(
    val title: String,
    val servings: Int,
    val ingredients: List<ParsedIngredient>,
    val steps: List<String>,
    val notes: String,
    val confidence: Float,
)

/**
 * 把小红书正文 / 抖音文案 / B站简介切成食材与步骤。
 * 目标是“给出可编辑的草稿”，不是完美解析：宁可少切，也不要把错误结构当成事实。
 */
object TextParser {

    private val ingredientHeaders = listOf(
        "食材", "用料", "配料", "材料", "主料", "辅料", "调料", "酱汁", "腌料", "调味",
    )
    private val stepHeaders = listOf(
        "步骤", "做法", "制作", "教程", "流程", "烹饪过程", "操作",
    )
    private val unitWords = listOf(
        "克", "千克", "公斤", "g", "kg", "毫升", "ml", "升", "l", "勺", "汤匙", "茶匙",
        "个", "颗", "瓣", "片", "根", "段", "只", "斤", "两", "杯", "碗", "把", "块", "条", "张",
        "适量", "少许", "若干", "适量左右",
    )
    private val unitRegex = Regex(
        """(克|千克|公斤|g|kg|毫升|ml|升|l|勺|汤匙|茶匙|个|颗|瓣|片|根|段|只|斤|两|杯|碗|把|块|条|张|适量|少许|若干)""",
        RegexOption.IGNORE_CASE,
    )
    private val numberRange = Regex("""^(\d+(?:\.\d+)?)\s*[-~～至]\s*(\d+(?:\.\d+)?)$""")
    private val numberedStep = Regex("""^\s*(?:第[一二三四五六七八九十\d]+步[：:.、]?|(\d{1,2})[.、)）:：]|[①②③④⑤⑥⑦⑧⑨⑩])\s*(.+)$""")

    fun parse(
        title: String,
        body: String,
        author: String? = null,
        fallbackTitle: String? = null,
    ): ParsedRecipe {
        val text = stripSocialNoise(body).replace("\r\n", "\n").replace('\r', '\n').trim()
        val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

        val servings = detectServings(text) ?: 2
        val titleOut = title.trim().ifBlank {
            fallbackTitle ?: lines.firstOrNull()?.take(40) ?: "未命名菜谱"
        }

        val sections = splitSections(lines)
        val ingredientLines = sections.ingredients.ifEmpty {
            lines.takeWhile { looksLikeIngredient(it) }.take(20).map { null as String? to it }
        }
        val ingredientGrouped = groupIngredients(ingredientLines)
        val ingredients = ingredientGrouped.mapNotNull { parseIngredient(it.first, it.second) }

        val stepLines = sections.steps.ifEmpty { lines.filter { numberedStep.find(it) != null } }
        val steps = parseSteps(stepLines)

        val notes = buildNotes(lines, sections, ingredients, steps)

        val confidence = when {
            ingredients.size >= 3 && steps.size >= 2 -> 0.9f
            ingredients.isNotEmpty() && steps.isNotEmpty() -> 0.65f
            ingredients.isNotEmpty() || steps.isNotEmpty() -> 0.4f
            else -> 0.15f
        }
        return ParsedRecipe(titleOut, servings, ingredients, steps, notes, confidence)
    }

    private data class Sections(
        val ingredients: List<Pair<String?, String>>,
        val steps: List<String>,
    )

    private fun splitSections(lines: List<String>): Sections {
        val ingredientLines = mutableListOf<Pair<String?, String>>()
        val stepLines = mutableListOf<String>()
        var mode = 0 // 0 none, 1 ingredient, 2 step
        var group: String? = null

        for (raw in lines) {
            val line = raw.trim(' ', '-', '—', '·')
            if (line.isEmpty()) continue
            // “酱汁：1勺生抽➕1勺蚝油”这类一行多料的写法
            val inlineGroup = Regex(
                """^(酱汁|腌料|调料|调味汁|料汁|碗汁|食材|用料|配料|主料|辅料)[：:]\s*(.+)$""",
            ).find(line)
            if (inlineGroup != null) {
                mode = 1
                group = inlineGroup.groupValues[1]
                inlineGroup.groupValues[2]
                    .split(Regex("""[➕+＋、；;]"""))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { ingredientLines += group to it }
                continue
            }
            val header = detectHeader(line)
            if (header != null) {
                mode = header.first
                group = header.second
                continue
            }
            when (mode) {
                1 -> if (line.length <= 60) ingredientLines += group to line
                2 -> stepLines += line
                else -> Unit
            }
        }

        // 没有显式“步骤”标题时，只认编号行，避免把正文段落/标签当成步骤
        if (stepLines.isEmpty()) {
            lines.mapNotNull { linen -> numberedStep.find(linen)?.groupValues?.get(2)?.trim() }
                .filter { it.length >= 2 }
                .forEach { stepLines += it }
        }
        return Sections(ingredientLines, stepLines)
    }

    /** @return (mode, groupName) */
    private fun detectHeader(line: String): Pair<Int, String?>? {
        val stripped = line.trim(' ', '：', ':', '#', '*', '【', '】')
        if (stripped.length > 20) return null
        if (stepHeaders.any { stripped.startsWith(it) }) return 2 to null
        if (ingredientHeaders.any { stripped.startsWith(it) }) {
            val rest = ingredientHeaders.firstOrNull { stripped.startsWith(it) }
                ?.let { stripped.removePrefix(it) }
                ?.trim(' ', '：', ':', '（', '）', '(', ')')
            return 1 to rest?.takeIf { it.isNotBlank() && it.length <= 10 }
        }
        return null
    }

    private fun groupIngredients(lines: List<Pair<String?, String>>): List<Pair<String?, String>> {
        var current: String? = null
        val out = mutableListOf<Pair<String?, String>>()
        for ((group, line) in lines) {
            if (group != null) current = group
            // 以冒号结尾的短行可能是子分组，例如“腌料：”
            if (line.endsWith("：") || line.endsWith(":") || line.endsWith("酱汁")) {
                current = line.trim(' ', '：', ':')
                continue
            }
            out += current to line
        }
        return out
    }

    fun looksLikeIngredient(line: String): Boolean {
        if (line.length > 60) return false
        if (line.startsWith("步骤") || line.startsWith("做法")) return false
        val hasUnit = unitRegex.containsMatchIn(line)
        val hasSeparator = line.contains('：') || line.contains(':')
        val hasNumber = Regex("""\d""").containsMatchIn(line)
        return (hasUnit && hasNumber) || (hasSeparator && line.length <= 40) ||
            Regex("""^[^：:]{1,12}[：:].{1,30}$""").matches(line)
    }

    fun parseIngredient(group: String?, raw: String): ParsedIngredient? {
        var line = raw.trim().trimStart('-', '•', '·', '*')
        line = line.replace(Regex("""^[（(]\d+[）)]"""), "").trim()
        if (line.length < 1) return null

        // 形式零：1勺生抽 / 2个土豆 / 2-3勺生抽（数量在前，支持区间）
        val amountFirst = Regex(
            """^(\d+(?:\.\d+)?)(?:\s*[-~～至]\s*(\d+(?:\.\d+)?))?\s*(勺|汤匙|茶匙|克|g|kg|毫升|ml|升|l|个|颗|瓣|片|根|段|只|斤|两|杯|碗|把|块|条|张)?\s*(.{1,20})$""",
            RegexOption.IGNORE_CASE,
        ).find(line)
        if (amountFirst != null) {
            val first = amountFirst.groupValues[1].toDoubleOrNull()
            val second = amountFirst.groupValues[2].toDoubleOrNull()
            val amount = when {
                first == null -> null
                second != null -> (first + second) / 2.0
                else -> first
            }
            val unit = amountFirst.groupValues[3].trim().ifBlank { null }
            val name = amountFirst.groupValues[4].trim().trim('，', ',', '。')
            if (name.isNotEmpty()) {
                return ParsedIngredient(
                    name = name,
                    amount = amount,
                    unit = unit,
                    rawText = raw,
                    group = group,
                    vague = amount == null,
                )
            }
        }

        // 形式零·二：适量水 / 少许盐 / 适量水搅拌均匀即可
        val vagueHead = Regex(
            """^(适量|少许|若干|少量)\s*([^\s，。、]{1,4}?)(?=搅拌|煮|烧|炒|切|翻|拌|混合|均匀|即可|$)""",
        ).find(line)
        if (vagueHead != null) {
            return ParsedIngredient(
                name = vagueHead.groupValues[2].trim(),
                rawText = raw,
                group = group,
                vague = true,
            )
        }

        // 形式一：名称：数量单位
        val colon = Regex("""^([^：:]{1,20})[：:]\s*(.{1,40})$""").find(line)
        if (colon != null) {
            val name = colon.groupValues[1].trim()
            val rest = colon.groupValues[2].trim()
            val (amount, unit) = parseAmount(rest)
            return ParsedIngredient(
                name = name,
                amount = amount,
                unit = unit,
                rawText = raw,
                group = group,
                vague = amount == null && IngredientScalerIsVague(rest),
            )
        }

        // 形式二：名称 数量单位
        val spaced = Regex("""^(.{1,24}?)\s+(\d+(?:\.\d+)?(?:\s*[-~～至]\s*\d+(?:\.\d+)?)?)\s*([^\s\d]{0,6})$""")
            .find(line)
        if (spaced != null) {
            val name = spaced.groupValues[1].trim()
            val numText = spaced.groupValues[2].replace(" ", "")
            val unit = spaced.groupValues[3].trim().ifBlank { null }
            val range = numberRange.find(numText)
            val amount = if (range != null) {
                (range.groupValues[1].toDouble() + range.groupValues[2].toDouble()) / 2.0
            } else {
                numText.toDoubleOrNull()
            }
            return ParsedIngredient(
                name = name, amount = amount, unit = unit, rawText = raw, group = group,
                vague = amount == null,
            )
        }

        // 形式三：名称（没有数量）：只保留名称
        if (line.length <= 30 && !line.contains('。')) {
            return ParsedIngredient(
                name = line.trim(' ', '，', ',', '。'),
                rawText = raw,
                group = group,
                vague = IngredientScalerIsVague(line) || parseAmount(line).first == null,
            )
        }
        return null
    }

    private fun IngredientScalerIsVague(text: String): Boolean =
        listOf("适量", "少许", "若干", "随意", "按需", "少量").any { text.contains(it) }

    private fun parseAmount(text: String): Pair<Double?, String?> {
        val t = text.trim()
            .removePrefix("约")
            .removePrefix("大约")
            .removeSuffix("左右")
            .trim()
        val range = Regex("""(\d+(?:\.\d+)?)\s*[-~～至]\s*(\d+(?:\.\d+)?)\s*([^\s]*)""").find(t)
        if (range != null) {
            val avg = (range.groupValues[1].toDouble() + range.groupValues[2].toDouble()) / 2.0
            return avg to range.groupValues[3].ifBlank { null }
        }
        val m = Regex("""^(\d+(?:\.\d+)?)\s*(.*)$""").find(t)
        if (m != null) {
            val unit = m.groupValues[2].trim().ifBlank { null }
            return m.groupValues[1].toDouble() to unit
        }
        // “一勺”“两勺”等中文数字
        val cn = Regex("""^([一二三四五六七八九十两]+)\s*(勺|汤匙|茶匙|个|颗|瓣|片|根|段|只|杯|碗|把|块|条|张)$""").find(t)
        if (cn != null) {
            val n = cnNumber(cn.groupValues[1])
            if (n != null) return n.toDouble() to cn.groupValues[2]
        }
        return null to null
    }

    private fun cnNumber(s: String): Int? {
        val map = mapOf('一' to 1, '二' to 2, '两' to 2, '三' to 3, '四' to 4, '五' to 5,
            '六' to 6, '七' to 7, '八' to 8, '九' to 9, '十' to 10)
        return when {
            s == "十" -> 10
            s.length == 1 -> map[s[0]]
            s.length == 2 && s[0] == '十' -> 10 + (map[s[1]] ?: return null)
            s.length == 2 && s[1] == '十' -> (map[s[0]] ?: return null) * 10
            else -> null
        }
    }

    private fun parseSteps(lines: List<String>): List<String> {
        val out = mutableListOf<String>()
        for (raw in lines) {
            val m = numberedStep.find(raw)
            val text = (m?.groupValues?.get(2) ?: raw).trim()
                .trim('-', '•', '·', '*', ' ')
            if (text.length < 2) continue
            if (looksLikeIngredient(text) && out.isEmpty()) continue
            out += text.take(300)
        }
        return out.distinct().take(30)
    }

    private fun stripSocialNoise(text: String): String = text
        .replace(Regex("""#[^#\s]{1,30}\[话题\]#"""), " ")
        .replace(Regex("""#[^\s#]{1,30}"""), " ")
        .replace(Regex("""@[^\s@]{1,30}"""), " ")
        .replace(Regex("""\[[^\[\]]{1,8}\]"""), " ")
        .replace(Regex("""[ \t]{2,}"""), " ")

    private fun buildNotes(
        lines: List<String>,
        sections: Sections,
        ingredients: List<ParsedIngredient>,
        steps: List<String>,
    ): String {
        val joined = lines.joinToString("\n")
        val leftover = lines.filter { line ->
            !looksLikeIngredient(line) &&
                numberedStep.find(line) == null &&
                detectHeader(line) == null &&
                steps.none { it == line } &&
                line.length > 12
        }
        return if (leftover.isEmpty()) "" else leftover.take(8).joinToString("\n").take(1200)
    }

    private fun detectServings(text: String): Int? {
        Regex("""(\d)\s*[-~～至]\s*(\d)\s*(?:人|人份|位)""").find(text)?.let {
            return it.groupValues[2].toIntOrNull()?.coerceIn(1, 12)
        }
        Regex("""(\d)\s*(?:人份|人|位)""").find(text)?.let {
            return it.groupValues[1].toIntOrNull()?.coerceIn(1, 12)
        }
        return null
    }
}
