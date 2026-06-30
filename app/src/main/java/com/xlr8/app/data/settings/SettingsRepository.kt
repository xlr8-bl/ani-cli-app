package com.xlr8.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xlr8.app.domain.model.TranslationType
import com.xlr8.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** All user preferences. The device is the account, so these live on-device in DataStore. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val amoled: Boolean = false,
    /** "best", "1080", "720", "480", "360", or "worst". */
    val defaultQuality: String = "best",
    val defaultTranslation: TranslationType = TranslationType.SUB,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "xlr8_settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[KEY_THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[KEY_DYNAMIC] ?: true,
            amoled = prefs[KEY_AMOLED] ?: false,
            defaultQuality = prefs[KEY_QUALITY] ?: "best",
            defaultTranslation = prefs[KEY_TRANSLATION]?.let {
                if (it == TranslationType.DUB.apiValue) TranslationType.DUB else TranslationType.SUB
            } ?: TranslationType.SUB,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[KEY_THEME] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = edit { it[KEY_DYNAMIC] = enabled }
    suspend fun setAmoled(enabled: Boolean) = edit { it[KEY_AMOLED] = enabled }
    suspend fun setDefaultQuality(quality: String) = edit { it[KEY_QUALITY] = quality }
    suspend fun setDefaultTranslation(translation: TranslationType) =
        edit { it[KEY_TRANSLATION] = translation.apiValue }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC = booleanPreferencesKey("dynamic_color")
        private val KEY_AMOLED = booleanPreferencesKey("amoled")
        private val KEY_QUALITY = stringPreferencesKey("default_quality")
        private val KEY_TRANSLATION = stringPreferencesKey("default_translation")
    }
}
