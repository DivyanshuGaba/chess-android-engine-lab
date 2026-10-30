package com.divyanshu.chess.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.EngineType
import com.divyanshu.chess.domain.model.GameMode
import com.divyanshu.chess.domain.model.GameRecord
import com.divyanshu.chess.domain.model.GameResult
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.Side

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moves: String,
    val mode: String,
    val humanSide: String,
    val difficulty: String,
    val result: String?,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "600000") val whiteTimeMs: Long = GameRecord.DEFAULT_TIME_MS,
    @ColumnInfo(defaultValue = "600000") val blackTimeMs: Long = GameRecord.DEFAULT_TIME_MS,
    @ColumnInfo(defaultValue = "0") val engine: Int = 0,
)

fun GameRecord.toEntity() = GameEntity(
    id = id,
    moves = moves.joinToString(" ") { it.uci },
    mode = mode.name,
    humanSide = humanSide.name,
    difficulty = difficulty.name,
    engine = engine.ordinal,
    result = result?.name,
    updatedAt = updatedAt,
    whiteTimeMs = whiteTimeMs,
    blackTimeMs = blackTimeMs,
)

fun GameEntity.toRecord() = GameRecord(
    id = id,
    moves = moves.split(" ").filter { it.isNotBlank() }.map(Move::fromUci),
    mode = GameMode.valueOf(mode),
    humanSide = Side.valueOf(humanSide),
    difficulty = Difficulty.valueOf(difficulty),
    engine = EngineType.entries.getOrElse(engine) { EngineType.BUILT_IN },
    result = result?.let(GameResult::valueOf),
    updatedAt = updatedAt,
    whiteTimeMs = whiteTimeMs,
    blackTimeMs = blackTimeMs,
)
