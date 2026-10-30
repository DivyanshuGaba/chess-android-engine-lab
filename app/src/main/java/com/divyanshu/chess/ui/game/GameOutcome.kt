package com.divyanshu.chess.ui.game

import com.divyanshu.chess.domain.model.GameResult
import com.divyanshu.chess.domain.model.GameStatus
import com.divyanshu.chess.domain.model.Side

data class Outcome(val title: String, val explanation: String, val topicId: String)

/** Describes, in plain words, why the game just ended; null while it is still going. */
fun outcomeFor(state: GameUiState): Outcome? {
    val result = state.result ?: return null
    val winner = when (result) {
        GameResult.WHITE_WINS -> "White"
        GameResult.BLACK_WINS -> "Black"
        GameResult.DRAW -> null
    }
    val loser = when (winner) {
        "White" -> "Black"
        "Black" -> "White"
        else -> null
    }

    return when {
        state.forcedEnd == ForcedEnd.TIMEOUT -> Outcome(
            title = "Time's up",
            explanation = "$loser ran out of time, so $winner wins. A lost clock ends the game even if the position on the board was better.",
            topicId = "timeout",
        )
        state.forcedEnd == ForcedEnd.RESIGNATION -> Outcome(
            title = "$loser resigned",
            explanation = "$loser gave up the game, so $winner wins.",
            topicId = "resignation",
        )
        state.status == GameStatus.CHECKMATE -> Outcome(
            title = "Checkmate!",
            explanation = buildString {
                append("$loser's king is attacked and has no legal way out: it can't step to a safe square, ")
                append("block the attack or capture the attacker. $winner wins.")
                state.moveHistory.lastOrNull()?.let { append(" Final move: $it.") }
            },
            topicId = "checkmate",
        )
        state.status == GameStatus.STALEMATE -> Outcome(
            title = "Stalemate",
            explanation = "${sideName(state.sideToMove)} has no legal move, but their king is not in check. " +
                "That is a stalemate, and the game is a draw.",
            topicId = "stalemate",
        )
        state.status == GameStatus.DRAW_FIFTY_MOVE -> Outcome(
            title = "Draw by the fifty-move rule",
            explanation = "Fifty moves in a row went by without a capture or a pawn move, so the game is a draw.",
            topicId = "fifty-move",
        )
        state.status == GameStatus.DRAW_REPETITION -> Outcome(
            title = "Draw by repetition",
            explanation = "The same position has now appeared three times, so the game is a draw.",
            topicId = "repetition",
        )
        state.status == GameStatus.DRAW_INSUFFICIENT_MATERIAL -> Outcome(
            title = "Draw: not enough pieces",
            explanation = "Neither player has enough pieces left to force a checkmate, so the game is a draw.",
            topicId = "insufficient-material",
        )
        else -> Outcome("Game over", "The game ended in a draw.", "stalemate")
    }
}

private fun sideName(side: Side) = if (side == Side.WHITE) "White" else "Black"
