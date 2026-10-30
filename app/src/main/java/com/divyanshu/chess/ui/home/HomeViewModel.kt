package com.divyanshu.chess.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyanshu.chess.domain.ai.EngineAvailability
import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.EngineType
import com.divyanshu.chess.domain.model.GameMode
import com.divyanshu.chess.domain.model.GameRecord
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.domain.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewGameSettings(
    val mode: GameMode = GameMode.VS_COMPUTER,
    val humanSide: Side = Side.WHITE,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val engine: EngineType = EngineType.STOCKFISH,
)

data class HomeUiState(
    val settings: NewGameSettings = NewGameSettings(),
    val inProgress: GameRecord? = null,
    val stockfishAvailable: Boolean = false,
)

sealed interface HomeEvent {
    data class OpenGame(val gameId: Long) : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: GameRepository,
    private val availability: EngineAvailability,
) : ViewModel() {

    private val settings = MutableStateFlow(NewGameSettings())
    private val eventChannel = Channel<HomeEvent>(Channel.BUFFERED)

    val events: Flow<HomeEvent> = eventChannel.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        settings,
        repository.observeInProgress(),
    ) { settings, inProgress ->
        val available = availability.isStockfishAvailable
        HomeUiState(
            settings = if (available) settings else settings.copy(engine = EngineType.BUILT_IN),
            inProgress = inProgress,
            stockfishAvailable = available,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onModeSelected(mode: GameMode) = settings.update { it.copy(mode = mode) }

    fun onSideSelected(side: Side) = settings.update { it.copy(humanSide = side) }

    fun onDifficultySelected(difficulty: Difficulty) = settings.update { it.copy(difficulty = difficulty) }

    fun onEngineSelected(engine: EngineType) = settings.update { it.copy(engine = engine) }

    fun onStartGame() {
        viewModelScope.launch {
            val current = settings.value
            val id = repository.save(
                GameRecord(
                    mode = current.mode,
                    humanSide = current.humanSide,
                    difficulty = current.difficulty,
                    engine = if (availability.isStockfishAvailable) current.engine else EngineType.BUILT_IN,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            eventChannel.send(HomeEvent.OpenGame(id))
        }
    }

    fun onResumeGame() {
        val id = uiState.value.inProgress?.id ?: return
        eventChannel.trySend(HomeEvent.OpenGame(id))
    }
}
