package com.divyanshu.chess.domain.model

data class Piece(val type: PieceType, val side: Side) {

    val fenChar: Char
        get() = if (side == Side.WHITE) type.fenChar.uppercaseChar() else type.fenChar

    companion object {
        fun fromFenChar(char: Char): Piece =
            Piece(PieceType.fromFenChar(char), if (char.isUpperCase()) Side.WHITE else Side.BLACK)
    }
}
