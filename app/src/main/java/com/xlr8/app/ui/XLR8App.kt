package com.xlr8.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xlr8.app.ui.detail.DetailScreen
import com.xlr8.app.ui.home.HomeScreen
import com.xlr8.app.ui.navigation.Routes
import com.xlr8.app.ui.navigation.TopLevelDestination
import com.xlr8.app.ui.placeholder.PlaceholderScreen
import com.xlr8.app.ui.theme.ThemeMode
import com.xlr8.app.ui.theme.XLR8Theme

@Composable
fun XLR8App() {
    // Theme preferences will be wired to DataStore in the Settings step; defaults for now.
    XLR8Theme(themeMode = ThemeMode.SYSTEM, dynamicColor = true, amoled = false) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        val currentDestination = backStackEntry?.destination
                        TopLevelDestination.entries.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(dest.icon, contentDescription = null) },
                                label = { Text(stringResource(dest.labelRes)) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = TopLevelDestination.HOME.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(TopLevelDestination.HOME.route) {
                    HomeScreen(
                        onAnimeClick = { id -> navController.navigate(Routes.detail(id)) },
                    )
                }
                composable(TopLevelDestination.SEARCH.route) {
                    PlaceholderScreen("Search", "Debounced AniList search lands here in the next step.")
                }
                composable(TopLevelDestination.LIBRARY.route) {
                    PlaceholderScreen("Library", "Watchlist, history and resume — backed by on-device Room.")
                }
                composable(TopLevelDestination.DOWNLOADS.route) {
                    PlaceholderScreen("Downloads", "Offline episodes with progress and storage usage.")
                }
                composable(TopLevelDestination.SETTINGS.route) {
                    PlaceholderScreen("Settings", "Theme, default quality, privacy statement and backup/restore.")
                }
                composable(Routes.DETAIL) { entry ->
                    val id = entry.arguments?.getString("anilistId")?.toIntOrNull() ?: return@composable
                    DetailScreen(
                        anilistId = id,
                        onBack = { navController.popBackStack() },
                        onAnimeClick = { relatedId -> navController.navigate(Routes.detail(relatedId)) },
                    )
                }
            }
        }
    }
}
