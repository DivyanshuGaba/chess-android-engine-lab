package com.divyanshu.chess.ui

import com.divyanshu.chess.domain.ai.EngineInfo
import com.divyanshu.chess.domain.model.GameRecord
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.engine.AnalysisUpdate
import com.divyanshu.chess.engine.EngineCrashedException
import com.divyanshu.chess.engine.EngineDetails
import com.divyanshu.chess.engine.PositionAnalyzer
import com.divyanshu.chess.engine.SearchLimits
import com.divyanshu.chess.lab.BenchmarkReport
import com.divyanshu.chess.lab.LabDataSource
import com.divyanshu.chess.lab.TestReport
import com.divyanshu.chess.testutil.FakeEngineAvailability
import com.divyanshu.chess.testutil.FakeGameRepository
import com.divyanshu.chess.testutil.MainDispatcherRule
import com.divyanshu.chess.ui.lab.AnalysisMode
import com.divyanshu.chess.ui.lab.LabViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LabViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private class FakeDetails(name: String? = "TestFish 1") : EngineDetails {
        override val engineName: StateFlow<String?> = MutableStateFlow(name)
        override val binaryPath: String? = "/data/app/libtestfish.so"
        override val abi: String = "arm64-v8a"
    }

    private class FakeLabData(
        private val report: BenchmarkReport? = null,
        private val tests: TestReport? = null,
    ) : LabDataSource {
        override suspend fun benchmarks() = report

        override suspend fun tests() = tests
    }

    private val info = EngineInfo("TestFish 1", 10, 35, null, 5000, 1_000_000, 5, listOf(Move.fromUci("e2e4"), Move.fromUci("e7e5")))

    private fun viewModel(
        analyzer: PositionAnalyzer,
        repository: FakeGameRepository = FakeGameRepository(),
        data: LabDataSource = FakeLabData(),
    ) = LabViewModel(analyzer, repository, FakeEngineAvailability(true), FakeDetails(), data)

    @Test
    fun `engine facts are shown`() = runTest {
        val state = viewModel(PositionAnalyzer { _, _ -> flow { } }).uiState.value
        assertTrue(state.stockfishAvailable)
        assertEquals("TestFish 1", state.engineName)
        assertEquals("arm64-v8a", state.abi)
    }

    @Test
    fun `analysis shows progress then the final line`() = runTest {
        var seenLimits: SearchLimits? = null
        val analyzer = PositionAnalyzer { _, limits ->
            seenLimits = limits
            flow {
                emit(AnalysisUpdate.Progress(info.copy(depth = 5)))
                emit(AnalysisUpdate.Done(info, Move.fromUci("e2e4")))
            }
        }
        val vm = viewModel(analyzer)
        vm.onDepthSelected(12)
        vm.onAnalyze()

        val state = vm.uiState.value
        assertEquals(SearchLimits(depth = 12), seenLimits)
        assertFalse(state.analyzing)
        val line = state.analysis!!
        assertTrue(line.finished)
        assertEquals("e4", line.bestMove)
        assertEquals("+0.35", line.score)
        assertEquals(listOf("e4", "e5"), line.pv)
    }

    @Test
    fun `time mode uses a movetime limit`() = runTest {
        var seenLimits: SearchLimits? = null
        val vm = viewModel(PositionAnalyzer { _, limits -> seenLimits = limits; flow { } })
        vm.onModeSelected(AnalysisMode.TIME)
        vm.onTimeSelected(3_000)
        vm.onAnalyze()
        assertEquals(SearchLimits(movetimeMs = 3_000), seenLimits)
    }

    @Test
    fun `the position comes from the game in progress`() = runTest {
        var analysed: GameState? = null
        val repository = FakeGameRepository(listOf(GameRecord(id = 1, moves = listOf(Move.fromUci("e2e4")))))
        val vm = viewModel(PositionAnalyzer { state, _ -> analysed = state; flow { } }, repository)
        vm.onAnalyze()
        assertEquals(com.divyanshu.chess.domain.model.Side.BLACK, analysed?.sideToMove)
        assertEquals("Current game, 1 moves played", vm.uiState.value.positionLabel)
    }

    @Test
    fun `stopping cancels the running analysis`() = runTest {
        val vm = viewModel(PositionAnalyzer { _, _ -> flow { awaitCancellation() } })
        vm.onAnalyze()
        assertTrue(vm.uiState.value.analyzing)
        vm.onStop()
        assertFalse(vm.uiState.value.analyzing)
    }

    @Test
    fun `an engine error is reported not thrown`() = runTest {
        val vm = viewModel(PositionAnalyzer { _, _ -> flow { throw EngineCrashedException("engine died") } })
        vm.onAnalyze()
        assertEquals("engine died", vm.uiState.value.error)
        assertFalse(vm.uiState.value.analyzing)
    }

    @Test
    fun `no bundled results means nothing is shown`() = runTest {
        val state = viewModel(PositionAnalyzer { _, _ -> flow { } }).uiState.value
        assertTrue(state.dataLoaded)
        assertNull(state.benchmarks)
        assertNull(state.tests)
    }

    @Test
    fun `bundled results are passed through unchanged`() = runTest {
        val report = BenchmarkReport("t", emptyList())
        val tests = TestReport("t", emptyList())
        val state = viewModel(PositionAnalyzer { _, _ -> flow { } }, data = FakeLabData(report, tests)).uiState.value
        assertNotNull(state.benchmarks)
        assertEquals(report, state.benchmarks)
        assertEquals(tests, state.tests)
    }
}
