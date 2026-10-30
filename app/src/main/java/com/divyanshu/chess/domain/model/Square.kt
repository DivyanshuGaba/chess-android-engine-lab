package com.divyanshu.chess.domain.model

/** Board square encoded as rank * 8 + file, so a1 = 0 and h8 = 63. */
@JvmInline
value class Square(val index: Int) {

    val file: Int get() = index and 7
    val rank: Int get() = index shr 3

    override fun toString(): String = "${'a' + file}${rank + 1}"

    companion object {
        fun of(file: Int, rank: Int): Square = Square(rank * 8 + file)

        fun isOnBoard(file: Int, rank: Int): Boolean = file in 0..7 && rank in 0..7

        fun parse(text: String): Square {
            require(text.length == 2) { "Invalid square '$text'" }
            val file = text[0] - 'a'
            val rank = text[1] - '1'
            require(isOnBoard(file, rank)) { "Invalid square '$text'" }
            return of(file, rank)
        }
    }
}
