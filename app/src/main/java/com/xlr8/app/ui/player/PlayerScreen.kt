package com.xlr8.app.ui.player

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.xlr8.app.domain.model.TranslationType
import kotlinx.coroutines.delay

@UnstableApi
@Composable
fun PlayerScreen(
    anilistId: Int,
    episode: Int,
    translation: TranslationType,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { viewModel.load(anilistId, episode, translation) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val player = viewModel.player

    // Force landscape while the player is on screen; restore on exit.
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Pause-safe: save progress when the app is backgrounded.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.saveNow()
                Lifecycle.Event.ON_STOP -> {
                    viewModel.saveNow()
                    player.pause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Live transport state, polled cheaply for the seek bar.
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition
            durationMs = player.duration.coerceAtLeast(0L)
            isPlaying = player.isPlaying
            delay(500)
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3500)
            controlsVisible = false
        }
    }
    var overlayHint by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(overlayHint) {
        if (overlayHint != null) {
            delay(1200)
            overlayHint = null
        }
    }

    // Surface one-shot messages (download queued, HLS unsupported, ...) as a brief hint.
    LaunchedEffect(state.message) {
        state.message?.let {
            overlayHint = it
            viewModel.consumeMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    keepScreenOn = true
                    this.player = player
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Gesture layer: tap toggles controls, double-tap seeks, vertical drags
        // change brightness (left half) and volume (right half).
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible },
                            onDoubleTap = { offset ->
                                if (offset.x < widthPx / 2) {
                                    player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
                                    overlayHint = "« 10s"
                                } else {
                                    player.seekTo(player.currentPosition + 10_000)
                                    overlayHint = "10s »"
                                }
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        var onLeft = true
                        detectVerticalDragGestures(
                            onDragStart = { offset -> onLeft = offset.x < widthPx / 2 },
                        ) { change, dragAmount ->
                            change.consume()
                            // Drag up (negative) increases.
                            val deltaFraction = -dragAmount / heightPx
                            if (onLeft) {
                                val lp = activity?.window?.attributes
                                if (lp != null) {
                                    val cur = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                                    val next = (cur + deltaFraction).coerceIn(0.01f, 1f)
                                    lp.screenBrightness = next
                                    activity.window.attributes = lp
                                    overlayHint = "Brightness ${(next * 100).toInt()}%"
                                }
                            } else if (audioManager != null) {
                                val step = if (dragAmount < 0) 1 else -1
                                // Throttle so a single swipe doesn't blast through the range.
                                if (kotlin.math.abs(dragAmount) > 6f) {
                                    val frac = audioManager.nudgeVolume(step)
                                    overlayHint = "Volume ${(frac * 100).toInt()}%"
                                }
                            }
                        }
                    },
            )
        }

        overlayHint?.let { hint ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = hint,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        state.errorMessage?.let { msg ->
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(msg, color = Color.White, modifier = Modifier.padding(24.dp))
            }
        }

        // Auto-next banner.
        state.autoNextCountdown?.let { seconds ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Next episode in ${seconds}s", color = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "PLAY NOW",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures { viewModel.playNext() }
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "CANCEL",
                        color = Color.White,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures { viewModel.cancelAutoNext() }
                        },
                    )
                }
            }
        }

        AnimatedVisibility(visible = controlsVisible) {
            PlayerControls(
                state = state,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                onBack = onBack,
                onPlayPause = { if (isPlaying) player.pause() else player.play() },
                onSeek = { player.seekTo(it) },
                onPrevious = viewModel::playPrevious,
                onNext = viewModel::playNext,
                onSelectQuality = viewModel::selectQuality,
                onSelectEpisode = { viewModel.playEpisode(it, resume = false) },
                onSwitchTranslation = viewModel::switchTranslation,
                onSetSpeed = { player.setPlaybackSpeed(it) },
                onEnterPip = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        activity?.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                    }
                },
                onDownload = viewModel::downloadCurrent,
            )
        }
    }
}
