package com.weiji.wang.feature.recipes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import com.weiji.wang.core.data.MenuRepository
import com.weiji.wang.core.data.RecipeRepository
import com.weiji.wang.core.data.db.RecipeCard
import com.weiji.wang.core.model.Platform
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.ConfirmDialog
import com.weiji.wang.ui.components.EmptyState
import com.weiji.wang.ui.components.FilterPill
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.PlatformBadge
import com.weiji.wang.ui.components.RecipeActionSheet
import com.weiji.wang.ui.components.RecipeCover
import com.weiji.wang.ui.components.focusRing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RecipeFilter(val label: String) {
    ALL("全部"),
    FAVORITE("收藏"),
    NEVER("没做过"),
    QUICK("30 分钟内"),
}

class RecipesViewModel(
    private val repo: RecipeRepository,
    private val menu: MenuRepository,
) : ViewModel() {
    val filter = MutableStateFlow(RecipeFilter.ALL)

    val cards = filter
        .flatMapLatest { value -> repo.observeCards().map { list -> applyFilter(list, value) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(value: RecipeFilter) {
        filter.value = value
    }

    fun toggleFavorite(card: RecipeCard) {
        viewModelScope.launch { repo.setFavorite(card.id, !card.favorite) }
    }

    suspend fun addToToday(card: RecipeCard): Boolean = menu.addToday(card.id)

    fun delete(card: RecipeCard, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteRecipe(card.id)
            onDone()
        }
    }

    private fun applyFilter(list: List<RecipeCard>, filter: RecipeFilter): List<RecipeCard> = when (filter) {
        RecipeFilter.ALL -> list
        RecipeFilter.FAVORITE -> list.filter { it.favorite }
        RecipeFilter.NEVER -> list.filter { it.lastCookedAt == null }
        RecipeFilter.QUICK -> list.filter { (it.totalMinutes ?: 999) <= 30 }
    }
}

@Composable
fun RecipesScreen(nav: NavHostController, snackbarHostState: SnackbarHostState) {
    val container = LocalContainer.current
    val vm: RecipesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RecipesViewModel(container.recipes, container.menu) }
        },
    )
    val cards by vm.cards.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val width = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val columns = if (width >= 840) 3 else 2
    val scope = rememberCoroutineScope()

    var menuCard by remember { mutableStateOf<RecipeCard?>(null) }
    var deleteCard by remember { mutableStateOf<RecipeCard?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            PageHeader(
                title = "菜谱",
                subtitle = if (cards.isEmpty()) null else "${cards.size} 道",
                horizontalPadding = 0,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            RecipesSearchBar(onClick = { nav.navigate("search") })
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                items(RecipeFilter.entries) { value ->
                    FilterPill(text = value.label, selected = filter == value) { vm.setFilter(value) }
                }
            }
        }
        if (cards.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                    EmptyState(
                        title = "还没有菜谱",
                        body = "从抖音、小红书或 B 站分享一条链接进来。",
                        primaryLabel = "去收录",
                        onPrimary = { nav.navigate("capture") },
                    )
                }
            }
        } else {
            items(cards, key = { it.id }) { card ->
                RecipeGridCard(
                    card = card,
                    onClick = { nav.navigate("recipe/${card.id}") },
                    onLongClick = { menuCard = card },
                    onFavorite = { vm.toggleFavorite(card) },
                )
            }
        }
    }

    val sheetCard = menuCard
    if (sheetCard != null) {
        RecipeActionSheet(
            title = sheetCard.title,
            favorite = sheetCard.favorite,
            onDismiss = { menuCard = null },
            onAddToday = {
                menuCard = null
                scope.launch {
                    val ok = vm.addToToday(sheetCard)
                    snackbarHostState.showSnackbar(
                        if (ok) "已加入今天吃什么 · 从今天吃什么移除时会自动减去对应食材"
                        else "加入失败，菜谱可能已被删除",
                    )
                }
            },
            onToggleFavorite = {
                menuCard = null
                vm.toggleFavorite(sheetCard)
            },
            onDelete = {
                menuCard = null
                deleteCard = sheetCard
            },
        )
    }

    val target = deleteCard
    if (target != null) {
        ConfirmDialog(
            title = "删除菜谱？",
            body = "「${target.title}」会从菜谱里移除。已写入采购清单的食材会保留，吃过的菜历史快照不受影响。",
            onDismiss = { deleteCard = null },
            onConfirm = {
                deleteCard = null
                vm.delete(target) {
                    scope.launch { snackbarHostState.showSnackbar("已删除菜谱") }
                }
            },
        )
    }
}

@Composable
private fun RecipesSearchBar(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "搜菜名、食材、作者",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecipeGridCard(
    card: RecipeCard,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavorite: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .focusRing(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box {
            RecipeCover(
                relativePath = card.coverPath,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                    .clickable(onClick = onFavorite),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (card.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (card.favorite) "取消收藏" else "收藏",
                    tint = if (card.favorite) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (card.platform != null) {
                    PlatformBadge(Platform.fromKey(card.platform), compact = true)
                } else {
                    Text(
                        text = "手动",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${card.servings} 人",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
