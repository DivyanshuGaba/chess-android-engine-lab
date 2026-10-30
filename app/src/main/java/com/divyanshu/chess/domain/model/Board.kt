package com.divyanshu.chess.domain.model

/** Immutable 8x8 board; changes go through [edit], which returns a new board. */
class Board private constructor(private val cells: Array<Piece?>) {

    operator fun get(square: Square): Piece? = cells[square.index]

    val pieces: List<Pair<Square, Piece>>
        get() = cells.indices.mapNotNull { i -> cells[i]?.let { Square(i) to it } }

    fun edit(block: Editor.() -> Unit): Board {
        val copy = cells.copyOf()
        Editor(copy).block()
        return Board(copy)
    }

    fun kingSquare(side: Side): Square? {
        for (i in cells.indices) {
            val piece = cells[i]
            if (piece != null && piece.type == PieceType.KING && piece.side == side) return Square(i)
        }
        return null
    }

    class Editor internal constructor(private val cells: Array<Piece?>) {
        operator fun get(square: Square): Piece? = cells[square.index]

        operator fun set(square: Square, piece: Piece?) {
            cells[square.index] = piece
        }
    }

    override fun equals(other: Any?): Boolean = other is Board && cells.contentEquals(other.cells)

    override fun hashCode(): Int = cells.contentHashCode()

    companion object {
        val EMPTY = Board(arrayOfNulls(64))
    }
}
