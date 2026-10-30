package com.divyanshu.chess.engine

import com.divyanshu.chess.testutil.ScriptedTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UciEngineTest {

    private val start = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    private suspend fun startedEngine(transport: ScriptedTransport = ScriptedTransport()): UciEngine =
        UciEngine(transport).also { it.start() }

    @Test
    fun `start handshakes with uci then isready`() = runTest {
        val transport = ScriptedTransport()
        val engine = startedEngine(transport)

        assertEquals(listOf("uci", "isready"), transport.sent)
        assertEquals("TestFish 1.0", engine.id?.name)
        assertEquals(listOf("Hash", "UCI_Elo", "UCI_LimitStrength", "Threads", "MultiPV"), engine.options.map { it.name })
    }

    @Test
    fun `options are sent then confirmed`() = runTest {
        val transport = ScriptedTransport()
        val engine = startedEngine(transport)
        engine.setOptions(mapOf("UCI_Elo" to "1800", "UCI_LimitStrength" to "true"))

        assertTrue("setoption name UCI_Elo value 1800" in transport.sent)
        assertTrue("setoption name UCI_LimitStrength value true" in transport.sent)
        assertEquals("isready", transport.sent.last())
    }

    @Test
    fun `an option the engine does not have is rejected`() = runTest {
        val engine = startedEngine()
        try {
            engine.setOptions(mapOf("Imaginary" to "1"))
            fail("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `search returns the bestmove and the deepest info`() = runTest {
        val transport = ScriptedTransport()
        val engine = startedEngine(transport)
        val seen = mutableListOf<Int?>()

        val outcome = engine.search(start, SearchLimits(depth = 2)) { seen += it.depth }

        assertEquals("e2e4", outcome.bestMove)
        assertEquals(2, outcome.info?.depth)
        assertEquals(listOf(1, 2), seen)
        assertTrue("position fen $start" in transport.sent)
        assertTrue("go depth 2" in transport.sent)
    }

    @Test
    fun `no legal move comes back as null`() = runTest {
        val transport = ScriptedTransport().apply { bestMove = "(none)" }
        assertNull(startedEngine(transport).search(start, SearchLimits(depth = 1)).bestMove)
    }

    @Test
    fun `cancelling a search sends stop and leaves the pipe clean`() = runTest {
        val transport = ScriptedTransport().apply { holdSearch = true }
        val engine = startedEngine(transport)

        val job = launch { engine.search(start, SearchLimits(depth = 30)) }
        runCurrent()
        job.cancel()
        job.join()

        assertTrue("stop" in transport.sent)
        assertFalse(transport.killed)

        transport.holdSearch = false
        transport.bestMove = "d2d4"
        assertEquals("d2d4", engine.search(start, SearchLimits(depth = 1)).bestMove)
    }

    @Test
    fun `a search that never answers times out and stops the engine`() = runTest {
        val transport = ScriptedTransport().apply { holdSearch = true }
        val engine = startedEngine(transport)
        try {
            engine.search(start, SearchLimits(movetimeMs = 100))
            fail("expected EngineTimeoutException")
        } catch (_: EngineTimeoutException) {
        }
        assertTrue("stop" in transport.sent)
    }

    @Test
    fun `an engine that dies mid search is reported as crashed`() = runTest {
        val transport = ScriptedTransport().apply { holdSearch = true }
        val engine = startedEngine(transport)
        val job = launch {
            try {
                engine.search(start, SearchLimits(depth = 30))
                fail("expected EngineCrashedException")
            } catch (_: EngineCrashedException) {
            }
        }
        runCurrent()
        transport.kill()
        job.join()
        assertFalse(engine.isAlive)
    }

    @Test
    fun `an engine that never says ready is killed`() = runTest {
        val transport = ScriptedTransport().apply { ignoreIsReady = true }
        try {
            UciEngine(transport).start()
            fail("expected EngineTimeoutException")
        } catch (_: EngineTimeoutException) {
        }
        assertTrue(transport.killed)
    }

    @Test
    fun `close shuts the transport`() = runTest {
        val transport = ScriptedTransport()
        startedEngine(transport).close()
        assertTrue(transport.closed)
    }
}
