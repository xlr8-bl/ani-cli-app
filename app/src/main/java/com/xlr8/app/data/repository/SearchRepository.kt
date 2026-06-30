package com.xlr8.app.data.repository

import com.xlr8.app.data.local.RecentSearchDao
import com.xlr8.app.data.local.RecentSearchEntity
import com.xlr8.app.data.remote.anilist.AniListService
import com.xlr8.app.domain.model.Anime
import kotlinx.coroutines.flow.Flow

/** AniList search plus locally-stored recent queries. */
class SearchRepository(
    private val anilist: AniListService,
    private val recentDao: RecentSearchDao,
) {

    fun recentSearches(): Flow<List<RecentSearchEntity>> = recentDao.observeRecent()

    suspend fun search(
        query: String,
        genre: String? = null,
        seasonYear: Int? = null,
        format: String? = null,
    ): List<Anime> = anilist.search(
        query = query,
        genre = genre,
        seasonYear = seasonYear,
        format = format,
    )

    suspend fun rememberQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            recentDao.add(RecentSearchEntity(trimmed, System.currentTimeMillis()))
        }
    }

    suspend fun removeRecent(query: String) = recentDao.remove(query)

    suspend fun clearRecent() = recentDao.clear()
}
