package com.divyanshu.chess.engine

import com.divyanshu.chess.domain.model.Difficulty

/** How hard Stockfish plays: an optional UCI_Elo cap plus the search budget per move. */
data class StockfishProfile(val limitElo: Int?, val limits: SearchLimits) {

    /** Every strength option is always listed so one game's cap never leaks into the next search. */
    fun options(): Map<String, String> = linkedMapOf(
        "UCI_LimitStrength" to (limitElo != null).toString(),
        "UCI_Elo" to (limitElo ?: MAX_ELO).toString(),
        "MultiPV" to "1",
        "Threads" to "1",
        "Hash" to "16",
    )

    companion object {
        const val MAX_ELO = 3190

        fun forDifficulty(difficulty: Difficulty): StockfishProfile = when (difficulty) {
            Difficulty.EASY -> StockfishProfile(1350, SearchLimits(movetimeMs = 100))
            Difficulty.MEDIUM -> StockfishProfile(1800, SearchLimits(movetimeMs = 300))
            Difficulty.HARD -> StockfishProfile(null, SearchLimits(movetimeMs = 1_000))
        }

        /** Full strength with caller-chosen limits, used for position analysis. */
        fun analysis(limits: SearchLimits) = StockfishProfile(null, limits)
    }
}
