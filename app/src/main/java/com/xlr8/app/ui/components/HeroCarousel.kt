package com.xlr8.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.xlr8.app.domain.model.Anime

/** Swipeable spotlight banners at the top of Home. */
@Composable
fun HeroCarousel(
    items: List<Anime>,
    onAnimeClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { items.size })

    Box(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(state = pagerState) { page ->
            val anime = items[page]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clickable { onAnimeClick(anime.anilistId) },
            ) {
                AsyncImage(
                    model = anime.bannerImageUrl ?: anime.coverImageUrl,
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // Bottom scrim so the title stays legible over any banner.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.45f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.85f),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
                ) {
                    anime.genres.take(3).joinToString(" · ").takeIf { it.isNotEmpty() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                    Text(
                        text = anime.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Page indicator dots.
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
        ) {
            repeat(items.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (selected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.4f)),
                )
            }
        }
    }
}
