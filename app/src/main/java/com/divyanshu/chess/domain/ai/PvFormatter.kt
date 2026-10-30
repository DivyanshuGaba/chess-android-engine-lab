package com.divyanshu.chess.domain.ai

import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.engine.San
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import kotlin.math.abs

object PvFormatter {

    /** Turns an engine line into standard notation, stopping at the first move that is not legal. */
    fun toSan(state: GameState, pv: List<Move>, maxMoves: Int = 8): List<String> {
        var current = state
        val out = mutableListOf<String>()
        for (move in pv.take(maxMoves)) {
            if (move !in ChessRules.legalMoves(current)) break
            out += San.format(current, move)
            current = ChessRules.applyMove(current, move)
        }
        return out
    }

    /** "+0.35" for centipawns, "#3" when White mates in three, "#-2" when Black mates in two. */
    fun formatScore(scoreCp: Int?, mateIn: Int?): String = when {
        mateIn != null -> "#$mateIn"
        scoreCp != null -> {
            val pawns = abs(scoreCp) / 100.0
            val sign = if (scoreCp > 0) "+" else if (scoreCp < 0) "-" else ""
            "$sign${"%.2f".format(pawns)}"
        }
        else -> "?"
    }
}
