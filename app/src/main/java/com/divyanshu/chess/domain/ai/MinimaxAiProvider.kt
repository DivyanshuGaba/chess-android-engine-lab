package com.divyanshu.chess.domain.ai

import com.divyanshu.chess.domain.model.Difficulty

/**
 * The built-in engine's strength presets. Hard uses the improved search at 0.3 s per move; it beat the
 * original depth-4 engine in every measured match (see engine_lab experiments exp010 to exp014). Easy and
 * Medium keep the original loose-playing minimax so those levels stay beatable.
 */
fun minimaxFor(difficulty: Difficulty): ChessAi = when (difficulty) {
    Difficulty.EASY -> MinimaxAi(depth = 1, noise = 120)
    Difficulty.MEDIUM -> MinimaxAi(depth = 3, noise = 10)
    Difficulty.HARD -> AdvancedAi(Searcher.Limits(movetimeMs = HARD_MOVE_TIME_MS))
}

const val HARD_MOVE_TIME_MS = 300L
