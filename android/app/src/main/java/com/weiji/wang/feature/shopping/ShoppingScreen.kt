package com.weiji.wang.feature.shopping

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
import androidx.compose.material3.OutlinedTextField
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
import com.weiji.wang.core.data.db.ShoppingItemEntity
import com.weiji.wang.core.model.Ulid
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.EmptyState
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.PrimaryButton
import com.weiji.wang.ui.components.SecondaryButton
import com.weiji.wang.ui.components.SectionHeader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(private val repo: RecipeRepository) : ViewModel() {
    private val listId = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            listId.value = repo.ensureShoppingList().id
        }
    }

    val items = listId.flatMapLatest { id ->
        if (id == null) kotlinx.coroutines.flow.flowOf(emptyList()) else repo.observeShoppingItems(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(item: ShoppingItemEntity) {
        viewModelScope.launch { repo.toggleShoppingItem(item.id, !item.checked) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repo.deleteShoppingItem(id) }
    }

    fun add(name: String) {
        val id = listId.value ?: return
        viewModelScope.launch { repo.addShoppingItem(id, name, null, null) }
    }

    fun clearChecked() {
        val id = listId.value ?: return
        viewModelScope.launch { repo.clearChecked(id) }
    }

    fun clearAll() {
        val id = listId.value ?: return
        viewModelScope.launch { repo.clearShoppingList(id) }
    }
}

@Composable
fun ShoppingScreen(nav: NavHostController) {
    val container = LocalContainer.current
    val vm: ShoppingViewModel = viewModel(
        factory = viewModelFactory { initializer { ShoppingViewModel(container.recipes) } },
    )
    val items by vm.items.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    val pending = items.filter { !it.checked }
    val checked = items.filter { it.checked }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            PageHeader(
                title = "采购",
                subtitle = if (items.isEmpty()) null else "还需买 ${pending.size} 项 · 已买 ${checked.size} 项",
                actions = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { showAdd = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "添加")
                    }
                },
            )
        }

        if (items.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(480.dp)) {
                    EmptyState(
                        title = "采购清单是空的",
                        body = "在菜谱详情点「全部加入采购清单」，或在右上角手动添加。",
                        primaryLabel = "去菜谱看看",
                        onPrimary = { nav.navigate("recipes") },
                    )
                }
            }
        } else {
            pending.groupBy { it.category ?: "其他" }.forEach { (category, group) ->
                item {
                    SectionHeader(title = category)
                }
                items(group, key = { it.id }) { item ->
                    ShoppingRow(item, onToggle = { vm.toggle(item) }, onDelete = { vm.delete(item.id) })
                }
            }
            if (checked.isNotEmpty()) {
                item { SectionHeader(title = "已购") }
                items(checked, key = { it.id }) { item ->
                    ShoppingRow(item, onToggle = { vm.toggle(item) }, onDelete = { vm.delete(item.id) })
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SecondaryButton("清除已购", onClick = vm::clearChecked)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SecondaryButton("清空清单", onClick = { showClear = true })
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddItemDialog(
            onDismiss = { showAdd = false },
            onAdd = { vm.add(it); showAdd = false },
        )
    }
    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("清空采购清单？") },
            text = { Text("所有条目都会被删除。") },
            confirmButton = {
                TextButton(onClick = { vm.clearAll(); showClear = false }) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ShoppingRow(item: ShoppingItemEntity, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (item.checked) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.surfaceVariant,
                )
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (item.checked) {
                Icon(Icons.Filled.Check, contentDescription = "已买", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(15.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                color = if (item.checked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            )
            if (item.amount != null) {
                Text(
                    text = "${if (item.amount % 1.0 == 0.0) item.amount.toInt() else item.amount}${item.unit.orEmpty()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.Filled.Delete,
            contentDescription = "删除",
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .size(22.dp)
                .clickable(onClick = onDelete),
        )
    }
}

@Composable
private fun AddItemDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加一项") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("名称，例如 牛腩 500g") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { if (value.isNotBlank()) onAdd(value) }) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
