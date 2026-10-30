package com.divyanshu.chess.engine

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Limits for one search; the engine stops at whichever is reached first. */
data class SearchLimits(
    val depth: Int? = null,
    val movetimeMs: Long? = null,
    val nodes: Long? = null,
) {
    init {
        require(depth != null || movetimeMs != null || nodes != null) { "A search needs at least one limit" }
    }

    fun toGoCommand(): String = buildString {
        append("go")
        depth?.let { append(" depth $it") }
        movetimeMs?.let { append(" movetime $it") }
        nodes?.let { append(" nodes $it") }
    }

    /** How long to wait for "bestmove" before treating the engine as stuck. */
    val timeoutMs: Long get() = (movetimeMs?.plus(GRACE_MS)) ?: DEFAULT_TIMEOUT_MS

    private companion object {
        const val GRACE_MS = 5_000L
        const val DEFAULT_TIMEOUT_MS = 120_000L
    }
}

data class SearchOutcome(
    val bestMove: String?,
    val ponder: String?,
    /** The deepest principal-variation info line seen during the search. */
    val info: UciInfo?,
    val elapsedMs: Long,
)

data class EngineId(val name: String, val author: String?)

/**
 * A UCI client. Commands are serialised, so only one search runs at a time. Cancelling a
 * search sends "stop" and drains the engine's answer so the next search starts clean.
 */
class UciEngine(
    private val transport: UciTransport,
    private val startTimeoutMs: Long = 15_000,
    private val readyTimeoutMs: Long = 10_000,
    private val stopTimeoutMs: Long = 2_000,
    private val clock: () -> Long = System::nanoTime,
) {
    private val mutex = Mutex()

    var id: EngineId? = null
        private set

    var options: List<UciOption> = emptyList()
        private set

    val isAlive: Boolean get() = transport.isAlive

    suspend fun start() = mutex.withLock {
        transport.send("uci")
        val found = mutableListOf<UciOption>()
        var name: String? = null
        var author: String? = null
        val finished = withTimeoutOrNull(startTimeoutMs) {
            while (true) {
                val line = transport.readLine()
                if (line == "uciok") break
                UciParser.parseId(line, "name")?.let { name = it }
                UciParser.parseId(line, "author")?.let { author = it }
                UciParser.parseOption(line)?.let(found::add)
            }
            true
        }
        if (finished == null) {
            transport.kill()
            throw EngineTimeoutException("Engine did not answer 'uci' within $startTimeoutMs ms")
        }
        id = EngineId(name ?: "unknown", author)
        options = found
        awaitReady()
    }

    suspend fun setOptions(values: Map<String, String>) = mutex.withLock {
        values.forEach { (name, value) ->
            require(options.isEmpty() || options.any { it.name == name }) { "Engine has no option '$name'" }
            transport.send("setoption name $name value $value")
        }
        awaitReady()
    }

    suspend fun newGame() = mutex.withLock {
        transport.send("ucinewgame")
        awaitReady()
    }

    suspend fun search(fen: String, limits: SearchLimits, onInfo: (UciInfo) -> Unit = {}): SearchOutcome =
        mutex.withLock {
            val startedAt = clock()
            transport.send("position fen $fen")
            transport.send(limits.toGoCommand())

            var deepest: UciInfo? = null
            try {
                val best = withTimeoutOrNull(limits.timeoutMs) {
                    var found: BestMove? = null
                    while (found == null) {
                        val line = transport.readLine()
                        found = UciParser.parseBestMove(line)
                        if (found == null) {
                            UciParser.parseInfo(line)?.let { info ->
                                if (info.pv.isNotEmpty() && (info.multiPv ?: 1) == 1 && info.bound == null) deepest = info
                                onInfo(info)
                            }
                        }
                    }
                    found
                }
                if (best == null) {
                    stopAndDrain()
                    throw EngineTimeoutException("No 'bestmove' within ${limits.timeoutMs} ms for ${limits.toGoCommand()}")
                }
                SearchOutcome(best.move, best.ponder, deepest, (clock() - startedAt) / 1_000_000)
            } catch (e: kotlinx.coroutines.CancellationException) {
                withContext(NonCancellable) { stopAndDrain() }
                throw e
            }
        }

    suspend fun close() {
        withContext(NonCancellable) { transport.close() }
    }

    private suspend fun awaitReady() {
        transport.send("isready")
        val ready = withTimeoutOrNull(readyTimeoutMs) {
            while (transport.readLine() != "readyok") { /* skip unrelated output */ }
            true
        }
        if (ready == null) {
            transport.kill()
            throw EngineTimeoutException("Engine did not answer 'isready' within $readyTimeoutMs ms")
        }
    }

    /** Stops a running search and consumes its "bestmove" so the pipe holds no stale output. */
    private suspend fun stopAndDrain() {
        if (!transport.isAlive) return
        try {
            transport.send("stop")
            val drained = withTimeoutOrNull(stopTimeoutMs) {
                while (UciParser.parseBestMove(transport.readLine()) == null) { /* discard */ }
                true
            }
            if (drained == null) transport.kill()
        } catch (_: EngineException) {
            transport.kill()
        }
    }
}
