package com.divyanshu.chess.domain.ai

import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.PieceType
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.domain.model.Square

/**
 * Material plus piece-square tables (the well-known "simplified evaluation function" tables), with a
 * separate king table for endgames and a bonus for the bishop pair. Scores are in centipawns from the
 * point of view of the side to move.
 */
object Evaluation {

    // Tables are written from White's side with rank 8 on the first row.
    private val pawn = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 50, 50, 50, 50, 50, 50, 50, 50, 10, 10, 20, 30, 30, 20, 10, 10,
        5, 5, 10, 25, 25, 10, 5, 5, 0, 0, 0, 20, 20, 0, 0, 0, 5, -5, -10, 0, 0, -10, -5, 5,
        5, 10, 10, -20, -20, 10, 10, 5, 0, 0, 0, 0, 0, 0, 0, 0,
    )
    private val knight = intArrayOf(
        -50, -40, -30, -30, -30, -30, -40, -50, -40, -20, 0, 0, 0, 0, -20, -40, -30, 0, 10, 15, 15, 10, 0, -30,
        -30, 5, 15, 20, 20, 15, 5, -30, -30, 0, 15, 20, 20, 15, 0, -30, -30, 5, 10, 15, 15, 10, 5, -30,
        -40, -20, 0, 5, 5, 0, -20, -40, -50, -40, -30, -30, -30, -30, -40, -50,
    )
    private val bishop = intArrayOf(
        -20, -10, -10, -10, -10, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 10, 10, 5, 0, -10,
        -10, 5, 5, 10, 10, 5, 5, -10, -10, 0, 10, 10, 10, 10, 0, -10, -10, 10, 10, 10, 10, 10, 10, -10,
        -10, 5, 0, 0, 0, 0, 5, -10, -20, -10, -10, -10, -10, -10, -10, -20,
    )
    private val rook = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 5, 10, 10, 10, 10, 10, 10, 5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, 0, 0, 0, 5, 5, 0, 0, 0,
    )
    private val queen = intArrayOf(
        -20, -10, -10, -5, -5, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 5, 5, 5, 0, -10,
        -5, 0, 5, 5, 5, 5, 0, -5, 0, 0, 5, 5, 5, 5, 0, -5, -10, 5, 5, 5, 5, 5, 0, -10,
        -10, 0, 5, 0, 0, 0, 0, -10, -20, -10, -10, -5, -5, -10, -10, -20,
    )
    private val kingMiddlegame = intArrayOf(
        -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30,
        -20, -30, -30, -40, -40, -30, -30, -20, -10, -20, -20, -20, -20, -20, -20, -10,
        20, 20, 0, 0, 0, 0, 20, 20, 20, 30, 10, 0, 0, 10, 30, 20,
    )
    private val kingEndgame = intArrayOf(
        -50, -40, -30, -20, -20, -30, -40, -50, -30, -20, -10, 0, 0, -10, -20, -30, -30, -10, 20, 30, 30, 20, -10, -30,
        -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 20, 30, 30, 20, -10, -30,
        -30, -30, 0, 0, 0, 0, -30, -30, -50, -30, -30, -30, -30, -30, -30, -50,
    )

    private const val BISHOP_PAIR = 30
    private const val TEMPO = 10
    private const val ENDGAME_MATERIAL = 2600

    fun evaluate(state: GameState, kind: EvaluationKind = EvaluationKind.PST): Int =
        if (kind == EvaluationKind.LEGACY) legacy(state) else tables(state)

    /** The original engine's evaluation, kept so the ablation can switch back to it. */
    private fun legacy(state: GameState): Int {
        var score = 0
        for (i in 0 until 64) {
            val piece = state.board[Square(i)] ?: continue
            val file = i and 7
            val rank = i shr 3
            val centre = if (piece.type == PieceType.KING) 0 else 6 - (kotlin.math.abs(2 * file - 7) + kotlin.math.abs(2 * rank - 7)) / 2
            val value = piece.type.value + centre * 4
            score += if (piece.side == Side.WHITE) value else -value
        }
        return if (state.sideToMove == Side.WHITE) score else -score
    }

    private fun tables(state: GameState): Int {
        var nonPawnMaterial = 0
        for (i in 0 until 64) {
            val piece = state.board[Square(i)] ?: continue
            if (piece.type != PieceType.PAWN && piece.type != PieceType.KING) nonPawnMaterial += piece.type.value
        }
        val endgame = nonPawnMaterial <= ENDGAME_MATERIAL

        var score = 0
        var whiteBishops = 0
        var blackBishops = 0
        for (i in 0 until 64) {
            val piece = state.board[Square(i)] ?: continue
            val file = i and 7
            val rank = i shr 3
            val white = piece.side == Side.WHITE
            val index = if (white) (7 - rank) * 8 + file else rank * 8 + file
            val table = when (piece.type) {
                PieceType.PAWN -> pawn
                PieceType.KNIGHT -> knight
                PieceType.BISHOP -> bishop
                PieceType.ROOK -> rook
                PieceType.QUEEN -> queen
                PieceType.KING -> if (endgame) kingEndgame else kingMiddlegame
            }
            val value = piece.type.value + table[index]
            if (white) score += value else score -= value
            if (piece.type == PieceType.BISHOP) if (white) whiteBishops++ else blackBishops++
        }
        if (whiteBishops >= 2) score += BISHOP_PAIR
        if (blackBishops >= 2) score -= BISHOP_PAIR
        return (if (state.sideToMove == Side.WHITE) score else -score) + TEMPO
    }
}
