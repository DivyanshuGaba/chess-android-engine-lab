package com.divyanshu.chess.testutil

import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.model.GameState

fun perft(state: GameState, depth: Int): Long {
    if (depth == 0) return 1
    val moves = ChessRules.legalMoves(state)
    if (depth == 1) return moves.size.toLong()
    return moves.sumOf { perft(ChessRules.applyMove(state, it), depth - 1) }
}
