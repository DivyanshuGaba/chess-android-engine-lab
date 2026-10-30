package com.divyanshu.chess.domain.ai

import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.Side
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

/** The improved built-in engine: [Searcher] limited by depth, nodes or time per move. */
class AdvancedAi(
    private val limits: Searcher.Limits,
    private val tableBits: Int = 18,
    private val features: Features = Features(),
) : ChessAi {

    override suspend fun chooseMove(state: GameState): Move? = search(state).move

    override suspend fun search(state: GameState): SearchResult {
        val job = coroutineContext[Job]
        return withContext(Dispatchers.Default) {
            val result = Searcher(tableBits, features).search(state, limits = limits, shouldStop = { job?.isActive == false })
            val last = result.last ?: return@withContext SearchResult(result.best, null)
            val sign = if (state.sideToMove == Side.WHITE) 1 else -1
            SearchResult(
                move = result.best,
                info = EngineInfo(
                    engineName = "Built-in advanced",
                    depth = last.depth,
                    scoreCp = last.scoreCp?.times(sign),
                    mateIn = last.mateIn?.times(sign),
                    nodes = last.nodes,
                    nps = if (last.timeMs > 0) last.nodes * 1_000 / last.timeMs else last.nodes,
                    timeMs = last.timeMs,
                    pv = last.pv,
                ),
            )
        }
    }
}
