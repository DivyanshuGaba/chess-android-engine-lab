package com.divyanshu.chess.engine

import android.content.Context
import android.os.Build
import com.divyanshu.chess.domain.ai.EngineAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Finds the Stockfish executable the build packaged as a native library. */
class StockfishLocator @Inject constructor(@ApplicationContext private val context: Context) {

    val binary: File? by lazy {
        val file = File(context.applicationInfo.nativeLibraryDir, BINARY_NAME)
        file.takeIf {
            Build.VERSION.SDK_INT >= MIN_SDK_FOR_BINARY && Build.SUPPORTED_ABIS.contains("arm64-v8a") &&
                it.exists() && it.canExecute()
        }
    }

    private companion object {
        const val BINARY_NAME = "libstockfish.so"

        /** The binary is built against Android 10 (API 29) system libraries. */
        const val MIN_SDK_FOR_BINARY = 29
    }
}

/** Read-only facts about the engine, shown in the Engine Lab. */
interface EngineDetails {
    val engineName: StateFlow<String?>
    val binaryPath: String?
    val abi: String
}

/**
 * Owns a single Stockfish process for the whole app. The process starts on first use, is shared
 * by games and analysis, and shuts itself down after a quiet spell to give the memory back.
 */
@Singleton
class SharedEngine @Inject constructor(
    private val locator: StockfishLocator,
) : EngineSession, EngineAvailability, EngineDetails {

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var engine: UciEngine? = null
    private var appliedOptions: Map<String, String> = emptyMap()
    private var idleJob: Job? = null

    private val _engineName = MutableStateFlow<String?>(null)
    override val engineName: StateFlow<String?> = _engineName.asStateFlow()

    override val isStockfishAvailable: Boolean get() = locator.binary != null

    override val binaryPath: String? get() = locator.binary?.absolutePath

    override val abi: String get() = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"

    override suspend fun <T> withEngine(options: Map<String, String>, block: suspend (UciEngine) -> T): T =
        mutex.withLock {
            idleJob?.cancel()
            try {
                val running = ensureStarted()
                val changed = options.filter { (name, value) -> appliedOptions[name] != value }
                if (changed.isNotEmpty()) {
                    running.setOptions(changed)
                    appliedOptions = appliedOptions + changed
                }
                block(running)
            } catch (e: EngineException) {
                discard()
                throw e
            } finally {
                scheduleIdleShutdown()
            }
        }

    /** Stops the engine process now. Safe to call at any time. */
    suspend fun shutdown() = mutex.withLock { discard() }

    private suspend fun ensureStarted(): UciEngine {
        engine?.takeIf { it.isAlive }?.let { return it }
        discard()
        val binary = locator.binary ?: throw EngineUnavailableException("Stockfish is not available on this device")
        val started = withContext(Dispatchers.IO) {
            UciEngine(ProcessUciTransport(listOf(binary.absolutePath), binary.parentFile))
        }
        try {
            started.start()
        } catch (e: Throwable) {
            started.close()
            throw e
        }
        engine = started
        appliedOptions = emptyMap()
        _engineName.value = started.id?.name
        return started
    }

    private suspend fun discard() {
        engine?.close()
        engine = null
        appliedOptions = emptyMap()
    }

    private fun scheduleIdleShutdown() {
        idleJob = scope.launch {
            delay(IDLE_SHUTDOWN_MS)
            shutdown()
        }
    }

    private companion object {
        const val IDLE_SHUTDOWN_MS = 45_000L
    }
}
