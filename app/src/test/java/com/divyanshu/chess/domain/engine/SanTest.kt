package com.divyanshu.chess.domain.engine

import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import org.junit.Assert.assertEquals
import org.junit.Test

class SanTest {

    private fun san(fen: String, uci: String) = San.format(Fen.parse(fen), Move.fromUci(uci))

    @Test
    fun `pawn and piece moves`() {
        val start = GameState.initial()
        assertEquals("e4", San.format(start, Move.fromUci("e2e4")))
        assertEquals("Nf3", San.format(start, Move.fromUci("g1f3")))
    }

    @Test
    fun `captures include the file for pawns`() {
        assertEquals("exd5", san("4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1", "e4d5"))
        assertEquals("Qxd5", san("4k3/8/8/3p4/8/8/8/3QK3 w - - 0 1", "d1d5"))
    }

    @Test
    fun `castling uses letter o notation`() {
        val fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"
        assertEquals("O-O", san(fen, "e1g1"))
        assertEquals("O-O-O", san(fen, "e1c1"))
    }

    @Test
    fun `promotion gets a check suffix`() {
        assertEquals("a8=Q+", san("4k3/P7/8/8/8/8/8/4K3 w - - 0 1", "a7a8q"))
    }

    @Test
    fun `mating move gets a hash suffix`() {
        assertEquals("Qh4#", san("rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 2", "d8h4"))
    }

    @Test
    fun `rooks on one rank are told apart by file`() {
        val fen = "4k3/8/8/8/8/8/6K1/R6R w - - 0 1"
        assertEquals("Rad1", san(fen, "a1d1"))
        assertEquals("Rhd1", san(fen, "h1d1"))
    }

    @Test
    fun `knights on one file are told apart by rank`() {
        val fen = "4k3/8/8/6N1/8/8/8/4K1N1 w - - 0 1"
        assertEquals("N1f3", san(fen, "g1f3"))
        assertEquals("N5f3", san(fen, "g5f3"))
    }
}
