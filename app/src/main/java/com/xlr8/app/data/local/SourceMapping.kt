package com.xlr8.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

/**
 * Cached AniList ID → AllAnime ID mapping, so a show's source is resolved only once.
 * Lives on-device; the device is the account.
 */
@Serializable
@Entity(tableName = "source_mapping")
data class SourceMappingEntity(
    @PrimaryKey val anilistId: Int,
    val allAnimeId: String,
    val allAnimeName: String,
    val subEpisodes: Int,
    val dubEpisodes: Int,
    val updatedAt: Long,
)

@Dao
interface SourceMappingDao {

    @Query("SELECT * FROM source_mapping WHERE anilistId = :anilistId LIMIT 1")
    suspend fun find(anilistId: Int): SourceMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mapping: SourceMappingEntity)

    @Query("DELETE FROM source_mapping WHERE anilistId = :anilistId")
    suspend fun clear(anilistId: Int)

    @Query("SELECT * FROM source_mapping")
    suspend fun snapshot(): List<SourceMappingEntity>

    @Query("DELETE FROM source_mapping")
    suspend fun clearAll()
}
