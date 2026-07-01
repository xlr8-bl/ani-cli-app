package com.xlr8.app.ui.home

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xlr8.app.R
import com.xlr8.app.ui.components.ContinueWatchingRow
import com.xlr8.app.ui.components.HeroCarousel
import com.xlr8.app.ui.components.SectionRow
import com.xlr8.app.ui.easter.EasterEggs

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAnimeClick: (Int) -> Unit,
    onResume: (anilistId: Int, episode: Int) -> Unit = { _, _ -> },
    onOpenSecretMenu: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        XLR8Header(onOpenSecretMenu = onOpenSecretMenu)
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when {
                state.isLoading -> LoadingState()
                state.errorMessage != null && !state.hasContent ->
                    ErrorState(message = state.errorMessage!!, onRetry = viewModel::retry)
                else -> HomeContent(state = state, onAnimeClick = onAnimeClick, onResume = onResume)
            }
        }
    }
}

/** Brand header. Tapping the logo 8 times within a few seconds unlocks the Secret Menu. */
@Composable
private fun XLR8Header(onOpenSecretMenu: () -> Unit) {
    val context = LocalContext.current
    var taps by remember { mutableIntStateOf(0) }
    var lastTapAt by remember { mutableLongStateOf(0L) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val now = System.currentTimeMillis()
                taps = if (now - lastTapAt < 1500) taps + 1 else 1
                lastTapAt = now
                if (taps >= 8) {
                    taps = 0
                    Toast.makeText(context, EasterEggs.BLEACH_MANTRA, Toast.LENGTH_SHORT).show()
                    onOpenSecretMenu()
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Manga head + shout-bubble lockup; light/dark asset chosen by theme (drawable-night).
        Image(
            painter = painterResource(R.drawable.xlr8_banner),
            contentDescription = "XLR8",
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(58.dp),
        )
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

/** Skeleton placeholders with a subtle shimmer while the first feeds load. */
@Composable
private fun LoadingState() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton-alpha",
    )
    val shimmer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero placeholder.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shimmer),
        )
        repeat(2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmer),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(shimmer),
                    )
                }
            }
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
