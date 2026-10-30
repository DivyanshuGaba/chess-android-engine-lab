package com.divyanshu.chess.testutil

import com.divyanshu.chess.engine.EngineCrashedException
import com.divyanshu.chess.engine.UciTransport
import kotlinx.coroutines.channels.Channel

/** A stand-in engine that answers UCI commands from a script, so protocol code is tested without a process. */
class ScriptedTransport : UciTransport {
    val sent = mutableListOf<String>()
    var bestMove = "e2e4"
    var infoLines = listOf(
        "info depth 1 seldepth 1 multipv 1 score cp 20 nodes 20 nps 20000 time 1 pv e2e4",
        "info depth 2 seldepth 2 multipv 1 score cp 31 nodes 120 nps 60000 time 2 pv e2e4 e7e5",
    )
    var holdSearch = false
    var ignoreIsReady = false
    var closed = false
    var killed = false

    private var searching = false
    private val inbox = Channel<String>(Channel.UNLIMITED)
    private var alive = true

    override val isAlive: Boolean get() = alive

    override fun send(line: String) {
        sent += line
        when {
            line == "uci" -> {
                emit("id name TestFish 1.0")
                emit("id author Tests")
                emit("option name Hash type spin default 16 min 1 max 1024")
                emit("option name UCI_Elo type spin default 1320 min 1320 max 3190")
                emit("option name UCI_LimitStrength type check default false")
                emit("option name Threads type spin default 1 min 1 max 8")
                emit("option name MultiPV type spin default 1 min 1 max 8")
                emit("uciok")
            }
            line == "isready" -> if (!ignoreIsReady) emit("readyok")
            line.startsWith("go") -> if (holdSearch) searching = true else answerSearch()
            line == "stop" -> if (searching) {
                searching = false
                answerSearch()
            }
        }
    }

    private fun answerSearch() {
        infoLines.forEach(::emit)
        emit("bestmove $bestMove")
    }

    fun emit(line: String) {
        inbox.trySend(line)
    }

    override suspend fun readLine(): String =
        inbox.receiveCatching().getOrNull() ?: throw EngineCrashedException("scripted engine closed")

    override fun close() {
        closed = true
        alive = false
        inbox.close()
    }

    override fun kill() {
        killed = true
        alive = false
        inbox.close()
    }
}
