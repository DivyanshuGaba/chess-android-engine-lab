package com.divyanshu.chess.domain.ai

import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import kotlinx.coroutines.CancellationException

/** Plays with [primary], and falls back to [fallback] if the primary engine fails or misbehaves. */
class ResilientAi(
    private val primary: ChessAi,
    private val fallback: ChessAi,
    private val onFallback: (Throwable) -> Unit = {},
) : ChessAi {

    override suspend fun chooseMove(state: GameState): Move? = search(state).move

    override suspend fun search(state: GameState): SearchResult = try {
        primary.search(state)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onFallback(e)
        val result = fallback.search(state)
        result.copy(info = result.info?.let { it.copy(engineName = "${it.engineName} (fallback)") })
    }
}
