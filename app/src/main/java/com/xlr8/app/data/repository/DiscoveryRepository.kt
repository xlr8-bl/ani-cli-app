package com.xlr8.app.data.repository

import com.xlr8.app.data.AnimeSeason
import com.xlr8.app.data.remote.anilist.AniListService
import com.xlr8.app.domain.model.Anime

/** A show plus the freshly-aired episode that surfaced it. */
data class JustAired(
    val anime: Anime,
    val episode: Int,
    val airedAt: Long,
)

/**
 * Pulls the AniList-backed discovery feeds that populate the Home screen.
 * Pure metadata; playable sources are resolved separately via AllAnime.
 */
class DiscoveryRepository(private val anilist: AniListService) {

    suspend fun trending(): List<Anime> =
        anilist.mediaPage(sort = listOf("TRENDING_DESC", "POPULARITY_DESC"))

    suspend fun popularThisSeason(): List<Anime> {
        val (season, year) = AnimeSeason.current()
        return anilist.mediaPage(
            sort = listOf("POPULARITY_DESC"),
            season = season,
            seasonYear = year,
        )
    }

    suspend fun upcomingNextSeason(): List<Anime> {
        val (season, year) = AnimeSeason.next()
        return anilist.mediaPage(
            sort = listOf("POPULARITY_DESC"),
            season = season,
            seasonYear = year,
            status = "NOT_YET_RELEASED",
        )
    }

    suspend fun top100(): List<Anime> =
        anilist.mediaPage(sort = listOf("SCORE_DESC"), perPage = 25)

    /**
     * Episodes that aired in roughly the last [withinDays] days. These power the
     * "Just Aired" row and the NEW badges; duplicates per show are collapsed to the
     * most recent episode.
     */
    suspend fun justAired(withinDays: Int = 7): List<JustAired> {
        val now = System.currentTimeMillis() / 1000
        val from = now - withinDays * 24L * 60 * 60
        val schedules = anilist.airingSchedule(airingAtGreater = from, airingAtLesser = now)
        return schedules
            .mapNotNull { s -> s.media?.let { JustAired(it.toDomain(), s.episode, s.airingAt) } }
            .distinctBy { it.anime.anilistId }
    }

    /**
     * Lightweight "recommended for you" derived from the genres the user watches most.
     * Falls back to popular when there's no history yet.
     */
    suspend fun recommendedFor(topGenres: List<String>): List<Anime> {
        val genre = topGenres.firstOrNull() ?: return emptyList()
        return anilist.search(query = "", genre = genre, perPage = 20)
    }
}
