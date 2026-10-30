package com.divyanshu.chess

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.divyanshu.chess.domain.model.GameMode
import com.divyanshu.chess.domain.model.GameResult
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.GameStatus
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.domain.model.Square
import com.divyanshu.chess.ui.game.ClockUiState
import com.divyanshu.chess.ui.game.EngineLine
import com.divyanshu.chess.ui.game.GameContent
import com.divyanshu.chess.ui.game.GameUiState
import com.divyanshu.chess.ui.game.trackPieces
import com.divyanshu.chess.ui.theme.AppBackground
import com.divyanshu.chess.ui.theme.ChessTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BoardUiTest {

    @get:Rule
    val compose = createComposeRule()

    private val start = GameState.initial()

    private fun baseState(): GameUiState {
        val tracked = trackPieces(emptyList())
        return GameUiState(
            isLoading = false,
            board = List(64) { start.board[Square(it)] },
            pieces = tracked.pieces,
            mode = GameMode.VS_COMPUTER,
        )
    }

    private fun show(
        state: GameUiState,
        clock: ClockUiState = ClockUiState(),
        onSquare: (Square) -> Unit = {},
    ) {
        compose.setContent {
            ChessTheme {
                AppBackground {
                    GameContent(
                        state = state,
                        clock = clock,
                        vibrationEnabled = true,
                        onVibrationToggled = {},
                        onBack = {},
                        onLearn = {},
                        onSquareClick = onSquare,
                        onPromotionChosen = {},
                        onPromotionDismissed = {},
                        onUndo = {},
                        onResign = {},
                        onRematch = {},
                    )
                }
            }
        }
    }

    @Test
    fun tapping_a_square_reports_that_square() {
        var tapped: Square? = null
        show(baseState()) { tapped = it }
        compose.onNodeWithTag("square_e2").performClick()
        assertEquals(Square.parse("e2"), tapped)
    }

    @Test
    fun every_square_is_on_screen() {
        show(baseState())
        for (file in 'a'..'h') for (rank in 1..8) {
            compose.onNodeWithTag("square_$file$rank").assertIsDisplayed()
        }
    }

    @Test
    fun the_engine_analysis_line_is_shown() {
        val line = EngineLine("Stockfish 19", 14, "+0.35", 1_200_000, 1000, listOf("e5", "Nf3", "Nc6"))
        show(baseState().copy(engineLine = line))
        compose.onNodeWithTag("engine_line").assertIsDisplayed()
        compose.onNodeWithText("Stockfish 19", substring = true).assertIsDisplayed()
        compose.onNodeWithText("e5 Nf3 Nc6").assertIsDisplayed()
    }

    @Test
    fun both_clocks_are_shown() {
        show(baseState(), ClockUiState(whiteSeconds = 540, blackSeconds = 125, running = Side.BLACK))
        compose.onNodeWithTag("clock_white").assertTextContains("9:00")
        compose.onNodeWithTag("clock_black").assertTextContains("2:05")
    }

    @Test
    fun a_finished_game_explains_why_it_ended() {
        val state = baseState().copy(
            status = GameStatus.STALEMATE,
            result = GameResult.DRAW,
            sideToMove = Side.BLACK,
        )
        show(state)
        compose.onNodeWithTag("game_over_dialog").assertIsDisplayed()
        compose.onNodeWithText("Stalemate").assertIsDisplayed()
        compose.onNodeWithText("Learn why").assertIsDisplayed()
    }
}
