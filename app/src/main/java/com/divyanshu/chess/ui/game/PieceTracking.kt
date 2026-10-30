package com.divyanshu.chess.ui.game

import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.Piece
import com.divyanshu.chess.domain.model.PieceType
import com.divyanshu.chess.domain.model.Square
import kotlin.math.abs

/** A piece with a stable id, so the UI can animate the same piece from square to square. */
data class TrackedPiece(val id: Int, val piece: Piece, val square: Square)

data class TrackedBoard(
    val pieces: List<TrackedPiece>,
    val lastMovedId: Int?,
    val lastCaptured: TrackedPiece?,
)

fun trackPieces(moves: List<Move>): TrackedBoard {
    var state = GameState.initial()
    val ids = HashMap<Square, Int>()
    state.board.pieces.forEach { (square, _) -> ids[square] = square.index }
    var movedId: Int? = null
    var captured: TrackedPiece? = null

    for (move in moves) {
        val piece = checkNotNull(state.board[move.from])
        val movingId = checkNotNull(ids.remove(move.from))
        val enPassant = piece.type == PieceType.PAWN && move.to == state.enPassant && state.board[move.to] == null
        val victimSquare = if (enPassant) Square.of(move.to.file, move.from.rank) else move.to

        captured = ids.remove(victimSquare)?.let { id ->
            TrackedPiece(id, checkNotNull(state.board[victimSquare]), victimSquare)
        }
        ids[move.to] = movingId

        if (piece.type == PieceType.KING && abs(move.to.file - move.from.file) == 2) {
            val kingSide = move.to.file == 6
            val rookFrom = Square.of(if (kingSide) 7 else 0, move.from.rank)
            val rookTo = Square.of(if (kingSide) 5 else 3, move.from.rank)
            ids.remove(rookFrom)?.let { ids[rookTo] = it }
        }

        movedId = movingId
        state = ChessRules.applyMove(state, move)
    }

    val pieces = state.board.pieces.map { (square, piece) -> TrackedPiece(ids.getValue(square), piece, square) }
    return TrackedBoard(pieces, movedId, captured)
}
