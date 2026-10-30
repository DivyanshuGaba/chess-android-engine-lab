package com.divyanshu.chess.domain.engine

import com.divyanshu.chess.domain.model.Board
import com.divyanshu.chess.domain.model.CastlingRights
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Piece
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.domain.model.Square

object Fen {
    const val STARTING_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    fun parse(fen: String): GameState {
        val fields = fen.trim().split(Regex("\\s+"))
        require(fields.size >= 4) { "FEN needs at least 4 fields: '$fen'" }

        val rows = fields[0].split("/")
        require(rows.size == 8) { "FEN needs 8 ranks: '$fen'" }

        val board = Board.EMPTY.edit {
            rows.forEachIndexed { rowIndex, row ->
                val rank = 7 - rowIndex
                var file = 0
                for (char in row) {
                    if (char.isDigit()) {
                        file += char.digitToInt()
                    } else {
                        require(file < 8) { "Rank overflows: '$row'" }
                        this[Square.of(file, rank)] = Piece.fromFenChar(char)
                        file++
                    }
                }
                require(file == 8) { "Rank must have 8 files: '$row'" }
            }
        }

        val side = when (fields[1]) {
            "w" -> Side.WHITE
            "b" -> Side.BLACK
            else -> throw IllegalArgumentException("Bad side to move '${fields[1]}'")
        }

        val rights = fields[2]
        val castling = CastlingRights(
            whiteKingSide = 'K' in rights,
            whiteQueenSide = 'Q' in rights,
            blackKingSide = 'k' in rights,
            blackQueenSide = 'q' in rights,
        )

        return GameState(
            board = board,
            sideToMove = side,
            castlingRights = castling,
            enPassant = fields[3].takeIf { it != "-" }?.let(Square::parse),
            halfmoveClock = fields.getOrNull(4)?.toIntOrNull() ?: 0,
            fullmoveNumber = fields.getOrNull(5)?.toIntOrNull() ?: 1,
        )
    }

    fun format(state: GameState): String = "${positionKey(state)} ${state.halfmoveClock} ${state.fullmoveNumber}"

    /** The first four FEN fields: everything that decides whether two positions repeat. */
    fun positionKey(state: GameState): String {
        val placement = (7 downTo 0).joinToString("/") { rank ->
            buildString {
                var empty = 0
                for (file in 0..7) {
                    val piece = state.board[Square.of(file, rank)]
                    if (piece == null) {
                        empty++
                    } else {
                        if (empty > 0) append(empty)
                        empty = 0
                        append(piece.fenChar)
                    }
                }
                if (empty > 0) append(empty)
            }
        }
        val side = if (state.sideToMove == Side.WHITE) "w" else "b"
        val rights = with(state.castlingRights) {
            buildString {
                if (whiteKingSide) append('K')
                if (whiteQueenSide) append('Q')
                if (blackKingSide) append('k')
                if (blackQueenSide) append('q')
            }.ifEmpty { "-" }
        }
        return "$placement $side $rights ${state.enPassant ?: "-"}"
    }
}
