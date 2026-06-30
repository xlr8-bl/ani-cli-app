package com.xlr8.app.data.repository

import com.xlr8.app.data.local.RecentSearchDao
import com.xlr8.app.data.local.SourceMappingDao
import com.xlr8.app.data.local.SourceMappingEntity
import com.xlr8.app.data.local.WatchProgressDao
import com.xlr8.app.data.local.WatchProgressEntity
import com.xlr8.app.data.local.WatchlistDao
import com.xlr8.app.data.local.WatchlistEntity
import com.xlr8.app.data.local.RecentSearchEntity
import com.xlr8.app.data.settings.SettingsRepository
import com.xlr8.app.domain.model.TranslationType
import com.xlr8.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/** The full on-device state, serialized for manual export/import (no server, ever). */
@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = 0,
    val watchlist: List<WatchlistEntity> = emptyList(),
    val watchProgress: List<WatchProgressEntity> = emptyList(),
    val sourceMappings: List<SourceMappingEntity> = emptyList(),
    val recentSearches: List<RecentSearchEntity> = emptyList(),
    val settings: SettingsBackup = SettingsBackup(),
)

@Serializable
data class SettingsBackup(
    val themeMode: String = "SYSTEM",
    val dynamicColor: Boolean = true,
    val amoled: Boolean = false,
    val defaultQuality: String = "best",
    val defaultTranslation: String = "sub",
)

/**
 * Exports/imports the user's on-device state to a local JSON file they choose via the
 * system file picker. This is the only "sync" XLR8 offers — there is no server.
 */
class BackupRepository(
    private val watchlistDao: WatchlistDao,
    private val watchProgressDao: WatchProgressDao,
    private val sourceMappingDao: SourceMappingDao,
    private val recentSearchDao: RecentSearchDao,
    private val settingsRepository: SettingsRepository,
) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun export(output: OutputStream) {
        val settings = settingsRepository.settings.first()
        val data = BackupData(
            exportedAt = System.currentTimeMillis(),
            watchlist = watchlistDao.snapshot(),
            watchProgress = watchProgressDao.snapshot(),
            sourceMappings = sourceMappingDao.snapshot(),
            recentSearches = recentSearchDao.snapshot(),
            settings = SettingsBackup(
                themeMode = settings.themeMode.name,
                dynamicColor = settings.dynamicColor,
                amoled = settings.amoled,
                defaultQuality = settings.defaultQuality,
                defaultTranslation = settings.defaultTranslation.apiValue,
            ),
        )
        output.bufferedWriter().use { it.write(json.encodeToString(BackupData.serializer(), data)) }
    }

    suspend fun import(input: InputStream) {
        val text = input.bufferedReader().use { it.readText() }
        val data = json.decodeFromString(BackupData.serializer(), text)

        data.watchlist.forEach { watchlistDao.add(it) }
        data.watchProgress.forEach { watchProgressDao.upsert(it) }
        data.sourceMappings.forEach { sourceMappingDao.upsert(it) }
        data.recentSearches.forEach { recentSearchDao.add(it) }

        settingsRepository.setThemeMode(
            runCatching { ThemeMode.valueOf(data.settings.themeMode) }.getOrDefault(ThemeMode.SYSTEM),
        )
        settingsRepository.setDynamicColor(data.settings.dynamicColor)
        settingsRepository.setAmoled(data.settings.amoled)
        settingsRepository.setDefaultQuality(data.settings.defaultQuality)
        settingsRepository.setDefaultTranslation(
            if (data.settings.defaultTranslation == TranslationType.DUB.apiValue) TranslationType.DUB
            else TranslationType.SUB,
        )
    }
}
