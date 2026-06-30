package com.xlr8.app.data.remote.allanime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AllAnimeResponse<T>(val data: T? = null)

// ---- Search ----

@Serializable
data class ShowsData(val shows: ShowsConnection = ShowsConnection())

@Serializable
data class ShowsConnection(val edges: List<ShowEdge> = emptyList())

@Serializable
data class ShowEdge(
    @SerialName("_id") val id: String,
    val name: String = "",
    val availableEpisodes: AvailableEpisodes? = null,
)

@Serializable
data class AvailableEpisodes(
    val sub: Int = 0,
    val dub: Int = 0,
    val raw: Int = 0,
)

// ---- Episodes detail ----

@Serializable
data class ShowDetailData(val show: ShowDetail? = null)

@Serializable
data class ShowDetail(
    @SerialName("_id") val id: String,
    val availableEpisodesDetail: EpisodesDetail = EpisodesDetail(),
)

@Serializable
data class EpisodesDetail(
    val sub: List<String> = emptyList(),
    val dub: List<String> = emptyList(),
    val raw: List<String> = emptyList(),
)

// ---- Episode sources ----

@Serializable
data class EpisodeData(val episode: EpisodeSources? = null)

@Serializable
data class EpisodeSources(
    val episodeString: String? = null,
    val sourceUrls: List<SourceUrlDto> = emptyList(),
)

@Serializable
data class SourceUrlDto(
    val sourceUrl: String,
    val priority: Double = 0.0,
    val sourceName: String? = null,
    val type: String? = null,
    val className: String? = null,
)

// ---- Clock provider JSON (the de-obfuscated link list) ----

@Serializable
data class ClockResponse(val links: List<ClockLink> = emptyList())

@Serializable
data class ClockLink(
    val link: String = "",
    val resolutionStr: String? = null,
    val hls: Boolean? = null,
    val mp4: Boolean? = null,
    val src: String? = null,
)
