package com.divyanshu.chess.testutil

import com.divyanshu.chess.domain.ai.AiProvider
import com.divyanshu.chess.domain.ai.ChessAi
import com.divyanshu.chess.domain.clock.ClockTicker
import com.divyanshu.chess.domain.feedback.HapticEvent
import com.divyanshu.chess.domain.feedback.Haptics
import com.divyanshu.chess.domain.ai.EngineAvailability
import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.EngineType
import com.divyanshu.chess.domain.model.GameRecord
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.ThemeMode
import com.divyanshu.chess.domain.repository.GameRepository
import com.divyanshu.chess.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeGameRepository(initial: List<GameRecord> = emptyList()) : GameRepository {

    private val records = MutableStateFlow(initial.associateBy { it.id })
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    val saved: List<GameRecord> get() = records.value.values.toList()

    override fun observeAll(): Flow<List<GameRecord>> =
        records.map { it.values.sortedByDescending { record -> record.updatedAt } }

    override fun observeInProgress(): Flow<GameRecord?> =
        records.map { map -> map.values.filter { it.result == null }.maxByOrNull { it.updatedAt } }

    override suspend fun get(id: Long): GameRecord? = records.value[id]

    override suspend fun save(record: GameRecord): Long {
        val id = if (record.id == 0L) nextId++ else record.id
        records.value = records.value + (id to record.copy(id = id))
        return id
    }

    override suspend fun delete(id: Long) {
        records.value = records.value - id
    }

    override suspend fun deleteAll() {
        records.value = emptyMap()
    }
}

class FakeAi(private val reply: (GameState) -> Move?) : ChessAi {
    override suspend fun chooseMove(state: GameState): Move? = reply(state)
}

class FakeAiProvider(private val ai: ChessAi) : AiProvider {
    override fun create(engine: EngineType, difficulty: Difficulty): ChessAi = ai
}

class FakeSettingsRepository(initialVibration: Boolean = true) : SettingsRepository {
    private val vibration = MutableStateFlow(initialVibration)

    val current: Boolean get() = vibration.value

    override val vibrationEnabled: Flow<Boolean> = vibration

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        vibration.value = enabled
    }

    private val theme = MutableStateFlow(ThemeMode.SYSTEM)

    override val themeMode: Flow<ThemeMode> = theme

    val currentTheme: ThemeMode get() = theme.value

    override suspend fun setThemeMode(mode: ThemeMode) {
        theme.value = mode
    }
}

class FakeHaptics : Haptics {
    val events = mutableListOf<HapticEvent>()

    override fun perform(event: HapticEvent) {
        events += event
    }
}

/** Tests drive time by hand: the first value emitted is the baseline, later ones advance the clock. */
class FakeTicker : ClockTicker {
    private val shared = MutableSharedFlow<Long>()

    override fun ticks(): Flow<Long> = shared

    suspend fun emit(nowMs: Long) = shared.emit(nowMs)
}

class FakeEngineAvailability(override val isStockfishAvailable: Boolean = true) : EngineAvailability
