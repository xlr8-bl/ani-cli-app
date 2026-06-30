package com.xlr8.app.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xlr8.app.data.local.DownloadEntity
import com.xlr8.app.data.local.DownloadStatus
import java.util.Locale

@Composable
fun DownloadsScreen(
    onPlayOffline: (anilistId: Int, episode: Int) -> Unit,
    viewModel: DownloadsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Downloads",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        )
        Text(
            text = "${formatBytes(state.storageUsedBytes)} used",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
        )

        if (state.downloads.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    "No downloads yet.\nTap the download icon in the player to save an episode offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.downloads, key = { it.anilistId * 100000L + it.episode }) { download ->
                DownloadRow(
                    download = download,
                    onPlay = { onPlayOffline(download.anilistId, download.episode) },
                    onPause = { viewModel.pause(download) },
                    onResume = { viewModel.resume(download) },
                    onDelete = { viewModel.delete(download) },
                )
            }
        }
    }
}

@Composable
private fun DownloadRow(
    download: DownloadEntity,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
) {
    val status = download.statusEnum()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = status == DownloadStatus.COMPLETED, onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = download.coverImageUrl,
            contentDescription = download.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 64.dp, height = 90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = download.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Episode ${download.episode} · ${download.quality}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = statusLabel(download),
                style = MaterialTheme.typography.labelSmall,
                color = when (status) {
                    DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                    DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (status == DownloadStatus.RUNNING || status == DownloadStatus.QUEUED || status == DownloadStatus.PAUSED) {
                LinearProgressIndicator(
                    progress = { download.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(top = 4.dp),
                )
            }
        }
        when (status) {
            DownloadStatus.RUNNING, DownloadStatus.QUEUED ->
                IconButton(onClick = onPause) {
                    Icon(Icons.Filled.Pause, contentDescription = "Pause")
                }
            DownloadStatus.PAUSED, DownloadStatus.FAILED ->
                IconButton(onClick = onResume) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Resume")
                }
            DownloadStatus.COMPLETED ->
                IconButton(onClick = onPlay) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete")
        }
    }
}

private fun statusLabel(download: DownloadEntity): String = when (download.statusEnum()) {
    DownloadStatus.QUEUED -> "Queued"
    DownloadStatus.RUNNING -> "Downloading… ${(download.progressFraction * 100).toInt()}%"
    DownloadStatus.PAUSED -> "Paused · ${(download.progressFraction * 100).toInt()}%"
    DownloadStatus.COMPLETED -> "Downloaded · ${formatBytes(download.downloadedBytes)}"
    DownloadStatus.FAILED -> "Failed — tap resume to retry"
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format(Locale.US, "%.1f GB", mb / 1024)
    else String.format(Locale.US, "%.0f MB", mb)
}
