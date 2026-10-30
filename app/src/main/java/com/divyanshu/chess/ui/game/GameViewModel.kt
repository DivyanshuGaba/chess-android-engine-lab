package com.divyanshu.chess.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyanshu.chess.di.DefaultDispatcher
import com.divyanshu.chess.domain.ai.AiProvider
import com.divyanshu.chess.domain.ai.EngineInfo
import com.divyanshu.chess.domain.ai.PvFormatter
import com.divyanshu.chess.domain.clock.ClockTicker
import com.divyanshu.chess.domain.engine.ChessRules
import com.divyanshu.chess.domain.feedback.HapticEvent
import com.divyanshu.chess.domain.feedback.Haptics
import com.divyanshu.chess.domain.model.Game
import com.divyanshu.chess.domain.model.GameMode
import com.divyanshu.chess.domain.model.GameRecord
import com.divyanshu.chess.domain.model.GameResult
import com.divyanshu.chess.domain.model.GameStatus
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.Piece
import com.divyanshu.chess.domain.model.PieceType
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.domain.model.Square
import com.divyanshu.chess.domain.repository.GameRepository
import com.divyanshu.chess.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PendingPromotion(val from: Square, val to: Square, val side: Side)

enum class ForcedEnd { RESIGNATION, TIMEOUT }

/** One-off navigation requests from the game screen. */
sealed interface GameNavigation {
    data class OpenGame(val gameId: Long) : GameNavigation
}

/** A notable thing that just happened on the board, announced briefly above it. */
enum class GameEvent { CHECK, CASTLING, EN_PASSANT, PROMOTION }

data class ClockUiState(
    val whiteSeconds: Int = secondsFor(GameRecord.DEFAULT_TIME_MS),
    val blackSeconds: Int = secondsFor(GameRecord.DEFAULT_TIME_MS),
    val running: Side? = null,
)

/** What the engine said about the move it just played, ready to show on screen. */
data class EngineLine(
    val engineName: String,
    val depth: Int,
    val score: String,
    val nps: Long,
    val timeMs: Long,
    val pv: List<String>,
    val scoreCp: Int? = null,
    val mateIn: Int? = null,
)

data class GameUiState(
    val isLoading: Boolean = true,
    val board: List<Piece?> = List(64) { null },
    val pieces: List<TrackedPiece> = emptyList(),
    val lastMovedId: Int? = null,
    val lastCapture: TrackedPiece? = null,
    val moveCount: Int = 0,
    val mode: GameMode = GameMode.VS_COMPUTER,
    val bottomSide: Side = Side.WHITE,
    val sideToMove: Side = Side.WHITE,
    val selected: Square? = null,
    val legalTargets: Set<Square> = emptySet(),
    val lastMove: Move? = null,
    val checkedKing: Square? = null,
    val status: GameStatus = GameStatus.ONGOING,
    val result: GameResult? = null,
    val forcedEnd: ForcedEnd? = null,
    val lastEvent: GameEvent? = null,
    val engineLine: EngineLine? = null,
    val moveHistory: List<String> = emptyList(),
    val capturedByWhite: List<PieceType> = emptyList(),
    val capturedByBlack: List<PieceType> = emptyList(),
    val isThinking: Boolean = false,
    val pendingPromotion: PendingPromotion? = null,
    val canUndo: Boolean = false,
) {
    val isGameOver: Boolean get() = result != null
}

@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GameRepository,
    private val aiProvider: AiProvider,
    private val settings: SettingsRepository,
    private val haptics: Haptics,
    private val ticker: ClockTicker,
    @DefaultDispatcher private val aiDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val gameId: Long = checkNotNull(savedStateHandle[GAME_ID_ARG])

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _clock = MutableStateFlow(ClockUiState())
    val clock: StateFlow<ClockUiState> = _clock.asStateFlow()

    private val navigationChannel = Channel<GameNavigation>(Channel.BUFFERED)
    val navigation: Flow<GameNavigation> = navigationChannel.receiveAsFlow()

    val vibrationEnabled: StateFlow<Boolean> = settings.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var record = GameRecord()
    private var game = Game.new()
    private var forcedResult: GameResult? = null
    private var forcedEnd: ForcedEnd? = null
    private var whiteMs = GameRecord.DEFAULT_TIME_MS
    private var blackMs = GameRecord.DEFAULT_TIME_MS
    private var selected: Square? = null
    private var pendingPromotion: PendingPromotion? = null
    private var aiJob: Job? = null
    private var clockJob: Job? = null
    private var thinking = false
    private var inBackground = false
    private var engineLine: EngineLine? = null

    init {
        viewModelScope.launch {
            repository.get(gameId)?.let { loaded ->
                record = loaded
                game = Game.fromMoves(loaded.moves)
                whiteMs = loaded.whiteTimeMs
                blackMs = loaded.blackTimeMs
                if (loaded.result != null && game.result == null) {
                    forcedResult = loaded.result
                    val loserMs = if (loaded.result == GameResult.WHITE_WINS) blackMs else whiteMs
                    forcedEnd = if (loserMs <= 0) ForcedEnd.TIMEOUT else ForcedEnd.RESIGNATION
                }
            }
            publish(loading = false)
            syncClock()
            maybeStartComputerTurn()
        }
    }

    fun onSquareClick(square: Square) {
        if (!isHumanTurn()) return
        val from = selected
        val targets = from?.let { game.legalMovesFrom(it) }.orEmpty()
        val candidates = targets.filter { it.to == square }

        when {
            candidates.isEmpty() -> {
                val ownPiece = game.state.board[square]?.takeIf { it.side == game.state.sideToMove }
                selected = if (ownPiece != null && square != from) square else null
                publish()
            }
            candidates.first().promotion != null -> {
                pendingPromotion = PendingPromotion(candidates.first().from, square, game.state.sideToMove)
                publish()
            }
            else -> playHumanMove(candidates.first())
        }
    }

    fun onPromotionChosen(type: PieceType) {
        val pending = pendingPromotion ?: return
        pendingPromotion = null
        playHumanMove(Move(pending.from, pending.to, type))
    }

    fun onPromotionDismissed() {
        pendingPromotion = null
        publish()
    }

    fun onUndo() {
        if (!canUndo()) return
        aiJob?.cancel()
        thinking = false
        val count = if (record.mode == GameMode.VS_COMPUTER && game.state.sideToMove == record.humanSide) 2 else 1
        game = game.undo(count)
        selected = null
        engineLine = null
        persist()
        syncClock()
        publish()
    }

    fun onResign() {
        if (currentResult() != null) return
        val loser = if (record.mode == GameMode.VS_COMPUTER) record.humanSide else game.state.sideToMove
        finishWithForcedResult(GameResult.winnerOf(loser.opposite), ForcedEnd.RESIGNATION)
    }

    /** Starts a fresh game against the same opponent, with the same settings. */
    fun onRematch() {
        viewModelScope.launch {
            val id = repository.save(
                GameRecord(
                    mode = record.mode,
                    humanSide = record.humanSide,
                    difficulty = record.difficulty,
                    engine = record.engine,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            navigationChannel.send(GameNavigation.OpenGame(id))
        }
    }

    fun onVibrationToggled(enabled: Boolean) {
        viewModelScope.launch { settings.setVibrationEnabled(enabled) }
    }

    /** The clock stops while the app is off screen so a phone call never costs a player time. */
    fun onAppBackgrounded() {
        inBackground = true
        syncClock()
        persist()
    }

    fun onAppForegrounded() {
        inBackground = false
        syncClock()
    }

    private fun playHumanMove(move: Move) {
        applyMove(move)
        maybeStartComputerTurn()
    }

    private fun applyMove(move: Move) {
        val piecesBefore = game.state.board.pieces.size
        game = game.play(move)
        selected = null
        persist()
        syncClock()
        publish()
        feedback(
            when {
                game.result != null -> HapticEvent.GAME_OVER
                game.status == GameStatus.CHECK -> HapticEvent.CHECK
                game.state.board.pieces.size < piecesBefore -> HapticEvent.CAPTURE
                else -> HapticEvent.MOVE
            },
        )
    }

    private fun maybeStartComputerTurn() {
        if (record.mode != GameMode.VS_COMPUTER || currentResult() != null) return
        if (game.state.sideToMove == record.humanSide) return

        aiJob?.cancel()
        val snapshot = game.state
        aiJob = viewModelScope.launch {
            thinking = true
            publish()
            try {
                val ai = aiProvider.create(record.engine, record.difficulty)
                val result = coroutineScope {
                    val pause = launch { delay(MIN_REPLY_MS) }
                    val chosen = withContext(aiDispatcher) { ai.search(snapshot) }
                    pause.join()
                    chosen
                }
                val move = result.move
                engineLine = result.info?.let { lineFor(snapshot, it) }
                thinking = false
                if (move != null) applyMove(move) else publish()
            } finally {
                thinking = false
            }
        }
    }

    /** Starts or stops the clock so it runs only for the side to move while a game is live. */
    private fun syncClock() {
        val shouldRun = game.moves.isNotEmpty() && currentResult() == null && !inBackground
        if (shouldRun && clockJob?.isActive != true) {
            clockJob = viewModelScope.launch {
                var last: Long? = null
                ticker.ticks().collect { now ->
                    val previous = last ?: now
                    last = now
                    tick(now - previous)
                }
            }
        } else if (!shouldRun) {
            clockJob?.cancel()
            clockJob = null
        }
        publishClock()
    }

    private fun tick(elapsedMs: Long) {
        if (elapsedMs <= 0 || currentResult() != null) return
        val side = game.state.sideToMove
        val remaining = (timeOf(side) - elapsedMs).coerceAtLeast(0)
        setTime(side, remaining)
        if (remaining == 0L) {
            finishWithForcedResult(GameResult.winnerOf(side.opposite), ForcedEnd.TIMEOUT)
            feedback(HapticEvent.GAME_OVER)
        } else {
            publishClock()
        }
    }

    private fun finishWithForcedResult(result: GameResult, reason: ForcedEnd) {
        aiJob?.cancel()
        thinking = false
        pendingPromotion = null
        selected = null
        forcedResult = result
        forcedEnd = reason
        persist()
        syncClock()
        publish()
    }

    private fun timeOf(side: Side) = if (side == Side.WHITE) whiteMs else blackMs

    private fun setTime(side: Side, ms: Long) {
        if (side == Side.WHITE) whiteMs = ms else blackMs = ms
    }

    private fun feedback(event: HapticEvent) {
        if (vibrationEnabled.value) haptics.perform(event)
    }

    private fun isHumanTurn(): Boolean {
        if (currentResult() != null || thinking || pendingPromotion != null) return false
        return record.mode == GameMode.LOCAL_TWO_PLAYER || game.state.sideToMove == record.humanSide
    }

    private fun canUndo(): Boolean {
        if (currentResult() != null) return false
        val needed = if (record.mode == GameMode.VS_COMPUTER && game.state.sideToMove == record.humanSide) 2 else 1
        return game.moves.size >= needed
    }

    private fun currentResult(): GameResult? = game.result ?: forcedResult

    private fun persist() {
        record = record.copy(
            moves = game.moves,
            result = currentResult(),
            updatedAt = System.currentTimeMillis(),
            whiteTimeMs = whiteMs,
            blackTimeMs = blackMs,
        )
        viewModelScope.launch {
            val id = repository.save(record)
            if (record.id == 0L) record = record.copy(id = id)
        }
    }

    private fun publishClock() {
        val running = if (game.moves.isNotEmpty() && currentResult() == null && !inBackground) {
            game.state.sideToMove
        } else {
            null
        }
        _clock.value = ClockUiState(secondsFor(whiteMs), secondsFor(blackMs), running)
    }

    private fun publish(loading: Boolean = false) {
        val state = game.state
        val targets = selected?.let { game.legalMovesFrom(it).map { move -> move.to }.toSet() }.orEmpty()
        val tracked = trackPieces(game.moves)
        _uiState.value = GameUiState(
            isLoading = loading,
            board = List(64) { state.board[Square(it)] },
            pieces = tracked.pieces,
            lastMovedId = tracked.lastMovedId,
            lastCapture = tracked.lastCaptured,
            moveCount = game.moves.size,
            mode = record.mode,
            bottomSide = if (record.mode == GameMode.VS_COMPUTER) record.humanSide else Side.WHITE,
            sideToMove = state.sideToMove,
            selected = selected,
            legalTargets = targets,
            lastMove = game.lastMove,
            checkedKing = state.board.kingSquare(state.sideToMove)
                ?.takeIf { ChessRules.isInCheck(state) },
            status = game.status,
            result = currentResult(),
            forcedEnd = forcedEnd.takeIf { game.result == null },
            lastEvent = if (currentResult() == null) eventFor(tracked) else null,
            engineLine = engineLine,
            moveHistory = game.sanMoves,
            capturedByWhite = game.capturedBy(Side.WHITE),
            capturedByBlack = game.capturedBy(Side.BLACK),
            isThinking = thinking,
            pendingPromotion = pendingPromotion,
            canUndo = canUndo(),
        )
    }

    private fun lineFor(state: com.divyanshu.chess.domain.model.GameState, info: EngineInfo) = EngineLine(
        engineName = info.engineName,
        depth = info.depth,
        score = PvFormatter.formatScore(info.scoreCp, info.mateIn),
        nps = info.nps,
        timeMs = info.timeMs,
        pv = PvFormatter.toSan(state, info.pv),
        scoreCp = info.scoreCp,
        mateIn = info.mateIn,
    )

    private fun eventFor(tracked: TrackedBoard): GameEvent? {
        val move = game.lastMove ?: return null
        return when {
            game.status == GameStatus.CHECK -> GameEvent.CHECK
            game.sanMoves.last().startsWith("O-O") -> GameEvent.CASTLING
            move.promotion != null -> GameEvent.PROMOTION
            tracked.lastCaptured?.let { it.square != move.to } == true -> GameEvent.EN_PASSANT
            else -> null
        }
    }

    companion object {
        const val GAME_ID_ARG = "gameId"
        const val MIN_REPLY_MS = 450L
    }
}

/** Whole seconds remaining, rounded up so a clock never shows 0:00 while time is left. */
fun secondsFor(ms: Long): Int = ((ms + 999) / 1000).toInt()

fun formatClock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
