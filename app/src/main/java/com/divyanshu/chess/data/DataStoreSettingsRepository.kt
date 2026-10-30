package com.divyanshu.chess.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.divyanshu.chess.domain.model.ThemeMode
import com.divyanshu.chess.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    override val vibrationEnabled: Flow<Boolean> =
        context.settingsStore.data.map { it[VIBRATION] ?: true }

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[VIBRATION] = enabled }
    }

    override val themeMode: Flow<ThemeMode> = context.settingsStore.data.map { prefs ->
        prefs[THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsStore.edit { it[THEME] = mode.name }
    }

    private companion object {
        val VIBRATION = booleanPreferencesKey("vibration_enabled")
        val THEME = stringPreferencesKey("theme_mode")
    }
}
