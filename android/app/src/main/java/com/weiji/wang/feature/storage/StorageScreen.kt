package com.weiji.wang.feature.storage

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.weiji.wang.core.AppContainer
import com.weiji.wang.core.data.db.WeijiDatabase
import com.weiji.wang.core.storage.RecipeSnapshot
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.CircleIconButton
import com.weiji.wang.ui.components.ConfirmDialog
import com.weiji.wang.ui.components.Hairline
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.RecipeCover
import com.weiji.wang.ui.components.SecondaryButton
import com.weiji.wang.ui.components.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class StorageStats(
    val totalBytes: Long = 0,
    val videoBytes: Long = 0,
    val imageBytes: Long = 0,
    val historyBytes: Long = 0,
    val databaseBytes: Long = 0,
    val tempBytes: Long = 0,
    val freeBytes: Long = 0,
)

data class RecipeStorageRow(
    val id: String,
    val title: String,
    val coverPath: String?,
    val bytes: Long,
    val hasVideo: Boolean,
    val imageCount: Int,
)

data class StorageUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val stats: StorageStats = StorageStats(),
    val recipes: List<RecipeStorageRow> = emptyList(),
)

class StorageViewModel(private val container: AppContainer) : ViewModel() {

    var ui by mutableStateOf(StorageUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        if (ui.loading && ui.stats.totalBytes > 0) return
        ui = ui.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { compute() }
                .onSuccess { ui = StorageUiState(loading = false, stats = it.first, recipes = it.second) }
                .onFailure {
                    ui = ui.copy(loading = false, error = it.message ?: "读取失败")
                }
        }
    }

    private suspend fun compute(): Pair<StorageStats, List<RecipeStorageRow>> = withContext(Dispatchers.IO) {
        val files = container.files
        val rootFiles = files.root.walkTopDown().filter { it.isFile }.toList()
        val recipeMediaFiles = files.recipesDir.walkTopDown().filter { it.isFile }.toList()
        val videoBytes = recipeMediaFiles.filter { it.extension.lowercase() in videoExtensions }.sumOf { it.length() }
        val imageBytes = recipeMediaFiles.filter { it.extension.lowercase() in imageExtensions }.sumOf { it.length() }
        val historyBytes = files.historyDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val databaseBytes = databaseFiles().sumOf { it.length() }
        val tempBytes = cacheBytes()
        val rootBytes = rootFiles.sumOf { it.length() }
        val stats = StorageStats(
            totalBytes = rootBytes + databaseBytes + tempBytes,
            videoBytes = videoBytes,
            imageBytes = imageBytes,
            historyBytes = historyBytes,
            databaseBytes = databaseBytes,
            tempBytes = tempBytes,
            freeBytes = files.freeBytes(),
        )
        val rows = container.database.recipeDao().all().map { recipe ->
            val dir = files.recipeDir(recipe.id)
            val recipeFiles = dir.listFiles()?.filter { it.isFile }.orEmpty()
            RecipeStorageRow(
                id = recipe.id,
                title = recipe.title,
                coverPath = recipe.coverPath,
                bytes = recipeFiles.sumOf { it.length() },
                hasVideo = recipeFiles.any { it.extension.lowercase() in videoExtensions },
                imageCount = recipeFiles.count { it.extension.lowercase() in imageExtensions },
            )
        }.sortedByDescending { it.bytes }
        stats to rows
    }

    private fun databaseFiles(): List<File> {
        val base = container.appContext.getDatabasePath(WeijiDatabase.NAME)
        return listOf(base, File(base.path + "-wal"), File(base.path + "-shm")).filter { it.exists() }
    }

    private fun cacheBytes(): Long {
        val cache = container.appContext.cacheDir
        return cache.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun cleanRecipeMedia(recipeId: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val details = container.database.recipeDao().details(recipeId)
                        ?: error("菜谱不存在")
                    val dir = container.files.recipeDir(recipeId)
                    val keep = setOf(RecipeSnapshot.FILE_NAME, "recipe.md")
                    dir.listFiles()
                        ?.filter { it.isFile && it.name !in keep && !it.name.endsWith(".part") }
                        ?.forEach { it.delete() }
                    container.recipes.saveRecipe(
                        recipe = details.recipe.copy(coverPath = null),
                        ingredientList = details.ingredients,
                        stepList = details.steps.map { it.copy(mediaPath = null) },
                        source = details.source,
                        mediaList = emptyList(),
                    )
                }
            }.onSuccess {
                refresh()
                onDone("已清理这道菜的媒体")
            }.onFailure { onDone("清理失败：${it.message}") }
        }
    }

    fun cleanTemp(onDone: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { container.files.cleanTempFiles(olderThanMs = 0) }
            }.onSuccess {
                refresh()
                onDone("已清理临时文件")
            }.onFailure { onDone("清理失败：${it.message}") }
        }
    }

    fun clearAllData(onDone: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    container.database.clearAllTables()
                    container.files.recipesDir.listFiles()?.forEach { it.deleteRecursively() }
                    container.files.historyDir.listFiles()?.forEach { it.deleteRecursively() }
                    container.files.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                }
            }.onSuccess {
                refresh()
                onDone("已清空本机数据")
            }.onFailure { onDone("清空失败：${it.message}") }
        }
    }

    private val videoExtensions = setOf("mp4", "mov", "m4v", "webm", "mkv", "avi")
    private val imageExtensions = setOf("webp", "jpg", "jpeg", "png", "gif", "heic")
}

@Composable
fun StorageScreen(nav: NavHostController) {
    val container = LocalContainer.current
    val vm: StorageViewModel = viewModel(
        factory = viewModelFactory { initializer { StorageViewModel(container) } },
    )
    val state = vm.ui
    val context = LocalContext.current
    var cleaningRecipe by remember { mutableStateOf<RecipeStorageRow?>(null) }
    var showClearAll by remember { mutableStateOf(false) }
    var clearConfirmText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                PageHeader(
                    title = "存储空间",
                    subtitle = "本机占用",
                    actions = {
                        CircleIconButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "返回",
                            onClick = { nav.popBackStack() },
                        )
                    },
                )
            }

            when {
                state.loading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "计算中…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                state.error != null -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "读取失败",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = state.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(16.dp))
                            SecondaryButton(text = "重试", onClick = vm::refresh)
                        }
                    }
                }

                else -> {
                    val stats = state.stats
                    item {
                        StorageOverview(stats)
                    }

                    item { SectionHeader(title = "明细") }
                    item {
                        SettingsGroup {
                            StorageDetailRow("视频", stats.videoBytes)
                            Hairline()
                            StorageDetailRow("图片", stats.imageBytes)
                            Hairline()
                            StorageDetailRow("历史快照封面", stats.historyBytes)
                            Hairline()
                            StorageDetailRow("数据库", stats.databaseBytes)
                            Hairline()
                            StorageDetailRow("临时文件", stats.tempBytes)
                        }
                    }

                    item { SectionHeader(title = "按菜谱") }
                    if (state.recipes.isEmpty()) {
                        item {
                            Text(
                                text = "还没有菜谱媒体。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    } else {
                        items(state.recipes, key = { it.id }) { recipe ->
                            RecipeStorageItem(
                                recipe = recipe,
                                onClean = { cleaningRecipe = recipe },
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(20.dp))
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SecondaryButton(
                                text = "清理临时文件",
                                onClick = {
                                    vm.cleanTemp { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "清空全部数据",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clearConfirmText = ""
                                    showClearAll = true
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
                }
            }
        }
    }

    cleaningRecipe?.let { recipe ->
        ConfirmDialog(
            title = "清理「${recipe.title}」的媒体？",
            body = "删除图片和视频？文字、食材、步骤和备注会保留。",
            confirmLabel = "清理媒体",
            onDismiss = { cleaningRecipe = null },
            onConfirm = {
                vm.cleanRecipeMedia(recipe.id) {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
                cleaningRecipe = null
            },
        )
    }

    if (showClearAll) {
        AlertDialog(
            onDismissRequest = { showClearAll = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("清空全部数据？") },
            text = {
                Column {
                    Text(
                        text = "本机所有菜谱、媒体、历史与采购清单都会被删除。平台登录信息不会清除。此操作不可撤销。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = clearConfirmText,
                        onValueChange = { clearConfirmText = it },
                        label = { Text("输入「删除」确认") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.clearAllData {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                        showClearAll = false
                    },
                    enabled = clearConfirmText.trim() == "删除",
                ) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAll = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun StorageOverview(stats: StorageStats) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(text = formatBytes(stats.totalBytes), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(2.dp))
        Text(
            text = "已用 · 可用 ${formatBytes(stats.freeBytes)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        StorageBar(
            segments = listOf(
                MaterialTheme.colorScheme.primary to stats.videoBytes,
                MaterialTheme.colorScheme.tertiary to stats.imageBytes,
                MaterialTheme.colorScheme.onSurfaceVariant to stats.historyBytes,
                MaterialTheme.colorScheme.outline to stats.databaseBytes,
                MaterialTheme.colorScheme.secondary to stats.tempBytes,
            ),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "视频 ${formatBytes(stats.videoBytes)} · 图片 ${formatBytes(stats.imageBytes)} · " +
                "历史封面 ${formatBytes(stats.historyBytes)} · 数据库 ${formatBytes(stats.databaseBytes)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StorageBar(segments: List<Pair<Color, Long>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val total = segments.sumOf { it.second }.coerceAtLeast(1)
        segments.forEach { (color, bytes) ->
            val fraction = bytes.toFloat() / total
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(fraction)
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun StorageDetailRow(title: String, bytes: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatBytes(bytes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecipeStorageItem(recipe: RecipeStorageRow, onClean: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipeCover(
            relativePath = recipe.coverPath,
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(10.dp),
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
            Text(
                text = mediaSummary(recipe),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatBytes(recipe.bytes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = "清理媒体",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClean)
                .padding(horizontal = 6.dp, vertical = 6.dp),
        )
    }
}

private fun mediaSummary(recipe: RecipeStorageRow): String = when {
    recipe.hasVideo && recipe.imageCount > 0 -> "视频 + ${recipe.imageCount} 张图"
    recipe.hasVideo -> "视频"
    recipe.imageCount > 0 -> "${recipe.imageCount} 张图"
    else -> "无媒体"
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        content()
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> "0 B"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}
