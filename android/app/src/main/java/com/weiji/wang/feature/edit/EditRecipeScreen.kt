package com.weiji.wang.feature.edit

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.weiji.wang.core.data.RecipeRepository
import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.MediaEntity
import com.weiji.wang.core.data.db.RecipeEntity
import com.weiji.wang.core.data.db.RecipeWithDetails
import com.weiji.wang.core.data.db.SourceEntity
import com.weiji.wang.core.data.db.StepEntity
import com.weiji.wang.core.import.LinkParser
import com.weiji.wang.core.import.TextParser
import com.weiji.wang.core.model.Platform
import com.weiji.wang.core.model.Ulid
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.PrimaryButton
import kotlinx.coroutines.launch

class EditViewModel(
    private val recipeId: String,
    private val repo: RecipeRepository,
) : ViewModel() {
    val isNew: Boolean = recipeId == "new"

    var loaded by mutableStateOf(isNew)
        private set
    var title by mutableStateOf("")
    var servings by mutableStateOf(2)
    var minutes by mutableStateOf("")
    var notes by mutableStateOf("")
    var ingredientsText by mutableStateOf("")
    var stepsText by mutableStateOf("")
    var sourceUrl by mutableStateOf("")
    var error by mutableStateOf<String?>(null)

    private var existing: RecipeWithDetails? = null

    init {
        if (!isNew) {
            viewModelScope.launch {
                val details = repo.details(recipeId)
                existing = details
                if (details != null) fill(details)
                loaded = true
            }
        }
    }

    private fun fill(details: RecipeWithDetails) {
        title = details.recipe.title
        servings = details.recipe.servings
        minutes = details.recipe.totalMinutes?.toString().orEmpty()
        notes = details.recipe.notes
        sourceUrl = details.source?.originalUrl.orEmpty()
        ingredientsText = details.ingredients.joinToString("\n") { item ->
            val amount = when {
                item.amount == null -> item.rawText.takeIf { it.length <= 16 } ?: "适量"
                item.amount % 1.0 == 0.0 -> "${item.amount.toInt()}${item.unit.orEmpty()}"
                else -> "${item.amount}${item.unit.orEmpty()}"
            }
            "${item.name} $amount".trim()
        }
        stepsText = details.steps.joinToString("\n") { it.text }
    }

    fun save(onDone: (String) -> Unit) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            error = "菜名不能为空"
            return
        }
        val body = buildString {
            appendLine("食材")
            appendLine(ingredientsText)
            appendLine("步骤")
            appendLine(stepsText)
        }
        val parsed = TextParser.parse(cleanTitle, body)
        val now = System.currentTimeMillis()
        val id = existing?.recipe?.id ?: Ulid.next()
        val source = buildSource(id, now)
        val base = existing?.recipe
        val recipe = if (base != null) {
            base.copy(
                title = cleanTitle,
                servings = servings.coerceIn(1, 12),
                totalMinutes = minutes.toIntOrNull()?.takeIf { it > 0 },
                notes = notes,
                sourceId = source?.id,
                updatedAt = now,
            )
        } else {
            RecipeEntity(
                id = id,
                title = cleanTitle,
                servings = servings.coerceIn(1, 12),
                totalMinutes = minutes.toIntOrNull()?.takeIf { it > 0 },
                notes = notes,
                sourceId = source?.id,
                createdAt = now,
                updatedAt = now,
            )
        }
        val ingredientEntities = parsed.ingredients.mapIndexed { index, item ->
            IngredientEntity(
                id = Ulid.next(),
                recipeId = id,
                position = index,
                groupName = item.group,
                name = item.name,
                amount = item.amount,
                unit = item.unit,
                rawText = item.rawText,
                scalable = !item.vague,
            )
        }
        val stepTexts = parsed.steps.ifEmpty {
            stepsText.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        }
        val stepEntities = stepTexts.mapIndexed { index, text ->
            StepEntity(
                id = Ulid.next(),
                recipeId = id,
                position = index,
                text = text,
            )
        }
        viewModelScope.launch {
            repo.saveRecipe(
                recipe = recipe,
                ingredientList = ingredientEntities,
                stepList = stepEntities,
                source = source,
                mediaList = existing?.media ?: emptyList<MediaEntity>(),
            )
            onDone(id)
        }
    }

    private fun buildSource(recipeId: String, now: Long): SourceEntity? {
        val url = sourceUrl.trim()
        if (url.isEmpty()) return null
        val old = existing?.source?.takeIf { it.originalUrl == url }
        return SourceEntity(
            id = old?.id ?: Ulid.next(),
            recipeId = recipeId,
            platform = old?.platform ?: LinkParser.detect(url).key,
            originalUrl = url,
            canonicalUrl = old?.canonicalUrl,
            platformContentId = old?.platformContentId ?: LinkParser.extractContentId(Platform.OTHER, url),
            authorName = old?.authorName,
            authorId = old?.authorId,
            sourceTitle = old?.sourceTitle ?: title,
            textBody = old?.textBody,
            remoteCoverUrl = old?.remoteCoverUrl,
            shareText = old?.shareText,
            importedAt = old?.importedAt ?: now,
            fetchStatus = old?.fetchStatus ?: "MANUAL",
        )
    }
}

@Composable
fun EditRecipeScreen(nav: NavHostController, recipeId: String) {
    val container = LocalContainer.current
    val vm: EditViewModel = viewModel(
        key = "edit-$recipeId",
        factory = viewModelFactory { initializer { EditViewModel(recipeId, container.recipes) } },
    )

    if (!vm.loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("载入中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item {
            PageHeader(
                title = if (vm.isNew) "新建菜谱" else "编辑",
                subtitle = "自动解析的结果可以随时改",
                actions = {
                    TextButton(onClick = { vm.save { id -> nav.navigate("recipe/$id") { popUpTo("edit/$recipeId") { inclusive = true } } } }) {
                        Text("保存")
                    }
                },
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = vm.title,
                    onValueChange = { vm.title = it; vm.error = null },
                    label = { Text("菜名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    isError = vm.error != null,
                    supportingText = vm.error?.let { { Text(it) } },
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = vm.servings.toString(),
                        onValueChange = { vm.servings = it.toIntOrNull() ?: 1 },
                        label = { Text("几人份") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        value = vm.minutes,
                        onValueChange = { vm.minutes = it.filter { c -> c.isDigit() } },
                        label = { Text("时长（分钟）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = vm.sourceUrl,
                    onValueChange = { vm.sourceUrl = it },
                    label = { Text("来源链接（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text("食材", style = MaterialTheme.typography.titleMedium)
                Text(
                    "每行一项：牛腩 500克、生抽 2勺。只有名称也可以。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = vm.ingredientsText,
                    onValueChange = { vm.ingredientsText = it },
                    minLines = 6,
                    maxLines = 14,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text("步骤", style = MaterialTheme.typography.titleMedium)
                Text(
                    "每行一步，按顺序。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = vm.stepsText,
                    onValueChange = { vm.stepsText = it },
                    minLines = 8,
                    maxLines = 20,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text("备注", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = vm.notes,
                    onValueChange = { vm.notes = it },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(18.dp))
                PrimaryButton(
                    text = "保存",
                    onClick = {
                        vm.save { id ->
                            nav.navigate("recipe/$id") { popUpTo("edit/$recipeId") { inclusive = true } }
                        }
                    },
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
