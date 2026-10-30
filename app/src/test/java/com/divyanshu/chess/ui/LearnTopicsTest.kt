package com.divyanshu.chess.ui

import com.divyanshu.chess.domain.engine.Fen
import com.divyanshu.chess.domain.model.Square
import com.divyanshu.chess.ui.learn.LearnTopics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnTopicsTest {

    @Test
    fun `topic ids are unique`() {
        val ids = LearnTopics.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every example position is valid and its marks are on the board`() {
        LearnTopics.all.forEach { topic ->
            topic.fen?.let { Fen.parse(it) }
            topic.marks.forEach { Square.parse(it.square) }
        }
    }

    @Test
    fun `the engine confirms every example shows what the lesson says`() {
        LearnTopics.all.forEach { topic ->
            topic.proof?.let { proof -> assertTrue("${topic.id} example is wrong", proof()) }
        }
    }

    @Test
    fun `every lesson explains what it is and why it happens`() {
        LearnTopics.all.forEach { topic ->
            assertTrue(topic.id, topic.summary.isNotBlank() && topic.why.isNotBlank())
        }
    }

    @Test
    fun `lessons with a diagram have a proof and a legend`() {
        LearnTopics.all.filter { it.fen != null }.forEach { topic ->
            assertTrue("${topic.id} needs a proof", topic.proof != null)
            assertTrue("${topic.id} needs a legend", topic.legend.isNotEmpty())
        }
    }

    @Test
    fun `the situations the game reports are all covered`() {
        val ids = LearnTopics.all.map { it.id }.toSet()
        listOf(
            "check", "checkmate", "stalemate", "insufficient-material", "fifty-move",
            "repetition", "timeout", "resignation", "castling", "en-passant", "promotion",
        ).forEach { assertTrue("missing $it", it in ids) }
    }
}
