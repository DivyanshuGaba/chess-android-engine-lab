package com.divyanshu.chess.domain.model

enum class GameStatus(val isOver: Boolean) {
    ONGOING(false),
    CHECK(false),
    CHECKMATE(true),
    STALEMATE(true),
    DRAW_FIFTY_MOVE(true),
    DRAW_INSUFFICIENT_MATERIAL(true),
    DRAW_REPETITION(true),
}
