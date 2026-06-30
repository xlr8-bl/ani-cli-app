package com.xlr8.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xlr8.app.data.local.WatchlistEntity
import com.xlr8.app.domain.model.Anime
import com.xlr8.app.ui.components.AnimePosterCard

@Composable
fun LibraryScreen(
    onAnimeClick: (Int) -> Unit,
    viewModel: LibraryViewModel = viewModel(),
) {
    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Watchlist",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        if (watchlist.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    "Your watchlist is empty.\nAdd shows from their detail page.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
            return
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(watchlist, key = { it.anilistId }) { entry ->
                AnimePosterCard(
                    anime = entry.toAnime(),
                    onClick = { onAnimeClick(entry.anilistId) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun WatchlistEntity.toAnime(): Anime = Anime(
    anilistId = anilistId,
    title = title,
    englishTitle = null,
    nativeTitle = null,
    coverImageUrl = coverImageUrl,
    bannerImageUrl = null,
    description = null,
    genres = emptyList(),
    averageScore = averageScore,
    status = null,
    season = null,
    seasonYear = null,
    format = format,
    episodeCount = null,
    studio = null,
    nextAiringAt = null,
    nextAiringEpisode = null,
)
