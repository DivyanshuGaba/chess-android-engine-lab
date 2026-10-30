package com.divyanshu.chess.ui

import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.EngineType
import com.divyanshu.chess.domain.model.GameMode
import com.divyanshu.chess.domain.model.GameRecord
import com.divyanshu.chess.domain.model.GameResult
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.testutil.FakeEngineAvailability
import com.divyanshu.chess.testutil.FakeGameRepository
import com.divyanshu.chess.testutil.MainDispatcherRule
import com.divyanshu.chess.ui.history.HistoryViewModel
import com.divyanshu.chess.ui.home.HomeEvent
import com.divyanshu.chess.ui.home.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeAndHistoryViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `settings changes are reflected in the state`() = runTest {
        val vm = HomeViewModel(FakeGameRepository(), FakeEngineAvailability())
        backgroundScope.launch(mainRule.dispatcher) { vm.uiState.collect {} }

        vm.onModeSelected(GameMode.LOCAL_TWO_PLAYER)
        vm.onSideSelected(Side.BLACK)
        vm.onDifficultySelected(Difficulty.HARD)

        val settings = vm.uiState.value.settings
        assertEquals(GameMode.LOCAL_TWO_PLAYER, settings.mode)
        assertEquals(Side.BLACK, settings.humanSide)
        assertEquals(Difficulty.HARD, settings.difficulty)
    }

    @Test
    fun `starting a game saves a record and opens it`() = runTest {
        val repository = FakeGameRepository()
        val vm = HomeViewModel(repository, FakeEngineAvailability())
        vm.onDifficultySelected(Difficulty.EASY)

        vm.onStartGame()

        val event = vm.events.first()
        val saved = repository.saved.single()
        assertEquals(HomeEvent.OpenGame(saved.id), event)
        assertEquals(Difficulty.EASY, saved.difficulty)
    }

    @Test
    fun `resume is offered only for unfinished games`() = runTest {
        val finished = GameRecord(id = 1, result = GameResult.DRAW, updatedAt = 5)
        val open = GameRecord(id = 2, updatedAt = 3)
        val vm = HomeViewModel(FakeGameRepository(listOf(finished, open)), FakeEngineAvailability())
        backgroundScope.launch(mainRule.dispatcher) { vm.uiState.collect {} }

        assertEquals(2L, vm.uiState.value.inProgress?.id)
        vm.onResumeGame()
        assertEquals(HomeEvent.OpenGame(2), vm.events.first())
    }

    @Test
    fun `no resume without an unfinished game`() = runTest {
        val vm = HomeViewModel(FakeGameRepository(listOf(GameRecord(id = 1, result = GameResult.DRAW))), FakeEngineAvailability())
        backgroundScope.launch(mainRule.dispatcher) { vm.uiState.collect {} }
        assertNull(vm.uiState.value.inProgress)
    }

    @Test
    fun `history lists games newest first`() = runTest {
        val repository = FakeGameRepository(listOf(GameRecord(id = 1, updatedAt = 1), GameRecord(id = 2, updatedAt = 9)))
        val vm = HistoryViewModel(repository)
        backgroundScope.launch(mainRule.dispatcher) { vm.uiState.collect {} }

        assertEquals(listOf(2L, 1L), vm.uiState.value.games.map { it.id })
    }

    @Test
    fun `deleting removes the game from history`() = runTest {
        val repository = FakeGameRepository(listOf(GameRecord(id = 1), GameRecord(id = 2)))
        val vm = HistoryViewModel(repository)
        backgroundScope.launch(mainRule.dispatcher) { vm.uiState.collect {} }

        vm.onDelete(1)

        assertEquals(listOf(2L), vm.uiState.value.games.map { it.id })
    }

    @Test
    fun `delete all empties the history`() = runTest {
        val repository = FakeGameRepository(listOf(GameRecord(id = 1), GameRecord(id = 2, result = GameResult.DRAW)))
        val vm = HistoryViewModel(repository)
        backgroundScope.launch(mainRule.dispatcher) { vm.uiState.collect {} }

        vm.onDeleteAll()

        assertEquals(emptyList<GameRecord>(), vm.uiState.value.games)
    }

    @Test
    fun `stockfish is the default engine when it is available`() = runTest {
        val repository = FakeGameRepository()
        val vm = HomeViewModel(repository, FakeEngineAvailability(true))
        backgroundScope.launch(mainRule.dispatcher) { vm.uiState.collect {} }

        assertEquals(EngineType.STOCKFISH, vm.uiState.value.settings.engine)
        vm.onStartGame()
        assertEquals(EngineType.STOCKFISH, repository.saved.single().engine)
    }

    @Test
    fun `the built in engine is used when stockfish is unavailable`() = runTest {
        val repository = FakeGameRepository()
        val vm = HomeViewModel(repository, FakeEngineAvailability(false))
        backgroundScope.launch(mainRule.dispatcher) { vm.uiState.collect {} }

        assertEquals(EngineType.BUILT_IN, vm.uiState.value.settings.engine)
        assertEquals(false, vm.uiState.value.stockfishAvailable)
        vm.onStartGame()
        assertEquals(EngineType.BUILT_IN, repository.saved.single().engine)
    }

    @Test
    fun `the player can pick the built in engine`() = runTest {
        val repository = FakeGameRepository()
        val vm = HomeViewModel(repository, FakeEngineAvailability(true))
        backgroundScope.launch(mainRule.dispatcher) { vm.uiState.collect {} }

        vm.onEngineSelected(EngineType.BUILT_IN)
        vm.onStartGame()
        assertEquals(EngineType.BUILT_IN, repository.saved.single().engine)
    }
}
