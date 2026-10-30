package com.divyanshu.chess.domain.engine

import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.PieceType

object San {

    fun format(state: GameState, move: Move): String {
        val piece = checkNotNull(state.board[move.from]) { "No piece on ${move.from}" }
        val next = ChessRules.applyMove(state, move)
        val suffix = when {
            !ChessRules.isInCheck(next) -> ""
            ChessRules.legalMoves(next).isEmpty() -> "#"
            else -> "+"
        }

        if (piece.type == PieceType.KING && kotlin.math.abs(move.to.file - move.from.file) == 2) {
            return (if (move.to.file == 6) "O-O" else "O-O-O") + suffix
        }

        val isCapture = state.board[move.to] != null ||
            (piece.type == PieceType.PAWN && move.to == state.enPassant)
        val text = StringBuilder()
        if (piece.type == PieceType.PAWN) {
            if (isCapture) text.append('a' + move.from.file)
        } else {
            text.append(piece.type.fenChar.uppercaseChar())
            text.append(disambiguation(state, move, piece.type))
        }
        if (isCapture) text.append('x')
        text.append(move.to)
        move.promotion?.let { text.append('=').append(it.fenChar.uppercaseChar()) }
        return text.append(suffix).toString()
    }

    private fun disambiguation(state: GameState, move: Move, type: PieceType): String {
        val rivals = ChessRules.legalMoves(state).filter {
            it.to == move.to && it.from != move.from && state.board[it.from]?.type == type
        }
        if (rivals.isEmpty()) return ""
        val sharesFile = rivals.any { it.from.file == move.from.file }
        val sharesRank = rivals.any { it.from.rank == move.from.rank }
        return when {
            !sharesFile -> ('a' + move.from.file).toString()
            !sharesRank -> (move.from.rank + 1).toString()
            else -> move.from.toString()
        }
    }
}
