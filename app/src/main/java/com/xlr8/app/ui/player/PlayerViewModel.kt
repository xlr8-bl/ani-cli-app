package com.xlr8.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.xlr8.app.data.remote.allanime.AllAnimeApi
import com.xlr8.app.data.repository.AllAnimeRepository
import com.xlr8.app.data.repository.AnimeDetailRepository
import com.xlr8.app.data.repository.DownloadRepository
import com.xlr8.app.data.repository.PlaybackRepository
import com.xlr8.app.di.ServiceLocator
import com.xlr8.app.domain.model.AllAnimeShow
import com.xlr8.app.domain.model.Anime
import com.xlr8.app.domain.model.TranslationType
import com.xlr8.app.domain.model.VideoSource
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** Set when AniList↔AllAnime matching failed and the user must link a source manually. */
    val needsManualLink: Boolean = false,
    val title: String = "",
    val anilistId: Int = 0,
    val translation: TranslationType = TranslationType.SUB,
    val availableTranslations: Set<TranslationType> = emptySet(),
    val episodes: List<Int> = emptyList(),
    val currentEpisode: Int = 0,
    val qualities: List<VideoSource> = emptyList(),
    val currentQuality: VideoSource? = null,
    /** Seconds remaining before the next episode auto-plays, or null when not counting down. */
    val autoNextCountdown: Int? = null,
    /** True while the current episode is playing from a local downloaded file. */
    val playingOffline: Boolean = false,
    /** One-shot user-facing message (e.g. "Download queued"); cleared after it's shown. */
    val message: String? = null,
) {
    val hasNext: Boolean get() = episodes.indexOf(currentEpisode).let { it >= 0 && it < episodes.lastIndex }
    val hasPrevious: Boolean get() = episodes.indexOf(currentEpisode) > 0
}

@UnstableApi
class PlayerViewModel(
    private val detailRepository: AnimeDetailRepository,
    private val allAnimeRepository: AllAnimeRepository,
    private val playbackRepository: PlaybackRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    constructor() : this(
        ServiceLocator.animeDetailRepository,
        ServiceLocator.allAnimeRepository,
        ServiceLocator.playbackRepository,
        ServiceLocator.downloadRepository,
    )

    val player: ExoPlayer = buildPlayer()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var anime: Anime? = null
    private var coverUrl: String? = null
    private var show: AllAnimeShow? = null
    private var episodeStrings: Map<Int, String> = emptyMap()
    private var progressJob: Job? = null
    private var countdownJob: Job? = null
    private var loaded = false

    private fun buildPlayer(): ExoPlayer {
        val player = ExoPlayer.Builder(ServiceLocator.applicationContext).build()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) onEpisodeEnded()
            }
        })
        return player
    }

    /** Entry point: fetch metadata, resolve the source, and start the requested episode. */
    fun load(anilistId: Int, episode: Int, translation: TranslationType) {
        if (loaded) return
        loaded = true
        _state.value = PlayerUiState(isLoading = true, anilistId = anilistId, translation = translation)
        viewModelScope.launch {
            try {
                val detail = detailRepository.detail(anilistId)
                anime = detail.anime
                coverUrl = detail.anime.coverImageUrl
                val resolvedShow = allAnimeRepository.resolveShow(detail.anime)
                if (resolvedShow == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        needsManualLink = true,
                        title = detail.anime.title,
                    )
                    return@launch
                }
                show = resolvedShow
                val available = buildSet {
                    if (resolvedShow.subEpisodes > 0) add(TranslationType.SUB)
                    if (resolvedShow.dubEpisodes > 0) add(TranslationType.DUB)
                }
                val effectiveTranslation =
                    if (translation in available || available.isEmpty()) translation
                    else available.first()
                _state.value = _state.value.copy(
                    title = detail.anime.title,
                    availableTranslations = available,
                    translation = effectiveTranslation,
                )
                loadEpisodeList(effectiveTranslation)
                playEpisode(episode, resume = true)
            } catch (t: Throwable) {
                // No network for metadata? If the episode is downloaded, play it offline anyway.
                val offline = downloadRepository.completedFor(anilistId, episode)
                if (offline != null) {
                    startOfflineOnly(anilistId, episode, offline.title, offline.coverImageUrl)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = t.message ?: "Couldn't start playback.",
                    )
                }
            }
        }
    }

    /** Plays a downloaded episode with no AniList/AllAnime calls (true offline mode). */
    private fun startOfflineOnly(anilistId: Int, episode: Int, title: String, cover: String?) {
        anime = Anime(
            anilistId = anilistId,
            title = title,
            englishTitle = null,
            nativeTitle = null,
            coverImageUrl = cover,
            bannerImageUrl = null,
            description = null,
            genres = emptyList(),
            averageScore = null,
            status = null,
            season = null,
            seasonYear = null,
            format = null,
            episodeCount = null,
            studio = null,
            nextAiringAt = null,
            nextAiringEpisode = null,
        )
        coverUrl = cover
        episodeStrings = mapOf(episode to episode.toString())
        _state.value = _state.value.copy(title = title, episodes = listOf(episode))
        playEpisode(episode, resume = true)
    }

    private suspend fun loadEpisodeList(translation: TranslationType) {
        val current = show ?: return
        val numbers = allAnimeRepository.episodeNumbers(current, translation)
        // Keep integer episodes for the UI while remembering the original AllAnime strings.
        val map = numbers.mapNotNull { s -> s.toDoubleOrNull()?.let { it.toInt() to s } }.toMap()
        episodeStrings = map
        _state.value = _state.value.copy(episodes = map.keys.sorted(), translation = translation)
    }

    /** Switches sub/dub, reloading the episode list and re-resolving the current episode. */
    fun switchTranslation(translation: TranslationType) {
        if (translation == _state.value.translation) return
        viewModelScope.launch {
            saveProgress()
            loadEpisodeList(translation)
            val target = _state.value.currentEpisode.takeIf { episodeStrings.containsKey(it) }
                ?: _state.value.episodes.firstOrNull() ?: return@launch
            playEpisode(target, resume = false)
        }
    }

    fun playEpisode(episode: Int, resume: Boolean) {
        cancelAutoNext()
        val translation = _state.value.translation
        _state.value = _state.value.copy(isLoading = true, currentEpisode = episode, errorMessage = null)
        viewModelScope.launch {
            try {
                // Offline-first: if this episode is downloaded, play the local file and skip the network.
                // Checked before requiring the online show, so playback works with no connectivity.
                val offline = downloadRepository.completedFor(_state.value.anilistId, episode)
                if (offline != null) {
                    val localSource = VideoSource(
                        url = File(offline.localPath).toUri().toString(),
                        quality = offline.quality,
                        isHls = false,
                        providerName = "Downloaded",
                    )
                    val resumeMs = if (resume) {
                        playbackRepository.progressFor(_state.value.anilistId, episode)
                            ?.takeIf { !it.isFinished }?.positionMs ?: 0L
                    } else 0L
                    _state.value = _state.value.copy(
                        isLoading = false,
                        playingOffline = true,
                        qualities = listOf(localSource),
                        currentQuality = localSource,
                    )
                    prepare(localSource, resumeMs)
                    startProgressTicker()
                    return@launch
                }

                val current = show
                if (current == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "This episode isn't downloaded and no source is available offline.",
                    )
                    return@launch
                }
                val episodeString = episodeStrings[episode] ?: episode.toString()
                val sources = allAnimeRepository.episodeSources(current, translation, episodeString)
                if (sources.isEmpty()) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "No playable sources for episode $episode.",
                    )
                    return@launch
                }
                val preferred = sources.first() // already sorted best-first
                val resumeMs = if (resume) {
                    playbackRepository.progressFor(_state.value.anilistId, episode)
                        ?.takeIf { !it.isFinished }?.positionMs ?: 0L
                } else 0L

                _state.value = _state.value.copy(
                    isLoading = false,
                    playingOffline = false,
                    qualities = sources,
                    currentQuality = preferred,
                )
                prepare(preferred, resumeMs)
                startProgressTicker()
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "Couldn't resolve this episode.",
                )
            }
        }
    }

    /** Manual quality override — keeps the current position. */
    fun selectQuality(source: VideoSource) {
        if (source.url == _state.value.currentQuality?.url) return
        val position = player.currentPosition
        _state.value = _state.value.copy(currentQuality = source)
        prepare(source, position)
    }

    fun playNext() {
        val s = _state.value
        val idx = s.episodes.indexOf(s.currentEpisode)
        if (idx in 0 until s.episodes.lastIndex) playEpisode(s.episodes[idx + 1], resume = false)
    }

    fun playPrevious() {
        val s = _state.value
        val idx = s.episodes.indexOf(s.currentEpisode)
        if (idx > 0) playEpisode(s.episodes[idx - 1], resume = false)
    }

    private fun prepare(source: VideoSource, positionMs: Long) {
        val mediaItem = MediaItem.Builder()
            .setUri(source.url)
            .apply { if (source.isHls) setMimeType(MimeTypes.APPLICATION_M3U8) }
            .build()
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(source.headers["User-Agent"] ?: AllAnimeApi.USER_AGENT)
            .setDefaultRequestProperties(source.headers)
            .setAllowCrossProtocolRedirects(true)
        // DefaultDataSource also handles file:// for downloaded episodes.
        val factory = DefaultDataSource.Factory(ServiceLocator.applicationContext, httpFactory)
        player.setMediaSource(DefaultMediaSourceFactory(factory).createMediaSource(mediaItem))
        player.prepare()
        if (positionMs > 0) player.seekTo(positionMs)
        player.playWhenReady = true
    }

    private fun onEpisodeEnded() {
        viewModelScope.launch { saveProgress() }
        if (!_state.value.hasNext) return
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (seconds in 5 downTo 1) {
                _state.value = _state.value.copy(autoNextCountdown = seconds)
                delay(1_000)
            }
            _state.value = _state.value.copy(autoNextCountdown = null)
            playNext()
        }
    }

    /** Queues an offline download of the current episode at the best progressive quality. */
    fun downloadCurrent() {
        val a = anime ?: return
        val s = _state.value
        if (s.playingOffline) {
            _state.value = s.copy(message = "Already downloaded.")
            return
        }
        val source = s.currentQuality?.takeIf { !it.isHls }
            ?: s.qualities.firstOrNull { !it.isHls }
        if (source == null) {
            _state.value = s.copy(message = "This source is HLS-only; offline download isn't supported yet.")
            return
        }
        viewModelScope.launch {
            downloadRepository.enqueue(
                anilistId = s.anilistId,
                episode = s.currentEpisode,
                translation = s.translation,
                title = a.title,
                coverImageUrl = coverUrl,
                source = source,
            )
            _state.value = _state.value.copy(message = "Download queued (${source.quality}).")
        }
    }

    fun consumeMessage() {
        if (_state.value.message != null) _state.value = _state.value.copy(message = null)
    }

    fun cancelAutoNext() {
        countdownJob?.cancel()
        countdownJob = null
        if (_state.value.autoNextCountdown != null) {
            _state.value = _state.value.copy(autoNextCountdown = null)
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                delay(5_000)
                saveProgress()
            }
        }
    }

    /** Fire-and-forget save, used on lifecycle pause/stop while the VM is still alive. */
    fun saveNow() {
        viewModelScope.launch { saveProgress() }
    }

    suspend fun saveProgress() {
        val a = anime ?: return
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        playbackRepository.save(
            anilistId = _state.value.anilistId,
            episode = _state.value.currentEpisode,
            positionMs = player.currentPosition,
            durationMs = duration,
            translation = _state.value.translation,
            title = a.title,
            coverImageUrl = coverUrl,
        )
    }

    override fun onCleared() {
        // The screen calls saveNow() on pause; by here viewModelScope is cancelled, so just
        // tear the player down cleanly.
        progressJob?.cancel()
        countdownJob?.cancel()
        player.release()
    }
}
