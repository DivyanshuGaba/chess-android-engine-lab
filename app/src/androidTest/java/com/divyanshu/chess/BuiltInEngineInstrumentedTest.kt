package com.divyanshu.chess

import com.divyanshu.chess.domain.ai.AdvancedAi
import com.divyanshu.chess.domain.ai.HARD_MOVE_TIME_MS
import com.divyanshu.chess.domain.ai.Searcher
import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.engine.Fen
import com.divyanshu.chess.domain.model.Move
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The improved built-in engine on the real phone: legal moves, and the time budget is honoured. */
class BuiltInEngineInstrumentedTest {

    private val positions = listOf(
        Fen.STARTING_POSITION,
        "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
        "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
        "r1bq1rk1/ppp2ppp/2np1n2/2b1p3/2B1P3/2NP1N2/PPP2PPP/R1BQ1RK1 w - - 0 7",
    )

    @Test
    fun the_hard_level_returns_legal_moves_within_its_budget() = runBlocking {
        val ai = AdvancedAi(Searcher.Limits(movetimeMs = HARD_MOVE_TIME_MS))
        for (fen in positions) {
            val state = Fen.parse(fen)
            val started = System.nanoTime()
            val result = ai.search(state)
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            assertTrue("$fen took $elapsedMs ms", elapsedMs < HARD_MOVE_TIME_MS + 700)
            assertTrue(fen, result.move in ChessRules.legalMoves(state))
            assertNotNull(result.info)
            assertTrue("search reached no depth", result.info!!.depth >= 1)
        }
    }

    @Test
    fun the_engine_still_finds_a_mate_in_one_on_the_phone() = runBlocking {
        val state = Fen.parse("6k1/5ppp/8/8/8/8/8/R3K3 w - - 0 1")
        assertEquals(Move.fromUci("a1a8"), AdvancedAi(Searcher.Limits(depth = 4)).chooseMove(state))
    }
}
