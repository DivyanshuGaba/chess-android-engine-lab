package com.divyanshu.chess.domain.ai

import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.PieceType
import com.divyanshu.chess.domain.model.Side
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.ensureActive

/** Negamax with alpha-beta pruning; [noise] perturbs root scores so weaker levels play less sharply. */
class MinimaxAi(
    private val depth: Int,
    private val noise: Int = 0,
    private val random: Random = Random.Default,
) : ChessAi {

    override suspend fun chooseMove(state: GameState): Move? = search(state).move

    override suspend fun search(state: GameState): SearchResult {
        val startedAt = System.nanoTime()
        nodes = 0
        val moves = ordered(state, ChessRules.legalMoves(state))
        if (moves.isEmpty()) return SearchResult(null, null)

        var best: Move? = null
        var bestScore = Int.MIN_VALUE
        var bestTrueScore = 0
        var alpha = -INFINITY
        for (move in moves) {
            coroutineContext.ensureActive()
            val score = -search(ChessRules.applyMove(state, move), depth - 1, -INFINITY, -alpha, 1)
            val perturbed = if (noise > 0) score + random.nextInt(noise) else score
            if (perturbed > bestScore) {
                bestScore = perturbed
                bestTrueScore = score
                best = move
            }
            if (noise == 0) alpha = maxOf(alpha, score)
        }

        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        val whiteScore = if (state.sideToMove == Side.WHITE) bestTrueScore else -bestTrueScore
        val mate = abs(whiteScore) >= MATE - MAX_PLY
        val mateIn = if (mate) ((MATE - abs(whiteScore) + 1) / 2).let { if (whiteScore > 0) it else -it } else null
        val info = EngineInfo(
            engineName = "Built-in minimax",
            depth = depth,
            scoreCp = if (mate) null else whiteScore,
            mateIn = mateIn,
            nodes = nodes,
            nps = if (elapsedMs > 0) nodes * 1_000 / elapsedMs else nodes,
            timeMs = elapsedMs,
            pv = listOfNotNull(best),
        )
        return SearchResult(best, info)
    }

    private fun search(state: GameState, depth: Int, alphaIn: Int, beta: Int, ply: Int): Int {
        nodes++
        if (depth == 0) return evaluate(state)
        val moves = ChessRules.legalMoves(state)
        if (moves.isEmpty()) {
            return if (ChessRules.isInCheck(state)) -MATE + ply else 0
        }
        var alpha = alphaIn
        for (move in ordered(state, moves)) {
            val score = -search(ChessRules.applyMove(state, move), depth - 1, -beta, -alpha, ply + 1)
            if (score >= beta) return beta
            if (score > alpha) alpha = score
        }
        return alpha
    }

    private fun ordered(state: GameState, moves: List<Move>): List<Move> =
        moves.sortedByDescending { move ->
            val victim = state.board[move.to]?.type?.value ?: 0
            val attacker = state.board[move.from]?.type?.value ?: 0
            (if (victim > 0) 10 * victim - attacker else 0) + (move.promotion?.value ?: 0)
        }

    /** Material plus a small centralisation bonus, from the point of view of the side to move. */
    private fun evaluate(state: GameState): Int {
        var score = 0
        for ((square, piece) in state.board.pieces) {
            val sign = if (piece.side == Side.WHITE) 1 else -1
            val centre = if (piece.type == PieceType.KING) 0 else 6 - centreDistance(square.file, square.rank)
            score += sign * (piece.type.value + centre * CENTRE_WEIGHT)
        }
        return if (state.sideToMove == Side.WHITE) score else -score
    }

    private fun centreDistance(file: Int, rank: Int): Int =
        (abs(2 * file - 7) + abs(2 * rank - 7)) / 2

    private var nodes = 0L

    private companion object {
        const val MAX_PLY = 64
        const val INFINITY = 1_000_000
        const val MATE = 100_000
        const val CENTRE_WEIGHT = 4
    }
}
