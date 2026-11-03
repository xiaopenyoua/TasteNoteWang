package com.weiji.wang.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.weiji.wang.feature.capture.CaptureScreen
import com.weiji.wang.feature.edit.EditRecipeScreen
import com.weiji.wang.feature.history.HistoryScreen
import com.weiji.wang.feature.history.HistorySnapshotScreen
import com.weiji.wang.feature.home.HomeScreen
import com.weiji.wang.feature.login.LoginScreen
import com.weiji.wang.feature.migrate.MigrationScreen
import com.weiji.wang.feature.recipe.RecipeDetailScreen
import com.weiji.wang.feature.recipes.RecipesScreen
import com.weiji.wang.feature.search.SearchScreen
import com.weiji.wang.feature.settings.SettingsScreen
import com.weiji.wang.feature.shopping.ShoppingScreen
import com.weiji.wang.feature.storage.StorageScreen
import com.weiji.wang.ui.components.CaptureFab
import com.weiji.wang.ui.components.focusRing

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "今天", Icons.Filled.Home),
    Tab("recipes", "菜谱", Icons.AutoMirrored.Filled.List),
    Tab("shopping", "采购", Icons.Filled.ShoppingCart),
    Tab("settings", "我的", Icons.Filled.Person),
)

@Composable
fun WeijiApp(
    shareText: String?,
    shareUris: List<String>,
    onShareConsumed: () -> Unit,
) {
    val nav = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val width = LocalConfiguration.current.screenWidthDp
    val wide = width >= 600
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val hideChrome = route.startsWith("login")

    LaunchedEffect(shareText, shareUris) {
        if (!shareText.isNullOrBlank() || shareUris.isNotEmpty()) {
            val text = Uri.encode(shareText.orEmpty())
            val uris = Uri.encode(shareUris.joinToString("|"))
            nav.navigate("capture?text=$text&uris=$uris")
            onShareConsumed()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!wide && !hideChrome) BottomBar(nav, route)
        },
        floatingActionButton = {
            if (!wide && !hideChrome && route == "recipes") {
                RecipesFab { nav.navigate("capture") }
            }
        },
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (wide && !hideChrome) Rail(nav, route)
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                WeijiNavHost(nav, snackbarHostState)
            }
        }
    }
}

@Composable
private fun WeijiNavHost(nav: NavHostController, snackbarHostState: SnackbarHostState) {
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(nav, snackbarHostState) }
        composable("history") { HistoryScreen(nav) }
        composable(
            route = "history/{date}/{recipeId}",
            arguments = listOf(
                navArgument("date") { type = NavType.StringType },
                navArgument("recipeId") { type = NavType.StringType },
            ),
        ) { entry ->
            HistorySnapshotScreen(
                nav = nav,
                date = entry.arguments?.getString("date").orEmpty(),
                recipeId = entry.arguments?.getString("recipeId").orEmpty(),
            )
        }
        composable("recipes") { RecipesScreen(nav, snackbarHostState) }
        composable("search") { SearchScreen(nav, snackbarHostState) }
        composable("shopping") { ShoppingScreen(nav) }
        composable("settings") { SettingsScreen(nav) }
        composable("storage") { StorageScreen(nav) }
        composable("migrate") { MigrationScreen(nav) }
        composable(
            route = "capture?text={text}&uris={uris}",
            arguments = listOf(
                navArgument("text") { type = NavType.StringType; defaultValue = "" },
                navArgument("uris") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val text = entry.arguments?.getString("text").orEmpty()
            val uris = entry.arguments?.getString("uris").orEmpty()
                .split('|').filter { it.isNotBlank() }
            CaptureScreen(nav, text, uris)
        }
        composable("capture") { CaptureScreen(nav, "", emptyList()) }
        composable("recipe/{id}") { entry ->
            RecipeDetailScreen(nav, entry.arguments?.getString("id").orEmpty())
        }
        composable("login/{platform}") { entry ->
            LoginScreen(nav, entry.arguments?.getString("platform").orEmpty())
        }
        composable("edit/{id}") { entry ->
            EditRecipeScreen(nav, entry.arguments?.getString("id").orEmpty())
        }
    }
}

@Composable
private fun RecipesFab(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .focusRing(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "收录",
            tint = Color.Black,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun BottomBar(nav: NavHostController, route: String) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = route == tab.route || route.startsWith(tab.route + "/"),
                onClick = { nav.navigateTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(22.dp)) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                alwaysShowLabel = true,
            )
        }
    }
}

@Composable
private fun Rail(nav: NavHostController, route: String) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        modifier = Modifier.fillMaxHeight(),
    ) {
        tabs.forEach { tab ->
            NavigationRailItem(
                selected = route == tab.route || route.startsWith(tab.route + "/"),
                onClick = { nav.navigateTab(tab.route) },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(22.dp))
                    }
                },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
            )
        }
        Box(modifier = Modifier.padding(top = 12.dp)) {
            CaptureFab { nav.navigate("capture") }
        }
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
