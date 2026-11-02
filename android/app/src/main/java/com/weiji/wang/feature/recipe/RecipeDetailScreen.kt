package com.weiji.wang.feature.recipe

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.weiji.wang.core.data.RecipeRepository
import com.weiji.wang.core.data.db.CookLogEntity
import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.MediaEntity
import com.weiji.wang.core.data.db.RecipeWithDetails
import com.weiji.wang.core.model.IngredientScaler
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.MetaChip
import com.weiji.wang.ui.components.PlatformBadge
import com.weiji.wang.ui.components.PrimaryButton
import com.weiji.wang.ui.components.RecipeCover
import com.weiji.wang.ui.components.SegmentedControl
import com.weiji.wang.ui.components.SourceBar
import com.weiji.wang.core.model.Platform
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecipeDetailViewModel(
    private val recipeId: String,
    private val repo: RecipeRepository,
) : ViewModel() {
    val details = repo.observeDetails(recipeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val logs = repo.observeCookLogs(recipeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite(current: Boolean) {
        viewModelScope.launch { repo.setFavorite(recipeId, !current) }
    }

    fun addToShopping(items: List<IngredientEntity>) {
        viewModelScope.launch { repo.addIngredientsToShopping(recipeId, items) }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteRecipe(recipeId)
            onDone()
        }
    }
}

@Composable
fun RecipeDetailScreen(nav: NavHostController, recipeId: String) {
    val container = LocalContainer.current
    val vm: RecipeDetailViewModel = viewModel(
        key = "recipe-$recipeId",
        factory = viewModelFactory { initializer { RecipeDetailViewModel(recipeId, container.recipes) } },
    )
    val details by vm.details.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showDelete by remember { mutableStateOf(false) }
    val recipe = details

    if (recipe == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("载入中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var servings by rememberSaveable(recipe.recipe.id) { mutableIntStateOf(recipe.recipe.servings) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Box {
                    RecipeCover(
                        relativePath = recipe.recipe.coverPath,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        shape = RoundedCornerShape(0.dp),
                    )
                    BackButton(nav)
                }
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = recipe.recipe.title,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    if (recipe.recipe.subtitle.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = recipe.recipe.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    SourceBar(recipe.source)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetaChip("${servings} 人份")
                        recipe.recipe.totalMinutes?.let { MetaChip("约 $it 分钟") }
                        if (recipe.media.any { it.kind == MediaKind.VIDEO.name }) MetaChip("含视频")
                        if (recipe.recipe.favorite) MetaChip("已收藏")
                    }
                    Spacer(Modifier.height(14.dp))
                    SegmentedControl(
                        options = listOf("食材", "步骤", "原文", "记录"),
                        selectedIndex = tab,
                        onSelect = { tab = it },
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            when (tab) {
                0 -> ingredientsContent(
                    recipe = recipe,
                    servings = servings,
                    onServings = { servings = it.coerceIn(1, 12) },
                    onAddShopping = {
                        vm.addToShopping(recipe.ingredients)
                        Toast.makeText(context, "已加入采购清单", Toast.LENGTH_SHORT).show()
                    },
                )
                1 -> stepsContent(recipe, onCook = { nav.navigate("cook/${recipeId}") })
                2 -> originalContent(recipe)
                else -> logsContent(logs)
            }
        }

        BottomActions(
            favorite = recipe.recipe.favorite,
            onFavorite = { vm.toggleFavorite(recipe.recipe.favorite) },
            onCook = { nav.navigate("cook/$recipeId") },
            onEdit = { nav.navigate("edit/$recipeId") },
            onDelete = { showDelete = true },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("删除这道菜谱？") },
            text = { Text("会同时删除已下载的图片和视频，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    vm.delete { nav.popBackStack() }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun BackButton(nav: NavHostController) {
    Box(
        modifier = Modifier
            .statusBarsPadding()
            .padding(8.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f))
            .clickable { nav.popBackStack() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.ingredientsContent(
    recipe: RecipeWithDetails,
    servings: Int,
    onServings: (Int) -> Unit,
    onAddShopping: () -> Unit,
) {
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "份量",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            ServingsStepper(servings, onServings)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "按 ${recipe.recipe.servings} 人份记录，已换算为 $servings 人份",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
    }
    if (recipe.ingredients.isEmpty()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp),
            ) {
                Text(
                    text = "还没有食材。点右上角编辑，从原文里补几行。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        val grouped = recipe.ingredients.groupBy { it.groupName }
        grouped.forEach { (group, items) ->
            if (!group.isNullOrBlank()) {
                item {
                    Text(
                        text = group,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp),
                    )
                }
            }
            items(items.size) { index ->
                IngredientRow(
                    item = items[index],
                    baseServings = recipe.recipe.servings,
                    servings = servings,
                )
                if (index < items.size - 1) {
                    Box(
                        Modifier
                            .padding(start = 20.dp)
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
        }
    }
    item {
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            PrimaryButton(text = "全部加入采购清单", onClick = onAddShopping)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ServingsStepper(value: Int, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        StepButton("−") { onChange(value - 1) }
        Text(
            text = "$value 人",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        StepButton("+") { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun IngredientRow(item: IngredientEntity, baseServings: Int, servings: Int) {
    val amount = ingredientAmount(item, baseServings, servings)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyLarge,
            color = if (amount == "适量" || amount == "少许") MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun ingredientAmount(item: IngredientEntity, baseServings: Int, servings: Int): String {
    if (item.amount == null || !item.scalable) {
        return item.rawText.takeIf { it.length <= 12 } ?: "适量"
    }
    val range = IngredientScaler.scaleRange(item.rawText, baseServings, servings)
    if (range != null) return range
    val scaled = IngredientScaler.scale(item.amount, baseServings, servings) ?: return item.rawText
    return IngredientScaler.format(scaled, item.unit)
}

private fun androidx.compose.foundation.lazy.LazyListScope.stepsContent(
    recipe: RecipeWithDetails,
    onCook: () -> Unit,
) {
    val stepImages = recipe.media.filter { it.kind == MediaKind.STEP.name }.sortedBy { it.createdAt }
    val video = recipe.media.firstOrNull { it.kind == MediaKind.VIDEO.name }
    if (video != null) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(
                    text = "原视频",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                VideoPlayerCard(
                    relativePath = video.relativePath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
            }
        }
    }
    if (recipe.steps.isEmpty()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp),
            ) {
                Text(
                    text = "还没有拆出步骤。可以去「原文」看来源全文，或编辑补充。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        itemsIndexed(recipe.steps) { index, step ->
            StepCard(
                index = index,
                text = step.text,
                imagePath = step.mediaPath ?: stepImages.getOrNull(index)?.relativePath,
                durationSeconds = step.durationSeconds,
                timestamp = step.videoTimestampMs,
            )
        }
    }
    item {
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            PrimaryButton(text = "开始做", onClick = onCook)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StepCard(
    index: Int,
    text: String,
    imagePath: String?,
    durationSeconds: Int?,
    timestamp: Long?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "第 ${index + 1} 步",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            durationSeconds?.let {
                MetaChip("${it / 60} 分 ${it % 60} 秒")
            }
            timestamp?.let {
                Spacer(Modifier.width(6.dp))
                MetaChip("${it / 60000}:%02d".format((it / 1000) % 60), icon = Icons.Filled.PlayArrow)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = text.ifBlank { "按图示操作" },
            style = MaterialTheme.typography.bodyLarge,
        )
        if (imagePath != null) {
            Spacer(Modifier.height(10.dp))
            RecipeCover(
                relativePath = imagePath,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.originalContent(recipe: RecipeWithDetails) {
    val body = recipe.source?.textBody.orEmpty().ifBlank {
        recipe.source?.shareText.orEmpty()
    }
    item {
        Column(modifier = Modifier.padding(16.dp)) {
            if (body.isBlank()) {
                Text(
                    text = "没有抓取到原文。可以在编辑页补充，或点上方原链接查看。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SelectionContainer {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "自动解析可能有遗漏。以原文和原视频为准。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.logsContent(logs: List<CookLogEntity>) {
    if (logs.isEmpty()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp),
            ) {
                Text(
                    text = "还没做过。第一次做完，记一句「哪里不一样」，下次就不用重新想。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        itemsIndexed(logs) { _, log ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(log.cookedAt)),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${log.servings} 人份",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < (log.rating ?: 0)) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                if (log.notes.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(text = log.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun BottomActions(
    favorite: Boolean,
    onFavorite: () -> Unit,
    onCook: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onFavorite) {
            Icon(
                imageVector = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (favorite) "取消收藏" else "收藏",
                tint = if (favorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        Box(modifier = Modifier.width(160.dp)) {
            PrimaryButton(text = "开始做", onClick = onCook)
        }
    }
}
