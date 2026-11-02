package com.weiji.wang.feature.cook

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.weiji.wang.core.data.RecipeRepository
import com.weiji.wang.core.data.db.RecipeWithDetails
import com.weiji.wang.core.model.IngredientScaler
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Ulid
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.RecipeCover
import com.weiji.wang.ui.theme.CookStepStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CookTimer(
    val id: String,
    val label: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val running: Boolean,
)

class CookViewModel(
    private val recipeId: String,
    private val repo: RecipeRepository,
) : ViewModel() {
    val details = repo.observeDetails(recipeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var stepIndex by mutableIntStateOf(0)
        private set

    val timers = androidx.compose.runtime.mutableStateListOf<CookTimer>()
    private var ticker: Job? = null

    fun setStep(index: Int) {
        stepIndex = index
    }

    fun addTimer(seconds: Int) {
        timers += CookTimer(Ulid.next(), "${seconds / 60} 分钟", seconds, seconds, true)
        startTicker()
    }

    fun toggleTimer(id: String) {
        val index = timers.indexOfFirst { it.id == id }
        if (index >= 0) {
            val timer = timers[index]
            timers[index] = timer.copy(running = !timer.running)
            if (timers[index].running) startTicker()
        }
    }

    fun removeTimer(id: String) {
        timers.removeAll { it.id == id }
    }

    fun record(servings: Int, rating: Int, notes: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.recordCook(recipeId, servings, rating.takeIf { it > 0 }, notes)
            onDone()
        }
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            while (timers.any { it.running }) {
                delay(1_000)
                for (index in timers.indices) {
                    val timer = timers[index]
                    if (!timer.running) continue
                    val remaining = (timer.remainingSeconds - 1).coerceAtLeast(0)
                    timers[index] = timer.copy(
                        remainingSeconds = remaining,
                        running = remaining > 0,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CookModeScreen(nav: NavHostController, recipeId: String) {
    val container = LocalContainer.current
    val vm: CookViewModel = viewModel(
        key = "cook-$recipeId",
        factory = viewModelFactory { initializer { CookViewModel(recipeId, container.recipes) } },
    )
    val details by vm.details.collectAsStateWithLifecycle()
    val recipe = details
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var showExit by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }
    var showIngredients by remember { mutableStateOf(false) }
    var showTimerPicker by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    if (recipe == null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("载入中…", color = Color.White.copy(alpha = 0.6f))
        }
        return
    }

    val steps = recipe.steps
    val pageCount = steps.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    fun goNext() {
        if (pagerState.currentPage >= pageCount - 1) {
            showFinish = true
        } else {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
        }
    }

    fun goPrev() {
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        vm.setStep(pagerState.currentPage)
    }

    DisposableEffect(Unit) {
        CookKeys.bind(next = { goNext() }, prev = { goPrev() })
        onDispose { CookKeys.unbind() }
    }

    BackHandler { showExit = true }

    // 计时归零时震动一次
    val vibrated = remember { mutableSetOf<String>() }
    LaunchedEffect(vm.timers.toList()) {
        vm.timers.filter { it.remainingSeconds == 0 && it.id !in vibrated }.forEach {
            vibrated += it.id
            vibrate(context)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButtonDark(onClick = { showExit = true }) {
                    Icon(Icons.Filled.Close, contentDescription = "退出", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.recipe.title,
                        color = Color.White.copy(alpha = 0.55f),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
                Text(
                    text = "${(pagerState.currentPage + 1).coerceAtMost(pageCount)}/$pageCount",
                    color = Color.White.copy(alpha = 0.55f),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable { showTimerPicker = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text("计时", color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable { showIngredients = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text("食材", color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                }
            }

            if (steps.isEmpty()) {
                Box(Modifier.weight(1f).padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = recipe.source?.textBody?.take(600) ?: "这道菜还没有步骤。\n回到详情页补充，或直接看原文。",
                        color = Color.White,
                        style = CookStepStyle,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    pageSpacing = 0.dp,
                    beyondBoundsPageCount = 1,
                ) { page ->
                    val step = steps[page]
                    val stepImages = recipe.media.filter { it.kind == MediaKind.STEP.name }.sortedBy { it.createdAt }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "第 ${page + 1} 步",
                            color = Color(0xFFFF9F0A),
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = step.text.ifBlank { "按图示操作" },
                            color = Color.White,
                            style = CookStepStyle,
                        )
                        step.durationSeconds?.let {
                            Spacer(Modifier.height(18.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable { vm.addTimer(it) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = "需要 ${it / 60} 分 ${it % 60} 秒 · 点此计时",
                                    color = Color(0xFFFF9F0A),
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                        val imagePath = step.mediaPath ?: stepImages.getOrNull(page)?.relativePath
                        if (imagePath != null) {
                            Spacer(Modifier.height(18.dp))
                            RecipeCover(
                                relativePath = imagePath,
                                modifier = Modifier.fillMaxWidth().height(240.dp),
                                shape = RoundedCornerShape(16.dp),
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            if (vm.timers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    vm.timers.take(3).forEach { timer ->
                        TimerChip(timer, onClick = { vm.toggleTimer(timer.id) }, onRemove = { vm.removeTimer(timer.id) })
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable { showTimerPicker = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "加计时", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CookNavButton(
                    text = "上一步",
                    enabled = pagerState.currentPage > 0,
                    modifier = Modifier.weight(1f),
                    onClick = { goPrev() },
                )
                CookNavButton(
                    text = if (pagerState.currentPage >= pageCount - 1) "做完了" else "下一步",
                    enabled = true,
                    primary = true,
                    modifier = Modifier.weight(1.4f),
                    onClick = { goNext() },
                )
            }
        }
    }

    if (showExit) {
        AlertDialog(
            onDismissRequest = { showExit = false },
            title = { Text("退出烹饪？") },
            text = { Text("当前步骤会保留，下次可以接着做。") },
            confirmButton = {
                TextButton(onClick = { showExit = false; nav.popBackStack() }) { Text("退出") }
            },
            dismissButton = { TextButton(onClick = { showExit = false }) { Text("继续做") } },
        )
    }

    if (showIngredients) {
        IngredientDialog(recipe, onDismiss = { showIngredients = false })
    }

    if (showTimerPicker) {
        TimerPickerDialog(
            onDismiss = { showTimerPicker = false },
            onPick = {
                vm.addTimer(it)
                showTimerPicker = false
            },
        )
    }

    if (showFinish) {
        FinishDialog(
            defaultServings = recipe.recipe.servings,
            onDismiss = { showFinish = false },
            onSave = { servings, rating, notes ->
                vm.record(servings, rating, notes) {
                    showFinish = false
                    nav.popBackStack()
                }
            },
        )
    }
}

@Composable
private fun IconButtonDark(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun CookNavButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.05f)
                    primary -> Color(0xFFFF9F0A)
                    else -> Color.White.copy(alpha = 0.10f)
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = when {
                !enabled -> Color.White.copy(alpha = 0.3f)
                primary -> Color.Black
                else -> Color.White
            },
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TimerChip(timer: CookTimer, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (timer.running) Color(0xFFFF9F0A).copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "%02d:%02d".format(timer.remainingSeconds / 60, timer.remainingSeconds % 60),
            color = if (timer.running) Color(0xFFFF9F0A) else Color.White.copy(alpha = 0.6f),
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "移除计时", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun IngredientDialog(recipe: RecipeWithDetails, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("食材（${recipe.recipe.servings} 人份）") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                recipe.ingredients.forEach { item ->
                    val amount = if (item.amount == null) item.rawText.take(12)
                    else IngredientScaler.format(item.amount, item.unit)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(item.name, modifier = Modifier.weight(1f))
                        Text(amount, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } },
    )
}

@Composable
private fun TimerPickerDialog(onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("计时多久？") },
        text = {
            Column {
                listOf(60, 180, 300, 600).forEach { seconds ->
                    Text(
                        text = if (seconds >= 60) "${seconds / 60} 分钟" else "$seconds 秒",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(seconds) }
                            .padding(vertical = 14.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun FinishDialog(
    defaultServings: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String) -> Unit,
) {
    var rating by remember { mutableIntStateOf(0) }
    var notes by remember { mutableStateOf("") }
    var servings by remember { mutableIntStateOf(defaultServings) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录这次") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < rating) Color(0xFFFF9F0A)
                            else androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier
                                .size(30.dp)
                                .clickable { rating = index + 1 }
                                .padding(2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("份量", modifier = Modifier.weight(1f))
                    Text(
                        text = "−",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { servings = (servings - 1).coerceAtLeast(1) }
                            .padding(horizontal = 12.dp),
                    )
                    Text("$servings 人")
                    Text(
                        text = "+",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { servings = (servings + 1).coerceAtMost(12) }
                            .padding(horizontal = 12.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("这次哪里不一样？") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(servings, rating, notes) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("跳过") } },
    )
}

private fun vibrate(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(VibratorManager::class.java))?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return
    if (Build.VERSION.SDK_INT >= 26) {
        vibrator.vibrate(VibrationEffect.createOneShot(600, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(600)
    }
}
