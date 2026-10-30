package com.divyanshu.chess.domain.engine

import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.GameStatus
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.PieceType
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.domain.model.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessRulesTest {

    private fun state(fen: String) = Fen.parse(fen)

    private fun GameState.legal(uci: String) = Move.fromUci(uci) in ChessRules.legalMoves(this)

    private fun GameState.play(uci: String) = ChessRules.applyMove(this, Move.fromUci(uci))

    @Test
    fun `white has twenty moves from the start`() {
        assertEquals(20, ChessRules.legalMoves(GameState.initial()).size)
    }

    @Test
    fun `pinned piece cannot leave the pin line`() {
        val s = state("4k3/8/8/8/4r3/8/4N3/4K3 w - - 0 1")
        assertTrue(ChessRules.legalMovesFrom(s, Square.parse("e2")).isEmpty())
    }

    @Test
    fun `king cannot step into check`() {
        val s = state("4k3/8/8/8/8/8/3r4/4K3 w - - 0 1")
        assertFalse(s.legal("e1d1"))
        assertFalse(s.legal("e1e2"))
        assertTrue(s.legal("e1d2"))
    }

    @Test
    fun `king side castling moves the rook too`() {
        val s = state("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        assertTrue(s.legal("e1g1"))
        val after = s.play("e1g1")
        assertEquals(PieceType.ROOK, after.board[Square.parse("f1")]?.type)
        assertEquals(PieceType.KING, after.board[Square.parse("g1")]?.type)
        assertEquals(null, after.board[Square.parse("h1")])
        assertFalse(after.castlingRights.whiteKingSide)
        assertFalse(after.castlingRights.whiteQueenSide)
    }

    @Test
    fun `cannot castle through an attacked square`() {
        val s = state("4k3/8/8/8/8/8/5r2/R3K2R w KQ - 0 1")
        assertFalse(s.legal("e1g1"))
        assertTrue(s.legal("e1c1"))
    }

    @Test
    fun `cannot castle out of check`() {
        val s = state("4k3/8/8/8/8/8/4r3/R3K2R w KQ - 0 1")
        assertFalse(s.legal("e1g1"))
        assertFalse(s.legal("e1c1"))
    }

    @Test
    fun `moving a rook drops only that sides castling right`() {
        val after = state("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1").play("h1h2")
        assertFalse(after.castlingRights.whiteKingSide)
        assertTrue(after.castlingRights.whiteQueenSide)
    }

    @Test
    fun `capturing a rook on its home square removes the right`() {
        val after = state("r3k2r/8/8/8/8/8/8/R3K2R b kqKQ - 0 1").play("h8h1")
        assertFalse(after.castlingRights.whiteKingSide)
        assertFalse(after.castlingRights.blackKingSide)
    }

    @Test
    fun `en passant capture removes the passed pawn`() {
        val s = state("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1")
        assertTrue(s.legal("e5d6"))
        val after = s.play("e5d6")
        assertEquals(null, after.board[Square.parse("d5")])
        assertEquals(PieceType.PAWN, after.board[Square.parse("d6")]?.type)
    }

    @Test
    fun `double push sets the en passant target`() {
        val after = GameState.initial().play("e2e4")
        assertEquals(Square.parse("e3"), after.enPassant)
        assertEquals(null, after.play("g8f6").enPassant)
    }

    @Test
    fun `promotion offers four pieces and replaces the pawn`() {
        val s = state("4k3/P7/8/8/8/8/8/4K3 w - - 0 1")
        val promotions = ChessRules.legalMovesFrom(s, Square.parse("a7"))
        assertEquals(4, promotions.size)
        val after = s.play("a7a8n")
        assertEquals(PieceType.KNIGHT, after.board[Square.parse("a8")]?.type)
        assertEquals(Side.WHITE, after.board[Square.parse("a8")]?.side)
    }

    @Test
    fun `fools mate is checkmate`() {
        val s = state("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3")
        assertEquals(GameStatus.CHECKMATE, ChessRules.status(s))
    }

    @Test
    fun `boxed in king without check is stalemate`() {
        val s = state("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")
        assertEquals(GameStatus.STALEMATE, ChessRules.status(s))
    }

    @Test
    fun `check is reported while moves remain`() {
        val s = state("4k3/8/8/8/8/8/4r3/4K3 w - - 0 1")
        assertEquals(GameStatus.CHECK, ChessRules.status(s))
    }

    @Test
    fun `bare kings are a draw`() {
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, ChessRules.status(state("4k3/8/8/8/8/8/8/4K3 w - - 0 1")))
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, ChessRules.status(state("4k3/8/8/8/8/8/8/3NK3 w - - 0 1")))
    }

    @Test
    fun `fifty move rule ends the game at one hundred half moves`() {
        assertEquals(GameStatus.DRAW_FIFTY_MOVE, ChessRules.status(state("4k3/8/8/8/8/8/R7/4K3 w - - 100 80")))
        assertEquals(GameStatus.ONGOING, ChessRules.status(state("4k3/8/8/8/8/8/R7/4K3 w - - 99 80")))
    }

    @Test
    fun `halfmove clock resets on pawn moves and captures`() {
        val s = state("4k3/8/8/3p4/4P3/8/8/4K3 w - - 7 20")
        assertEquals(0, s.play("e4d5").halfmoveClock)
        assertEquals(8, s.play("e1e2").halfmoveClock)
    }

    @Test
    fun `fullmove number advances after black moves`() {
        val s = state("4k3/8/8/8/8/8/8/4K3 w - - 0 20")
        assertEquals(20, s.play("e1e2").fullmoveNumber)
        assertEquals(21, s.play("e1e2").play("e8e7").fullmoveNumber)
    }
}
