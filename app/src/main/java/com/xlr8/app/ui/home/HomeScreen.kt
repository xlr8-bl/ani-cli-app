package com.xlr8.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xlr8.app.R
import com.xlr8.app.ui.components.ContinueWatchingRow
import com.xlr8.app.ui.components.HeroCarousel
import com.xlr8.app.ui.components.SectionRow

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAnimeClick: (Int) -> Unit,
    onResume: (anilistId: Int, episode: Int) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading -> LoadingState()
            state.errorMessage != null && !state.hasContent ->
                ErrorState(message = state.errorMessage!!, onRetry = viewModel::retry)
            else -> HomeContent(state = state, onAnimeClick = onAnimeClick, onResume = onResume)
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onAnimeClick: (Int) -> Unit,
    onResume: (anilistId: Int, episode: Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (state.spotlight.isNotEmpty()) {
            item(key = "hero") {
                HeroCarousel(items = state.spotlight, onAnimeClick = onAnimeClick)
            }
        }
        if (state.continueWatching.isNotEmpty()) {
            item(key = "continue") {
                ContinueWatchingRow(items = state.continueWatching, onResume = onResume)
            }
        }
        if (state.justAired.isNotEmpty()) {
            item(key = "just_aired") {
                SectionRow(
                    title = stringResource(R.string.row_just_aired),
                    items = state.justAired.map { it.anime },
                    onAnimeClick = onAnimeClick,
                    isNew = { true },
                    subtitle = { anime ->
                        state.justAired.firstOrNull { it.anime.anilistId == anime.anilistId }
                            ?.let { "Ep ${it.episode}" }
                    },
                )
            }
        }
        item(key = "trending") {
            SectionRow(
                title = stringResource(R.string.row_trending),
                items = state.trending,
                onAnimeClick = onAnimeClick,
            )
        }
        item(key = "popular") {
            SectionRow(
                title = stringResource(R.string.row_popular_season),
                items = state.popularThisSeason,
                onAnimeClick = onAnimeClick,
            )
        }
        item(key = "upcoming") {
            SectionRow(
                title = stringResource(R.string.row_new_releases),
                items = state.upcoming,
                onAnimeClick = onAnimeClick,
            )
        }
        item(key = "top100") {
            SectionRow(
                title = stringResource(R.string.row_top_100),
                items = state.top100,
                onAnimeClick = onAnimeClick,
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                text = "the bleach is peak and the peak is bleach",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.state_error),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}
