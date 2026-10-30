package com.divyanshu.chess.ui

import androidx.lifecycle.SavedStateHandle
import com.divyanshu.chess.domain.feedback.HapticEvent
import com.divyanshu.chess.domain.ai.EngineInfo
import com.divyanshu.chess.domain.ai.SearchResult
import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.GameMode
import com.divyanshu.chess.domain.model.GameRecord
import com.divyanshu.chess.domain.model.GameResult
import com.divyanshu.chess.domain.model.GameStatus
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.PieceType
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.domain.model.Square
import com.divyanshu.chess.testutil.FakeAi
import com.divyanshu.chess.testutil.FakeAiProvider
import com.divyanshu.chess.testutil.FakeHaptics
import com.divyanshu.chess.testutil.FakeSettingsRepository
import com.divyanshu.chess.testutil.FakeTicker
import com.divyanshu.chess.testutil.FakeGameRepository
import com.divyanshu.chess.testutil.MainDispatcherRule
import com.divyanshu.chess.ui.game.ForcedEnd
import com.divyanshu.chess.ui.game.GameEvent
import com.divyanshu.chess.ui.game.GameNavigation
import com.divyanshu.chess.ui.game.GameViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val ticker = FakeTicker()
    private val haptics = FakeHaptics()
    private val settings = FakeSettingsRepository()

    private fun sq(text: String) = Square.parse(text)

    private fun record(
        mode: GameMode = GameMode.LOCAL_TWO_PLAYER,
        humanSide: Side = Side.WHITE,
        vararg moves: String,
    ) = GameRecord(id = 1, moves = moves.map(Move::fromUci), mode = mode, humanSide = humanSide)

    private fun viewModel(
        record: GameRecord,
        repository: FakeGameRepository = FakeGameRepository(listOf(record)),
        reply: (com.divyanshu.chess.domain.model.GameState) -> Move? = { null },
    ) = GameViewModel(
        savedStateHandle = SavedStateHandle(mapOf(GameViewModel.GAME_ID_ARG to record.id)),
        repository = repository,
        aiProvider = FakeAiProvider(FakeAi(reply)),
        settings = settings,
        haptics = haptics,
        ticker = ticker,
        aiDispatcher = mainRule.dispatcher,
    )

    @Test
    fun `loads the saved position`() = runTest {
        val vm = viewModel(record(moves = arrayOf("e2e4", "e7e5")))
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("e4", "e5"), state.moveHistory)
        assertEquals(Side.WHITE, state.sideToMove)
    }

    @Test
    fun `selecting a piece shows its legal targets`() = runTest {
        val vm = viewModel(record())
        vm.onSquareClick(sq("e2"))
        assertEquals(sq("e2"), vm.uiState.value.selected)
        assertEquals(setOf(sq("e3"), sq("e4")), vm.uiState.value.legalTargets)
    }

    @Test
    fun `cannot select the opponents piece`() = runTest {
        val vm = viewModel(record())
        vm.onSquareClick(sq("e7"))
        assertNull(vm.uiState.value.selected)
    }

    @Test
    fun `clicking a target plays the move and saves it`() = runTest {
        val repository = FakeGameRepository(listOf(record()))
        val vm = viewModel(record(), repository)
        vm.onSquareClick(sq("e2"))
        vm.onSquareClick(sq("e4"))

        assertEquals(listOf("e4"), vm.uiState.value.moveHistory)
        assertEquals(Side.BLACK, vm.uiState.value.sideToMove)
        assertEquals(listOf(Move.fromUci("e2e4")), repository.saved.single().moves)
    }

    @Test
    fun `clicking an empty square clears the selection`() = runTest {
        val vm = viewModel(record())
        vm.onSquareClick(sq("e2"))
        vm.onSquareClick(sq("e5"))
        assertNull(vm.uiState.value.selected)
    }

    @Test
    fun `clicking another own piece switches the selection`() = runTest {
        val vm = viewModel(record())
        vm.onSquareClick(sq("e2"))
        vm.onSquareClick(sq("g1"))
        assertEquals(sq("g1"), vm.uiState.value.selected)
    }

    @Test
    fun `promotion waits for the player to choose a piece`() = runTest {
        val vm = promotionPositionViewModel()

        vm.onSquareClick(sq("a7"))
        vm.onSquareClick(sq("b8"))
        assertNotNull(vm.uiState.value.pendingPromotion)
        assertEquals(PieceType.PAWN, vm.uiState.value.board[sq("a7").index]?.type)

        vm.onPromotionChosen(PieceType.KNIGHT)
        assertNull(vm.uiState.value.pendingPromotion)
        assertEquals(PieceType.KNIGHT, vm.uiState.value.board[sq("b8").index]?.type)
        assertEquals(Side.WHITE, vm.uiState.value.board[sq("b8").index]?.side)
    }

    @Test
    fun `checkmate ends the game and records the result`() = runTest {
        val repository = FakeGameRepository(listOf(record(moves = arrayOf("f2f3", "e7e5", "g2g4"))))
        val vm = viewModel(repository.saved.single(), repository)
        vm.onSquareClick(sq("d8"))
        vm.onSquareClick(sq("h4"))

        val state = vm.uiState.value
        assertEquals(GameStatus.CHECKMATE, state.status)
        assertEquals(GameResult.BLACK_WINS, state.result)
        assertEquals(sq("e1"), state.checkedKing)
        assertEquals(GameResult.BLACK_WINS, repository.saved.single().result)
    }

    @Test
    fun `no moves are accepted after the game is over`() = runTest {
        val vm = viewModel(record(moves = arrayOf("f2f3", "e7e5", "g2g4", "d8h4")))
        vm.onSquareClick(sq("a2"))
        assertNull(vm.uiState.value.selected)
    }

    @Test
    fun `resigning in a two player game gives the win to the other side`() = runTest {
        val repository = FakeGameRepository(listOf(record()))
        val vm = viewModel(record(), repository)
        vm.onResign()
        assertEquals(GameResult.BLACK_WINS, vm.uiState.value.result)
        assertEquals(GameResult.BLACK_WINS, repository.saved.single().result)
    }

    @Test
    fun `two player board keeps white at the bottom`() = runTest {
        val vm = viewModel(record())
        assertEquals(Side.WHITE, vm.uiState.value.bottomSide)
        vm.onSquareClick(sq("e2"))
        vm.onSquareClick(sq("e4"))
        assertEquals(Side.BLACK, vm.uiState.value.sideToMove)
        assertEquals(Side.WHITE, vm.uiState.value.bottomSide)
    }

    @Test
    fun `pieces keep their ids when they move`() = runTest {
        val vm = viewModel(record())
        val pawnId = vm.uiState.value.pieces.single { it.square == sq("e2") }.id
        vm.onSquareClick(sq("e2"))
        vm.onSquareClick(sq("e4"))

        val state = vm.uiState.value
        assertEquals(pawnId, state.pieces.single { it.square == sq("e4") }.id)
        assertEquals(pawnId, state.lastMovedId)
    }

    @Test
    fun `clocks do not run before the first move`() = runTest {
        val vm = viewModel(record())
        ticker.emit(0)
        ticker.emit(5_000)
        assertEquals(600, vm.clock.value.whiteSeconds)
        assertNull(vm.clock.value.running)
    }

    @Test
    fun `black clock runs after white moves`() = runTest {
        val vm = viewModel(record())
        vm.onSquareClick(sq("e2"))
        vm.onSquareClick(sq("e4"))
        assertEquals(Side.BLACK, vm.clock.value.running)

        ticker.emit(0)
        ticker.emit(5_000)

        assertEquals(595, vm.clock.value.blackSeconds)
        assertEquals(600, vm.clock.value.whiteSeconds)
    }

    @Test
    fun `running out of time loses the game`() = runTest {
        val loaded = record(moves = arrayOf("e2e4")).copy(blackTimeMs = 3_000)
        val repository = FakeGameRepository(listOf(loaded))
        val vm = viewModel(loaded, repository)

        ticker.emit(0)
        ticker.emit(4_000)

        val state = vm.uiState.value
        assertEquals(GameResult.WHITE_WINS, state.result)
        assertEquals(ForcedEnd.TIMEOUT, state.forcedEnd)
        assertEquals(0, vm.clock.value.blackSeconds)
        assertNull(vm.clock.value.running)
        assertEquals(GameResult.WHITE_WINS, repository.saved.single().result)
        assertEquals(listOf(HapticEvent.GAME_OVER), haptics.events)
    }

    @Test
    fun `clock pauses while the app is in the background`() = runTest {
        val vm = viewModel(record(moves = arrayOf("e2e4")))
        ticker.emit(0)
        vm.onAppBackgrounded()
        ticker.emit(60_000)
        assertEquals(600, vm.clock.value.blackSeconds)
        assertNull(vm.clock.value.running)

        vm.onAppForegrounded()
        ticker.emit(100_000)
        ticker.emit(101_000)
        assertEquals(599, vm.clock.value.blackSeconds)
    }

    @Test
    fun `saved clock times are restored`() = runTest {
        val vm = viewModel(record(moves = arrayOf("e2e4")).copy(whiteTimeMs = 123_000, blackTimeMs = 45_000))
        assertEquals(123, vm.clock.value.whiteSeconds)
        assertEquals(45, vm.clock.value.blackSeconds)
    }

    @Test
    fun `clock time is saved with the game`() = runTest {
        val repository = FakeGameRepository(listOf(record()))
        val vm = viewModel(record(), repository)
        vm.onSquareClick(sq("e2"))
        vm.onSquareClick(sq("e4"))
        ticker.emit(0)
        ticker.emit(10_000)
        vm.onAppBackgrounded()
        assertEquals(590_000, repository.saved.single().blackTimeMs)
    }

    @Test
    fun `checkmate stops the clock`() = runTest {
        val vm = viewModel(record(moves = arrayOf("f2f3", "e7e5", "g2g4")))
        vm.onSquareClick(sq("d8"))
        vm.onSquareClick(sq("h4"))
        assertNull(vm.clock.value.running)
        ticker.emit(0)
        ticker.emit(5_000)
        assertEquals(600, vm.clock.value.whiteSeconds)
    }

    @Test
    fun `a quiet move vibrates once`() = runTest {
        val vm = viewModel(record())
        vm.onSquareClick(sq("e2"))
        vm.onSquareClick(sq("e4"))
        assertEquals(listOf(HapticEvent.MOVE), haptics.events)
    }

    @Test
    fun `a capture gets a stronger buzz`() = runTest {
        val vm = viewModel(record(moves = arrayOf("e2e4", "d7d5")))
        vm.onSquareClick(sq("e4"))
        vm.onSquareClick(sq("d5"))
        assertEquals(listOf(HapticEvent.CAPTURE), haptics.events)
    }

    @Test
    fun `check and checkmate have their own buzzes`() = runTest {
        val check = viewModel(record(moves = arrayOf("e2e4", "f7f6")))
        check.onSquareClick(sq("d1"))
        check.onSquareClick(sq("h5"))
        assertEquals(listOf(HapticEvent.CHECK), haptics.events)
        assertEquals(GameEvent.CHECK, check.uiState.value.lastEvent)

        haptics.events.clear()
        val mate = viewModel(record(moves = arrayOf("f2f3", "e7e5", "g2g4")))
        mate.onSquareClick(sq("d8"))
        mate.onSquareClick(sq("h4"))
        assertEquals(listOf(HapticEvent.GAME_OVER), haptics.events)
        assertNull(mate.uiState.value.lastEvent)
    }

    @Test
    fun `nothing vibrates when vibration is switched off`() = runTest {
        settings.setVibrationEnabled(false)
        val vm = viewModel(record())
        assertFalse(vm.vibrationEnabled.value)
        vm.onSquareClick(sq("e2"))
        vm.onSquareClick(sq("e4"))
        assertTrue(haptics.events.isEmpty())
    }

    @Test
    fun `the vibration toggle is saved to settings`() = runTest {
        val vm = viewModel(record())
        vm.onVibrationToggled(false)
        assertFalse(settings.current)
        assertFalse(vm.vibrationEnabled.value)
        vm.onVibrationToggled(true)
        assertTrue(vm.vibrationEnabled.value)
    }

    @Test
    fun `castling and promotion are announced`() = runTest {
        val castle = viewModel(record(moves = arrayOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6")))
        castle.onSquareClick(sq("e1"))
        castle.onSquareClick(sq("g1"))
        assertEquals(GameEvent.CASTLING, castle.uiState.value.lastEvent)

        val promotion = promotionPositionViewModel()
        promotion.onSquareClick(sq("a7"))
        promotion.onSquareClick(sq("b8"))
        promotion.onPromotionChosen(PieceType.QUEEN)
        assertEquals(GameEvent.PROMOTION, promotion.uiState.value.lastEvent)
    }

    @Test
    fun `en passant is announced`() = runTest {
        val vm = viewModel(record(moves = arrayOf("e2e4", "a7a6", "e4e5", "d7d5")))
        vm.onSquareClick(sq("e5"))
        vm.onSquareClick(sq("d6"))
        assertEquals(GameEvent.EN_PASSANT, vm.uiState.value.lastEvent)
    }

    @Test
    fun `resigning is recorded as a resignation`() = runTest {
        val vm = viewModel(record())
        vm.onResign()
        assertEquals(ForcedEnd.RESIGNATION, vm.uiState.value.forcedEnd)
    }

    @Test
    fun `undo takes back one move in a two player game`() = runTest {
        val vm = viewModel(record(moves = arrayOf("e2e4", "e7e5")))
        assertTrue(vm.uiState.value.canUndo)
        vm.onUndo()
        assertEquals(listOf("e4"), vm.uiState.value.moveHistory)
    }

    @Test
    fun `undo is unavailable on the first move`() = runTest {
        assertFalse(viewModel(record()).uiState.value.canUndo)
    }

    @Test
    fun `computer replies after the human moves`() = runTest {
        val loaded = record(mode = GameMode.VS_COMPUTER, humanSide = Side.WHITE)
        val vm = viewModel(loaded) { Move.fromUci("e7e5") }

        vm.onSquareClick(sq("e2"))
        vm.onSquareClick(sq("e4"))
        assertTrue(vm.uiState.value.isThinking)
        advanceUntilIdle()

        assertEquals(listOf("e4", "e5"), vm.uiState.value.moveHistory)
        assertEquals(Side.WHITE, vm.uiState.value.sideToMove)
        assertFalse(vm.uiState.value.isThinking)
    }

    @Test
    fun `computer opens when the human plays black`() = runTest {
        val loaded = record(mode = GameMode.VS_COMPUTER, humanSide = Side.BLACK)
        val vm = viewModel(loaded) { Move.fromUci("d2d4") }
        advanceUntilIdle()

        assertEquals(listOf("d4"), vm.uiState.value.moveHistory)
        assertEquals(Side.BLACK, vm.uiState.value.bottomSide)
    }

    @Test
    fun `the engines analysis is shown after it moves`() = runTest {
        val loaded = record(mode = GameMode.VS_COMPUTER, humanSide = Side.WHITE)
        val reply = Move.fromUci("e7e5")
        val info = EngineInfo(
            engineName = "TestFish",
            depth = 12,
            scoreCp = -25,
            mateIn = null,
            nodes = 1000,
            nps = 2_000_000,
            timeMs = 90,
            pv = listOf(reply, Move.fromUci("g1f3")),
        )
        val ai = object : com.divyanshu.chess.domain.ai.ChessAi {
            override suspend fun chooseMove(state: com.divyanshu.chess.domain.model.GameState) = reply
            override suspend fun search(state: com.divyanshu.chess.domain.model.GameState) = SearchResult(reply, info)
        }
        val vm = GameViewModel(
            savedStateHandle = SavedStateHandle(mapOf(GameViewModel.GAME_ID_ARG to loaded.id)),
            repository = FakeGameRepository(listOf(loaded)),
            aiProvider = FakeAiProvider(ai),
            settings = settings,
            haptics = haptics,
            ticker = ticker,
            aiDispatcher = mainRule.dispatcher,
        )

        vm.onSquareClick(sq("e2"))
        vm.onSquareClick(sq("e4"))
        advanceUntilIdle()

        val line = vm.uiState.value.engineLine!!
        assertEquals("TestFish", line.engineName)
        assertEquals(12, line.depth)
        assertEquals("-0.25", line.score)
        assertEquals(listOf("e5", "Nf3"), line.pv)
    }

    @Test
    fun `undo clears the engine analysis`() = runTest {
        val loaded = record(mode = GameMode.VS_COMPUTER, humanSide = Side.WHITE, "e2e4", "e7e5")
        val vm = viewModel(loaded)
        vm.onUndo()
        assertNull(vm.uiState.value.engineLine)
    }

    @Test
    fun `undo against the computer rewinds a full turn`() = runTest {
        val loaded = record(mode = GameMode.VS_COMPUTER, humanSide = Side.WHITE, "e2e4", "e7e5")
        val vm = viewModel(loaded)
        vm.onUndo()
        assertTrue(vm.uiState.value.moveHistory.isEmpty())
    }

    @Test
    fun `human cannot move while it is the computers turn`() = runTest {
        val loaded = record(mode = GameMode.VS_COMPUTER, humanSide = Side.WHITE, "e2e4")
        val vm = viewModel(loaded) { null }
        vm.onSquareClick(sq("e7"))
        assertNull(vm.uiState.value.selected)
    }

    @Test
    fun `rematch saves a fresh game with the same settings and opens it`() = runTest {
        val loaded = record(mode = GameMode.VS_COMPUTER, humanSide = Side.BLACK, "e2e4").copy(difficulty = Difficulty.HARD)
        val repository = FakeGameRepository(listOf(loaded))
        val vm = viewModel(loaded, repository) { null }
        val first = async { vm.navigation.first() }
        vm.onRematch()
        advanceUntilIdle()
        val opened = first.await() as GameNavigation.OpenGame
        val created = repository.get(opened.gameId)!!
        assertTrue(opened.gameId != loaded.id)
        assertTrue(created.moves.isEmpty())
        assertEquals(Side.BLACK, created.humanSide)
        assertEquals(Difficulty.HARD, created.difficulty)
        assertEquals(GameMode.VS_COMPUTER, created.mode)
    }

    private fun promotionPositionViewModel(): GameViewModel {
        val moves = listOf(
            "a2a4", "b7b5", "a4b5", "a7a6", "b5a6", "g8f6", "a6a7", "f6g8",
        )
        val loaded = GameRecord(id = 1, moves = moves.map(Move::fromUci), mode = GameMode.LOCAL_TWO_PLAYER)
        return viewModel(loaded, FakeGameRepository(listOf(loaded)))
    }
}
