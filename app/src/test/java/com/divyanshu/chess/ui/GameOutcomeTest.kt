package com.divyanshu.chess.ui

import com.divyanshu.chess.domain.model.GameResult
import com.divyanshu.chess.domain.model.GameStatus
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.ui.game.ForcedEnd
import com.divyanshu.chess.ui.game.GameUiState
import com.divyanshu.chess.ui.game.formatClock
import com.divyanshu.chess.ui.game.outcomeFor
import com.divyanshu.chess.ui.game.secondsFor
import com.divyanshu.chess.ui.learn.LearnTopics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameOutcomeTest {

    private fun ended(
        status: GameStatus,
        result: GameResult?,
        forcedEnd: ForcedEnd? = null,
        sideToMove: Side = Side.WHITE,
        history: List<String> = emptyList(),
    ) = GameUiState(
        isLoading = false,
        status = status,
        result = result,
        forcedEnd = forcedEnd,
        sideToMove = sideToMove,
        moveHistory = history,
    )

    @Test
    fun `a live game has no outcome`() {
        assertNull(outcomeFor(GameUiState(isLoading = false)))
    }

    @Test
    fun `checkmate names the winner and the final move`() {
        val outcome = outcomeFor(
            ended(GameStatus.CHECKMATE, GameResult.BLACK_WINS, sideToMove = Side.WHITE, history = listOf("f3", "e5", "g4", "Qh4#")),
        )!!
        assertEquals("Checkmate!", outcome.title)
        assertTrue(outcome.explanation.contains("White's king"))
        assertTrue(outcome.explanation.contains("Black wins"))
        assertTrue(outcome.explanation.contains("Qh4#"))
        assertEquals("checkmate", outcome.topicId)
    }

    @Test
    fun `stalemate explains the king is not in check`() {
        val outcome = outcomeFor(ended(GameStatus.STALEMATE, GameResult.DRAW, sideToMove = Side.BLACK))!!
        assertEquals("Stalemate", outcome.title)
        assertTrue(outcome.explanation.startsWith("Black has no legal move"))
        assertEquals("stalemate", outcome.topicId)
    }

    @Test
    fun `every kind of draw has an explanation and a lesson`() {
        val draws = mapOf(
            GameStatus.DRAW_FIFTY_MOVE to "fifty-move",
            GameStatus.DRAW_REPETITION to "repetition",
            GameStatus.DRAW_INSUFFICIENT_MATERIAL to "insufficient-material",
        )
        draws.forEach { (status, topic) ->
            val outcome = outcomeFor(ended(status, GameResult.DRAW))!!
            assertEquals(topic, outcome.topicId)
            assertTrue(outcome.explanation.isNotBlank())
        }
    }

    @Test
    fun `timeout and resignation say who lost and why`() {
        val timeout = outcomeFor(ended(GameStatus.ONGOING, GameResult.WHITE_WINS, ForcedEnd.TIMEOUT))!!
        assertTrue(timeout.explanation.startsWith("Black ran out of time"))
        assertEquals("timeout", timeout.topicId)

        val resigned = outcomeFor(ended(GameStatus.ONGOING, GameResult.BLACK_WINS, ForcedEnd.RESIGNATION))!!
        assertEquals("White resigned", resigned.title)
        assertEquals("resignation", resigned.topicId)
    }

    @Test
    fun `every outcome links to an existing lesson`() {
        val states = listOf(
            ended(GameStatus.CHECKMATE, GameResult.WHITE_WINS),
            ended(GameStatus.STALEMATE, GameResult.DRAW),
            ended(GameStatus.DRAW_FIFTY_MOVE, GameResult.DRAW),
            ended(GameStatus.DRAW_REPETITION, GameResult.DRAW),
            ended(GameStatus.DRAW_INSUFFICIENT_MATERIAL, GameResult.DRAW),
            ended(GameStatus.ONGOING, GameResult.WHITE_WINS, ForcedEnd.TIMEOUT),
            ended(GameStatus.ONGOING, GameResult.WHITE_WINS, ForcedEnd.RESIGNATION),
        )
        states.forEach { assertNotNull(LearnTopics.byId(outcomeFor(it)!!.topicId)) }
    }

    @Test
    fun `clock text rounds up and pads seconds`() {
        assertEquals("10:00", formatClock(600))
        assertEquals("0:05", formatClock(5))
        assertEquals("9:59", formatClock(secondsFor(598_500)))
        assertEquals(1, secondsFor(1))
        assertEquals(0, secondsFor(0))
    }
}
