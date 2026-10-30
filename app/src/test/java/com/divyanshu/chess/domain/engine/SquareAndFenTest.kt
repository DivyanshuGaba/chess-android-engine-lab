package com.divyanshu.chess.domain.engine

import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.Piece
import com.divyanshu.chess.domain.model.PieceType
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.domain.model.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SquareAndFenTest {

    @Test
    fun `square index maps a1 to zero and h8 to 63`() {
        assertEquals(0, Square.parse("a1").index)
        assertEquals(63, Square.parse("h8").index)
        assertEquals("e4", Square.of(4, 3).toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `square rejects off board text`() {
        Square.parse("i9")
    }

    @Test
    fun `move round trips through uci`() {
        val move = Move(Square.parse("e7"), Square.parse("e8"), PieceType.KNIGHT)
        assertEquals("e7e8n", move.uci)
        assertEquals(move, Move.fromUci("e7e8n"))
    }

    @Test
    fun `starting position parses and formats back`() {
        val state = Fen.parse(Fen.STARTING_POSITION)
        assertEquals(Fen.STARTING_POSITION, Fen.format(state))
        assertEquals(Piece(PieceType.KING, Side.WHITE), state.board[Square.parse("e1")])
        assertEquals(Piece(PieceType.QUEEN, Side.BLACK), state.board[Square.parse("d8")])
    }

    @Test
    fun `fen keeps en passant square and clocks`() {
        val fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2"
        val state = Fen.parse(fen)
        assertEquals(Square.parse("e6"), state.enPassant)
        assertEquals(2, state.fullmoveNumber)
        assertEquals(fen, Fen.format(state))
    }

    @Test
    fun `fen without castling rights formats a dash`() {
        val state = Fen.parse("4k3/8/8/8/8/8/8/4K3 b - - 12 40")
        assertEquals("4k3/8/8/8/8/8/8/4K3 b - - 12 40", Fen.format(state))
        assertNull(state.enPassant)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fen with a short rank is rejected`() {
        Fen.parse("rnbqkbnr/ppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    }

    @Test
    fun `initial state equals parsed starting fen`() {
        assertEquals(Fen.parse(Fen.STARTING_POSITION), GameState.initial())
    }
}
