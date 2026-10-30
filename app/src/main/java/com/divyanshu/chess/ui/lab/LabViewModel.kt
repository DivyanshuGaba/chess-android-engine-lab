package com.divyanshu.chess.ui.lab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyanshu.chess.domain.ai.EngineAvailability
import com.divyanshu.chess.domain.ai.EngineInfo
import com.divyanshu.chess.domain.ai.PvFormatter
import com.divyanshu.chess.domain.engine.San
import com.divyanshu.chess.domain.model.Game
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.repository.GameRepository
import com.divyanshu.chess.engine.AnalysisUpdate
import com.divyanshu.chess.engine.EngineDetails
import com.divyanshu.chess.engine.EngineException
import com.divyanshu.chess.engine.PositionAnalyzer
import com.divyanshu.chess.engine.SearchLimits
import com.divyanshu.chess.lab.BenchmarkReport
import com.divyanshu.chess.lab.LabDataSource
import com.divyanshu.chess.lab.TestReport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AnalysisMode { DEPTH, TIME }

data class AnalysisLine(
    val engineName: String,
    val depth: Int,
    val score: String,
    val bestMove: String?,
    val pv: List<String>,
    val nps: Long,
    val nodes: Long,
    val timeMs: Long,
    val finished: Boolean,
)

data class LabUiState(
    val stockfishAvailable: Boolean = false,
    val engineName: String? = null,
    val binaryPath: String? = null,
    val abi: String = "",
    val mode: AnalysisMode = AnalysisMode.DEPTH,
    val depth: Int = 14,
    val timeMs: Long = 1_000,
    val positionLabel: String = "Starting position",
    val analyzing: Boolean = false,
    val analysis: AnalysisLine? = null,
    val error: String? = null,
    val benchmarks: BenchmarkReport? = null,
    val tests: TestReport? = null,
    val dataLoaded: Boolean = false,
)

@HiltViewModel
class LabViewModel @Inject constructor(
    private val analyzer: PositionAnalyzer,
    private val repository: GameRepository,
    private val availability: EngineAvailability,
    private val details: EngineDetails,
    private val labData: LabDataSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LabUiState(
            stockfishAvailable = availability.isStockfishAvailable,
            engineName = details.engineName.value,
            binaryPath = details.binaryPath,
            abi = details.abi,
        ),
    )
    val uiState: StateFlow<LabUiState> = _uiState.asStateFlow()

    private var analysisJob: Job? = null

    init {
        viewModelScope.launch {
            val benchmarks = labData.benchmarks()
            val tests = labData.tests()
            _uiState.update { it.copy(benchmarks = benchmarks, tests = tests, dataLoaded = true) }
        }
        viewModelScope.launch {
            details.engineName.collect { name -> _uiState.update { it.copy(engineName = name) } }
        }
    }

    fun onModeSelected(mode: AnalysisMode) = _uiState.update { it.copy(mode = mode) }

    fun onDepthSelected(depth: Int) = _uiState.update { it.copy(depth = depth) }

    fun onTimeSelected(timeMs: Long) = _uiState.update { it.copy(timeMs = timeMs) }

    fun onAnalyze() {
        if (analysisJob?.isActive == true) return
        analysisJob = viewModelScope.launch {
            val current = _uiState.value
            val inProgress = repository.observeInProgress().first()
            val state = inProgress?.let { Game.fromMoves(it.moves).state } ?: GameState.initial()
            val limits = when (current.mode) {
                AnalysisMode.DEPTH -> SearchLimits(depth = current.depth)
                AnalysisMode.TIME -> SearchLimits(movetimeMs = current.timeMs)
            }
            _uiState.update {
                it.copy(
                    analyzing = true,
                    analysis = null,
                    error = null,
                    positionLabel = inProgress?.let { game -> "Current game, ${game.moves.size} moves played" }
                        ?: "Starting position",
                )
            }
            try {
                analyzer.analyze(state, limits).collect { update ->
                    val line = when (update) {
                        is AnalysisUpdate.Progress -> lineFor(state, update.info, null, finished = false)
                        is AnalysisUpdate.Done -> update.info?.let { lineFor(state, it, update.bestMove, finished = true) }
                    }
                    if (line != null) _uiState.update { it.copy(analysis = line) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: EngineException) {
                _uiState.update { it.copy(error = e.message ?: "The engine reported an error") }
            } finally {
                _uiState.update { it.copy(analyzing = false) }
            }
        }
    }

    fun onStop() {
        analysisJob?.cancel()
    }

    private fun lineFor(state: GameState, info: EngineInfo, best: Move?, finished: Boolean): AnalysisLine {
        val pv = PvFormatter.toSan(state, info.pv, maxMoves = 10)
        return AnalysisLine(
            engineName = info.engineName,
            depth = info.depth,
            score = PvFormatter.formatScore(info.scoreCp, info.mateIn),
            bestMove = (best ?: info.pv.firstOrNull())?.let { San.format(state, it) },
            pv = pv,
            nps = info.nps,
            nodes = info.nodes,
            timeMs = info.timeMs,
            finished = finished,
        )
    }

    override fun onCleared() {
        analysisJob?.cancel()
    }
}
