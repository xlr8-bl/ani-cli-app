package com.xlr8.app.data.repository

import com.xlr8.app.data.local.WatchProgressDao
import com.xlr8.app.data.local.WatchProgressEntity
import com.xlr8.app.domain.model.TranslationType
import kotlinx.coroutines.flow.Flow

/** Persists and reads on-device resume positions / watch history. */
class PlaybackRepository(private val dao: WatchProgressDao) {

    suspend fun progressFor(anilistId: Int, episode: Int): WatchProgressEntity? =
        dao.find(anilistId, episode)

    fun continueWatching(): Flow<List<WatchProgressEntity>> = dao.continueWatching()

    suspend fun save(
        anilistId: Int,
        episode: Int,
        positionMs: Long,
        durationMs: Long,
        translation: TranslationType,
        title: String,
        coverImageUrl: String?,
    ) {
        // Ignore trivially-short positions so we don't clutter Continue Watching.
        if (positionMs < 1_000) return
        dao.upsert(
            WatchProgressEntity(
                anilistId = anilistId,
                episode = episode,
                positionMs = positionMs,
                durationMs = durationMs,
                translation = translation.apiValue,
                title = title,
                coverImageUrl = coverImageUrl,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun clearAll() = dao.clearAll()
}
