package com.xlr8.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Resume position + lightweight history for an episode. Keyed by (show, episode) so each
 * episode keeps its own position. Doubles as the Continue Watching source on Home.
 */
@Serializable
@Entity(tableName = "watch_progress", primaryKeys = ["anilistId", "episode"])
data class WatchProgressEntity(
    val anilistId: Int,
    val episode: Int,
    val positionMs: Long,
    val durationMs: Long,
    val translation: String,
    val title: String,
    val coverImageUrl: String?,
    val updatedAt: Long,
) {
    /** Treat near-complete playback as finished so we don't resume at the credits. */
    val isFinished: Boolean
        get() = durationMs > 0 && positionMs >= durationMs - FINISH_THRESHOLD_MS

    val progressFraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    companion object {
        const val FINISH_THRESHOLD_MS = 30_000L
    }
}

@Dao
interface WatchProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: WatchProgressEntity)

    @Query("SELECT * FROM watch_progress WHERE anilistId = :anilistId AND episode = :episode LIMIT 1")
    suspend fun find(anilistId: Int, episode: Int): WatchProgressEntity?

    /** Most recently watched episode per show, newest first — powers Continue Watching. */
    @Query(
        """
        SELECT * FROM watch_progress
        WHERE updatedAt IN (
            SELECT MAX(updatedAt) FROM watch_progress GROUP BY anilistId
        )
        ORDER BY updatedAt DESC
        """,
    )
    fun continueWatching(): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress")
    suspend fun snapshot(): List<WatchProgressEntity>

    @Query("DELETE FROM watch_progress")
    suspend fun clearAll()
}
