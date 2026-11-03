package com.weiji.wang.feature.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.weiji.wang.core.data.db.ShoppingItemEntity
import com.weiji.wang.core.model.IngredientScaler
import com.weiji.wang.core.model.MergedShoppingItem
import com.weiji.wang.ui.components.Hairline
import com.weiji.wang.ui.components.PrimaryButton
import com.weiji.wang.ui.components.SecondaryButton
import java.time.LocalDate

/** 「添加采购项」底部 Sheet：名称 / 数量 / 单位三字段。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, amount: Double?, unit: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        SheetBody(
            title = "添加采购项",
            note = "食材与规格分开填；只加入采购清单，不改任何菜谱。",
        ) {
            ItemFields(
                name = name,
                amountText = amountText,
                unit = unit,
                onName = { name = it },
                onAmount = { amountText = it },
                onUnit = { unit = it },
            )
            Hint("数量可写 2、2-3、适量；单位可空，模糊量时单位不生效。")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SecondaryButton(text = "取消", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                }
                Box(modifier = Modifier.weight(1f)) {
                    PrimaryButton(
                        text = "添加",
                        enabled = name.isNotBlank(),
                        onClick = { onAdd(name.trim(), amountOf(amountText), unitOf(amountText, unit)) },
                    )
                }
            }
        }
    }
}

/** 「编辑采购项」底部 Sheet：保存后该采购项与菜谱脱离。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemSheet(
    item: ShoppingItemEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Double?, unit: String?) -> Unit,
) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var amountText by remember(item.id) { mutableStateOf(amountTextOf(item.amount)) }
    var unit by remember(item.id) { mutableStateOf(item.unit.orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        SheetBody(
            title = "编辑采购项",
            note = "只改这次采购；保存后这一项与菜谱脱离。",
        ) {
            ItemFields(
                name = name,
                amountText = amountText,
                unit = unit,
                onName = { name = it },
                onAmount = { amountText = it },
                onUnit = { unit = it },
            )
            Hint("数量可写 2、2-3、适量；单位可空，模糊量时单位不生效。")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SecondaryButton(text = "取消", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                }
                Box(modifier = Modifier.weight(1f)) {
                    PrimaryButton(
                        text = "保存",
                        enabled = name.isNotBlank(),
                        onClick = { onSave(name.trim(), amountOf(amountText), unitOf(amountText, unit)) },
                    )
                }
            }
        }
    }
}

/** 合并行的来源编辑器：逐条修改，底部显示合并后的展示值。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergedItemSheet(
    merged: MergedShoppingItem,
    recipeTitles: Map<String, String>,
    onDismiss: () -> Unit,
    onSaveSource: (id: String, name: String, amount: Double?, unit: String?) -> Unit,
) {
    var editingId by remember { mutableStateOf<String?>(null) }
    var editName by remember { mutableStateOf("") }
    var editAmount by remember { mutableStateOf("") }
    var editUnit by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        SheetBody(
            title = "编辑「${merged.name}」",
            note = "合并了 ${merged.sources.size} 个来源，按来源逐条改；菜谱来源保存后与菜谱脱离。",
        ) {
            merged.sources.forEach { source ->
                if (editingId == source.id) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ItemFields(
                            name = editName,
                            amountText = editAmount,
                            unit = editUnit,
                            onName = { editName = it },
                            onAmount = { editAmount = it },
                            onUnit = { editUnit = it },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { editingId = null }) {
                                Text("取消")
                            }
                            TextButton(
                                onClick = {
                                    onSaveSource(
                                        source.id,
                                        editName.trim(),
                                        amountOf(editAmount),
                                        unitOf(editAmount, editUnit),
                                    )
                                    editingId = null
                                },
                                enabled = editName.isNotBlank(),
                            ) {
                                Text("保存这一条", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sourceTitle(source, recipeTitles),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = amountLabel(source),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = {
                                editingId = source.id
                                editName = source.name
                                editAmount = amountTextOf(source.amount)
                                editUnit = source.unit.orEmpty()
                            },
                        ) {
                            Text("编辑", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Hairline()
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "合并显示 ${merged.display}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            PrimaryButton(text = "完成", onClick = onDismiss)
        }
    }
}

@Composable
private fun SheetBody(
    title: String,
    note: String,
    content: @Composable () -> Unit,
) {
    val maxWidth = if (LocalConfiguration.current.screenWidthDp >= 600) 560.dp else null
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (maxWidth != null) Modifier.width(maxWidth) else Modifier)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .imePadding(),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ItemFields(
    name: String,
    amountText: String,
    unit: String,
    onName: (String) -> Unit,
    onAmount: (String) -> Unit,
    onUnit: (String) -> Unit,
) {
    val vague = isVagueAmount(amountText)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            placeholder = { Text("食材", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = itemFieldColors(),
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = amountText,
            onValueChange = onAmount,
            placeholder = { Text("数量", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false) },
            singleLine = true,
            modifier = Modifier.width(72.dp),
            shape = RoundedCornerShape(12.dp),
            colors = itemFieldColors(),
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = unit,
            onValueChange = onUnit,
            placeholder = { Text("单位", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false) },
            singleLine = true,
            enabled = !vague,
            modifier = Modifier.width(60.dp),
            shape = RoundedCornerShape(12.dp),
            colors = itemFieldColors(),
            textStyle = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun itemFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
    errorBorderColor = MaterialTheme.colorScheme.error,
)

@Composable
private fun Hint(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.tertiary,
    )
    Spacer(Modifier.height(16.dp))
}

private fun sourceTitle(source: ShoppingItemEntity, recipeTitles: Map<String, String>): String {
    val recipeId = source.recipeId ?: return "手动添加"
    return recipeTitles[recipeId]
        ?: source.sourceDate?.let(::menuDate).orEmpty().ifBlank { "来自菜谱" }
}

private fun menuDate(key: String): String = runCatching {
    val date = LocalDate.parse(key)
    "${date.monthValue}月${date.dayOfMonth}日菜单"
}.getOrElse { "往期菜单" }

private fun amountLabel(source: ShoppingItemEntity): String {
    val amount = source.amount ?: return "适量"
    return IngredientScaler.format(amount, source.unit)
}

internal fun amountTextOf(amount: Double?): String {
    if (amount == null) return ""
    return if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()
}

internal fun amountOf(text: String): Double? {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || IngredientScaler.isVague(trimmed)) return null
    trimmed.toDoubleOrNull()?.let { return it }
    val match = Regex("""(\d+(?:\.\d+)?)\s*[-~～至]\s*(\d+(?:\.\d+)?)""").find(trimmed)
        ?: return null
    val first = match.groupValues[1].toDoubleOrNull() ?: return null
    val second = match.groupValues[2].toDoubleOrNull() ?: return null
    return (first + second) / 2.0
}

internal fun unitOf(amountText: String, unit: String): String? {
    if (amountOf(amountText) == null) return null
    return unit.trim().takeIf { it.isNotEmpty() }
}

private fun isVagueAmount(text: String): Boolean =
    text.isNotBlank() && (IngredientScaler.isVague(text) || amountOf(text) == null)
