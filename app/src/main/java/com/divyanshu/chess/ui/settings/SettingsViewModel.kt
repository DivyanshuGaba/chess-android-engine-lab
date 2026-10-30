package com.divyanshu.chess.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyanshu.chess.domain.model.ThemeMode
import com.divyanshu.chess.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val vibrationEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(settings.vibrationEnabled, settings.themeMode) { vibration, theme ->
        SettingsUiState(vibration, theme)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun onVibrationToggled(enabled: Boolean) {
        viewModelScope.launch { settings.setVibrationEnabled(enabled) }
    }

    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }
}
