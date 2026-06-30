package com.xlr8.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xlr8.app.data.repository.AllAnimeRepository
import com.xlr8.app.data.repository.BackupRepository
import com.xlr8.app.data.repository.PlaybackRepository
import com.xlr8.app.data.repository.SearchRepository
import com.xlr8.app.data.settings.AppSettings
import com.xlr8.app.data.settings.SettingsRepository
import com.xlr8.app.di.ServiceLocator
import com.xlr8.app.domain.model.TranslationType
import com.xlr8.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val playbackRepository: PlaybackRepository,
    private val searchRepository: SearchRepository,
    private val allAnimeRepository: AllAnimeRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    constructor() : this(
        ServiceLocator.settingsRepository,
        ServiceLocator.playbackRepository,
        ServiceLocator.searchRepository,
        ServiceLocator.allAnimeRepository,
        ServiceLocator.backupRepository,
    )

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    fun setAmoled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAmoled(enabled) }
    fun setDefaultQuality(quality: String) = viewModelScope.launch { settingsRepository.setDefaultQuality(quality) }
    fun setDefaultTranslation(t: TranslationType) = viewModelScope.launch { settingsRepository.setDefaultTranslation(t) }

    fun clearHistory() = viewModelScope.launch { playbackRepository.clearAll() }

    fun clearCache() = viewModelScope.launch {
        allAnimeRepository.clearSourceCache()
        searchRepository.clearRecent()
    }

    fun exportTo(output: OutputStream, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = runCatching { output.use { backupRepository.export(it) } }.isSuccess
        onResult(ok)
    }

    fun importFrom(input: InputStream, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = runCatching { input.use { backupRepository.import(it) } }.isSuccess
        onResult(ok)
    }
}
