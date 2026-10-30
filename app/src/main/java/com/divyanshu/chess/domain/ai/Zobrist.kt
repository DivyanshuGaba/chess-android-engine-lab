package com.divyanshu.chess.domain.ai

import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.domain.model.Square
import java.util.Random

/** 64-bit position hashing: equal positions (pieces, side, castling, en passant) hash equally. */
object Zobrist {
    private val random = Random(0x5EED_C0DEL)
    private val pieces = Array(12) { LongArray(64) { random.nextLong() } }
    private val blackToMove = random.nextLong()
    private val castling = LongArray(16) { random.nextLong() }
    private val enPassantFile = LongArray(8) { random.nextLong() }

    fun hash(state: GameState): Long {
        var key = 0L
        for (i in 0 until 64) {
            val piece = state.board[Square(i)] ?: continue
            key = key xor pieces[piece.side.ordinal * 6 + piece.type.ordinal][i]
        }
        if (state.sideToMove == Side.BLACK) key = key xor blackToMove
        val rights = state.castlingRights
        val mask = (if (rights.whiteKingSide) 1 else 0) or (if (rights.whiteQueenSide) 2 else 0) or
            (if (rights.blackKingSide) 4 else 0) or (if (rights.blackQueenSide) 8 else 0)
        key = key xor castling[mask]
        state.enPassant?.let { key = key xor enPassantFile[it.file] }
        return key
    }
}
