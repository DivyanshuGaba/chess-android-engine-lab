package com.divyanshu.chess

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Debug
import android.os.PowerManager
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.divyanshu.chess.engine.DefaultAiProvider
import com.divyanshu.chess.engine.SharedEngine
import com.divyanshu.chess.engine.StockfishLocator
import com.divyanshu.chess.ui.spectator.SpectatorSpeed
import com.divyanshu.chess.ui.spectator.SpectatorViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * A stability soak: several full engine-vs-engine matches in a row through the real spectator ViewModel,
 * recording memory, engine processes and device heat. It only runs when started with `-e soak true`.
 */
class SpectatorSoakRun {

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun engineProcesses(): Int {
        val process = Runtime.getRuntime().exec(arrayOf("ps", "-A"))
        return process.inputStream.bufferedReader().readLines().count { it.contains("libstockfish") }
    }

    private fun batteryTenthsCelsius(): Int =
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1

    private fun batteryPercent(): Int =
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1

    private fun thermalStatus(): Int =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).currentThermalStatus

    private fun javaHeapMb(): Long {
        System.gc()
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576
    }

    @Test
    fun several_full_matches_run_without_leaks_or_overheating() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("start with -e soak true to run the soak", args.getString("soak") == "true")
        val games = args.getString("games")?.toIntOrNull() ?: 5

        val shared = SharedEngine(StockfishLocator(context))
        assumeTrue("Stockfish is not available on this device", shared.isStockfishAvailable)
        val viewModel = withTimeout(10_000) {
            kotlinx.coroutines.withContext(Dispatchers.Main) { SpectatorViewModel(DefaultAiProvider(shared), shared, Dispatchers.Default) }
        }
        viewModel.onSpeedChanged(SpectatorSpeed.FAST)

        val report = StringBuilder()
        fun log(line: String) {
            Log.i("Soak", line)
            report.appendLine(line)
        }

        val heapAtStart = javaHeapMb()
        log("start: battery ${batteryPercent()}%, ${batteryTenthsCelsius() / 10.0} C, thermal ${thermalStatus()}, heap $heapAtStart MB")
        var worstThermal = thermalStatus()
        var errors = 0
        var unfinished = 0

        repeat(games) { index ->
            val started = System.nanoTime()
            withTimeout(5_000) { kotlinx.coroutines.withContext(Dispatchers.Main) { viewModel.onStart() } }
            var finished = false
            val deadline = System.nanoTime() + 6 * 60_000_000_000L
            while (System.nanoTime() < deadline) {
                if (viewModel.uiState.value.finished) {
                    finished = true
                    break
                }
                delay(500)
            }
            val state = viewModel.uiState.value
            if (!finished) unfinished++
            if (state.error != null) errors++
            worstThermal = maxOf(worstThermal, thermalStatus())
            log(
                "game ${index + 1}: ${if (finished) state.outcome?.title ?: state.error ?: "stopped at move limit" else "DID NOT FINISH"}, " +
                    "${state.board.moveCount} plies, ${(System.nanoTime() - started) / 1_000_000_000} s, " +
                    "battery ${batteryPercent()}%, ${batteryTenthsCelsius() / 10.0} C, thermal ${thermalStatus()}, " +
                    "heap ${javaHeapMb()} MB, native ${Debug.getNativeHeapAllocatedSize() / 1_048_576} MB, engine processes ${engineProcesses()}",
            )
        }

        withTimeout(5_000) { kotlinx.coroutines.withContext(Dispatchers.Main) { viewModel.onReset() } }
        shared.shutdown()
        delay(800)
        val heapAtEnd = javaHeapMb()
        val leftover = engineProcesses()
        log("end: heap $heapAtEnd MB (start $heapAtStart MB), engine processes after shutdown $leftover, worst thermal status $worstThermal")

        File(context.filesDir, "soak.txt").writeText(report.toString())

        assertEquals("every match should finish", 0, unfinished)
        assertEquals("no match should stop on an engine error", 0, errors)
        assertEquals("the engine process should be gone after shutdown", 0, leftover)
        assertTrue("Java heap grew from $heapAtStart MB to $heapAtEnd MB", heapAtEnd - heapAtStart < 64)
        assertTrue("device reached thermal status $worstThermal", worstThermal < PowerManager.THERMAL_STATUS_SEVERE)
    }
}
