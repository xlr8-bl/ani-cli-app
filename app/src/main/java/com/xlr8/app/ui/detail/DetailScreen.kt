package com.xlr8.app.ui.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xlr8.app.domain.model.AnimeDetail
import com.xlr8.app.ui.easter.EasterEggs
import com.xlr8.app.util.stripHtml

/**
 * Full Detail page: banner hero, synopsis, season selector, episode grid with real
 * thumbnails, character/voice-actor row and a Related row. Tapping a season switches
 * in place; tapping a related show navigates to it.
 */
@Composable
fun DetailScreen(
    anilistId: Int,
    onBack: () -> Unit,
    onAnimeClick: (Int) -> Unit = {},
    onPlayEpisode: (Int) -> Unit = {},
    viewModel: DetailViewModel = viewModel(),
) {
    LaunchedEffect(anilistId) { viewModel.load(anilistId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            state.detail != null -> {
                val inWatchlist by viewModel.inWatchlist.collectAsStateWithLifecycle()
                DetailContent(
                    detail = state.detail!!,
                    inWatchlist = inWatchlist,
                    onToggleWatchlist = viewModel::toggleWatchlist,
                    onSeasonClick = viewModel::switchTo,
                    onRelatedClick = onAnimeClick,
                    onPlayEpisode = onPlayEpisode,
                )
            }
            else -> DetailError(message = state.errorMessage, onRetry = viewModel::retry)
        }

        // Floating back button over the hero.
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(8.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f)),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        // Hidden in-joke banner (One Piece roast / Bleach mantra), dismissible & auto-hiding.
        state.easterEgg?.let { egg ->
            LaunchedEffect(egg) {
                kotlinx.coroutines.delay(6000)
                viewModel.consumeEasterEgg()
            }
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clickable { viewModel.consumeEasterEgg() },
            ) {
                Text(
                    text = egg,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: AnimeDetail,
    inWatchlist: Boolean,
    onToggleWatchlist: () -> Unit,
    onSeasonClick: (Int) -> Unit,
    onRelatedClick: (Int) -> Unit,
    onPlayEpisode: (Int) -> Unit,
) {
    val anime = detail.anime

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item(key = "hero") { Hero(detail) }

        item(key = "actions") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val firstEpisode = detail.episodes.firstOrNull()?.number ?: 1
                Button(
                    onClick = { onPlayEpisode(firstEpisode) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (detail.episodes.isEmpty()) "Play" else "Play Ep $firstEpisode")
                }
                FilledTonalButton(onClick = onToggleWatchlist) {
                    Icon(
                        if (inWatchlist) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Watchlist",
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (inWatchlist) "Saved" else "Watchlist")
                }
            }
        }

        anime.description?.takeIf { it.isNotBlank() }?.let { desc ->
            item(key = "synopsis") { Synopsis(desc.stripHtml()) }
        }

        if (detail.seasons.isNotEmpty()) {
            item(key = "seasons") {
                SectionHeader("Seasons & Related Arcs")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(detail.seasons, key = { it.anilistId }) { related ->
                        RelatedCard(related = related, onClick = { onSeasonClick(related.anilistId) })
                    }
                }
            }
        }

        if (detail.episodes.isNotEmpty()) {
            item(key = "episodes_header") {
                SectionHeader("Episodes · ${detail.episodes.size}")
            }
            // Two-column grid emitted as chunked rows so it scrolls with the page.
            items(detail.episodes.chunked(2), key = { it.first().number }) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowItems.forEach { ep ->
                        EpisodeCard(
                            episode = ep,
                            onClick = { onPlayEpisode(ep.number) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        if (detail.characters.isNotEmpty()) {
            item(key = "characters") {
                SectionHeader("Characters & Voice Actors")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(detail.characters, key = { it.characterId }) { role ->
                        CharacterCard(role)
                    }
                }
            }
        }

        if (detail.otherRelations.isNotEmpty()) {
            item(key = "related") {
                SectionHeader("Related")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(detail.otherRelations, key = { it.anilistId }) { related ->
                        RelatedCard(related = related, onClick = { onRelatedClick(related.anilistId) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Hero(detail: AnimeDetail) {
    val anime = detail.anime
    Column(modifier = Modifier.fillMaxWidth()) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
    ) {
        AsyncImage(
            model = anime.bannerImageUrl ?: anime.coverImageUrl,
            contentDescription = anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.3f to Color.Transparent,
                        1f to MaterialTheme.colorScheme.background,
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            AsyncImage(
                model = anime.coverImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                anime.englishTitle?.takeIf { it != anime.title }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    anime.scoreOutOfTen?.let { MetaChip("★ ${"%.1f".format(it)}") }
                    anime.status?.let { MetaChip(prettyRelation(it)) }
                    anime.seasonYear?.let { MetaChip(it.toString()) }
                    // Hidden 1-star "community rating" that only ever appears on One Piece.
                    if (EasterEggs.isOnePiece(anime)) MetaChip("★ 1.0 community")
                }
            }
        }
    }
    // Genre chips below the hero. Long-press any genre to reveal the REAL Big Three.
    if (anime.genres.isNotEmpty()) {
        var showBigThree by remember { mutableStateOf(false) }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            items(anime.genres) { genre ->
                MetaChip(
                    genre,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { showBigThree = true },
                    ),
                )
            }
        }
        if (showBigThree) {
            Text(
                text = "${EasterEggs.REAL_BIG_THREE_CAPTION}  ${EasterEggs.REAL_BIG_THREE.joinToString(" · ")}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp),
            )
        }
    }
    }
}

@Composable
private fun Synopsis(text: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { expanded = !expanded },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (expanded) "Show less" else "Show more",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun DetailError(message: String?, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Couldn't load this show", style = MaterialTheme.typography.titleMedium)
            message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Retry") }
        }
    }
}
