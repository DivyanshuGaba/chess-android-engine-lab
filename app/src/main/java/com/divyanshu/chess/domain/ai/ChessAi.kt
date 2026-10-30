package com.divyanshu.chess.domain.ai

import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.EngineType
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move

/** What an engine reports about a search. Scores are always from White's point of view. */
data class EngineInfo(
    val engineName: String,
    val depth: Int,
    val scoreCp: Int?,
    val mateIn: Int?,
    val nodes: Long,
    val nps: Long,
    val timeMs: Long,
    val pv: List<Move>,
)

data class SearchResult(val move: Move?, val info: EngineInfo?)

interface ChessAi {
    suspend fun chooseMove(state: GameState): Move?

    suspend fun search(state: GameState): SearchResult = SearchResult(chooseMove(state), null)
}

fun interface AiProvider {
    fun create(engine: EngineType, difficulty: Difficulty): ChessAi
}

/** Reports whether the bundled Stockfish binary can run on this device. */
interface EngineAvailability {
    val isStockfishAvailable: Boolean
}
