package com.xlr8.app.data.remote.anilist

import com.xlr8.app.domain.model.Anime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Generic GraphQL request envelope. */
@Serializable
data class GraphQLRequest(
    val query: String,
    val variables: kotlinx.serialization.json.JsonObject,
)

@Serializable
data class GraphQLResponse<T>(
    val data: T? = null,
    val errors: List<GraphQLError>? = null,
)

@Serializable
data class GraphQLError(val message: String)

// ---- Page / Media DTOs ----

@Serializable
data class PageData(@SerialName("Page") val page: Page)

@Serializable
data class Page(
    val media: List<MediaDto> = emptyList(),
    val airingSchedules: List<AiringScheduleDto> = emptyList(),
)

@Serializable
data class MediaTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null,
)

@Serializable
data class MediaCoverImage(
    val extraLarge: String? = null,
    val large: String? = null,
    val color: String? = null,
)

@Serializable
data class StudioConnection(val nodes: List<StudioNode> = emptyList())

@Serializable
data class StudioNode(val name: String? = null, val isAnimationStudio: Boolean = false)

@Serializable
data class NextAiringEpisode(
    val airingAt: Long? = null,
    val episode: Int? = null,
)

@Serializable
data class MediaDto(
    val id: Int,
    val title: MediaTitle? = null,
    val coverImage: MediaCoverImage? = null,
    val bannerImage: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val averageScore: Int? = null,
    val status: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val format: String? = null,
    val episodes: Int? = null,
    val studios: StudioConnection? = null,
    val nextAiringEpisode: NextAiringEpisode? = null,
) {
    fun toDomain(): Anime = Anime(
        anilistId = id,
        title = title?.romaji ?: title?.english ?: "Untitled",
        englishTitle = title?.english,
        nativeTitle = title?.native,
        coverImageUrl = coverImage?.extraLarge ?: coverImage?.large,
        bannerImageUrl = bannerImage,
        description = description,
        genres = genres,
        averageScore = averageScore,
        status = status,
        season = season,
        seasonYear = seasonYear,
        format = format,
        episodeCount = episodes,
        studio = studios?.nodes?.firstOrNull { it.isAnimationStudio }?.name
            ?: studios?.nodes?.firstOrNull()?.name,
        nextAiringAt = nextAiringEpisode?.airingAt,
        nextAiringEpisode = nextAiringEpisode?.episode,
    )
}

// ---- Airing schedule (for "Just Aired" / NEW detection) ----

@Serializable
data class AiringScheduleDto(
    val id: Int,
    val airingAt: Long,
    val episode: Int,
    val media: MediaDto? = null,
)
