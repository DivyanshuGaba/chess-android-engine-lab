package com.divyanshu.chess.ui

import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.PieceType
import com.divyanshu.chess.domain.model.Square
import com.divyanshu.chess.ui.game.trackPieces
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PieceTrackingTest {

    private fun track(vararg uci: String) = trackPieces(uci.map(Move::fromUci))

    private fun sq(text: String) = Square.parse(text)

    @Test
    fun `starting position has thirty two distinct pieces`() {
        val board = track()
        assertEquals(32, board.pieces.size)
        assertEquals(32, board.pieces.map { it.id }.toSet().size)
        assertNull(board.lastMovedId)
    }

    @Test
    fun `a moved piece keeps its id`() {
        val before = track().pieces.single { it.square == sq("g1") }.id
        val after = track("g1f3")
        assertEquals(before, after.pieces.single { it.square == sq("f3") }.id)
        assertEquals(before, after.lastMovedId)
        assertNull(after.lastCaptured)
    }

    @Test
    fun `a capture removes the victim and reports it`() {
        val board = track("e2e4", "d7d5", "e4d5")
        val victimId = track().pieces.single { it.square == sq("d7") }.id
        assertEquals(31, board.pieces.size)
        assertEquals(victimId, board.lastCaptured?.id)
        assertEquals(sq("d5"), board.lastCaptured?.square)
    }

    @Test
    fun `castling moves the rook with its own id`() {
        val rookId = track().pieces.single { it.square == sq("h1") }.id
        val board = track("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6", "e1g1")
        assertEquals(rookId, board.pieces.single { it.square == sq("f1") }.id)
        assertEquals(PieceType.KING, board.pieces.single { it.square == sq("g1") }.piece.type)
    }

    @Test
    fun `en passant removes the pawn behind the target square`() {
        val board = track("e2e4", "a7a6", "e4e5", "d7d5", "e5d6")
        val victimId = track().pieces.single { it.square == sq("d7") }.id
        assertEquals(victimId, board.lastCaptured?.id)
        assertEquals(sq("d5"), board.lastCaptured?.square)
        assertEquals(31, board.pieces.size)
    }

    @Test
    fun `promotion keeps the pawns id with the new type`() {
        val pawnId = track().pieces.single { it.square == sq("a2") }.id
        val board = track("a2a4", "b7b5", "a4b5", "a7a6", "b5a6", "g8f6", "a6a7", "f6g8", "a7b8q")
        val promoted = board.pieces.single { it.square == sq("b8") }
        assertEquals(pawnId, promoted.id)
        assertEquals(PieceType.QUEEN, promoted.piece.type)
    }
}
