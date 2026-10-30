package com.divyanshu.chess.uci

import com.divyanshu.chess.domain.ai.EvaluationKind
import com.divyanshu.chess.domain.ai.Features
import com.divyanshu.chess.domain.ai.MinimaxAi
import com.divyanshu.chess.domain.ai.Searcher
import com.divyanshu.chess.domain.ai.Zobrist
import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.engine.Fen
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.Side
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking

/**
 * A UCI front end for the app's two built-in engines, so the benchmark harness can play them like
 * any other engine. `Variant` picks "baseline" (the original fixed-depth minimax) or "advanced".
 */
fun main() {
    val engine = UciFrontEnd()
    while (true) {
        val line = readLine() ?: break
        if (!engine.handle(line.trim())) break
    }
    engine.stopAndJoin()
}

private class UciFrontEnd {
    private var variant = "advanced"
    private var baselineDepth = 4
    private var hashMegabytes = 32
    private var features = Features()
    private var searcher = Searcher(tableBitsFor(hashMegabytes), features)

    private var state = GameState.initial()
    private var earlier = mutableListOf<Long>()

    private val stopFlag = AtomicBoolean(false)
    private var worker: Thread? = null

    fun handle(line: String): Boolean {
        val tokens = line.split(' ').filter { it.isNotEmpty() }
        when (tokens.firstOrNull()) {
            "uci" -> {
                println("id name BuiltIn-UCI 1.0")
                println("id author ChessApp")
                println("option name Variant type combo default advanced var baseline var advanced")
                println("option name Depth type spin default 4 min 1 max 12")
                println("option name Hash type spin default 32 min 1 max 512")
                println("option name Quiescence type check default true")
                println("option name TranspositionTable type check default true")
                println("option name Reductions type check default true")
                println("option name OrderingHeuristics type check default true")
                println("option name CheckExtension type check default true")
                println("option name Evaluation type combo default pst var pst var legacy")
                println("uciok")
            }
            "isready" -> println("readyok")
            "setoption" -> setOption(tokens)
            "ucinewgame" -> {
                stopAndJoin()
                searcher.clear()
            }
            "position" -> {
                stopAndJoin()
                position(tokens)
            }
            "go" -> go(tokens)
            "stop" -> stopFlag.set(true)
            "quit" -> return false
        }
        return true
    }

    fun stopAndJoin() {
        stopFlag.set(true)
        worker?.join()
        worker = null
    }

    private fun setOption(tokens: List<String>) {
        val name = tokens.drop(2).takeWhile { it != "value" }.joinToString(" ")
        val value = tokens.dropWhile { it != "value" }.drop(1).joinToString(" ")
        when (name) {
            "Variant" -> variant = value
            "Depth" -> baselineDepth = value.toIntOrNull() ?: baselineDepth
            "Hash" -> {
                hashMegabytes = value.toIntOrNull() ?: hashMegabytes
                rebuildSearcher()
            }
            "Quiescence" -> { features = features.copy(quiescence = value == "true"); rebuildSearcher() }
            "TranspositionTable" -> { features = features.copy(transpositionTable = value == "true"); rebuildSearcher() }
            "Reductions" -> { features = features.copy(reductions = value == "true"); rebuildSearcher() }
            "OrderingHeuristics" -> { features = features.copy(orderingHeuristics = value == "true"); rebuildSearcher() }
            "CheckExtension" -> { features = features.copy(checkExtension = value == "true"); rebuildSearcher() }
            "Evaluation" -> {
                features = features.copy(evaluation = if (value == "legacy") EvaluationKind.LEGACY else EvaluationKind.PST)
                rebuildSearcher()
            }
        }
    }

    private fun rebuildSearcher() {
        searcher = Searcher(tableBitsFor(hashMegabytes), features)
    }

    private fun position(tokens: List<String>) {
        val movesAt = tokens.indexOf("moves")
        val setup = if (movesAt >= 0) tokens.subList(1, movesAt) else tokens.drop(1)
        state = if (setup.firstOrNull() == "fen") Fen.parse(setup.drop(1).joinToString(" ")) else GameState.initial()
        earlier = mutableListOf()
        if (movesAt >= 0) {
            for (text in tokens.drop(movesAt + 1)) {
                earlier += Zobrist.hash(state)
                state = ChessRules.applyMove(state, Move.fromUci(text))
            }
        }
    }

    private fun go(tokens: List<String>) {
        stopAndJoin()
        var depth: Int? = null
        var nodes: Long? = null
        var movetime: Long? = null
        var whiteClock: Long? = null
        var blackClock: Long? = null
        var i = 1
        while (i < tokens.size) {
            when (tokens[i]) {
                "depth" -> depth = tokens.getOrNull(++i)?.toIntOrNull()
                "nodes" -> nodes = tokens.getOrNull(++i)?.toLongOrNull()
                "movetime" -> movetime = tokens.getOrNull(++i)?.toLongOrNull()
                "wtime" -> whiteClock = tokens.getOrNull(++i)?.toLongOrNull()
                "btime" -> blackClock = tokens.getOrNull(++i)?.toLongOrNull()
            }
            i++
        }
        val clock = if (state.sideToMove == Side.WHITE) whiteClock else blackClock
        if (movetime == null && clock != null) movetime = (clock / 30).coerceAtLeast(10)
        val limits = Searcher.Limits(depth, nodes, movetime)

        stopFlag.set(false)
        val snapshot = state
        val history = earlier.toList()
        worker = thread(name = "search", isDaemon = true) {
            val best = if (variant == "baseline") runBaseline(snapshot, depth ?: baselineDepth) else runAdvanced(snapshot, history, limits)
            println("bestmove ${best?.uci ?: "0000"}")
        }
    }

    private fun runAdvanced(state: GameState, history: List<Long>, limits: Searcher.Limits): Move? {
        val sign = 1
        val effective = if (limits.depth == null && limits.nodes == null && limits.movetimeMs == null) {
            Searcher.Limits(depth = Searcher.DEFAULT_DEPTH)
        } else {
            limits
        }
        val result = searcher.search(state, history, effective, shouldStop = { stopFlag.get() }) { p ->
            val score = p.mateIn?.let { "mate $it" } ?: "cp ${p.scoreCp?.times(sign)}"
            val nps = if (p.timeMs > 0) p.nodes * 1000 / p.timeMs else p.nodes
            println("info depth ${p.depth} seldepth ${p.depth} score $score nodes ${p.nodes} nps $nps time ${p.timeMs} pv ${p.pv.joinToString(" ") { it.uci }}")
        }
        return result.best
    }

    private fun runBaseline(state: GameState, depth: Int): Move? {
        val result = runBlocking { MinimaxAi(depth = depth).search(state) }
        result.info?.let { info ->
            // The baseline reports from White's point of view; UCI wants the side to move's.
            val sign = if (state.sideToMove == Side.WHITE) 1 else -1
            val score = info.mateIn?.let { "mate ${it * sign}" } ?: "cp ${(info.scoreCp ?: 0) * sign}"
            println("info depth ${info.depth} seldepth ${info.depth} score $score nodes ${info.nodes} nps ${info.nps} time ${info.timeMs} pv ${info.pv.joinToString(" ") { it.uci }}")
        }
        return result.move
    }
}

private fun tableBitsFor(megabytes: Int): Int {
    // Each entry uses two longs (16 bytes).
    var bits = 10
    while (bits < 26 && (16L shl bits) < megabytes * 1_048_576L) bits++
    return bits
}
