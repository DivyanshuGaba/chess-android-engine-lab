package com.divyanshu.chess.domain.model

import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.engine.Fen
import com.divyanshu.chess.domain.engine.San

/** Immutable game history; every [play] or [undo] returns a new instance. */
class Game private constructor(
    private val states: List<GameState>,
    val moves: List<Move>,
    val sanMoves: List<String>,
) {

    val state: GameState get() = states.last()

    val legalMoves: List<Move> by lazy { ChessRules.legalMoves(state) }

    val lastMove: Move? get() = moves.lastOrNull()

    val status: GameStatus by lazy {
        val base = ChessRules.status(state)
        if (!base.isOver && repetitionCount() >= 3) GameStatus.DRAW_REPETITION else base
    }

    val result: GameResult? by lazy {
        when (status) {
            GameStatus.CHECKMATE -> GameResult.winnerOf(state.sideToMove.opposite)
            GameStatus.ONGOING, GameStatus.CHECK -> null
            else -> GameResult.DRAW
        }
    }

    fun legalMovesFrom(square: Square): List<Move> = legalMoves.filter { it.from == square }

    fun play(move: Move): Game {
        require(move in legalMoves) { "Illegal move ${move.uci}" }
        return Game(
            states = states + ChessRules.applyMove(state, move),
            moves = moves + move,
            sanMoves = sanMoves + San.format(state, move),
        )
    }

    fun undo(count: Int = 1): Game {
        val keep = (moves.size - count).coerceAtLeast(0)
        return Game(states.take(keep + 1), moves.take(keep), sanMoves.take(keep))
    }

    /** Pieces of the opposing side that [side] has taken, ordered from most to least valuable. */
    fun capturedBy(side: Side): List<PieceType> {
        val remaining = state.board.pieces.map { it.second }.filter { it.side == side.opposite }
        return STARTING_COUNTS.flatMap { (type, count) ->
            val missing = (count - remaining.count { it.type == type }).coerceAtLeast(0)
            List(missing) { type }
        }.sortedByDescending { it.value }
    }

    private fun repetitionCount(): Int {
        val key = Fen.positionKey(state)
        return states.count { Fen.positionKey(it) == key }
    }

    companion object {
        private val STARTING_COUNTS = listOf(
            PieceType.PAWN to 8,
            PieceType.KNIGHT to 2,
            PieceType.BISHOP to 2,
            PieceType.ROOK to 2,
            PieceType.QUEEN to 1,
        )

        fun new(): Game = Game(listOf(GameState.initial()), emptyList(), emptyList())

        fun fromMoves(moves: List<Move>): Game = moves.fold(new()) { game, move -> game.play(move) }
    }
}
