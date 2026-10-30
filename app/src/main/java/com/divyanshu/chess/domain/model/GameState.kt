package com.divyanshu.chess.domain.model

import com.divyanshu.chess.domain.engine.Fen

data class GameState(
    val board: Board,
    val sideToMove: Side = Side.WHITE,
    val castlingRights: CastlingRights = CastlingRights.NONE,
    val enPassant: Square? = null,
    val halfmoveClock: Int = 0,
    val fullmoveNumber: Int = 1,
) {
    companion object {
        fun initial(): GameState = Fen.parse(Fen.STARTING_POSITION)
    }
}
