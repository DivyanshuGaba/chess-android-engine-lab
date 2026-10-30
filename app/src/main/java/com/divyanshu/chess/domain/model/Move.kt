package com.divyanshu.chess.domain.model

data class Move(val from: Square, val to: Square, val promotion: PieceType? = null) {

    val uci: String get() = "$from$to${promotion?.fenChar ?: ""}"

    companion object {
        fun fromUci(text: String): Move {
            require(text.length in 4..5) { "Invalid move '$text'" }
            return Move(
                from = Square.parse(text.substring(0, 2)),
                to = Square.parse(text.substring(2, 4)),
                promotion = text.getOrNull(4)?.let(PieceType::fromFenChar),
            )
        }
    }
}
