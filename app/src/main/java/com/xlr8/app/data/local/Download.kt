package com.xlr8.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

enum class DownloadStatus { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED }

/** A single offline download, keyed by (show, episode, translation). On-device only. */
@Entity(tableName = "downloads", primaryKeys = ["anilistId", "episode", "translation"])
data class DownloadEntity(
    val anilistId: Int,
    val episode: Int,
    val translation: String,
    val title: String,
    val coverImageUrl: String?,
    val quality: String,
    val sourceUrl: String,
    val localPath: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: String,
    val updatedAt: Long,
) {
    val progressFraction: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    fun statusEnum(): DownloadStatus =
        runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.QUEUED)
}

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(download: DownloadEntity)

    @Query("SELECT * FROM downloads ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE anilistId = :anilistId AND episode = :episode AND translation = :translation LIMIT 1")
    suspend fun find(anilistId: Int, episode: Int, translation: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE anilistId = :anilistId AND episode = :episode AND status = 'COMPLETED' LIMIT 1")
    suspend fun findCompleted(anilistId: Int, episode: Int): DownloadEntity?

    @Query("UPDATE downloads SET downloadedBytes = :downloaded, totalBytes = :total, updatedAt = :now WHERE anilistId = :anilistId AND episode = :episode AND translation = :translation")
    suspend fun updateProgress(anilistId: Int, episode: Int, translation: String, downloaded: Long, total: Long, now: Long)

    @Query("UPDATE downloads SET status = :status, updatedAt = :now WHERE anilistId = :anilistId AND episode = :episode AND translation = :translation")
    suspend fun updateStatus(anilistId: Int, episode: Int, translation: String, status: String, now: Long)

    @Query("DELETE FROM downloads WHERE anilistId = :anilistId AND episode = :episode AND translation = :translation")
    suspend fun delete(anilistId: Int, episode: Int, translation: String)

    @Query("SELECT COALESCE(SUM(downloadedBytes), 0) FROM downloads")
    fun observeStorageUsed(): Flow<Long>
}
