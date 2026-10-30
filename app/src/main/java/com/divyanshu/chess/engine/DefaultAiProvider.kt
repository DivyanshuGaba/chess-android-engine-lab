package com.divyanshu.chess.engine

import android.util.Log
import com.divyanshu.chess.domain.ai.AiProvider
import com.divyanshu.chess.domain.ai.ChessAi
import com.divyanshu.chess.domain.ai.ResilientAi
import com.divyanshu.chess.domain.ai.minimaxFor
import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.EngineType
import javax.inject.Inject

class DefaultAiProvider @Inject constructor(private val session: SharedEngine) : AiProvider {

    override fun create(engine: EngineType, difficulty: Difficulty): ChessAi = when (engine) {
        EngineType.BUILT_IN -> minimaxFor(difficulty)
        EngineType.STOCKFISH -> ResilientAi(
            primary = StockfishAi(session, StockfishProfile.forDifficulty(difficulty)),
            fallback = minimaxFor(difficulty),
            onFallback = { Log.w(TAG, "Stockfish failed; the built-in engine takes over", it) },
        )
    }

    private companion object {
        const val TAG = "ChessEngine"
    }
}
