package com.divyanshu.chess.domain.repository

import com.divyanshu.chess.domain.model.GameRecord
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observeAll(): Flow<List<GameRecord>>

    fun observeInProgress(): Flow<GameRecord?>

    suspend fun get(id: Long): GameRecord?

    /** Inserts or updates the record and returns its id. */
    suspend fun save(record: GameRecord): Long

    suspend fun delete(id: Long)

    suspend fun deleteAll()
}
