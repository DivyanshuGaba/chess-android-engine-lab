package com.divyanshu.chess.domain.engine

import com.divyanshu.chess.testutil.perft
import org.junit.Assert.assertEquals
import org.junit.Test

/** Node counts from the published perft reference positions; any rule bug shows up here. */
class PerftTest {

    private fun assertPerft(fen: String, vararg expected: Long) {
        val state = Fen.parse(fen)
        expected.forEachIndexed { index, nodes ->
            assertEquals("depth ${index + 1} of $fen", nodes, perft(state, index + 1))
        }
    }

    @Test
    fun `starting position`() = assertPerft(Fen.STARTING_POSITION, 20, 400, 8_902, 197_281)

    @Test
    fun `kiwipete`() = assertPerft(
        "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
        48, 2_039, 97_862,
    )

    @Test
    fun `rook endgame with en passant pins`() = assertPerft("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1", 14, 191, 2_812, 43_238)

    @Test
    fun `promotions and castling tangle`() = assertPerft(
        "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
        6, 264, 9_467,
    )

    @Test
    fun `promotion capture checks`() = assertPerft(
        "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
        44, 1_486, 62_379,
    )
}
