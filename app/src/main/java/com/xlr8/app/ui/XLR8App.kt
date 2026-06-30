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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.xlr8.app.data.settings.AppSettings
import com.xlr8.app.di.ServiceLocator
import com.xlr8.app.domain.model.TranslationType
import com.xlr8.app.ui.detail.DetailScreen
import com.xlr8.app.ui.downloads.DownloadsScreen
import com.xlr8.app.ui.easter.SecretMenuScreen
import com.xlr8.app.ui.home.HomeScreen
import com.xlr8.app.ui.library.LibraryScreen
import com.xlr8.app.ui.navigation.Routes
import com.xlr8.app.ui.navigation.TopLevelDestination
import com.xlr8.app.ui.search.SearchScreen
import com.xlr8.app.ui.settings.SettingsScreen
import com.xlr8.app.ui.player.PlayerScreen
import com.xlr8.app.ui.theme.XLR8Theme

@UnstableApi
@Composable
fun XLR8App() {
    val settings by ServiceLocator.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    XLR8Theme(
        themeMode = settings.themeMode,
        dynamicColor = settings.dynamicColor,
        amoled = settings.amoled,
    ) {
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
                        onResume = { id, episode -> navController.navigate(Routes.player(id, episode)) },
                        onOpenSecretMenu = { navController.navigate(Routes.SECRET_MENU) },
                    )
                }
                composable(TopLevelDestination.SEARCH.route) {
                    SearchScreen(
                        onAnimeClick = { id -> navController.navigate(Routes.detail(id)) },
                    )
                }
                composable(TopLevelDestination.LIBRARY.route) {
                    LibraryScreen(
                        onAnimeClick = { id -> navController.navigate(Routes.detail(id)) },
                    )
                }
                composable(TopLevelDestination.DOWNLOADS.route) {
                    DownloadsScreen(
                        onPlayOffline = { id, episode -> navController.navigate(Routes.player(id, episode)) },
                    )
                }
                composable(TopLevelDestination.SETTINGS.route) {
                    SettingsScreen()
                }
                composable(Routes.DETAIL) { entry ->
                    val id = entry.arguments?.getString("anilistId")?.toIntOrNull() ?: return@composable
                    DetailScreen(
                        anilistId = id,
                        onBack = { navController.popBackStack() },
                        onAnimeClick = { relatedId -> navController.navigate(Routes.detail(relatedId)) },
                        onPlayEpisode = { episode -> navController.navigate(Routes.player(id, episode)) },
                    )
                }
                composable(Routes.SECRET_MENU) {
                    SecretMenuScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.PLAYER) { entry ->
                    val id = entry.arguments?.getString("anilistId")?.toIntOrNull() ?: return@composable
                    val episode = entry.arguments?.getString("episode")?.toIntOrNull() ?: 1
                    val translation = if (entry.arguments?.getString("translation") == "dub") {
                        TranslationType.DUB
                    } else {
                        TranslationType.SUB
                    }
                    PlayerScreen(
                        anilistId = id,
                        episode = episode,
                        translation = translation,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
