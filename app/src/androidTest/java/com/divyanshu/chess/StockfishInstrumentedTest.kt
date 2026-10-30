package com.divyanshu.chess

import androidx.test.platform.app.InstrumentationRegistry
import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.engine.Fen
import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.engine.SearchLimits
import com.divyanshu.chess.engine.SharedEngine
import com.divyanshu.chess.engine.StockfishAi
import com.divyanshu.chess.engine.StockfishLocator
import com.divyanshu.chess.engine.StockfishProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Runs the real Stockfish binary that ships inside the APK, on the device. */
class StockfishInstrumentedTest {

    private lateinit var shared: SharedEngine

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        shared = SharedEngine(StockfishLocator(context))
    }

    @After
    fun tearDown() = runBlocking { shared.shutdown() }

    private fun stockfishProcesses(): Int {
        val process = Runtime.getRuntime().exec(arrayOf("ps", "-A"))
        return process.inputStream.bufferedReader().readLines().count { it.contains("libstockfish") }
    }

    @Test
    fun the_binary_is_packaged_and_executable() {
        assertTrue("Stockfish should be available on an arm64 device", shared.isStockfishAvailable)
        assertNotNull(shared.binaryPath)
    }

    @Test
    fun the_engine_starts_and_identifies_itself() = runBlocking {
        val name = withTimeout(20_000) { shared.withEngine(emptyMap()) { it.id?.name } }
        assertTrue("unexpected engine name: $name", name!!.startsWith("Stockfish"))
        assertEquals(name, shared.engineName.value)
        shared.withEngine(emptyMap()) { engine ->
            assertTrue(engine.options.any { it.name == "UCI_Elo" })
            assertTrue(engine.isAlive)
        }
    }

    @Test
    fun every_difficulty_returns_a_legal_move() = runBlocking {
        val state = GameState.initial()
        for (difficulty in Difficulty.entries) {
            val ai = StockfishAi(shared, StockfishProfile.forDifficulty(difficulty))
            val result = withTimeout(20_000) { ai.search(state) }
            assertTrue("$difficulty returned ${result.move}", result.move in ChessRules.legalMoves(state))
            assertNotNull(result.info)
        }
    }

    @Test
    fun the_engine_finds_a_mate_in_one() = runBlocking {
        val state = Fen.parse("6k1/5ppp/8/8/8/8/8/R3K3 w - - 0 1")
        val ai = StockfishAi(shared, StockfishProfile.analysis(SearchLimits(depth = 8)))
        assertEquals(Move.fromUci("a1a8"), withTimeout(20_000) { ai.search(state) }.move)
    }

    @Test
    fun moves_in_special_positions_are_legal() = runBlocking {
        val fens = listOf(
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
            "4k3/P7/8/8/8/8/8/4K3 w - - 0 1",
        )
        val ai = StockfishAi(shared, StockfishProfile.analysis(SearchLimits(depth = 8)))
        for (fen in fens) {
            val state = Fen.parse(fen)
            assertTrue(fen, withTimeout(20_000) { ai.search(state) }.move in ChessRules.legalMoves(state))
        }
    }

    @Test
    fun cancelling_a_search_is_quick_and_the_engine_stays_usable() = runBlocking {
        val ai = StockfishAi(shared, StockfishProfile.analysis(SearchLimits(depth = 60)))
        val job = launch(Dispatchers.Default) { ai.search(GameState.initial()) }
        delay(500)
        val started = System.nanoTime()
        job.cancel()
        job.join()
        val cancelMs = (System.nanoTime() - started) / 1_000_000
        assertTrue("cancelling took $cancelMs ms", cancelMs < 2_000)

        val next = StockfishAi(shared, StockfishProfile.analysis(SearchLimits(depth = 5)))
        assertNotNull(withTimeout(20_000) { next.search(GameState.initial()) }.move)
    }

    @Test
    fun shutting_down_leaves_no_engine_process() = runBlocking {
        shared.withEngine(emptyMap()) { assertTrue(it.isAlive) }
        assertTrue("engine should be running", stockfishProcesses() >= 1)

        shared.shutdown()
        delay(500)
        assertEquals(0, stockfishProcesses())
    }

    @Test
    fun the_engine_restarts_after_a_shutdown() = runBlocking {
        val ai = StockfishAi(shared, StockfishProfile.forDifficulty(Difficulty.EASY))
        assertNotNull(withTimeout(20_000) { ai.search(GameState.initial()) }.move)
        shared.shutdown()
        assertNotNull(withTimeout(20_000) { ai.search(GameState.initial()) }.move)
    }
}
