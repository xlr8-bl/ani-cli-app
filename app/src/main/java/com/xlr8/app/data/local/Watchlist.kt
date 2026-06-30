package com.xlr8.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** A show the user saved to their watchlist. On-device only. */
@Serializable
@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val anilistId: Int,
    val title: String,
    val coverImageUrl: String?,
    val format: String?,
    val averageScore: Int?,
    val addedAt: Long,
)

@Dao
interface WatchlistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entry: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE anilistId = :anilistId")
    suspend fun remove(anilistId: Int)

    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE anilistId = :anilistId)")
    fun isSaved(anilistId: Int): Flow<Boolean>

    @Query("SELECT * FROM watchlist")
    suspend fun snapshot(): List<WatchlistEntity>
}
