package com.divyanshu.chess

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.engine.SharedEngine
import com.divyanshu.chess.engine.StockfishAi
import com.divyanshu.chess.engine.StockfishLocator
import com.divyanshu.chess.engine.StockfishProfile
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Proves the engine never stalls the UI thread: a 60 Hz probe on the main looper must stay on time. */
class MainThreadResponsivenessTest {

    private val main = Handler(Looper.getMainLooper())

    private fun probeMainThread(running: AtomicBoolean, lateness: MutableList<Long>) {
        val due = SystemClock.uptimeMillis() + FRAME_MS
        main.postAtTime({
            lateness += SystemClock.uptimeMillis() - due
            if (running.get()) probeMainThread(running, lateness)
        }, due)
    }

    @Test
    fun the_main_thread_stays_responsive_while_stockfish_thinks() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val shared = SharedEngine(StockfishLocator(context))
        val ai = StockfishAi(shared, StockfishProfile.forDifficulty(Difficulty.HARD))
        val lateness = CopyOnWriteArrayList<Long>()
        val running = AtomicBoolean(true)

        main.post { probeMainThread(running, lateness) }
        try {
            repeat(3) {
                val result = withContext(Dispatchers.Default) { ai.search(GameState.initial()) }
                assertNotNull(result.move)
            }
        } finally {
            running.set(false)
            shared.shutdown()
        }

        val sorted = lateness.sorted()
        val p95 = sorted[(sorted.size * 95 / 100).coerceAtMost(sorted.lastIndex)]
        val worst = sorted.last()
        assertTrue("main thread only ticked ${sorted.size} times in about 3 s of thinking", sorted.size > 60)
        assertTrue("95th percentile lateness was $p95 ms", p95 < 50)
        assertTrue("worst main-thread stall was $worst ms", worst < 250)
    }

    @Test
    fun starting_a_search_from_the_main_thread_does_not_block_it() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val shared = SharedEngine(StockfishLocator(context))
        val ai = StockfishAi(shared, StockfishProfile.forDifficulty(Difficulty.MEDIUM))
        try {
            val result = withContext(Dispatchers.Main) { ai.search(GameState.initial()) }
            assertNotNull(result.move)
        } finally {
            shared.shutdown()
        }
    }

    private companion object {
        const val FRAME_MS = 16L
    }
}
