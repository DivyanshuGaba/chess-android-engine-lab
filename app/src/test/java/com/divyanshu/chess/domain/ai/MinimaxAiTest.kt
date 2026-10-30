package com.divyanshu.chess.domain.ai

import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.engine.Fen
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MinimaxAiTest {

    private val ai = MinimaxAi(depth = 3)

    @Test
    fun `returns null when there are no legal moves`() = runTest {
        val mated = Fen.parse("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3")
        assertNull(ai.chooseMove(mated))
    }

    @Test
    fun `plays a legal move from the start`() = runTest {
        val start = GameState.initial()
        val move = ai.chooseMove(start)
        assertTrue(move in ChessRules.legalMoves(start))
    }

    @Test
    fun `finds mate in one`() = runTest {
        val state = Fen.parse("6k1/5ppp/8/8/8/8/8/R3K3 w - - 0 1")
        assertEquals(Move.fromUci("a1a8"), ai.chooseMove(state))
    }

    @Test
    fun `takes a free queen`() = runTest {
        val state = Fen.parse("4k3/8/8/3q4/8/8/8/3RK3 w - - 0 1")
        assertEquals(Move.fromUci("d1d5"), ai.chooseMove(state))
    }

    @Test
    fun `noisy ai still only plays legal moves`() = runTest {
        val start = GameState.initial()
        repeat(10) {
            assertTrue(MinimaxAi(depth = 1, noise = 120).chooseMove(start) in ChessRules.legalMoves(start))
        }
    }

    @Test
    fun `search reports depth nodes and a principal move`() = runTest {
        val result = ai.search(GameState.initial())
        val info = result.info!!
        assertEquals(3, info.depth)
        assertTrue(info.nodes > 0)
        assertEquals(result.move, info.pv.first())
        assertEquals("Built-in minimax", info.engineName)
    }

    @Test
    fun `a forced mate is reported as mate not centipawns`() = runTest {
        val info = ai.search(Fen.parse("6k1/5ppp/8/8/8/8/8/R3K3 w - - 0 1")).info!!
        assertNull(info.scoreCp)
        assertEquals(1, info.mateIn)
    }
}
