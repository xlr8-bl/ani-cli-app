package com.xlr8.app.domain.model

/** A show as XLR8 surfaces it across the UI, merged from AniList metadata. */
data class Anime(
    val anilistId: Int,
    val title: String,
    val englishTitle: String?,
    val nativeTitle: String?,
    val coverImageUrl: String?,
    val bannerImageUrl: String?,
    val description: String?,
    val genres: List<String>,
    val averageScore: Int?,        // 0..100
    val status: String?,           // RELEASING, FINISHED, NOT_YET_RELEASED, ...
    val season: String?,           // WINTER, SPRING, SUMMER, FALL
    val seasonYear: Int?,
    val format: String?,           // TV, MOVIE, OVA, ...
    val episodeCount: Int?,
    val studio: String?,
    /** Epoch seconds the next episode airs, when known. */
    val nextAiringAt: Long?,
    /** Episode number that airs next, when known. */
    val nextAiringEpisode: Int?,
) {
    /** Score on the familiar 0–10 scale, or null when AniList has no score yet. */
    val scoreOutOfTen: Double? get() = averageScore?.let { it / 10.0 }
}
