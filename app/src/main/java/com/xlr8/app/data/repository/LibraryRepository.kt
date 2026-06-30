package com.xlr8.app.data.repository

import com.xlr8.app.data.local.WatchlistDao
import com.xlr8.app.data.local.WatchlistEntity
import com.xlr8.app.domain.model.Anime
import kotlinx.coroutines.flow.Flow

/** On-device watchlist (the "library"). */
class LibraryRepository(private val dao: WatchlistDao) {

    fun observeWatchlist(): Flow<List<WatchlistEntity>> = dao.observeAll()

    fun isInWatchlist(anilistId: Int): Flow<Boolean> = dao.isSaved(anilistId)

    suspend fun add(anime: Anime) {
        dao.add(
            WatchlistEntity(
                anilistId = anime.anilistId,
                title = anime.title,
                coverImageUrl = anime.coverImageUrl,
                format = anime.format,
                averageScore = anime.averageScore,
                addedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun remove(anilistId: Int) = dao.remove(anilistId)
}
