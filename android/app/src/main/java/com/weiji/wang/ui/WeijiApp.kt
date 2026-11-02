package com.weiji.wang.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.weiji.wang.feature.capture.CaptureScreen
import com.weiji.wang.feature.cook.CookModeScreen
import com.weiji.wang.feature.edit.EditRecipeScreen
import com.weiji.wang.feature.home.HomeScreen
import com.weiji.wang.feature.login.LoginScreen
import com.weiji.wang.feature.recipe.RecipeDetailScreen
import com.weiji.wang.feature.recipes.RecipesScreen
import com.weiji.wang.feature.settings.SettingsScreen
import com.weiji.wang.feature.shopping.ShoppingScreen
import com.weiji.wang.ui.components.CaptureFab

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "今天", Icons.Filled.Home),
    Tab("recipes", "菜谱", Icons.Filled.List),
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
    val width = LocalConfiguration.current.screenWidthDp
    val wide = width >= 600
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val immersive = route.startsWith("cook")

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
        bottomBar = {
            if (!wide && !immersive) BottomBar(nav, route)
        },
        floatingActionButton = {
            if (!wide && !immersive && route in setOf("home", "recipes")) {
                CaptureFab { nav.navigate("capture") }
            }
        },
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (wide && !immersive) Rail(nav, route)
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                WeijiNavHost(nav)
            }
        }
    }
}

@Composable
private fun WeijiNavHost(nav: NavHostController) {
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(nav) }
        composable("recipes") { RecipesScreen(nav) }
        composable("shopping") { ShoppingScreen(nav) }
        composable("settings") { SettingsScreen(nav) }
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
        composable("cook/{id}") { entry ->
            CookModeScreen(nav, entry.arguments?.getString("id").orEmpty())
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
