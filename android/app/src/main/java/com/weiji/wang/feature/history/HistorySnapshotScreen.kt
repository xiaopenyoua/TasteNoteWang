package com.weiji.wang.feature.history

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.weiji.wang.core.data.MenuEntry
import com.weiji.wang.core.data.MenuRepository
import com.weiji.wang.core.data.RecipeRepository
import com.weiji.wang.core.model.IngredientScaler
import com.weiji.wang.core.model.Platform
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.CircleIconButton
import com.weiji.wang.ui.components.EmptyState
import com.weiji.wang.ui.components.MetaChip
import com.weiji.wang.ui.components.PlatformBadge
import com.weiji.wang.ui.components.RecipeCover
import com.weiji.wang.ui.components.SectionHeader
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

internal data class SnapshotState(
    val loading: Boolean,
    val entry: MenuEntry?,
)

internal class HistorySnapshotViewModel(
    date: String,
    recipeId: String,
    menu: MenuRepository,
    recipes: RecipeRepository,
) : ViewModel() {
    val state = menu.observeDay(date)
        .map { list -> SnapshotState(loading = false, entry = list.firstOrNull { it.recipeId == recipeId }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SnapshotState(true, null))

    val hasCurrentRecipe = recipes.observeDetails(recipeId)
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}

@Composable
fun HistorySnapshotScreen(nav: NavHostController, date: String, recipeId: String) {
    val container = LocalContainer.current
    val vm: HistorySnapshotViewModel = viewModel(
        key = "history-$date-$recipeId",
        factory = viewModelFactory {
            initializer {
                HistorySnapshotViewModel(date, recipeId, container.menu, container.recipes)
            }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val hasCurrentRecipe by vm.hasCurrentRecipe.collectAsStateWithLifecycle()

    val entry = state.entry
    val snapshot = entry?.snapshot

    if (state.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (entry == null || snapshot == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            EmptyState(
                title = "快照不存在",
                body = "这条记录可能已经被删除。",
                primaryLabel = "返回",
                onPrimary = { nav.popBackStack() },
            )
        }
        return
    }

    val files = container.files
    val uriHandler = LocalUriHandler.current
    val recipe = snapshot.recipe

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "返回",
                    onClick = { nav.popBackStack() },
                )
                Spacer(Modifier.weight(1f))
                if (hasCurrentRecipe) {
                    Text(
                        text = "打开当前菜谱",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { nav.navigate("recipe/$recipeId") }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
        }

        item {
            RecipeCover(
                relativePath = entry.coverPath ?: recipe.coverPath,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(16f / 9f),
                shape = RoundedCornerShape(16.dp),
            )
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.headlineLarge,
                )
                if (recipe.subtitle.isNotBlank()) {
                    Text(
                        text = recipe.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = snapshotSubtitle(entry.date, recipe.servings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            val source = snapshot.source
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (source != null) {
                    PlatformBadge(Platform.fromKey(source.platform))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = source.authorName?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "原作者未知",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "原链接 ↗",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { runCatching { uriHandler.openUri(source.originalUrl) } }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                } else {
                    Text(
                        text = "手动创建 · 无来源链接",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetaChip("${recipe.servings} 人份")
                recipe.totalMinutes?.let { MetaChip("$it 分钟") }
                MetaChip("当天的快照")
            }
        }

        item { SectionHeader(title = "食材") }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                snapshot.ingredients.forEach { ingredient ->
                    val amount = if (ingredient.amount != null) {
                        IngredientScaler.format(ingredient.amount, ingredient.unit)
                    } else {
                        ingredient.rawText.ifBlank { "适量" }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = ingredient.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = amount,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (snapshot.steps.isNotEmpty()) {
            item { SectionHeader(title = "步骤") }
            itemsIndexed(snapshot.steps) { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.text,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        step.durationSeconds?.let {
                            Text(
                                text = durationLabel(it),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        val mediaPath = step.mediaPath
                        if (mediaPath != null && files.exists(mediaPath)) {
                            RecipeCover(
                                relativePath = mediaPath,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .height(160.dp),
                                shape = RoundedCornerShape(12.dp),
                            )
                        }
                    }
                }
            }
        }

        if (recipe.notes.isNotBlank()) {
            item { SectionHeader(title = "备注") }
            item {
                Text(
                    text = recipe.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                Text(
                    text = "这是 ${dateLabel(entry.date)} 加入时的快照；之后修改或删除菜谱都不会改变这里。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                if (!hasCurrentRecipe) {
                    Text(
                        text = "原菜谱已删除，这份快照仍保留。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

private fun durationLabel(seconds: Int): String =
    if (seconds >= 60) "${seconds / 60} 分钟" else "$seconds 秒"

private val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

private fun snapshotSubtitle(dateKey: String, servings: Int): String {
    val date = runCatching { LocalDate.parse(dateKey) }.getOrNull()
        ?: return "$dateKey · $servings 人份"
    return "${date.monthValue}月${date.dayOfMonth}日 · ${weekdays[date.dayOfWeek.value - 1]} · $servings 人份"
}

private fun dateLabel(dateKey: String): String {
    val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return dateKey
    return "${date.monthValue}月${date.dayOfMonth}日"
}
