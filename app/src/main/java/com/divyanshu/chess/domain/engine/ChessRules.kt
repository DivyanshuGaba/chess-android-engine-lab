package com.divyanshu.chess.domain.engine

import com.divyanshu.chess.domain.model.Board
import com.divyanshu.chess.domain.model.CastlingRights
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.GameStatus
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.Piece
import com.divyanshu.chess.domain.model.PieceType
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.domain.model.Square

object ChessRules {

    private val KNIGHT_FILE = intArrayOf(1, 2, 2, 1, -1, -2, -2, -1)
    private val KNIGHT_RANK = intArrayOf(2, 1, -1, -2, -2, -1, 1, 2)
    private val KING_FILE = intArrayOf(1, 1, 1, 0, -1, -1, -1, 0)
    private val KING_RANK = intArrayOf(1, 0, -1, -1, -1, 0, 1, 1)
    private val DIAGONAL_FILE = intArrayOf(1, 1, -1, -1)
    private val DIAGONAL_RANK = intArrayOf(1, -1, 1, -1)
    private val ORTHOGONAL_FILE = intArrayOf(1, -1, 0, 0)
    private val ORTHOGONAL_RANK = intArrayOf(0, 0, 1, -1)

    fun legalMoves(state: GameState): List<Move> {
        val side = state.sideToMove
        return pseudoLegalMoves(state).filter { !isInCheck(applyMove(state, it), side) }
    }

    /** Legal captures (including en passant) and promotions only, for quiescence search. */
    fun legalTacticalMoves(state: GameState): List<Move> {
        val side = state.sideToMove
        return pseudoLegalMoves(state)
            .filter { move ->
                move.promotion != null || state.board[move.to] != null ||
                    (move.to == state.enPassant && state.board[move.from]?.type == PieceType.PAWN)
            }
            .filter { !isInCheck(applyMove(state, it), side) }
    }

    fun legalMovesFrom(state: GameState, from: Square): List<Move> =
        legalMoves(state).filter { it.from == from }

    fun isInCheck(state: GameState, side: Side = state.sideToMove): Boolean {
        val king = state.board.kingSquare(side) ?: return false
        return isAttacked(state.board, king, side.opposite)
    }

    fun status(state: GameState): GameStatus {
        val inCheck = isInCheck(state)
        val hasMove = legalMoves(state).isNotEmpty()
        return when {
            !hasMove && inCheck -> GameStatus.CHECKMATE
            !hasMove -> GameStatus.STALEMATE
            state.halfmoveClock >= 100 -> GameStatus.DRAW_FIFTY_MOVE
            hasInsufficientMaterial(state.board) -> GameStatus.DRAW_INSUFFICIENT_MATERIAL
            inCheck -> GameStatus.CHECK
            else -> GameStatus.ONGOING
        }
    }

    /** Applies [move] without validating it; callers pass moves taken from [legalMoves]. */
    fun applyMove(state: GameState, move: Move): GameState {
        val piece = checkNotNull(state.board[move.from]) { "No piece on ${move.from}" }
        val side = state.sideToMove
        val captured = state.board[move.to]
        val isEnPassant = piece.type == PieceType.PAWN && move.to == state.enPassant && captured == null
        val isCastle = piece.type == PieceType.KING && kotlin.math.abs(move.to.file - move.from.file) == 2
        val promotionRank = if (side == Side.WHITE) 7 else 0

        val board = state.board.edit {
            this[move.from] = null
            this[move.to] = if (piece.type == PieceType.PAWN && move.to.rank == promotionRank) {
                Piece(move.promotion ?: PieceType.QUEEN, side)
            } else {
                piece
            }
            if (isEnPassant) this[Square.of(move.to.file, move.from.rank)] = null
            if (isCastle) {
                val rank = move.from.rank
                val kingSide = move.to.file == 6
                val rookFrom = Square.of(if (kingSide) 7 else 0, rank)
                val rookTo = Square.of(if (kingSide) 5 else 3, rank)
                this[rookTo] = this[rookFrom]
                this[rookFrom] = null
            }
        }

        val isDoublePush = piece.type == PieceType.PAWN && kotlin.math.abs(move.to.rank - move.from.rank) == 2
        return GameState(
            board = board,
            sideToMove = side.opposite,
            castlingRights = updatedRights(state.castlingRights, move),
            enPassant = if (isDoublePush) Square.of(move.from.file, (move.from.rank + move.to.rank) / 2) else null,
            halfmoveClock = if (piece.type == PieceType.PAWN || captured != null) 0 else state.halfmoveClock + 1,
            fullmoveNumber = state.fullmoveNumber + if (side == Side.BLACK) 1 else 0,
        )
    }

    fun isAttacked(board: Board, target: Square, by: Side): Boolean {
        val file = target.file
        val rank = target.rank

        val pawnRank = rank - by.pawnDirection
        for (df in intArrayOf(-1, 1)) {
            if (Square.isOnBoard(file + df, pawnRank) && isPiece(board, file + df, pawnRank, PieceType.PAWN, by)) return true
        }
        for (i in 0..7) {
            if (isPiece(board, file + KNIGHT_FILE[i], rank + KNIGHT_RANK[i], PieceType.KNIGHT, by)) return true
            if (isPiece(board, file + KING_FILE[i], rank + KING_RANK[i], PieceType.KING, by)) return true
        }
        return attackedAlong(board, file, rank, DIAGONAL_FILE, DIAGONAL_RANK, PieceType.BISHOP, by) ||
            attackedAlong(board, file, rank, ORTHOGONAL_FILE, ORTHOGONAL_RANK, PieceType.ROOK, by)
    }

    private fun isPiece(board: Board, file: Int, rank: Int, type: PieceType, side: Side): Boolean {
        if (!Square.isOnBoard(file, rank)) return false
        val piece = board[Square.of(file, rank)]
        return piece != null && piece.type == type && piece.side == side
    }

    private fun attackedAlong(
        board: Board,
        file: Int,
        rank: Int,
        dFile: IntArray,
        dRank: IntArray,
        slider: PieceType,
        by: Side,
    ): Boolean {
        for (d in dFile.indices) {
            var f = file + dFile[d]
            var r = rank + dRank[d]
            while (Square.isOnBoard(f, r)) {
                val piece = board[Square.of(f, r)]
                if (piece != null) {
                    if (piece.side == by && (piece.type == slider || piece.type == PieceType.QUEEN)) return true
                    break
                }
                f += dFile[d]
                r += dRank[d]
            }
        }
        return false
    }

    private fun pseudoLegalMoves(state: GameState): List<Move> {
        val moves = ArrayList<Move>(48)
        val board = state.board
        val side = state.sideToMove
        for (i in 0 until 64) {
            val from = Square(i)
            val piece = board[from] ?: continue
            if (piece.side != side) continue
            when (piece.type) {
                PieceType.PAWN -> addPawnMoves(state, from, moves)
                PieceType.KNIGHT -> addStepMoves(board, from, side, KNIGHT_FILE, KNIGHT_RANK, moves)
                PieceType.BISHOP -> addSlideMoves(board, from, side, DIAGONAL_FILE, DIAGONAL_RANK, moves)
                PieceType.ROOK -> addSlideMoves(board, from, side, ORTHOGONAL_FILE, ORTHOGONAL_RANK, moves)
                PieceType.QUEEN -> {
                    addSlideMoves(board, from, side, DIAGONAL_FILE, DIAGONAL_RANK, moves)
                    addSlideMoves(board, from, side, ORTHOGONAL_FILE, ORTHOGONAL_RANK, moves)
                }
                PieceType.KING -> {
                    addStepMoves(board, from, side, KING_FILE, KING_RANK, moves)
                    addCastlingMoves(state, from, moves)
                }
            }
        }
        return moves
    }

    private fun addPawnMoves(state: GameState, from: Square, moves: MutableList<Move>) {
        val board = state.board
        val side = state.sideToMove
        val direction = side.pawnDirection
        val startRank = if (side == Side.WHITE) 1 else 6
        val promotionRank = if (side == Side.WHITE) 7 else 0
        val nextRank = from.rank + direction

        fun add(to: Square) {
            if (to.rank == promotionRank) {
                PieceType.promotions.forEach { moves.add(Move(from, to, it)) }
            } else {
                moves.add(Move(from, to))
            }
        }

        val oneStep = Square.of(from.file, nextRank)
        if (board[oneStep] == null) {
            add(oneStep)
            if (from.rank == startRank) {
                val twoSteps = Square.of(from.file, nextRank + direction)
                if (board[twoSteps] == null) moves.add(Move(from, twoSteps))
            }
        }
        for (df in intArrayOf(-1, 1)) {
            val file = from.file + df
            if (file !in 0..7) continue
            val target = Square.of(file, nextRank)
            val occupant = board[target]
            if ((occupant != null && occupant.side != side) || (occupant == null && target == state.enPassant)) add(target)
        }
    }

    private fun addStepMoves(
        board: Board,
        from: Square,
        side: Side,
        dFile: IntArray,
        dRank: IntArray,
        moves: MutableList<Move>,
    ) {
        for (d in dFile.indices) {
            val file = from.file + dFile[d]
            val rank = from.rank + dRank[d]
            if (!Square.isOnBoard(file, rank)) continue
            val target = Square.of(file, rank)
            val occupant = board[target]
            if (occupant == null || occupant.side != side) moves.add(Move(from, target))
        }
    }

    private fun addSlideMoves(
        board: Board,
        from: Square,
        side: Side,
        dFile: IntArray,
        dRank: IntArray,
        moves: MutableList<Move>,
    ) {
        for (d in dFile.indices) {
            var file = from.file + dFile[d]
            var rank = from.rank + dRank[d]
            while (Square.isOnBoard(file, rank)) {
                val target = Square.of(file, rank)
                val occupant = board[target]
                if (occupant == null) {
                    moves.add(Move(from, target))
                } else {
                    if (occupant.side != side) moves.add(Move(from, target))
                    break
                }
                file += dFile[d]
                rank += dRank[d]
            }
        }
    }

    private fun addCastlingMoves(state: GameState, from: Square, moves: MutableList<Move>) {
        val side = state.sideToMove
        val rank = if (side == Side.WHITE) 0 else 7
        if (from != Square.of(4, rank)) return
        val board = state.board
        val enemy = side.opposite

        fun rookAt(file: Int) = board[Square.of(file, rank)] == Piece(PieceType.ROOK, side)
        fun empty(vararg files: Int) = files.all { board[Square.of(it, rank)] == null }
        fun safe(vararg files: Int) = files.none { isAttacked(board, Square.of(it, rank), enemy) }

        if (state.castlingRights.canCastle(side, kingSide = true) && rookAt(7) &&
            empty(5, 6) && safe(4, 5, 6)
        ) {
            moves.add(Move(from, Square.of(6, rank)))
        }
        if (state.castlingRights.canCastle(side, kingSide = false) && rookAt(0) &&
            empty(1, 2, 3) && safe(4, 3, 2)
        ) {
            moves.add(Move(from, Square.of(2, rank)))
        }
    }

    private fun updatedRights(rights: CastlingRights, move: Move): CastlingRights {
        var result = rights
        for (square in listOf(move.from, move.to)) {
            result = when (square.index) {
                0 -> result.copy(whiteQueenSide = false)
                7 -> result.copy(whiteKingSide = false)
                4 -> result.copy(whiteKingSide = false, whiteQueenSide = false)
                56 -> result.copy(blackQueenSide = false)
                63 -> result.copy(blackKingSide = false)
                60 -> result.copy(blackKingSide = false, blackQueenSide = false)
                else -> result
            }
        }
        return result
    }

    private fun hasInsufficientMaterial(board: Board): Boolean {
        val others = board.pieces.map { it.second }.filter { it.type != PieceType.KING }
        return others.isEmpty() ||
            (others.size == 1 && others[0].type in setOf(PieceType.KNIGHT, PieceType.BISHOP))
    }
}
