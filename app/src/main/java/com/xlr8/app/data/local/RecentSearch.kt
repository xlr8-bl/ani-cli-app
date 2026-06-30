package com.xlr8.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** A previously-run search query, kept locally for quick re-search. */
@Serializable
@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long,
)

@Dao
interface RecentSearchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entry: RecentSearchEntity)

    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT 10")
    fun observeRecent(): Flow<List<RecentSearchEntity>>

    @Query("SELECT * FROM recent_searches")
    suspend fun snapshot(): List<RecentSearchEntity>

    @Query("DELETE FROM recent_searches WHERE `query` = :query")
    suspend fun remove(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clear()
}
