package com.divyanshu.chess.engine

import com.divyanshu.chess.domain.ai.EngineInfo
import com.divyanshu.chess.domain.engine.Fen
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import javax.inject.Inject
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

sealed interface AnalysisUpdate {
    /** An intermediate result while the search is still deepening. */
    data class Progress(val info: EngineInfo) : AnalysisUpdate

    data class Done(val info: EngineInfo?, val bestMove: Move?) : AnalysisUpdate
}

fun interface PositionAnalyzer {
    /** Cancelling the collector stops the engine's search. */
    fun analyze(state: GameState, limits: SearchLimits): Flow<AnalysisUpdate>
}

class StockfishAnalyzer @Inject constructor(private val session: SharedEngine) : PositionAnalyzer {

    override fun analyze(state: GameState, limits: SearchLimits): Flow<AnalysisUpdate> = channelFlow {
        val fen = Fen.format(state)
        session.withEngine(StockfishProfile.analysis(limits).options()) { engine ->
            val name = engine.id?.name ?: "Stockfish"
            val outcome = engine.search(fen, limits) { info ->
                if (info.pv.isNotEmpty() && info.depth != null && (info.multiPv ?: 1) == 1 && info.bound == null) {
                    trySendBlocking(AnalysisUpdate.Progress(toEngineInfo(name, state.sideToMove, info, 0)))
                }
            }
            val final = outcome.info?.let { toEngineInfo(name, state.sideToMove, it, outcome.elapsedMs) }
            send(AnalysisUpdate.Done(final, outcome.bestMove?.let { runCatching { Move.fromUci(it) }.getOrNull() }))
        }
    }
}
