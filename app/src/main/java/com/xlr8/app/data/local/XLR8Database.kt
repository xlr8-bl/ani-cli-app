package com.xlr8.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * On-device database. Currently holds the source mapping cache; history, watchlist,
 * downloads and resume positions are added in later steps as new entities/DAOs.
 */
@Database(
    entities = [SourceMappingEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class XLR8Database : RoomDatabase() {

    abstract fun sourceMappingDao(): SourceMappingDao

    companion object {
        fun build(context: Context): XLR8Database =
            Room.databaseBuilder(
                context.applicationContext,
                XLR8Database::class.java,
                "xlr8.db",
            ).fallbackToDestructiveMigration().build()
    }
}
