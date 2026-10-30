package com.divyanshu.chess.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameTest {

    private fun play(vararg uci: String) = Game.fromMoves(uci.map(Move::fromUci))

    @Test
    fun `new game starts with white to move`() {
        val game = Game.new()
        assertEquals(Side.WHITE, game.state.sideToMove)
        assertEquals(GameStatus.ONGOING, game.status)
        assertNull(game.result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `illegal moves are rejected`() {
        Game.new().play(Move.fromUci("e2e5"))
    }

    @Test
    fun `san history is recorded`() {
        assertEquals(listOf("e4", "e5", "Nf3"), play("e2e4", "e7e5", "g1f3").sanMoves)
    }

    @Test
    fun `fools mate ends the game with a black win`() {
        val game = play("f2f3", "e7e5", "g2g4", "d8h4")
        assertEquals(GameStatus.CHECKMATE, game.status)
        assertEquals(GameResult.BLACK_WINS, game.result)
        assertEquals("Qh4#", game.sanMoves.last())
    }

    @Test
    fun `undo restores the previous position`() {
        val game = play("e2e4", "e7e5", "g1f3")
        val undone = game.undo(2)
        assertEquals(listOf(Move.fromUci("e2e4")), undone.moves)
        assertEquals(Side.BLACK, undone.state.sideToMove)
        assertEquals(1, undone.sanMoves.size)
    }

    @Test
    fun `undo past the start stays at the start`() {
        assertEquals(0, Game.new().undo(3).moves.size)
    }

    @Test
    fun `threefold repetition is a draw`() {
        val shuffle = arrayOf("g1f3", "g8f6", "f3g1", "f6g8")
        val game = play(*shuffle, *shuffle)
        assertEquals(GameStatus.DRAW_REPETITION, game.status)
        assertEquals(GameResult.DRAW, game.result)
    }

    @Test
    fun `two occurrences are not yet a draw`() {
        assertEquals(GameStatus.ONGOING, play("g1f3", "g8f6", "f3g1", "f6g8").status)
    }

    @Test
    fun `captured pieces are tracked per side`() {
        val game = play("e2e4", "d7d5", "e4d5", "d8d5")
        assertEquals(listOf(PieceType.PAWN), game.capturedBy(Side.WHITE))
        assertEquals(listOf(PieceType.PAWN), game.capturedBy(Side.BLACK))
    }
}
