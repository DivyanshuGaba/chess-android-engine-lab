package com.divyanshu.chess.ui

import com.divyanshu.chess.domain.model.ThemeMode
import com.divyanshu.chess.testutil.FakeSettingsRepository
import com.divyanshu.chess.testutil.MainDispatcherRule
import com.divyanshu.chess.ui.settings.SettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository()

    @Test
    fun `starts with the stored values`() = runTest {
        val state = SettingsViewModel(settings).uiState.value
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(true, state.vibrationEnabled)
    }

    @Test
    fun `selecting a theme is stored and reflected in the state`() = runTest {
        val vm = SettingsViewModel(settings)
        vm.onThemeSelected(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, settings.currentTheme)
        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)
    }

    @Test
    fun `vibration toggle is stored and reflected in the state`() = runTest {
        val vm = SettingsViewModel(settings)
        vm.onVibrationToggled(false)
        assertFalse(settings.current)
        assertFalse(vm.uiState.value.vibrationEnabled)
    }
}
