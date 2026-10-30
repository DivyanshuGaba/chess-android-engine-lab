package com.divyanshu.chess.domain.ai

import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.PieceType
import com.divyanshu.chess.domain.model.Square
import kotlin.math.abs

/**
 * Alpha-beta search with iterative deepening, a transposition table, quiescence search, killer and
 * history move ordering, late move reductions and repetition detection. Scores are centipawns from the
 * side to move's point of view. One instance is single-threaded; share it across moves to keep its table.
 */
class Searcher(tableBits: Int = 20, private val features: Features = Features()) {

    data class Limits(val depth: Int? = null, val nodes: Long? = null, val movetimeMs: Long? = null)

    data class Progress(
        val depth: Int,
        val score: Int,
        val scoreCp: Int?,
        val mateIn: Int?,
        val nodes: Long,
        val timeMs: Long,
        val pv: List<Move>,
    )

    class Result(val best: Move?, val last: Progress?)

    private val mask = (1 shl tableBits) - 1
    private val keys = LongArray(mask + 1)
    private val entries = LongArray(mask + 1)

    private var nodes = 0L
    private var aborted = false
    private var canAbort = false
    private var deadline = Long.MAX_VALUE
    private var nodeLimit = Long.MAX_VALUE
    private var shouldStop: () -> Boolean = { false }
    private val killers = Array(MAX_PLY) { IntArray(2) }
    private val history = Array(2) { IntArray(64 * 64) }
    private val path = LongArray(MAX_PLY + 2)
    private var earlier: List<Long> = emptyList()

    fun clear() {
        keys.fill(0)
        entries.fill(0)
        history.forEach { it.fill(0) }
        killers.forEach { it.fill(0) }
    }

    fun search(
        state: GameState,
        earlierPositions: List<Long> = emptyList(),
        limits: Limits = Limits(depth = DEFAULT_DEPTH),
        shouldStop: () -> Boolean = { false },
        onProgress: (Progress) -> Unit = {},
    ): Result {
        val rootMoves = ChessRules.legalMoves(state)
        if (rootMoves.isEmpty()) return Result(null, null)

        val started = System.nanoTime()
        nodes = 0
        aborted = false
        canAbort = false
        earlier = earlierPositions
        this.shouldStop = shouldStop
        nodeLimit = limits.nodes ?: Long.MAX_VALUE
        val budgetNs = limits.movetimeMs?.times(1_000_000)
        deadline = budgetNs?.let { started + it } ?: Long.MAX_VALUE
        killers.forEach { it.fill(0) }
        path[0] = Zobrist.hash(state)

        val maxDepth = if (rootMoves.size == 1) 1 else (limits.depth ?: MAX_DEPTH).coerceIn(1, MAX_DEPTH)
        var best = rootMoves.first()
        var last: Progress? = null

        for (depth in 1..maxDepth) {
            canAbort = depth > 1
            val (move, score) = searchRoot(state, rootMoves, depth)
            if (aborted) break
            best = move
            last = progress(state, depth, score, started, move)
            onProgress(last)
            if (abs(score) >= MATE_BOUND) break
            if (budgetNs != null && System.nanoTime() - started > budgetNs * 0.55) break
        }
        return Result(best, last)
    }

    private fun progress(state: GameState, depth: Int, score: Int, started: Long, best: Move): Progress {
        val mate = abs(score) >= MATE_BOUND
        val mateIn = if (mate) ((MATE - abs(score) + 1) / 2).let { if (score > 0) it else -it } else null
        return Progress(
            depth = depth,
            score = score,
            scoreCp = if (mate) null else score,
            mateIn = mateIn,
            nodes = nodes,
            timeMs = (System.nanoTime() - started) / 1_000_000,
            pv = principalVariation(state, depth).ifEmpty { listOf(best) },
        )
    }

    private fun searchRoot(state: GameState, rootMoves: List<Move>, depth: Int): Pair<Move, Int> {
        val ttMove = entryMove(probe(path[0]))
        val ordered = order(state, rootMoves, ttMove, 0)
        var alpha = -INFINITY
        var best = ordered.first()
        for (move in ordered) {
            val next = ChessRules.applyMove(state, move)
            path[1] = Zobrist.hash(next)
            val score = -negamax(next, depth - 1, -INFINITY, -alpha, 1)
            if (aborted) return best to alpha
            if (score > alpha) {
                alpha = score
                best = move
            }
        }
        store(path[0], encode(best), alpha, depth, EXACT, 0)
        return best to alpha
    }

    private fun negamax(state: GameState, depthIn: Int, alphaIn: Int, beta: Int, ply: Int): Int {
        nodes++
        if (canAbort && (nodes and 2047L) == 0L && stopRequested()) aborted = true
        if (aborted) return 0

        val hash = path[ply]
        if (state.halfmoveClock >= 100 || isRepetition(hash, ply)) return 0
        if (ply >= MAX_PLY - 1) return eval(state)

        val inCheck = ChessRules.isInCheck(state)
        val depth = if (inCheck && features.checkExtension) depthIn + 1 else depthIn
        if (depth <= 0) return if (features.quiescence) quiescence(state, alphaIn, beta, ply, 0) else eval(state)

        var ttMove = 0
        val entry = probe(hash)
        if (entry != 0L) {
            ttMove = entryMove(entry)
            if (entryDepth(entry) >= depth) {
                val score = fromTable(entryScore(entry), ply)
                when (entryFlag(entry)) {
                    EXACT -> return score
                    LOWER -> if (score >= beta) return score
                    UPPER -> if (score <= alphaIn) return score
                }
            }
        }

        val moves = ChessRules.legalMoves(state)
        if (moves.isEmpty()) return if (inCheck) -MATE + ply else 0

        var alpha = alphaIn
        var best = -INFINITY
        var bestMove = 0
        val ordered = order(state, moves, ttMove, ply)
        for ((index, move) in ordered.withIndex()) {
            val next = ChessRules.applyMove(state, move)
            path[ply + 1] = Zobrist.hash(next)
            val quiet = state.board[move.to] == null && move.promotion == null
            var score: Int
            if (features.reductions && !inCheck && quiet && depth >= 3 && index >= 4) {
                score = -negamax(next, depth - 2, -alpha - 1, -alpha, ply + 1)
                if (score > alpha && !aborted) score = -negamax(next, depth - 1, -beta, -alpha, ply + 1)
            } else {
                score = -negamax(next, depth - 1, -beta, -alpha, ply + 1)
            }
            if (aborted) return 0
            if (score > best) {
                best = score
                bestMove = encode(move)
            }
            if (score > alpha) {
                alpha = score
                if (alpha >= beta) {
                    if (quiet && features.orderingHeuristics) rememberQuiet(state, move, depth, ply)
                    break
                }
            }
        }

        val flag = when {
            best <= alphaIn -> UPPER
            best >= beta -> LOWER
            else -> EXACT
        }
        store(hash, bestMove, best, depth, flag, ply)
        return best
    }

    private fun quiescence(state: GameState, alphaIn: Int, beta: Int, ply: Int, qPly: Int): Int {
        nodes++
        if (canAbort && (nodes and 2047L) == 0L && stopRequested()) aborted = true
        if (aborted) return 0
        if (ply >= MAX_PLY - 1) return eval(state)

        val inCheck = ChessRules.isInCheck(state)
        var alpha = alphaIn
        val moves: List<Move>
        var best: Int
        val standPat: Int
        if (inCheck) {
            moves = ChessRules.legalMoves(state)
            if (moves.isEmpty()) return -MATE + ply
            standPat = -INFINITY
            best = -INFINITY
            if (qPly >= MAX_QUIESCENCE_PLY) return eval(state)
        } else {
            standPat = eval(state)
            if (standPat >= beta) return standPat
            if (standPat > alpha) alpha = standPat
            if (qPly >= MAX_QUIESCENCE_PLY) return standPat
            moves = ChessRules.legalTacticalMoves(state)
            best = standPat
        }

        for (move in order(state, moves, 0, ply)) {
            if (!inCheck && move.promotion == null && standPat + victimValue(state, move) + DELTA_MARGIN < alpha) continue
            val next = ChessRules.applyMove(state, move)
            path[ply + 1] = Zobrist.hash(next)
            val score = -quiescence(next, -beta, -alpha, ply + 1, qPly + 1)
            if (aborted) return 0
            if (score > best) best = score
            if (score > alpha) {
                alpha = score
                if (alpha >= beta) break
            }
        }
        return best
    }

    private fun order(state: GameState, moves: List<Move>, ttMove: Int, ply: Int): List<Move> {
        val side = state.sideToMove.ordinal
        val killer = if (features.orderingHeuristics && ply < MAX_PLY) killers[ply] else null
        return moves.sortedByDescending { move ->
            val code = encode(move)
            val victim = victimValue(state, move)
            when {
                code == ttMove && ttMove != 0 -> 10_000_000
                victim > 0 -> 1_000_000 + victim * 16 - (state.board[move.from]?.type?.value ?: 0) / 16
                move.promotion != null -> 900_000 + move.promotion.value
                killer != null && code == killer[0] -> 800_000
                killer != null && code == killer[1] -> 700_000
                features.orderingHeuristics -> history[side][move.from.index * 64 + move.to.index]
                else -> 0
            }
        }
    }

    private fun victimValue(state: GameState, move: Move): Int {
        state.board[move.to]?.let { return it.type.value }
        val mover = state.board[move.from]
        return if (mover?.type == PieceType.PAWN && move.to == state.enPassant) PieceType.PAWN.value else 0
    }

    private fun rememberQuiet(state: GameState, move: Move, depth: Int, ply: Int) {
        val code = encode(move)
        if (ply < MAX_PLY && killers[ply][0] != code) {
            killers[ply][1] = killers[ply][0]
            killers[ply][0] = code
        }
        val table = history[state.sideToMove.ordinal]
        val slot = move.from.index * 64 + move.to.index
        table[slot] = (table[slot] + depth * depth).coerceAtMost(HISTORY_LIMIT)
    }

    private fun isRepetition(hash: Long, ply: Int): Boolean {
        var i = ply - 2
        while (i >= 0) {
            if (path[i] == hash) return true
            i -= 2
        }
        return earlier.contains(hash)
    }

    private fun stopRequested(): Boolean =
        shouldStop() || System.nanoTime() >= deadline || nodes >= nodeLimit

    private fun principalVariation(root: GameState, depth: Int): List<Move> {
        val line = mutableListOf<Move>()
        val seen = HashSet<Long>()
        var state = root
        var hash = Zobrist.hash(root)
        while (line.size < depth && seen.add(hash)) {
            val code = entryMove(probe(hash))
            if (code == 0) break
            val move = decode(code)
            if (move !in ChessRules.legalMoves(state)) break
            line += move
            state = ChessRules.applyMove(state, move)
            hash = Zobrist.hash(state)
        }
        return line
    }

    // Transposition table: key array plus one packed long per entry (move, score, depth, flag).
    private fun slot(key: Long): Int = (key xor (key ushr 32)).toInt() and mask

    private fun eval(state: GameState): Int = Evaluation.evaluate(state, features.evaluation)

    private fun probe(key: Long): Long =
        if (features.transpositionTable && keys[slot(key)] == key) entries[slot(key)] else 0L

    private fun store(key: Long, move: Int, score: Int, depth: Int, flag: Int, ply: Int) {
        if (!features.transpositionTable) return
        val index = slot(key)
        val packed = move.toLong() or
            ((toTable(score, ply) + SCORE_OFFSET).toLong() shl 15) or
            (depth.toLong() shl 35) or
            (flag.toLong() shl 43)
        keys[index] = key
        entries[index] = packed
    }

    private fun entryMove(entry: Long): Int = (entry and 0x7FFF).toInt()
    private fun entryScore(entry: Long): Int = ((entry shr 15) and 0xFFFFF).toInt() - SCORE_OFFSET
    private fun entryDepth(entry: Long): Int = ((entry shr 35) and 0xFF).toInt()
    private fun entryFlag(entry: Long): Int = ((entry shr 43) and 0x3).toInt()

    private fun toTable(score: Int, ply: Int) = when {
        score >= MATE_BOUND -> score + ply
        score <= -MATE_BOUND -> score - ply
        else -> score
    }

    private fun fromTable(score: Int, ply: Int) = when {
        score >= MATE_BOUND -> score - ply
        score <= -MATE_BOUND -> score + ply
        else -> score
    }

    private fun encode(move: Move): Int =
        move.from.index or (move.to.index shl 6) or ((move.promotion?.ordinal ?: 0) shl 12)

    private fun decode(code: Int): Move = Move(
        Square(code and 63),
        Square((code shr 6) and 63),
        (code shr 12).takeIf { it > 0 }?.let { PieceType.entries[it] },
    )

    companion object {
        const val DEFAULT_DEPTH = 6
        const val MAX_DEPTH = 64
        const val MATE = 100_000
        private const val MAX_PLY = 96
        private const val MATE_BOUND = MATE - MAX_PLY - 8
        private const val INFINITY = 1_000_000
        private const val MAX_QUIESCENCE_PLY = 10
        private const val DELTA_MARGIN = 200
        private const val HISTORY_LIMIT = 400_000
        private const val SCORE_OFFSET = 524_288 // entries pack scores into 20 bits
        private const val EXACT = 1
        private const val LOWER = 2
        private const val UPPER = 3
    }
}
