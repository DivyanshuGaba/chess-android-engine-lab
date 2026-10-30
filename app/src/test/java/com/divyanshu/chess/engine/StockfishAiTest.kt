package com.divyanshu.chess.engine

import com.divyanshu.chess.domain.ai.ChessAi
import com.divyanshu.chess.domain.ai.PvFormatter
import com.divyanshu.chess.domain.ai.ResilientAi
import com.divyanshu.chess.domain.ai.SearchResult
import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.engine.Fen
import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.testutil.ScriptedTransport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StockfishAiTest {

    private class TestSession(private val engine: UciEngine) : EngineSession {
        override suspend fun <T> withEngine(options: Map<String, String>, block: suspend (UciEngine) -> T): T {
            engine.setOptions(options)
            return block(engine)
        }
    }

    private suspend fun aiFor(transport: ScriptedTransport, difficulty: Difficulty = Difficulty.MEDIUM): StockfishAi {
        val engine = UciEngine(transport).also { it.start() }
        return StockfishAi(TestSession(engine), StockfishProfile.forDifficulty(difficulty))
    }

    @Test
    fun `a legal engine move is returned with converted info`() = runTest {
        val result = aiFor(ScriptedTransport()).search(GameState.initial())
        assertEquals(Move.fromUci("e2e4"), result.move)
        assertEquals("TestFish 1.0", result.info?.engineName)
        assertEquals(31, result.info?.scoreCp)
        assertEquals(listOf(Move.fromUci("e2e4"), Move.fromUci("e7e5")), result.info?.pv)
    }

    @Test
    fun `scores are reported from whites point of view`() = runTest {
        val blackToMove = Fen.parse("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
        val transport = ScriptedTransport().apply {
            bestMove = "e7e5"
            infoLines = listOf("info depth 5 multipv 1 score cp 40 nodes 100 nps 1000 time 1 pv e7e5")
        }
        val result = aiFor(transport).search(blackToMove)
        assertEquals(-40, result.info?.scoreCp)
    }

    @Test
    fun `an illegal move from the engine is refused`() = runTest {
        val transport = ScriptedTransport().apply { bestMove = "e2e5" }
        try {
            aiFor(transport).search(GameState.initial())
            fail("expected IllegalEngineMoveException")
        } catch (e: IllegalEngineMoveException) {
            assertEquals("e2e5", e.move)
        }
    }

    @Test
    fun `a position with no moves never reaches the engine`() = runTest {
        val mated = Fen.parse("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3")
        val transport = ScriptedTransport()
        val ai = aiFor(transport)
        val before = transport.sent.size
        assertNull(ai.search(mated).move)
        assertEquals(before, transport.sent.size)
    }

    @Test
    fun `difficulty sets strength and search budget`() = runTest {
        val transport = ScriptedTransport()
        aiFor(transport, Difficulty.EASY).search(GameState.initial())
        assertTrue("setoption name UCI_Elo value 1350" in transport.sent)
        assertTrue("setoption name UCI_LimitStrength value true" in transport.sent)
        assertTrue("go movetime 100" in transport.sent)

        val hard = ScriptedTransport()
        aiFor(hard, Difficulty.HARD).search(GameState.initial())
        assertTrue("setoption name UCI_LimitStrength value false" in hard.sent)
        assertTrue("go movetime 1000" in hard.sent)
    }

    @Test
    fun `the engine plays from the start with a new game command`() = runTest {
        val transport = ScriptedTransport()
        aiFor(transport).search(GameState.initial())
        assertTrue("ucinewgame" in transport.sent)
    }

    @Test
    fun `a failing engine falls back to the secondary ai`() = runTest {
        val failing = object : ChessAi {
            override suspend fun chooseMove(state: GameState): Move? = throw EngineCrashedException("boom")
        }
        val backup = object : ChessAi {
            override suspend fun chooseMove(state: GameState): Move? = ChessRules.legalMoves(state).first()
            override suspend fun search(state: GameState) = SearchResult(chooseMove(state), null)
        }
        var reported: Throwable? = null
        val result = ResilientAi(failing, backup) { reported = it }.search(GameState.initial())
        assertTrue(result.move != null)
        assertTrue(reported is EngineCrashedException)
    }

    @Test
    fun `cancellation is never swallowed by the fallback`() = runTest {
        val cancelling = object : ChessAi {
            override suspend fun chooseMove(state: GameState): Move? = throw CancellationException("stop")
        }
        val backup = object : ChessAi {
            override suspend fun chooseMove(state: GameState): Move? = fail("fallback must not run") as Nothing
        }
        try {
            ResilientAi(cancelling, backup).search(GameState.initial())
            fail("expected CancellationException")
        } catch (_: CancellationException) {
        }
    }

    @Test
    fun `scores and lines are formatted for display`() {
        assertEquals("+0.35", PvFormatter.formatScore(35, null))
        assertEquals("-1.20", PvFormatter.formatScore(-120, null))
        assertEquals("0.00", PvFormatter.formatScore(0, null))
        assertEquals("#3", PvFormatter.formatScore(null, 3))
        assertEquals("#-2", PvFormatter.formatScore(null, -2))

        val line = listOf("e2e4", "e7e5", "g1f3", "b8c6").map(Move::fromUci)
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6"), PvFormatter.toSan(GameState.initial(), line))
        val broken = listOf("e2e4", "e7e5", "e2e4").map(Move::fromUci)
        assertEquals(listOf("e4", "e5"), PvFormatter.toSan(GameState.initial(), broken))
    }
}
