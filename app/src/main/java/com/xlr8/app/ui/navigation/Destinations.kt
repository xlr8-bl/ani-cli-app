package com.xlr8.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.xlr8.app.R

/** Top-level tabs shown in the bottom navigation bar. */
enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    HOME("home", R.string.nav_home, Icons.Outlined.Home),
    SEARCH("search", R.string.nav_search, Icons.Outlined.Search),
    LIBRARY("library", R.string.nav_library, Icons.Outlined.VideoLibrary),
    DOWNLOADS("downloads", R.string.nav_downloads, Icons.Outlined.Download),
    SETTINGS("settings", R.string.nav_settings, Icons.Outlined.Settings),
}

/** Non-tab routes. */
object Routes {
    const val DETAIL = "detail/{anilistId}"
    fun detail(anilistId: Int) = "detail/$anilistId"
}
