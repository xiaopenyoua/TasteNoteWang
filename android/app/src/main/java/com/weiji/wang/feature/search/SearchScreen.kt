package com.weiji.wang.feature.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
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
import com.weiji.wang.ui.components.PlatformBadge
import com.weiji.wang.ui.components.RecipeActionSheet
import com.weiji.wang.ui.components.RecipeCover
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repo: RecipeRepository,
    private val menu: MenuRepository,
) : ViewModel() {
    val query = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    val results = query
        .debounce(200)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { text -> if (text.isEmpty()) flowOf(emptyList()) else repo.search(text) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
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
}

private const val MAX_RESULTS = 100
private const val MAX_RECENTS = 8

@Composable
fun SearchScreen(nav: NavHostController, snackbarHostState: SnackbarHostState) {
    val container = LocalContainer.current
    val vm: SearchViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SearchViewModel(container.recipes, container.menu) }
        },
    )
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var menuCard by remember { mutableStateOf<RecipeCard?>(null) }
    var deleteCard by remember { mutableStateOf<RecipeCard?>(null) }

    val recentsSaver = listSaver<List<String>, String>(
        save = { it },
        restore = { it },
    )
    var recents by rememberSaveable(stateSaver = recentsSaver) {
        mutableStateOf(emptyList<String>())
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }

    fun rememberQuery(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        recents = (listOf(trimmed) + recents.filter { it != trimmed }).take(MAX_RECENTS)
    }

    fun openRecipe(card: RecipeCard) {
        rememberQuery(query)
        keyboard?.hide()
        nav.navigate("recipe/${card.id}")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchInput(
                value = query,
                onValueChange = vm::setQuery,
                focusRequester = focusRequester,
                onSearch = { rememberQuery(query) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "取消",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        keyboard?.hide()
                        nav.popBackStack()
                    }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }

        if (query.isBlank()) {
            IdleContent(
                recents = recents,
                onPickRecent = { picked ->
                    rememberQuery(picked)
                    vm.setQuery(picked)
                },
                onClear = { recents = emptyList() },
            )
        } else {
            ResultContent(
                query = query.trim(),
                results = results,
                onOpen = ::openRecipe,
                onLongPress = { menuCard = it },
            )
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
private fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
        )
        Box(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = "搜菜名、食材、作者",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    inner()
                },
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "清空",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") }
                    .padding(4.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdleContent(
    recents: List<String>,
    onPickRecent: (String) -> Unit,
    onClear: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
            if (recents.isEmpty()) {
                Spacer(Modifier.height(48.dp))
                Text(
                    text = "搜菜名、食材、作者或原文",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                return@Column
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "最近搜索",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "清除",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                recents.forEach { recent ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onPickRecent(recent) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = recent,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultContent(
    query: String,
    results: List<RecipeCard>,
    onOpen: (RecipeCard) -> Unit,
    onLongPress: (RecipeCard) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (results.isEmpty()) {
            Column(
                modifier = Modifier.padding(top = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "没找到",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "换个词试试",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Box
        }

        val shown = results.take(MAX_RESULTS)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Text(
                    text = "找到 ${results.size} 道",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(shown, key = { it.id }) { card ->
                SearchResultRow(
                    card = card,
                    query = query,
                    onClick = { onOpen(card) },
                    onLongClick = { onLongPress(card) },
                )
            }
            if (results.size > MAX_RESULTS) {
                item {
                    Text(
                        text = "结果太多，再输入几个字",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultRow(
    card: RecipeCard,
    query: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val platform = card.platform?.let { Platform.fromKey(it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipeCover(
            relativePath = card.coverPath,
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(10.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = matchHint(card, query),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (platform != null) {
                    Spacer(Modifier.width(6.dp))
                    PlatformBadge(platform, compact = true)
                }
            }
        }
    }
}

private fun matchHint(card: RecipeCard, query: String): String {
    val author = card.author
    return when {
        card.title.contains(query, ignoreCase = true) -> "菜名：${card.title}"
        author != null && author.contains(query, ignoreCase = true) -> "作者：@$author"
        else -> "食材或原文提到：$query"
    }
}
