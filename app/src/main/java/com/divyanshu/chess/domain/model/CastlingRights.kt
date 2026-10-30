package com.divyanshu.chess.domain.model

data class CastlingRights(
    val whiteKingSide: Boolean = false,
    val whiteQueenSide: Boolean = false,
    val blackKingSide: Boolean = false,
    val blackQueenSide: Boolean = false,
) {

    fun canCastle(side: Side, kingSide: Boolean): Boolean = when {
        side == Side.WHITE && kingSide -> whiteKingSide
        side == Side.WHITE -> whiteQueenSide
        kingSide -> blackKingSide
        else -> blackQueenSide
    }

    companion object {
        val ALL = CastlingRights(true, true, true, true)
        val NONE = CastlingRights()
    }
}
