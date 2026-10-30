package com.divyanshu.chess.domain.model

enum class PieceType(val fenChar: Char, val value: Int) {
    PAWN('p', 100),
    KNIGHT('n', 320),
    BISHOP('b', 330),
    ROOK('r', 500),
    QUEEN('q', 900),
    KING('k', 0);

    companion object {
        val promotions = listOf(QUEEN, ROOK, BISHOP, KNIGHT)

        fun fromFenChar(char: Char): PieceType =
            entries.firstOrNull { it.fenChar == char.lowercaseChar() }
                ?: throw IllegalArgumentException("Unknown piece '$char'")
    }
}
