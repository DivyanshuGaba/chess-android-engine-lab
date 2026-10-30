package com.divyanshu.chess.engine

import com.divyanshu.chess.domain.ai.EngineInfo
import com.divyanshu.chess.domain.ai.SearchResult
import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.engine.Fen
import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Talks to the real Stockfish binary on the development machine. Skipped when the binary has not
 * been fetched (run tools/fetch_stockfish.sh).
 */
class StockfishHostIntegrationTest {

    private val binary = File("../engine_lab/bin/stockfish-macos-universal")

    private fun transport(): ProcessUciTransport {
        assumeTrue("Stockfish binary not fetched", binary.canExecute())
        return ProcessUciTransport(listOf(binary.absolutePath))
    }

    private suspend fun startedEngine(): UciEngine = UciEngine(transport()).also { it.start() }

    @Test
    fun `the real engine starts and identifies itself`() = runBlocking {
        val engine = startedEngine()
        try {
            assertTrue(engine.id!!.name.startsWith("Stockfish"))
            val names = engine.options.map { it.name }
            assertTrue("UCI_Elo" in names && "Skill Level" in names && "Hash" in names)
            assertEquals(1320L, engine.options.first { it.name == "UCI_Elo" }.min)
        } finally {
            engine.close()
        }
    }

    @Test
    fun `the real engine returns a legal opening move`() = runBlocking {
        val engine = startedEngine()
        try {
            val state = GameState.initial()
            val outcome = engine.search(Fen.format(state), SearchLimits(depth = 8))
            assertTrue(Move.fromUci(outcome.bestMove!!) in ChessRules.legalMoves(state))
            assertTrue(outcome.info!!.depth!! >= 8)
        } finally {
            engine.close()
        }
    }

    @Test
    fun `the real engine finds a mate in one`() = runBlocking {
        val engine = startedEngine()
        try {
            val outcome = engine.search("6k1/5ppp/8/8/8/8/8/R3K3 w - - 0 1", SearchLimits(depth = 6))
            assertEquals("a1a8", outcome.bestMove)
            assertEquals(1, outcome.info?.scoreMate)
        } finally {
            engine.close()
        }
    }

    @Test
    fun `every move for a set of positions is legal under the app rules`() = runBlocking {
        val engine = startedEngine()
        val fens = listOf(
            Fen.STARTING_POSITION,
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
        )
        try {
            for (fen in fens) {
                val state = Fen.parse(fen)
                val best = engine.search(fen, SearchLimits(depth = 6)).bestMove!!
                assertTrue("$best illegal in $fen", Move.fromUci(best) in ChessRules.legalMoves(state))
            }
        } finally {
            engine.close()
        }
    }

    @Test
    fun `difficulty limits are accepted by the real engine`() = runBlocking {
        val engine = startedEngine()
        try {
            for (difficulty in Difficulty.entries) {
                engine.setOptions(StockfishProfile.forDifficulty(difficulty).options())
            }
        } finally {
            engine.close()
        }
    }

    @Test
    fun `cancelling a long search stops it and the engine stays usable`() = runBlocking {
        val engine = startedEngine()
        try {
            val job = launch(Dispatchers.Default) { engine.search(Fen.STARTING_POSITION, SearchLimits(depth = 60)) }
            delay(300)
            val started = System.nanoTime()
            job.cancel()
            job.join()
            val cancelMs = (System.nanoTime() - started) / 1_000_000
            assertTrue("cancelling took $cancelMs ms", cancelMs < 2_000)

            val outcome = engine.search(Fen.STARTING_POSITION, SearchLimits(depth = 4))
            assertTrue(outcome.bestMove != null)
        } finally {
            engine.close()
        }
    }

    @Test
    fun `closing the engine ends the process`() = runBlocking {
        val engine = startedEngine()
        engine.close()
        assertFalse(engine.isAlive)
    }

    @Test
    fun `a missing binary is reported as unavailable`() {
        try {
            ProcessUciTransport(listOf("/nonexistent/stockfish"))
            org.junit.Assert.fail("expected EngineUnavailableException")
        } catch (_: EngineUnavailableException) {
        }
    }

    @Test
    fun `the stockfish ai plays a legal move through the real process`() = runBlocking {
        val engine = startedEngine()
        try {
            val session = object : EngineSession {
                override suspend fun <T> withEngine(options: Map<String, String>, block: suspend (UciEngine) -> T): T {
                    engine.setOptions(options)
                    return block(engine)
                }
            }
            val state = GameState.initial()
            val result: SearchResult = StockfishAi(session, StockfishProfile.forDifficulty(Difficulty.EASY)).search(state)
            val info: EngineInfo = result.info!!
            assertTrue(result.move in ChessRules.legalMoves(state))
            assertTrue(info.engineName.startsWith("Stockfish"))
        } finally {
            engine.close()
        }
    }
}
