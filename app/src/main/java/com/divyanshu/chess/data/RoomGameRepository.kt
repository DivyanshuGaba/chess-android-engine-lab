package com.divyanshu.chess.data

import com.divyanshu.chess.domain.model.GameRecord
import com.divyanshu.chess.domain.repository.GameRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomGameRepository @Inject constructor(private val dao: GameDao) : GameRepository {

    override fun observeAll(): Flow<List<GameRecord>> =
        dao.observeAll().map { list -> list.map { it.toRecord() } }

    override fun observeInProgress(): Flow<GameRecord?> =
        dao.observeInProgress().map { it?.toRecord() }

    override suspend fun get(id: Long): GameRecord? = dao.get(id)?.toRecord()

    override suspend fun save(record: GameRecord): Long {
        val rowId = dao.upsert(record.toEntity())
        return if (record.id != 0L) record.id else rowId
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun deleteAll() = dao.deleteAll()
}
