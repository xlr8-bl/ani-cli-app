package com.xlr8.app.data.repository

import com.xlr8.app.data.local.SourceMappingDao
import com.xlr8.app.data.local.SourceMappingEntity
import com.xlr8.app.data.match.TitleMatcher
import com.xlr8.app.data.remote.allanime.AllAnimeService
import com.xlr8.app.domain.model.AllAnimeShow
import com.xlr8.app.domain.model.Anime
import com.xlr8.app.domain.model.TranslationType
import com.xlr8.app.domain.model.VideoSource

/**
 * Bridges AniList metadata to AllAnime playable sources: resolves the matching AllAnime
 * show (caching the mapping in Room), lists episodes, and resolves per-episode streams.
 */
class AllAnimeRepository(
    private val service: AllAnimeService,
    private val mappingDao: SourceMappingDao,
) {

    /**
     * Finds the AllAnime show for an AniList [anime], using the cached mapping when present.
     * Returns null when no confident match exists — callers should then offer manual linking.
     */
    suspend fun resolveShow(anime: Anime): AllAnimeShow? {
        mappingDao.find(anime.anilistId)?.let { cached ->
            return AllAnimeShow(cached.allAnimeId, cached.allAnimeName, cached.subEpisodes, cached.dubEpisodes)
        }
        val candidates = service.search(anime.title)
        val match = TitleMatcher.bestMatch(anime, candidates) ?: return null
        cacheMapping(anime.anilistId, match)
        return match
    }

    /** Manual fallback: raw AllAnime search results for the user to choose from. */
    suspend fun searchCandidates(query: String): List<AllAnimeShow> = service.search(query)

    /** Records the user's manual choice so future resolves are instant and correct. */
    suspend fun linkManually(anilistId: Int, show: AllAnimeShow) = cacheMapping(anilistId, show)

    /** Forgets a mapping (e.g. the user picked the wrong show). */
    suspend fun unlink(anilistId: Int) = mappingDao.clear(anilistId)

    /** Clears all cached AniList↔AllAnime mappings (re-resolved on next play). */
    suspend fun clearSourceCache() = mappingDao.clearAll()

    suspend fun episodeNumbers(show: AllAnimeShow, translation: TranslationType): List<String> =
        service.episodeNumbers(show.id, translation)

    /** Resolves all available quality streams for an episode, best quality first. */
    suspend fun episodeSources(
        show: AllAnimeShow,
        translation: TranslationType,
        episode: String,
    ): List<VideoSource> =
        service.episodeSources(show.id, translation, episode)
            .distinctBy { it.url }
            .sortedByDescending { it.heightOrZero }

    private suspend fun cacheMapping(anilistId: Int, show: AllAnimeShow) {
        mappingDao.upsert(
            SourceMappingEntity(
                anilistId = anilistId,
                allAnimeId = show.id,
                allAnimeName = show.name,
                subEpisodes = show.subEpisodes,
                dubEpisodes = show.dubEpisodes,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}
