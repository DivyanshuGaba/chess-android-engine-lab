package com.divyanshu.chess.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE result IS NULL ORDER BY updatedAt DESC LIMIT 1")
    fun observeInProgress(): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun get(id: Long): GameEntity?

    @Upsert
    suspend fun upsert(entity: GameEntity): Long

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM games")
    suspend fun deleteAll()
}
