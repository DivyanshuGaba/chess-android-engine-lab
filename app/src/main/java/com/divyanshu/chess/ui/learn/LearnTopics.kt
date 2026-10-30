package com.divyanshu.chess.ui.learn

import androidx.compose.ui.graphics.Color
import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.engine.Fen
import com.divyanshu.chess.domain.model.Game
import com.divyanshu.chess.domain.model.GameStatus
import com.divyanshu.chess.domain.model.Move

enum class LearnGroup(val title: String) {
    SITUATIONS("During the game"),
    ENDINGS("How a game ends"),
    SPECIAL("Special moves"),
}

enum class MarkKind(val color: Color) {
    DANGER(Color(0xFFE5483B)),
    ATTACKER(Color(0xFFFFA726)),
    FROM(Color(0xFFF5E663)),
    TO(Color(0xFF66BB6A)),
    BLOCKED(Color(0xFF90A4AE)),
    TARGET(Color(0xFF4FC3F7)),
}

data class LearnMark(val square: String, val kind: MarkKind)

data class LearnTopic(
    val id: String,
    val title: String,
    val group: LearnGroup,
    val summary: String,
    val why: String,
    val fen: String? = null,
    val marks: List<LearnMark> = emptyList(),
    val legend: List<Pair<MarkKind, String>> = emptyList(),
    /** Checks, with the chess engine, that the example really shows what the text claims. */
    val proof: (() -> Boolean)? = null,
)

private fun statusOf(fen: String) = ChessRules.status(Fen.parse(fen))

private fun canPlay(fen: String, uci: String) = Move.fromUci(uci) in ChessRules.legalMoves(Fen.parse(fen))

private const val CHECK_FEN = "4k3/8/8/8/8/8/4R3/4K3 b - - 0 1"
private const val MATE_FEN = "R5k1/5ppp/8/8/8/8/8/4K3 b - - 0 1"
private const val STALEMATE_FEN = "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"
private const val MATERIAL_FEN = "4k3/8/8/8/8/8/8/3NK3 w - - 0 1"
private const val FIFTY_FEN = "4k3/8/8/8/8/8/R7/4K3 w - - 100 80"
private const val CASTLE_FEN = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"
private const val EN_PASSANT_FEN = "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"
private const val PROMOTION_FEN = "4k3/P7/8/8/8/8/8/4K3 w - - 0 1"
private val SHUFFLE = listOf("g1f3", "g8f6", "f3g1", "f6g8")

object LearnTopics {

    val all: List<LearnTopic> = listOf(
        LearnTopic(
            id = "check",
            title = "Check",
            group = LearnGroup.SITUATIONS,
            summary = "A king is in check when an enemy piece attacks it, so it could be captured on the next move. " +
                "The game goes on, but the player in check must deal with it straight away.",
            why = "White's rook on e2 looks straight up the e-file at the black king on e8. Black must get out of check " +
                "by moving the king to a safe square, blocking the line, or capturing the rook. Black only has a king " +
                "here, so it has to step aside. Any other move would be illegal.",
            fen = CHECK_FEN,
            marks = listOf(LearnMark("e8", MarkKind.DANGER), LearnMark("e2", MarkKind.ATTACKER)),
            legend = listOf(MarkKind.DANGER to "King in check", MarkKind.ATTACKER to "Piece giving check"),
            proof = { statusOf(CHECK_FEN) == GameStatus.CHECK },
        ),
        LearnTopic(
            id = "checkmate",
            title = "Checkmate",
            group = LearnGroup.ENDINGS,
            summary = "Checkmate means the king is in check and there is no legal move that gets it out of check. " +
                "The game ends at once and the player who delivered the checkmate wins.",
            why = "Black's king on g8 is attacked by the rook on a8 along the back rank. It can't step forward because " +
                "its own pawns on f7, g7 and h7 are in the way. It can't step sideways because the rook covers the whole " +
                "rank. Nothing can block the rook or capture it. This pattern is called a back-rank mate.",
            fen = MATE_FEN,
            marks = listOf(
                LearnMark("g8", MarkKind.DANGER),
                LearnMark("a8", MarkKind.ATTACKER),
                LearnMark("f7", MarkKind.BLOCKED),
                LearnMark("g7", MarkKind.BLOCKED),
                LearnMark("h7", MarkKind.BLOCKED),
            ),
            legend = listOf(
                MarkKind.DANGER to "King with nowhere to go",
                MarkKind.ATTACKER to "Rook giving check",
                MarkKind.BLOCKED to "Its own pawns trap it",
            ),
            proof = { statusOf(MATE_FEN) == GameStatus.CHECKMATE },
        ),
        LearnTopic(
            id = "stalemate",
            title = "Stalemate",
            group = LearnGroup.ENDINGS,
            summary = "Stalemate happens when the player to move has no legal move but their king is not in check. " +
                "The game is a draw, not a win for either side.",
            why = "Black's king on h8 is not attacked, but every square it could move to (g8, g7 and h7) is covered by " +
                "White's queen on f7, and g7 by the king on g6 as well. Black has no other pieces, so there is no legal " +
                "move. It is a classic mistake when you are far ahead: trap the king without leaving it a square " +
                "and you throw the win away.",
            fen = STALEMATE_FEN,
            marks = listOf(
                LearnMark("h8", MarkKind.FROM),
                LearnMark("f7", MarkKind.ATTACKER),
                LearnMark("g8", MarkKind.BLOCKED),
                LearnMark("g7", MarkKind.BLOCKED),
                LearnMark("h7", MarkKind.BLOCKED),
            ),
            legend = listOf(
                MarkKind.FROM to "Black king, not in check",
                MarkKind.ATTACKER to "Queen covering the squares",
                MarkKind.BLOCKED to "Squares it can't use",
            ),
            proof = { statusOf(STALEMATE_FEN) == GameStatus.STALEMATE },
        ),
        LearnTopic(
            id = "insufficient-material",
            title = "Not enough pieces",
            group = LearnGroup.ENDINGS,
            summary = "A game is drawn when neither player has enough pieces to force a checkmate. Examples are king " +
                "against king, king and one knight against king, and king and one bishop against king.",
            why = "With only a knight and a king, White can never force checkmate against a lone king, however well " +
                "it plays. Instead of playing on pointlessly, the game is declared a draw.",
            fen = MATERIAL_FEN,
            marks = listOf(LearnMark("d1", MarkKind.ATTACKER)),
            legend = listOf(MarkKind.ATTACKER to "White's only piece besides the king"),
            proof = { statusOf(MATERIAL_FEN) == GameStatus.DRAW_INSUFFICIENT_MATERIAL },
        ),
        LearnTopic(
            id = "fifty-move",
            title = "Fifty-move rule",
            group = LearnGroup.ENDINGS,
            summary = "If fifty moves by each player (one hundred half-moves) pass with no pawn move and no capture, " +
                "the game is a draw. Real chess makes a player claim it. This app declares it for you.",
            why = "Here White has a rook and king against a bare king, which is winning, but neither side has captured " +
                "or moved a pawn for a hundred half-moves. Since no progress is being made, the game ends in a draw. " +
                "The counter resets to zero every time a pawn moves or a piece is captured.",
            fen = FIFTY_FEN,
            marks = listOf(LearnMark("a2", MarkKind.ATTACKER)),
            legend = listOf(MarkKind.ATTACKER to "A winning piece that never found the mate"),
            proof = { statusOf(FIFTY_FEN) == GameStatus.DRAW_FIFTY_MOVE },
        ),
        LearnTopic(
            id = "repetition",
            title = "Repetition",
            group = LearnGroup.ENDINGS,
            summary = "If the same position appears three times, with the same player to move and the same castling " +
                "and en passant rights, the game is a draw.",
            why = "Both players shuffled a knight out and back: Nf3, Nf6, Ng1, Ng8. After doing that twice, the starting " +
                "position has appeared three times, so the game is drawn. A player who is losing can use repetition " +
                "to save the game, for example with a perpetual check.",
            fen = Fen.STARTING_POSITION,
            marks = listOf(
                LearnMark("g1", MarkKind.FROM),
                LearnMark("f3", MarkKind.TO),
                LearnMark("g8", MarkKind.FROM),
                LearnMark("f6", MarkKind.TO),
            ),
            legend = listOf(MarkKind.FROM to "Knights leave", MarkKind.TO to "And come back"),
            proof = { Game.fromMoves((SHUFFLE + SHUFFLE).map(Move::fromUci)).status == GameStatus.DRAW_REPETITION },
        ),
        LearnTopic(
            id = "timeout",
            title = "Running out of time",
            group = LearnGroup.ENDINGS,
            summary = "Each player has a clock, ten minutes each in this app. Your clock runs only while it is your turn. " +
                "If it reaches 0:00 you lose the game, even if you were winning on the board.",
            why = "The clock starts after White's first move and pauses whenever the app is in the background, so a phone " +
                "call never costs you time. The player whose turn it is has the glowing clock. When it turns red, " +
                "less than thirty seconds remain.",
        ),
        LearnTopic(
            id = "resignation",
            title = "Resigning",
            group = LearnGroup.ENDINGS,
            summary = "A player can give up at any time by resigning. The opponent wins immediately.",
            why = "Players resign when the position is clearly lost and there is no point playing on. In this app, tap " +
                "Resign and confirm. Against the computer, resigning counts as a win for the computer.",
        ),
        LearnTopic(
            id = "castling",
            title = "Castling",
            group = LearnGroup.SPECIAL,
            summary = "Castling moves your king two squares towards a rook and puts that rook on the other side of the " +
                "king. It is the only move where two pieces move at once, and it tucks the king away while bringing " +
                "the rook into play.",
            why = "You can castle only if the king and that rook have never moved, every square between them is empty, " +
                "and the king is not in check and does not pass through or land on an attacked square. Here White can " +
                "castle short (king to g1, rook to f1) or long (king to c1, rook to d1). To castle, move the king two " +
                "squares.",
            fen = CASTLE_FEN,
            marks = listOf(
                LearnMark("e1", MarkKind.FROM),
                LearnMark("g1", MarkKind.TO),
                LearnMark("h1", MarkKind.TARGET),
                LearnMark("f1", MarkKind.TARGET),
            ),
            legend = listOf(
                MarkKind.FROM to "King starts",
                MarkKind.TO to "King lands",
                MarkKind.TARGET to "Rook jumps from h1 to f1",
            ),
            proof = { canPlay(CASTLE_FEN, "e1g1") && canPlay(CASTLE_FEN, "e1c1") },
        ),
        LearnTopic(
            id = "en-passant",
            title = "En passant",
            group = LearnGroup.SPECIAL,
            summary = "En passant is French for \"in passing\". When a pawn jumps two squares from its start and lands " +
                "right beside an enemy pawn, that enemy pawn may capture it as if it had moved only one square.",
            why = "Black has just played d7 to d5, landing next to White's pawn on e5. Had Black moved one square to d6, " +
                "the e5 pawn could have taken it, and the two-square jump doesn't let it dodge that. So White may " +
                "capture on d6, and the pawn on d5 is removed. It must be done on the very next move or the chance " +
                "is gone.",
            fen = EN_PASSANT_FEN,
            marks = listOf(
                LearnMark("e5", MarkKind.FROM),
                LearnMark("d6", MarkKind.TO),
                LearnMark("d5", MarkKind.DANGER),
            ),
            legend = listOf(
                MarkKind.FROM to "Capturing pawn",
                MarkKind.TO to "It lands here",
                MarkKind.DANGER to "Captured pawn is removed",
            ),
            proof = { canPlay(EN_PASSANT_FEN, "e5d6") },
        ),
        LearnTopic(
            id = "promotion",
            title = "Promotion",
            group = LearnGroup.SPECIAL,
            summary = "When a pawn reaches the far end of the board it must be promoted: it becomes a queen, rook, " +
                "bishop or knight of its own colour, whichever you choose.",
            why = "Pawns can only move forward, so reaching the last rank is a big achievement. Most players choose a " +
                "queen, the strongest piece, but a knight is sometimes better because of the way it moves. The app " +
                "asks which piece you want when the pawn arrives.",
            fen = PROMOTION_FEN,
            marks = listOf(LearnMark("a7", MarkKind.FROM), LearnMark("a8", MarkKind.TO)),
            legend = listOf(MarkKind.FROM to "Pawn one step away", MarkKind.TO to "Promotion square"),
            proof = {
                listOf("a7a8q", "a7a8r", "a7a8b", "a7a8n").all { canPlay(PROMOTION_FEN, it) }
            },
        ),
    )

    fun byId(id: String): LearnTopic? = all.firstOrNull { it.id == id }
}
