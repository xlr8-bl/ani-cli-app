package com.xlr8.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xlr8.app.domain.model.TranslationType
import com.xlr8.app.domain.model.VideoSource

/** Full custom controls overlay drawn above the video. */
@Composable
fun PlayerControls(
    state: PlayerUiState,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectQuality: (VideoSource) -> Unit,
    onSelectEpisode: (Int) -> Unit,
    onSwitchTranslation: (TranslationType) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onEnterPip: () -> Unit,
    onDownload: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.55f),
                    0.25f to Color.Transparent,
                    0.75f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.7f),
                ),
            ),
    ) {
        // ---- Top bar: back, title, source menus ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Episode ${state.currentEpisode} · ${state.translation.name.lowercase()}",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state.availableTranslations.size > 1) {
                TranslationMenu(state.translation, onSwitchTranslation)
            }
            EpisodeMenu(state.episodes, onSelectEpisode)
            QualityMenu(state.qualities, state.currentQuality, onSelectQuality)
            IconButton(onClick = onDownload) {
                Icon(
                    if (state.playingOffline) Icons.Filled.Check else Icons.Filled.Download,
                    contentDescription = "Download episode",
                    tint = Color.White,
                )
            }
            SpeedMenu(onSetSpeed)
            IconButton(onClick = onEnterPip) {
                Icon(Icons.Filled.PictureInPictureAlt, contentDescription = "Picture in picture", tint = Color.White)
            }
        }

        // ---- Center: prev / play-pause / next ----
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            IconButton(onClick = onPrevious, enabled = state.hasPrevious) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Previous episode",
                    tint = if (state.hasPrevious) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(40.dp),
                )
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp),
                )
            }
            IconButton(onClick = onNext, enabled = state.hasNext) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next episode",
                    tint = if (state.hasNext) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        // ---- Bottom: seek bar + times ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formatTime(positionMs), color = Color.White, style = MaterialTheme.typography.labelMedium)
            Slider(
                value = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
                onValueChange = { fraction -> if (durationMs > 0) onSeek((fraction * durationMs).toLong()) },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
            Text(formatTime(durationMs), color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun QualityMenu(
    qualities: List<VideoSource>,
    current: VideoSource?,
    onSelect: (VideoSource) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.HighQuality, contentDescription = "Quality", tint = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (qualities.isEmpty()) {
                DropdownMenuItem(text = { Text("No sources") }, onClick = { expanded = false })
            }
            qualities.forEach { source ->
                val label = buildString {
                    append(if (source.heightOrZero > 0) "${source.heightOrZero}p" else source.quality)
                    source.providerName?.let { append("  ·  $it") }
                }
                DropdownMenuItem(
                    text = { Text(label, fontWeight = if (source.url == current?.url) FontWeight.Bold else null) },
                    onClick = { onSelect(source); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun EpisodeMenu(episodes: List<Int>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    if (episodes.isEmpty()) return
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.VideoLibrary, contentDescription = "Episodes", tint = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            episodes.forEach { ep ->
                DropdownMenuItem(text = { Text("Episode $ep") }, onClick = { onSelect(ep); expanded = false })
            }
        }
    }
}

@Composable
private fun TranslationMenu(current: TranslationType, onSwitch: (TranslationType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Translate, contentDescription = "Sub or dub", tint = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TranslationType.entries.forEach { t ->
                DropdownMenuItem(
                    text = { Text(t.name, fontWeight = if (t == current) FontWeight.Bold else null) },
                    onClick = { onSwitch(t); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun SpeedMenu(onSetSpeed: (Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Speed, contentDescription = "Playback speed", tint = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            speeds.forEach { speed ->
                DropdownMenuItem(
                    text = { Text("${speed}x") },
                    onClick = { onSetSpeed(speed); expanded = false },
                )
            }
        }
    }
}
