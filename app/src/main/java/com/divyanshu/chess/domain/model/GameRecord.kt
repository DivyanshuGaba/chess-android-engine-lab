package com.divyanshu.chess.domain.model

enum class GameMode { LOCAL_TWO_PLAYER, VS_COMPUTER }

enum class Difficulty { EASY, MEDIUM, HARD }

data class GameRecord(
    val id: Long = 0,
    val moves: List<Move> = emptyList(),
    val mode: GameMode = GameMode.VS_COMPUTER,
    val humanSide: Side = Side.WHITE,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val engine: EngineType = EngineType.BUILT_IN,
    val result: GameResult? = null,
    val updatedAt: Long = 0,
    val whiteTimeMs: Long = DEFAULT_TIME_MS,
    val blackTimeMs: Long = DEFAULT_TIME_MS,
) {
    companion object {
        const val DEFAULT_TIME_MS = 10 * 60 * 1000L
    }
}
