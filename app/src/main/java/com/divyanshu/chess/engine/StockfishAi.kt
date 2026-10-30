package com.divyanshu.chess.engine

import com.divyanshu.chess.domain.ai.ChessAi
import com.divyanshu.chess.domain.ai.EngineInfo
import com.divyanshu.chess.domain.ai.SearchResult
import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.engine.Fen
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.Side

/** Runs one engine search at a time and hands back its [UciEngine]; implemented by [SharedEngine]. */
interface EngineSession {
    suspend fun <T> withEngine(options: Map<String, String>, block: suspend (UciEngine) -> T): T
}

/** A [ChessAi] backed by a UCI engine. Every move it returns is checked against the app's own rules. */
class StockfishAi(
    private val session: EngineSession,
    private val profile: StockfishProfile,
) : ChessAi {

    override suspend fun chooseMove(state: GameState): Move? = search(state).move

    override suspend fun search(state: GameState): SearchResult {
        val legal = ChessRules.legalMoves(state)
        if (legal.isEmpty()) return SearchResult(null, null)

        val fen = Fen.format(state)
        val (name, outcome) = session.withEngine(profile.options()) { engine ->
            if (state.fullmoveNumber == 1 && fen.startsWith(START_PLACEMENT)) engine.newGame()
            (engine.id?.name ?: "UCI engine") to engine.search(fen, profile.limits)
        }

        val move = outcome.bestMove?.let { runCatching { Move.fromUci(it) }.getOrNull() }
        if (move == null || move !in legal) throw IllegalEngineMoveException(outcome.bestMove ?: "(none)", fen)
        return SearchResult(move, outcome.info?.let { toEngineInfo(name, state.sideToMove, it, outcome.elapsedMs) })
    }

    private companion object {
        val START_PLACEMENT = Fen.STARTING_POSITION.substringBefore(' ')
    }
}

fun toEngineInfo(name: String, sideToMove: Side, info: UciInfo, elapsedMs: Long): EngineInfo {
    val sign = if (sideToMove == Side.WHITE) 1 else -1
    return EngineInfo(
        engineName = name,
        depth = info.depth ?: 0,
        scoreCp = info.scoreCp?.let { it * sign },
        mateIn = info.scoreMate?.let { it * sign },
        nodes = info.nodes ?: 0,
        nps = info.nps ?: 0,
        timeMs = info.timeMs ?: elapsedMs,
        pv = info.pv.mapNotNull { runCatching { Move.fromUci(it) }.getOrNull() },
    )
}
