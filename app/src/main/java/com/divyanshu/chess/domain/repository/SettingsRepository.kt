package com.divyanshu.chess.domain.repository

import com.divyanshu.chess.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val vibrationEnabled: Flow<Boolean>

    suspend fun setVibrationEnabled(enabled: Boolean)

    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
