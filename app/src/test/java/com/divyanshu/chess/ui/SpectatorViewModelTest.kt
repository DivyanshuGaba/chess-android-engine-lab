package com.divyanshu.chess.ui

import com.divyanshu.chess.domain.ai.AiProvider
import com.divyanshu.chess.domain.ai.ChessAi
import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.EngineType
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.testutil.FakeAi
import com.divyanshu.chess.testutil.FakeEngineAvailability
import com.divyanshu.chess.testutil.MainDispatcherRule
import com.divyanshu.chess.ui.spectator.Contestant
import com.divyanshu.chess.ui.spectator.SpectatorSpeed
import com.divyanshu.chess.ui.spectator.SpectatorViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpectatorViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    /** White is the built-in engine and Black is Stockfish, so each side can get its own script. */
    private class ScriptedProvider(white: List<String>, black: List<String>) : AiProvider {
        private val whiteMoves = ArrayDeque(white)
        private val blackMoves = ArrayDeque(black)

        override fun create(engine: EngineType, difficulty: Difficulty): ChessAi {
            val queue = if (engine == EngineType.STOCKFISH) blackMoves else whiteMoves
            return FakeAi { queue.removeFirstOrNull()?.let(Move::fromUci) }
        }
    }

    private fun foolsMate() = ScriptedProvider(listOf("f2f3", "g2g4"), listOf("e7e5", "d8h4"))

    private fun viewModel(provider: AiProvider = foolsMate(), stockfish: Boolean = true) =
        SpectatorViewModel(provider, FakeEngineAvailability(stockfish), mainRule.dispatcher)

    private fun TestScope.moves(vm: SpectatorViewModel) = vm.uiState.value.board.moveCount

    @Test
    fun `the default matchup is the built-in engine against stockfish`() = runTest {
        val state = viewModel().uiState.value
        assertEquals(EngineType.BUILT_IN, state.white.engine)
        assertEquals(EngineType.STOCKFISH, state.black.engine)
        assertFalse(state.started)
    }

    @Test
    fun `the setup screen already shows the starting position`() = runTest {
        val board = viewModel().uiState.value.board
        assertEquals(32, board.pieces.size)
        assertEquals(0, board.moveCount)
    }

    @Test
    fun `without stockfish both sides are built-in`() = runTest {
        val state = viewModel(stockfish = false).uiState.value
        assertEquals(EngineType.BUILT_IN, state.black.engine)
        assertFalse(state.stockfishAvailable)
    }

    @Test
    fun `a scripted game is played to checkmate and explained`() = runTest {
        val vm = viewModel()
        vm.onStart()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.finished)
        assertEquals(listOf("f3", "e5", "g4", "Qh4#"), state.board.moveHistory)
        assertEquals("Checkmate!", state.outcome?.title)
        assertFalse(state.stoppedAtLimit)
    }

    @Test
    fun `each move waits for the chosen speed`() = runTest {
        val vm = viewModel()
        vm.onSpeedChanged(SpectatorSpeed.SLOW)
        vm.onStart()
        runCurrent()
        assertEquals(1, moves(vm))

        advanceTimeBy(1_400)
        assertEquals(1, moves(vm))
        advanceTimeBy(200)
        assertEquals(2, moves(vm))
    }

    @Test
    fun `pausing holds the game and resuming continues it`() = runTest {
        val vm = viewModel()
        vm.onStart()
        runCurrent()
        vm.onPauseToggled()
        assertTrue(vm.uiState.value.paused)

        advanceTimeBy(10_000)
        assertEquals(1, moves(vm))

        vm.onPauseToggled()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.finished)
    }

    @Test
    fun `the matchup cannot change once the match has started`() = runTest {
        val vm = viewModel()
        vm.onStart()
        runCurrent()
        vm.onWhiteChanged(Contestant(EngineType.STOCKFISH, Difficulty.EASY))
        assertEquals(EngineType.BUILT_IN, vm.uiState.value.white.engine)
    }

    @Test
    fun `the matchup can be chosen before the match starts`() = runTest {
        val vm = viewModel()
        vm.onWhiteChanged(Contestant(EngineType.STOCKFISH, Difficulty.HARD))
        assertEquals(Contestant(EngineType.STOCKFISH, Difficulty.HARD), vm.uiState.value.white)
    }

    @Test
    fun `resetting returns to setup with a fresh board`() = runTest {
        val vm = viewModel()
        vm.onStart()
        runCurrent()
        vm.onReset()
        advanceTimeBy(10_000)

        val state = vm.uiState.value
        assertFalse(state.started)
        assertEquals(0, state.board.moveCount)
        assertEquals(32, state.board.pieces.size)
        assertNull(state.outcome)
    }

    @Test
    fun `a rematch starts again from the opening position`() = runTest {
        val vm = viewModel(ScriptedProvider(listOf("f2f3", "g2g4", "e2e4"), listOf("e7e5", "d8h4", "e7e5")))
        vm.onStart()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.finished)

        vm.onStart()
        runCurrent()
        assertFalse(vm.uiState.value.finished)
        assertEquals(1, moves(vm))
    }

    @Test
    fun `engine lines are shown for the side that moved`() = runTest {
        val info = com.divyanshu.chess.domain.ai.EngineInfo("TestFish", 9, 25, null, 100, 50_000, 5, listOf(Move.fromUci("f2f3")))
        val ai = object : ChessAi {
            override suspend fun chooseMove(state: com.divyanshu.chess.domain.model.GameState) = Move.fromUci("f2f3")
            override suspend fun search(state: com.divyanshu.chess.domain.model.GameState) =
                com.divyanshu.chess.domain.ai.SearchResult(Move.fromUci("f2f3"), info)
        }
        val vm = SpectatorViewModel({ _, _ -> ai }, FakeEngineAvailability(true), mainRule.dispatcher)
        vm.onStart()
        runCurrent()

        val line = vm.uiState.value.whiteLine
        assertNotNull(line)
        assertEquals("TestFish", line?.engineName)
        assertEquals("+0.25", line?.score)
        assertNull(vm.uiState.value.blackLine)
    }

    @Test
    fun `an illegal engine move stops the demo with a message instead of crashing`() = runTest {
        val vm = viewModel(ScriptedProvider(listOf("e2e5"), emptyList()))
        vm.onStart()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.finished)
        assertNull(state.outcome)
        assertTrue(state.error!!.contains("illegal move (e2e5)"))
        assertEquals(0, state.board.moveCount)
    }

    @Test
    fun `an engine with no move ends the demo without an error`() = runTest {
        val vm = viewModel(ScriptedProvider(emptyList(), emptyList()))
        vm.onStart()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.finished)
        assertNull(vm.uiState.value.error)
    }
}
