package com.divyanshu.chess.domain.ai

import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.engine.Fen
import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.Side
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedAiTest {

    private fun depth(n: Int) = AdvancedAi(Searcher.Limits(depth = n))

    @Test
    fun `plays a legal move from the start`() = runTest {
        val state = GameState.initial()
        assertTrue(depth(3).chooseMove(state) in ChessRules.legalMoves(state))
    }

    @Test
    fun `returns null when there is no legal move`() = runTest {
        val mated = Fen.parse("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3")
        assertNull(depth(3).chooseMove(mated))
    }

    @Test
    fun `finds mate in one and reports it as a mate score`() = runTest {
        val result = depth(4).search(Fen.parse("6k1/5ppp/8/8/8/8/8/R3K3 w - - 0 1"))
        assertEquals(Move.fromUci("a1a8"), result.move)
        assertEquals(1, result.info?.mateIn)
        assertNull(result.info?.scoreCp)
    }

    @Test
    fun `finds scholars mate`() = runTest {
        val state = Fen.parse("r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4")
        assertEquals(Move.fromUci("h5f7"), depth(3).chooseMove(state))
    }

    @Test
    fun `takes a free queen`() = runTest {
        val state = Fen.parse("4k3/8/8/3q4/8/8/8/3RK3 w - - 0 1")
        assertEquals(Move.fromUci("d1d5"), depth(2).chooseMove(state))
    }

    @Test
    fun `quiescence search avoids a poisoned pawn that the baseline grabs`() = runTest {
        // Qxd5 wins a pawn but exd5 wins the queen. A one-ply search without quiescence cannot see that.
        val state = Fen.parse("4k3/8/4p3/3p4/8/8/8/3QK3 w - - 0 1")
        val grab = Move.fromUci("d1d5")
        assertEquals("the baseline falls for it", grab, MinimaxAi(depth = 1).chooseMove(state))
        assertNotEquals("the advanced engine sees the recapture", grab, depth(1).chooseMove(state))
    }

    @Test
    fun `a node limit makes the search reproducible`() = runTest {
        val state = Fen.parse("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
        val limited = { AdvancedAi(Searcher.Limits(nodes = 20_000)) }
        val first = limited().search(state)
        val second = limited().search(state)
        assertEquals(first.move, second.move)
        assertEquals(first.info?.nodes, second.info?.nodes)
        assertTrue((first.info?.nodes ?: Long.MAX_VALUE) < 30_000)
    }

    @Test
    fun `a time limit is respected and still returns a move`() = runTest {
        val state = Fen.parse("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
        val started = System.nanoTime()
        val result = AdvancedAi(Searcher.Limits(movetimeMs = 100)).search(state)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertTrue("took $elapsedMs ms", elapsedMs < 600)
        assertTrue(result.move in ChessRules.legalMoves(state))
    }

    @Test
    fun `stop request returns the best move found so far`() {
        val state = Fen.parse("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
        var checks = 0
        val result = Searcher(16).search(state, limits = Searcher.Limits(depth = 30), shouldStop = { ++checks > 3 })
        assertNotNull(result.best)
        assertTrue(result.best in ChessRules.legalMoves(state))
    }

    @Test
    fun `search depth grows with the limit and the principal variation is legal`() {
        val state = GameState.initial()
        val progress = mutableListOf<Searcher.Progress>()
        Searcher(16).search(state, limits = Searcher.Limits(depth = 5), onProgress = { progress += it })
        assertEquals(listOf(1, 2, 3, 4, 5), progress.map { it.depth })
        var current = state
        for (move in progress.last().pv) {
            assertTrue(move in ChessRules.legalMoves(current))
            current = ChessRules.applyMove(current, move)
        }
    }

    @Test
    fun `a position that repeats scores as a draw instead of a win`() {
        // White is a rook up but the only way to keep the extra rook is not to repeat; with the earlier
        // position in the game history the search must still return a legal move without looping.
        val state = Fen.parse("4k3/8/8/8/8/8/R7/4K3 w - - 0 1")
        val earlier = listOf(Zobrist.hash(state))
        val result = Searcher(16).search(state, earlierPositions = earlier, limits = Searcher.Limits(depth = 5))
        assertTrue(result.best in ChessRules.legalMoves(state))
    }

    @Test
    fun `the engine plays itself to the end of a short game without an illegal move`() = runTest {
        var state = GameState.initial()
        repeat(40) {
            val move = AdvancedAi(Searcher.Limits(depth = 2)).chooseMove(state) ?: return@runTest
            assertTrue(move in ChessRules.legalMoves(state))
            state = ChessRules.applyMove(state, move)
        }
    }

    // ---- hashing and evaluation ----

    @Test
    fun `the same position reached by different move orders hashes equally`() {
        fun play(vararg uci: String) = uci.fold(GameState.initial()) { s, m -> ChessRules.applyMove(s, Move.fromUci(m)) }
        val a = play("g1f3", "g8f6", "b1c3", "b8c6")
        val b = play("b1c3", "b8c6", "g1f3", "g8f6")
        assertEquals(Zobrist.hash(a), Zobrist.hash(b))
    }

    @Test
    fun `side to move castling rights and en passant change the hash`() {
        val base = Fen.parse("r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 1")
        val noEp = Fen.parse("r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq - 0 1")
        val noCastle = Fen.parse("r3k2r/8/8/3pP3/8/8/8/R3K2R w - d6 0 1")
        val blackMoves = Fen.parse("r3k2r/8/8/3pP3/8/8/8/R3K2R b KQkq d6 0 1")
        val hashes = listOf(base, noEp, noCastle, blackMoves).map(Zobrist::hash)
        assertEquals(4, hashes.toSet().size)
    }

    @Test
    fun `evaluation is symmetric between colours`() {
        val fens = listOf(
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
        )
        for (fen in fens) {
            val state = Fen.parse(fen)
            assertEquals(fen, Evaluation.evaluate(state), Evaluation.evaluate(mirror(state)))
        }
    }

    @Test
    fun `the start position is balanced and material matters`() {
        assertTrue(Evaluation.evaluate(GameState.initial()) in -30..30)
        val up = Fen.parse("4k3/8/8/8/8/8/8/3QK3 w - - 0 1")
        assertTrue(Evaluation.evaluate(up) > 800)
        assertTrue(Evaluation.evaluate(up.copy(sideToMove = Side.BLACK)) < -700)
    }

    /** Flips the board vertically and swaps the colours, which must leave the evaluation unchanged. */
    private fun mirror(state: GameState): GameState {
        val rows = Fen.format(state).split(" ")
        val placement = rows[0].split("/").reversed().joinToString("/") { row ->
            row.map { if (it.isLetter()) (if (it.isUpperCase()) it.lowercaseChar() else it.uppercaseChar()) else it }.joinToString("")
        }
        val side = if (rows[1] == "w") "b" else "w"
        val rights = rows[2].map { if (it == '-') it else if (it.isUpperCase()) it.lowercaseChar() else it.uppercaseChar() }
            .joinToString("").let { if (it == "-") it else it }
        val ep = if (rows[3] == "-") "-" else "${rows[3][0]}${9 - (rows[3][1] - '0')}"
        return Fen.parse("$placement $side $rights $ep ${rows[4]} ${rows[5]}")
    }

    @Test
    fun `only the hard built-in level uses the improved engine`() {
        assertTrue(minimaxFor(Difficulty.HARD) is AdvancedAi)
        assertTrue(minimaxFor(Difficulty.MEDIUM) is MinimaxAi)
        assertTrue(minimaxFor(Difficulty.EASY) is MinimaxAi)
    }

    // ---- feature switches (used by the ablation benchmarks) ----

    @Test
    fun `turning quiescence off brings back the baselines horizon blunder`() = runTest {
        val state = Fen.parse("4k3/8/4p3/3p4/8/8/8/3QK3 w - - 0 1")
        val grab = Move.fromUci("d1d5")
        val off = AdvancedAi(Searcher.Limits(depth = 1), features = Features(quiescence = false))
        assertEquals(grab, off.chooseMove(state))
        assertNotEquals(grab, AdvancedAi(Searcher.Limits(depth = 1)).chooseMove(state))
    }

    @Test
    fun `every combination of switches still plays legal moves`() = runTest {
        val states = listOf(
            GameState.initial(),
            Fen.parse("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"),
            Fen.parse("4k3/P7/8/3pP3/8/8/8/4K3 w - d6 0 1"),
        )
        for (mask in 0 until 32) {
            for (evaluation in EvaluationKind.entries) {
                val features = Features(
                    quiescence = mask and 1 != 0,
                    transpositionTable = mask and 2 != 0,
                    reductions = mask and 4 != 0,
                    orderingHeuristics = mask and 8 != 0,
                    checkExtension = mask and 16 != 0,
                    evaluation = evaluation,
                )
                for (state in states) {
                    val move = AdvancedAi(Searcher.Limits(depth = 3), features = features).chooseMove(state)
                    assertTrue("$features", move in ChessRules.legalMoves(state))
                }
            }
        }
    }

    @Test
    fun `the transposition table never increases the work`() {
        val state = Fen.parse("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
        fun nodes(features: Features): Long {
            var last = 0L
            Searcher(18, features).search(state, limits = Searcher.Limits(depth = 5), onProgress = { last = it.nodes })
            return last
        }
        assertTrue(nodes(Features()) <= nodes(Features(transpositionTable = false)))
    }

    @Test
    fun `mate is still found with every optional technique off`() = runTest {
        val off = Features(false, false, false, false, false, EvaluationKind.LEGACY)
        val result = AdvancedAi(Searcher.Limits(depth = 4), features = off).search(Fen.parse("6k1/5ppp/8/8/8/8/8/R3K3 w - - 0 1"))
        assertEquals(Move.fromUci("a1a8"), result.move)
        assertEquals(1, result.info?.mateIn)
    }

    @Test
    fun `the legacy evaluation matches the original engines idea of material and centre`() {
        assertEquals(0, Evaluation.evaluate(GameState.initial(), EvaluationKind.LEGACY))
        val up = Fen.parse("4k3/8/8/8/8/8/8/3QK3 w - - 0 1")
        assertTrue(Evaluation.evaluate(up, EvaluationKind.LEGACY) > 800)
        assertEquals(Evaluation.evaluate(up, EvaluationKind.LEGACY), -Evaluation.evaluate(up.copy(sideToMove = Side.BLACK), EvaluationKind.LEGACY))
    }
}
