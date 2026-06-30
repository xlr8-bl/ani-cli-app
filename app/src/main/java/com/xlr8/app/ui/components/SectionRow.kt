package com.xlr8.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xlr8.app.domain.model.Anime

/** A titled horizontal row of poster cards. */
@Composable
fun SectionRow(
    title: String,
    items: List<Anime>,
    onAnimeClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isNew: (Anime) -> Boolean = { false },
    subtitle: (Anime) -> String? = { null },
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.anilistId }) { anime ->
                AnimePosterCard(
                    anime = anime,
                    onClick = { onAnimeClick(anime.anilistId) },
                    isNew = isNew(anime),
                    subtitle = subtitle(anime),
                )
            }
        }
    }
}
