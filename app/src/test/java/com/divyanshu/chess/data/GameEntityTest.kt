package com.divyanshu.chess.data

import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.EngineType
import com.divyanshu.chess.domain.model.GameMode
import com.divyanshu.chess.domain.model.GameRecord
import com.divyanshu.chess.domain.model.GameResult
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.Side
import org.junit.Assert.assertEquals
import org.junit.Test

class GameEntityTest {

    @Test
    fun `record survives a round trip through the entity`() {
        val record = GameRecord(
            id = 4,
            moves = listOf("e2e4", "e7e5", "a7a8q").map(Move::fromUci),
            mode = GameMode.VS_COMPUTER,
            humanSide = Side.BLACK,
            difficulty = Difficulty.HARD,
            engine = EngineType.STOCKFISH,
            result = GameResult.DRAW,
            updatedAt = 99,
        )
        assertEquals(record, record.toEntity().toRecord())
    }

    @Test
    fun `empty move list is stored as an empty string`() {
        val entity = GameRecord().toEntity()
        assertEquals("", entity.moves)
        assertEquals(emptyList<Move>(), entity.toRecord().moves)
    }
}
