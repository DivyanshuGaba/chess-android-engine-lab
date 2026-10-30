package com.divyanshu.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UciParserTest {

    @Test
    fun `a full info line is parsed`() {
        val info = UciParser.parseInfo(
            "info depth 18 seldepth 24 multipv 1 score cp 34 nodes 123456 nps 1500000 hashfull 12 tbhits 0 time 82 pv e2e4 e7e5 g1f3",
        )!!
        assertEquals(18, info.depth)
        assertEquals(24, info.selDepth)
        assertEquals(34, info.scoreCp)
        assertEquals(123456L, info.nodes)
        assertEquals(1_500_000L, info.nps)
        assertEquals(82L, info.timeMs)
        assertEquals(listOf("e2e4", "e7e5", "g1f3"), info.pv)
    }

    @Test
    fun `mate scores and bounds are recognised`() {
        val mate = UciParser.parseInfo("info depth 9 score mate -3 nodes 10 pv a1a8")!!
        assertEquals(-3, mate.scoreMate)
        assertNull(mate.scoreCp)

        val bound = UciParser.parseInfo("info depth 9 score cp 120 lowerbound nodes 10 pv a1a8")!!
        assertEquals(UciInfo.Bound.LOWER, bound.bound)
    }

    @Test
    fun `free text and non info lines are ignored`() {
        assertNull(UciParser.parseInfo("info string NNUE evaluation using nn-1a298aa575a0.nnue"))
        assertNull(UciParser.parseInfo("readyok"))
    }

    @Test
    fun `current move lines do not break parsing`() {
        val info = UciParser.parseInfo("info depth 5 currmove e2e4 currmovenumber 1")!!
        assertEquals(5, info.depth)
        assertEquals(emptyList<String>(), info.pv)
    }

    @Test
    fun `bestmove with and without ponder`() {
        assertEquals(BestMove("e2e4", "e7e5"), UciParser.parseBestMove("bestmove e2e4 ponder e7e5"))
        assertEquals(BestMove("a7a8q", null), UciParser.parseBestMove("bestmove a7a8q"))
        assertEquals(BestMove(null, null), UciParser.parseBestMove("bestmove (none)"))
        assertNull(UciParser.parseBestMove("info depth 1"))
    }

    @Test
    fun `options keep multi word names and defaults`() {
        val hash = UciParser.parseOption("option name Hash type spin default 16 min 1 max 33554432")!!
        assertEquals(UciOption("Hash", "spin", "16", 1, 33554432), hash)

        val skill = UciParser.parseOption("option name Skill Level type spin default 20 min 0 max 20")!!
        assertEquals("Skill Level", skill.name)

        val file = UciParser.parseOption("option name EvalFile type string default nn-1a298aa575a0.nnue")!!
        assertEquals("nn-1a298aa575a0.nnue", file.default)

        val empty = UciParser.parseOption("option name Debug Log File type string default <empty>")!!
        assertNotNull(empty.default)
    }

    @Test
    fun `id lines give name and author`() {
        assertEquals("Stockfish 19", UciParser.parseId("id name Stockfish 19", "name"))
        assertEquals("the Stockfish developers", UciParser.parseId("id author the Stockfish developers", "author"))
        assertNull(UciParser.parseId("id author someone", "name"))
    }

    @Test
    fun `go commands carry every limit`() {
        assertEquals("go depth 12", SearchLimits(depth = 12).toGoCommand())
        assertEquals("go movetime 300", SearchLimits(movetimeMs = 300).toGoCommand())
        assertEquals("go depth 8 movetime 100 nodes 5000", SearchLimits(8, 100, 5000).toGoCommand())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a search without limits is rejected`() {
        SearchLimits()
    }
}
