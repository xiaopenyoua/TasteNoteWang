package com.weiji.wang.feature.shopping

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.weiji.wang.core.data.RecipeRepository
import com.weiji.wang.core.data.ShoppingRepository
import com.weiji.wang.core.data.db.ShoppingItemEntity
import com.weiji.wang.core.model.IngredientScaler
import com.weiji.wang.core.model.MergedShoppingItem
import com.weiji.wang.core.model.mergeShoppingItems
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.CircleIconButton
import com.weiji.wang.ui.components.ConfirmDialog
import com.weiji.wang.ui.components.EmptyState
import com.weiji.wang.ui.components.FilterPill
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.SecondaryButton
import com.weiji.wang.ui.components.SectionHeader
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ShoppingUiState(
    val rawItems: List<ShoppingItemEntity> = emptyList(),
    val merged: List<MergedShoppingItem> = emptyList(),
    val recipeTitles: Map<String, String> = emptyMap(),
)

class ShoppingViewModel(
    private val shopping: ShoppingRepository,
    recipes: RecipeRepository,
) : ViewModel() {

    init {
        viewModelScope.launch { shopping.ensureList() }
    }

    val state = combine(shopping.observeItems(), recipes.observeCards()) { items, cards ->
        ShoppingUiState(
            rawItems = items,
            merged = mergeShoppingItems(items),
            recipeTitles = cards.associate { it.id to it.title },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingUiState())

    fun addManual(name: String, amount: Double?, unit: String?) {
        viewModelScope.launch { shopping.addManual(name, amount, unit) }
    }

    fun updateItem(id: String, name: String, amount: Double?, unit: String?) {
        viewModelScope.launch { shopping.updateItem(id, name, amount, unit) }
    }

    fun toggle(sources: List<ShoppingItemEntity>, checked: Boolean) {
        viewModelScope.launch { sources.forEach { shopping.toggle(it.id, checked) } }
    }

    fun delete(sources: List<ShoppingItemEntity>) {
        viewModelScope.launch { sources.forEach { shopping.delete(it.id) } }
    }

    fun clearChecked() {
        viewModelScope.launch { shopping.clearChecked() }
    }

    fun clearAll() {
        viewModelScope.launch { shopping.clearAll() }
    }
}

@Composable
fun ShoppingScreen(nav: NavHostController) {
    val container = LocalContainer.current
    val vm: ShoppingViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ShoppingViewModel(container.shoppingRepo, container.recipes) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var mergeEnabled by remember { mutableStateOf(true) }
    var showAdd by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var showClearChecked by remember { mutableStateOf(false) }
    var showClearAll by remember { mutableStateOf(false) }
    var editingRaw by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var editingMerged by remember { mutableStateOf<MergedShoppingItem?>(null) }
    var pendingAdd by remember { mutableStateOf<PendingAdd?>(null) }

    val merged = state.merged
    val rawItems = state.rawItems
    val pendingCount = if (mergeEnabled) merged.count { !it.checked } else rawItems.count { !it.checked }
    val checkedCount = if (mergeEnabled) merged.count { it.checked } else rawItems.count { it.checked }
    val mergedAway = if (mergeEnabled) (rawItems.size - merged.size).coerceAtLeast(0) else 0
    val liveMerged = editingMerged?.let { target ->
        merged.firstOrNull { it.name == target.name && it.unit == target.unit }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                PageHeader(
                    title = "采购",
                    subtitle = if (rawItems.isEmpty()) null else buildString {
                        append("还需买 $pendingCount 项 · 已买 $checkedCount 项")
                        if (mergedAway > 0) append(" · 已合并 $mergedAway 条")
                    },
                    actions = {
                        CircleIconButton(
                            icon = Icons.Filled.Add,
                            contentDescription = "添加采购项",
                            onClick = { showAdd = true },
                        )
                    },
                )
            }

            if (rawItems.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(480.dp)) {
                        EmptyState(
                            title = "采购清单是空的",
                            body = "点右上角 + 手动添加，或在菜谱详情、长按菜谱卡加入。",
                            primaryLabel = "去菜谱看看",
                            onPrimary = { nav.navigate("recipes") },
                        )
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterPill(
                            text = "合并相同食材",
                            selected = mergeEnabled,
                            onClick = { mergeEnabled = !mergeEnabled },
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { showRules = true }) {
                            Text("合并规则", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (mergeEnabled) {
                    val pending = merged.filter { !it.checked }
                    val checked = merged.filter { it.checked }
                    pending.groupBy { it.category ?: "其他" }.forEach { (category, group) ->
                        item { SectionHeader(title = category) }
                        items(group, key = { it.name + "|" + it.unit }) { item ->
                            MergedRow(
                                item = item,
                                subtitle = sourceSubtitle(item),
                                onToggle = { vm.toggle(item.sources, !item.checked) },
                                onClick = {
                                    if (item.sources.size == 1) editingRaw = item.sources.first()
                                    else editingMerged = item
                                },
                                onDelete = { vm.delete(item.sources) },
                            )
                        }
                    }
                    if (checked.isNotEmpty()) {
                        item { SectionHeader(title = "已购") }
                        items(checked, key = { it.name + "|" + it.unit }) { item ->
                            MergedRow(
                                item = item,
                                subtitle = sourceSubtitle(item),
                                onToggle = { vm.toggle(item.sources, !item.checked) },
                                onClick = {
                                    if (item.sources.size == 1) editingRaw = item.sources.first()
                                    else editingMerged = item
                                },
                                onDelete = { vm.delete(item.sources) },
                            )
                        }
                    }
                } else {
                    val pending = rawItems.filter { !it.checked }
                    val checked = rawItems.filter { it.checked }
                    pending.groupBy { it.category ?: "其他" }.forEach { (category, group) ->
                        item { SectionHeader(title = category) }
                        items(group, key = { it.id }) { item ->
                            RawRow(
                                item = item,
                                subtitle = rawSubtitle(item),
                                onToggle = { vm.toggle(listOf(item), !item.checked) },
                                onClick = { editingRaw = item },
                                onDelete = { vm.delete(listOf(item)) },
                            )
                        }
                    }
                    if (checked.isNotEmpty()) {
                        item { SectionHeader(title = "已购") }
                        items(checked, key = { it.id }) { item ->
                            RawRow(
                                item = item,
                                subtitle = rawSubtitle(item),
                                onToggle = { vm.toggle(listOf(item), !item.checked) },
                                onClick = { editingRaw = item },
                                onDelete = { vm.delete(listOf(item)) },
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SecondaryButton(
                                text = "清除已购",
                                onClick = { showClearChecked = true },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SecondaryButton(
                                text = "清空清单",
                                onClick = { showClearAll = true },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddItemSheet(
            onDismiss = { showAdd = false },
            onAdd = { name, amount, unit ->
                val exists = state.rawItems.any {
                    it.name.trim() == name && it.unit.orEmpty().trim() == unit.orEmpty().trim()
                }
                if (exists) {
                    pendingAdd = PendingAdd(name, amount, unit)
                } else {
                    vm.addManual(name, amount, unit)
                }
                showAdd = false
            },
        )
    }

    editingRaw?.let { item ->
        EditItemSheet(
            item = item,
            onDismiss = { editingRaw = null },
            onSave = { name, amount, unit ->
                vm.updateItem(item.id, name, amount, unit)
                editingRaw = null
                Toast.makeText(context, "已保存，这一项已与菜谱脱离", Toast.LENGTH_SHORT).show()
            },
        )
    }

    liveMerged?.let { item ->
        MergedItemSheet(
            merged = item,
            recipeTitles = state.recipeTitles,
            onDismiss = { editingMerged = null },
            onSaveSource = { id, name, amount, unit -> vm.updateItem(id, name, amount, unit) },
        )
    }

    pendingAdd?.let { add ->
        ConfirmDialog(
            title = "已有同名食材",
            body = "采购清单里已有「${add.name}」，仍要添加？添加后会按同单位合并显示。",
            confirmLabel = "仍要添加",
            destructive = false,
            onDismiss = { pendingAdd = null },
            onConfirm = {
                vm.addManual(add.name, add.amount, add.unit)
                pendingAdd = null
            },
        )
    }

    if (showRules) {
        AlertDialog(
            onDismissRequest = { showRules = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("合并规则") },
            text = {
                Text(
                    text = "只做读取时合并，不改原始条目：\n\n" +
                        "· 蒜 / 大蒜 / 蒜瓣 → 大蒜\n" +
                        "· 葱 / 小葱 → 小葱；蛋 / 鸡蛋 → 鸡蛋\n" +
                        "· 同单位直接相加，例如克 + 克、勺 + 勺\n" +
                        "· 大蒜 10 瓣 ≈ 1 头，显示为「1 头」或「1 头 + 2 瓣」\n" +
                        "· 勺 / 汤匙视为同一单位；不把克与个、克与勺互转\n\n" +
                        "关闭「合并相同食材」可看原始条目。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { showRules = false }) { Text("知道了") }
            },
        )
    }

    if (showClearChecked) {
        ConfirmDialog(
            title = "清除已购？",
            body = "所有已勾选的采购项都会被删除。",
            confirmLabel = "清除",
            onDismiss = { showClearChecked = false },
            onConfirm = {
                vm.clearChecked()
                showClearChecked = false
            },
        )
    }

    if (showClearAll) {
        ConfirmDialog(
            title = "清空采购清单？",
            body = "所有条目都会被删除，包括还没买的和已买的。",
            confirmLabel = "清空",
            onDismiss = { showClearAll = false },
            onConfirm = {
                vm.clearAll()
                showClearAll = false
            },
        )
    }
}

@Composable
private fun MergedRow(
    item: MergedShoppingItem,
    subtitle: String,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val partiallyChecked = !item.checked && item.sources.any { it.checked }
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(
            checked = item.checked,
            partial = partiallyChecked,
            onToggle = onToggle,
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                color = if (item.checked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.display,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        DeleteButton(onDelete)
    }
}

@Composable
private fun RawRow(
    item: ShoppingItemEntity,
    subtitle: String,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(checked = item.checked, partial = false, onToggle = onToggle)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                color = if (item.checked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = displayAmount(item),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        DeleteButton(onDelete)
    }
}

@Composable
private fun CheckCircle(checked: Boolean, partial: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (checked) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        when {
            checked -> Icon(
                Icons.Filled.Check,
                contentDescription = "已买",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(15.dp),
            )
            partial -> Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

@Composable
private fun DeleteButton(onDelete: () -> Unit) {
    Icon(
        Icons.Filled.Delete,
        contentDescription = "删除",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onDelete)
            .padding(8.dp),
    )
}

private data class PendingAdd(val name: String, val amount: Double?, val unit: String?)

private fun displayAmount(item: ShoppingItemEntity): String {
    val amount = item.amount ?: return "适量"
    return IngredientScaler.format(amount, item.unit)
}

private fun rawSubtitle(item: ShoppingItemEntity): String {
    val recipeId = item.recipeId
    if (recipeId == null) return "手动添加"
    return item.sourceDate?.let(::menuDate).orEmpty().ifBlank { "来自菜谱" }
}

private fun sourceSubtitle(item: MergedShoppingItem): String {
    val recipeSources = item.sources.filter { it.recipeId != null }
    if (recipeSources.isEmpty()) return "手动添加"
    val count = recipeSources.mapNotNull { it.recipeId }.distinct().size
    val date = recipeSources.firstNotNullOfOrNull { it.sourceDate }?.let(::menuDate)
    val manual = item.sources.any { it.recipeId == null }
    val base = buildString {
        if (date != null) append(date)
        if (count > 0) {
            if (date != null) append(" · ")
            append("$count 道菜谱")
        }
    }.ifBlank { "来自菜谱" }
    return if (manual) "$base + 手动" else base
}

private fun menuDate(key: String): String = runCatching {
    val date = LocalDate.parse(key)
    "${date.monthValue}月${date.dayOfMonth}日菜单"
}.getOrElse { "往期菜单" }
