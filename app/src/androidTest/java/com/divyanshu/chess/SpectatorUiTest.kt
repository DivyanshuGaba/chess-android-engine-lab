package com.divyanshu.chess

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.divyanshu.chess.ui.spectator.SpectatorContent
import com.divyanshu.chess.ui.spectator.SpectatorUiState
import com.divyanshu.chess.ui.theme.AppBackground
import com.divyanshu.chess.ui.theme.ChessTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SpectatorUiTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(state: SpectatorUiState, onStart: () -> Unit = {}, onPause: () -> Unit = {}) {
        compose.setContent {
            ChessTheme {
                AppBackground {
                    SpectatorContent(
                        state = state,
                        onWhiteChanged = {},
                        onBlackChanged = {},
                        onSpeedChanged = {},
                        onStart = onStart,
                        onPauseToggled = onPause,
                        onReset = {},
                    )
                }
            }
        }
    }

    @Test
    fun the_screen_says_it_is_a_demo_and_not_a_benchmark() {
        show(SpectatorUiState())
        compose.onNodeWithTag("demo_notice").assertIsDisplayed()
        compose.onNodeWithText("Demo, not a benchmark").assertIsDisplayed()
    }

    @Test
    fun before_the_match_you_can_start_but_not_pause() {
        var started = 0
        show(SpectatorUiState(), onStart = { started++ })
        compose.onNodeWithTag("pause_button").assertIsNotEnabled()
        compose.onNodeWithTag("start_match_button").assertIsEnabled().performClick()
        assertEquals(1, started)
    }

    @Test
    fun during_the_match_pause_is_available_and_the_matchup_is_locked() {
        var pauses = 0
        show(SpectatorUiState(started = true), onPause = { pauses++ })
        compose.onNodeWithTag("pause_button").assertIsEnabled().performClick()
        assertEquals(1, pauses)
        compose.onNodeWithText("Rematch").assertIsDisplayed()
        compose.onNodeWithTag("reset_button").assertIsDisplayed()
    }

    @Test
    fun a_finished_match_explains_the_result_without_any_tally() {
        val outcome = com.divyanshu.chess.ui.game.Outcome("Stalemate", "Black has no legal move, but their king is not in check.", "stalemate")
        show(SpectatorUiState(started = true, finished = true, outcome = outcome))
        compose.onNodeWithTag("spectator_outcome").assertIsDisplayed()
        compose.onNodeWithText("Stalemate").assertIsDisplayed()
    }
}
