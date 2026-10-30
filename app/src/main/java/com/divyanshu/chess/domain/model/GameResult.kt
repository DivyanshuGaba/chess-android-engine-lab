package com.divyanshu.chess.domain.model

enum class GameResult(val label: String) {
    WHITE_WINS("1-0"),
    BLACK_WINS("0-1"),
    DRAW("1/2-1/2");

    companion object {
        fun winnerOf(side: Side): GameResult = if (side == Side.WHITE) WHITE_WINS else BLACK_WINS
    }
}
