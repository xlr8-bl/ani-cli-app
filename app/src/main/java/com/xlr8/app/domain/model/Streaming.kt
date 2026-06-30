package com.xlr8.app.domain.model

/** Sub vs dub, mapped to the value AllAnime's API expects. */
enum class TranslationType(val apiValue: String) {
    SUB("sub"),
    DUB("dub"),
}

/** A show as identified on AllAnime (the playable-source side). */
data class AllAnimeShow(
    val id: String,
    val name: String,
    val subEpisodes: Int,
    val dubEpisodes: Int,
) {
    fun episodeCount(translation: TranslationType): Int =
        if (translation == TranslationType.DUB) dubEpisodes else subEpisodes
}

/** A single resolvable video stream for an episode at a given quality. */
data class VideoSource(
    val url: String,
    /** e.g. "1080", "720", "auto". */
    val quality: String,
    val isHls: Boolean,
    /** Human label for where the stream came from (provider/source name). */
    val providerName: String? = null,
    /** Headers some CDNs require for playback (Referer/User-Agent). */
    val headers: Map<String, String> = emptyMap(),
) {
    /** Numeric height parsed from [quality], for sorting; 0 when unknown ("auto"/"best"). */
    val heightOrZero: Int
        get() = Regex("(\\d{3,4})").find(quality)?.value?.toIntOrNull() ?: 0
}
