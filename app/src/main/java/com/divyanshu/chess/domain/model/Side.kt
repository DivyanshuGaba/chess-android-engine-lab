package com.divyanshu.chess.domain.model

enum class Side {
    WHITE,
    BLACK;

    val opposite: Side get() = if (this == WHITE) BLACK else WHITE

    val pawnDirection: Int get() = if (this == WHITE) 1 else -1
}
