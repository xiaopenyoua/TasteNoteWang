package com.weiji.wang.feature.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.weiji.wang.core.model.Platform
import com.weiji.wang.core.time.DayClock
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.CircleIconButton
import com.weiji.wang.ui.components.EmptyState
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.PlatformBadge
import com.weiji.wang.ui.components.RecipeCover
import com.weiji.wang.ui.components.SectionHeader
import com.weiji.wang.ui.components.SwipeToRevealDelete
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(
    menu: MenuRepository,
    recipes: RecipeRepository,
) : ViewModel() {
    val entries = menu.observeToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val count: StateFlow<Int?> = recipes.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun HomeScreen(nav: NavHostController, snackbarHostState: SnackbarHostState) {
    val container = LocalContainer.current
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HomeViewModel(container.menu, container.recipes) }
        },
    )
    val entries by vm.entries.collectAsStateWithLifecycle()
    val count by vm.count.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var openId by remember { mutableStateOf<String?>(null) }

    fun remove(recipeId: String) {
        openId = null
        scope.launch {
            container.menu.removeToday(recipeId)
            val result = snackbarHostState.showSnackbar(
                message = "已从今天吃什么移除",
                actionLabel = "撤销",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                container.menu.addToday(recipeId)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            PageHeader(
                title = "今天",
                subtitle = todayLabel(),
                actions = {
                    CircleIconButton(
                        icon = Icons.Filled.DateRange,
                        contentDescription = "吃过的菜",
                        onClick = { nav.navigate("history") },
                    )
                },
            )
        }

        if (count == 0) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(520.dp)) {
                    EmptyState(
                        title = "今天吃什么？",
                        body = "把抖音、小红书或 B 站刷到的教程收进来，味记会帮你排成能照着做的步骤。",
                        primaryLabel = "收录第一条",
                        onPrimary = { nav.navigate("capture") },
                        secondaryLabel = "手动创建",
                        onSecondary = { nav.navigate("edit/new") },
                    )
                }
            }
        } else if (count != null) {
            item { SectionHeader(title = "今天吃什么") }

            if (entries.isEmpty()) {
                item { MenuEmptyCard(onGoRecipes = { nav.navigate("recipes") }) }
            } else {
                items(entries, key = { it.recipeId }) { entry ->
                    MenuRow(
                        entry = entry,
                        revealed = openId == entry.recipeId,
                        onRevealedChange = { revealed ->
                            openId = when {
                                revealed -> entry.recipeId
                                openId == entry.recipeId -> null
                                else -> openId
                            }
                        },
                        onDelete = { remove(entry.recipeId) },
                        onClick = {
                            if (openId != null) {
                                openId = null
                            } else {
                                nav.navigate("recipe/${entry.recipeId}")
                            }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                item {
                    Text(
                        text = "左滑卡片可移除；移除会减去这道菜的采购食材，手动项保留。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    entry: MenuEntry,
    revealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val snapshot = entry.snapshot
    val title = snapshot?.recipe?.title ?: "菜谱已删除"
    val servings = snapshot?.recipe?.servings
    val platform = snapshot?.source?.platform?.let { Platform.fromKey(it) }
    val cover = entry.coverPath ?: snapshot?.recipe?.coverPath

    SwipeToRevealDelete(
        revealed = revealed,
        onRevealedChange = onRevealedChange,
        onDelete = onDelete,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecipeCover(
                relativePath = cover,
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (platform != null) {
                        PlatformBadge(platform, compact = true)
                        Spacer(Modifier.width(6.dp))
                    }
                    if (servings != null) {
                        Text(
                            text = "$servings 人份",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "已加入采购",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MenuEmptyCard(onGoRecipes: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(
            text = "还没有安排今天的菜",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "在菜谱详情或长按菜谱，点「今晚做这道」",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "去菜谱",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onGoRecipes)
                .padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

private val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

private fun todayLabel(): String {
    val date = LocalDate.parse(DayClock.todayKey())
    return "${date.monthValue}月${date.dayOfMonth}日 ${weekdays[date.dayOfWeek.value - 1]}"
}
