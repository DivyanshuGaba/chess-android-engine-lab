package com.divyanshu.chess.ui.spectator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyanshu.chess.di.DefaultDispatcher
import com.divyanshu.chess.domain.ai.AiProvider
import com.divyanshu.chess.domain.ai.EngineAvailability
import com.divyanshu.chess.domain.ai.EngineInfo
import com.divyanshu.chess.domain.ai.PvFormatter
import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.EngineType
import com.divyanshu.chess.domain.model.Game
import com.divyanshu.chess.domain.model.GameMode
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.domain.model.Square
import com.divyanshu.chess.ui.game.EngineLine
import com.divyanshu.chess.ui.game.GameUiState
import com.divyanshu.chess.ui.game.Outcome
import com.divyanshu.chess.ui.game.outcomeFor
import com.divyanshu.chess.ui.game.trackPieces
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Contestant(val engine: EngineType, val difficulty: Difficulty) {
    val label: String get() = "${engine.label}, ${difficulty.name.lowercase()}"
}

/** How long to hold each move on screen before the next engine starts thinking. */
enum class SpectatorSpeed(val label: String, val pauseMs: Long) {
    SLOW("Slow", 1_500),
    NORMAL("Normal", 600),
    FAST("Fast", 150),
}

data class SpectatorUiState(
    val board: GameUiState = GameUiState(isLoading = false, mode = GameMode.LOCAL_TWO_PLAYER),
    val white: Contestant = Contestant(EngineType.BUILT_IN, Difficulty.HARD),
    val black: Contestant = Contestant(EngineType.BUILT_IN, Difficulty.MEDIUM),
    val whiteLine: EngineLine? = null,
    val blackLine: EngineLine? = null,
    val speed: SpectatorSpeed = SpectatorSpeed.NORMAL,
    val started: Boolean = false,
    val paused: Boolean = false,
    val finished: Boolean = false,
    val outcome: Outcome? = null,
    val stoppedAtLimit: Boolean = false,
    val error: String? = null,
    val stockfishAvailable: Boolean = false,
)

/**
 * Lets two engines play each other for the viewer to watch. It is a demo: nothing is saved and no results
 * are tallied, because a handful of games on a phone cannot measure strength (see the Engine lab for that).
 */
@HiltViewModel
class SpectatorViewModel @Inject constructor(
    private val aiProvider: AiProvider,
    availability: EngineAvailability,
    @DefaultDispatcher private val aiDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val stockfish = availability.isStockfishAvailable

    private val _uiState = MutableStateFlow(
        SpectatorUiState(
            white = Contestant(EngineType.BUILT_IN, Difficulty.HARD),
            black = Contestant(if (stockfish) EngineType.STOCKFISH else EngineType.BUILT_IN, Difficulty.MEDIUM),
            stockfishAvailable = stockfish,
        ),
    )
    val uiState: StateFlow<SpectatorUiState> = _uiState.asStateFlow()

    private val paused = MutableStateFlow(false)
    private var game = Game.new()
    private var matchJob: Job? = null
    private var thinking = false

    init {
        publish()
    }

    fun onWhiteChanged(contestant: Contestant) {
        if (!_uiState.value.started) _uiState.update { it.copy(white = contestant) }
    }

    fun onBlackChanged(contestant: Contestant) {
        if (!_uiState.value.started) _uiState.update { it.copy(black = contestant) }
    }

    fun onSpeedChanged(speed: SpectatorSpeed) = _uiState.update { it.copy(speed = speed) }

    fun onStart() {
        matchJob?.cancel()
        game = Game.new()
        paused.value = false
        _uiState.update {
            it.copy(started = true, paused = false, finished = false, outcome = null, stoppedAtLimit = false, error = null, whiteLine = null, blackLine = null)
        }
        publish()
        matchJob = viewModelScope.launch { playMatch() }
    }

    fun onPauseToggled() {
        if (!_uiState.value.started || _uiState.value.finished) return
        paused.value = !paused.value
        _uiState.update { it.copy(paused = paused.value) }
    }

    /** Stops the match and returns to the setup screen. */
    fun onReset() {
        matchJob?.cancel()
        matchJob = null
        game = Game.new()
        thinking = false
        paused.value = false
        _uiState.update {
            it.copy(started = false, paused = false, finished = false, outcome = null, stoppedAtLimit = false, error = null, whiteLine = null, blackLine = null)
        }
        publish()
    }

    private suspend fun playMatch() {
        try {
            while (game.result == null && game.moves.size < MAX_PLIES) {
                paused.first { !it }
                val side = game.state.sideToMove
                val contestant = if (side == Side.WHITE) _uiState.value.white else _uiState.value.black
                val snapshot = game.state

                thinking = true
                publish()
                val result = withContext(aiDispatcher) { aiProvider.create(contestant.engine, contestant.difficulty).search(snapshot) }
                thinking = false

                val move = result.move ?: break
                if (move !in game.legalMoves) {
                    finish(error = "${contestant.label} returned an illegal move (${move.uci}), so the demo stopped.")
                    return
                }
                paused.first { !it }
                game = game.play(move)
                val line = result.info?.let { lineFor(snapshot, it) }
                _uiState.update { if (side == Side.WHITE) it.copy(whiteLine = line) else it.copy(blackLine = line) }
                publish()
                delay(_uiState.value.speed.pauseMs)
            }
            finish()
        } catch (e: CancellationException) {
            thinking = false
            throw e
        }
    }

    private fun finish(error: String? = null) {
        val board = boardState()
        _uiState.update {
            it.copy(
                board = board,
                finished = true,
                paused = false,
                outcome = if (error == null) outcomeFor(board) else null,
                stoppedAtLimit = error == null && game.result == null,
                error = error,
            )
        }
    }

    private fun publish() {
        val board = boardState()
        _uiState.update { it.copy(board = board) }
    }

    private fun boardState(): GameUiState {
        val state = game.state
        val tracked = trackPieces(game.moves)
        return GameUiState(
            isLoading = false,
            board = List(64) { state.board[Square(it)] },
            pieces = tracked.pieces,
            lastMovedId = tracked.lastMovedId,
            lastCapture = tracked.lastCaptured,
            moveCount = game.moves.size,
            mode = GameMode.LOCAL_TWO_PLAYER,
            bottomSide = Side.WHITE,
            sideToMove = state.sideToMove,
            lastMove = game.lastMove,
            checkedKing = state.board.kingSquare(state.sideToMove)?.takeIf { ChessRules.isInCheck(state) },
            status = game.status,
            result = game.result,
            moveHistory = game.sanMoves,
            capturedByWhite = game.capturedBy(Side.WHITE),
            capturedByBlack = game.capturedBy(Side.BLACK),
            isThinking = thinking,
        )
    }

    private fun lineFor(state: GameState, info: EngineInfo) = EngineLine(
        engineName = info.engineName,
        depth = info.depth,
        score = PvFormatter.formatScore(info.scoreCp, info.mateIn),
        nps = info.nps,
        timeMs = info.timeMs,
        pv = PvFormatter.toSan(state, info.pv, maxMoves = 6),
        scoreCp = info.scoreCp,
        mateIn = info.mateIn,
    )

    companion object {
        /** A demo should not run forever if two engines shuffle without a decisive result. */
        const val MAX_PLIES = 300
    }
}
