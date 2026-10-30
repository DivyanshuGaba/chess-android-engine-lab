package com.divyanshu.chess.ui.lab

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.divyanshu.chess.lab.ExperimentResult
import com.divyanshu.chess.ui.components.ScreenHeader
import com.divyanshu.chess.lab.TestReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineLabScreen(viewModel: LabViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLicense by rememberSaveable { mutableStateOf(false) }

    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("lab_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ScreenHeader("Engine lab", subtitle = "Live analysis, benchmarks and tests") }
            item { EngineCard(state) }
            item { LiveAnalysisCard(state, viewModel) }
            item { BenchmarkCard(state) }
            item { TestsCard(state.tests, state.dataLoaded) }
            item { LicenseCard(onShowLicense = { showLicense = true }) }
        }
    }

    if (showLicense) LicenseDialog(onDismiss = { showLicense = false })
}

@Composable
private fun LabCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        content()
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EngineCard(state: LabUiState) {
    LabCard("Engine", "Live status of this device") {
        Fact("Active engine:", if (state.stockfishAvailable) state.engineName ?: "Stockfish (starts on first use)" else "Built-in minimax only")
        Fact("Strength per difficulty:", "Easy 1350 Elo, Medium 1800 Elo, Hard full strength (UCI_Elo)")
        Fact("Search budget per move:", "Easy 0.1 s, Medium 0.3 s, Hard 1 s")
        Fact("Device ABI:", state.abi)
        state.binaryPath?.let { Fact("Binary:", it.substringAfterLast('/')) }
    }
}

@Composable
private fun LiveAnalysisCard(state: LabUiState, viewModel: LabViewModel) {
    LabCard("Live analysis", "Computed right now on this phone. Not a benchmark result.") {
        Text("Position: ${state.positionLabel}", style = MaterialTheme.typography.bodySmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnalysisMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.mode == mode,
                    onClick = { viewModel.onModeSelected(mode) },
                    label = { Text(if (mode == AnalysisMode.DEPTH) "Fixed depth" else "Fixed time") },
                )
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.mode == AnalysisMode.DEPTH) {
                listOf(8, 12, 14, 18, 22).forEach { depth ->
                    FilterChip(selected = state.depth == depth, onClick = { viewModel.onDepthSelected(depth) }, label = { Text("depth $depth") })
                }
            } else {
                listOf(500L, 1_000L, 3_000L, 5_000L).forEach { ms ->
                    FilterChip(selected = state.timeMs == ms, onClick = { viewModel.onTimeSelected(ms) }, label = { Text("${ms / 1000.0} s") })
                }
            }
        }

        if (state.analyzing) {
            OutlinedButton(onClick = viewModel::onStop, modifier = Modifier.fillMaxWidth().testTag("stop_button")) { Text("Stop") }
        } else {
            Button(
                onClick = viewModel::onAnalyze,
                enabled = state.stockfishAvailable,
                modifier = Modifier.fillMaxWidth().testTag("analyze_button"),
            ) { Text(if (state.stockfishAvailable) "Analyse position" else "Stockfish unavailable") }
        }

        if (state.analyzing && state.analysis == null) CircularProgressIndicator(Modifier.padding(4.dp))

        state.error?.let { Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodySmall) }

        state.analysis?.let { line ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag("analysis_result")) {
                Text(line.score, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Fact("Best move:", line.bestMove ?: "searching")
                Fact("Depth:", "${line.depth}" + if (line.finished) "" else " (deepening)")
                Fact("Speed:", "${line.nps / 1000}k nps, ${line.nodes} nodes, ${line.timeMs} ms")
                if (line.pv.isNotEmpty()) {
                    Text("Principal variation", style = MaterialTheme.typography.labelMedium)
                    Text(line.pv.joinToString(" "), style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun BenchmarkCard(state: LabUiState) {
    LabCard("Benchmark results", "Finished experiments, measured offline by engine_lab on a Mac") {
        val report = state.benchmarks
        when {
            !state.dataLoaded -> CircularProgressIndicator(Modifier.padding(4.dp))
            report == null || report.experiments.isEmpty() -> Text(
                "No benchmark results are bundled with this build. Run an experiment, then export it:\n\n" +
                    "engine_lab/run_experiment.sh exp001\npython3 tools/export_dashboard_data.py\n\n" +
                    "and rebuild the app. Nothing on this card is invented.",
                style = MaterialTheme.typography.bodySmall,
            )
            else -> {
                Text("Report generated ${report.generatedAt}", style = MaterialTheme.typography.labelSmall)
                report.experiments.forEach { ExperimentView(it) }
            }
        }
    }
}

@Composable
private fun ExperimentView(result: ExperimentResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp)
            .testTag("experiment_${result.id}"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(result.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(result.status.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        if (result.description.isNotBlank()) Text(result.description, style = MaterialTheme.typography.bodySmall)
        Fact("Baseline:", "${result.baseline.name}, ${result.baseline.limits}")
        Fact("Candidate:", "${result.candidate.name}, ${result.candidate.limits}")
        Fact("Games:", "${result.games} from ${result.pairs} opening pairs")
        Fact("Candidate W/D/L:", "${result.wins} / ${result.draws} / ${result.losses}")
        Fact("Score:", "%.2f%%".format(result.scorePct))
        if (result.ciMethod.isNotBlank() && !result.ciMethod.startsWith("n/a")) {
            val low = result.eloCiLow?.let { "%+.0f".format(it) } ?: "open-ended"
            val high = result.eloCiHigh?.let { "%+.0f".format(it) } ?: "open-ended"
            val diff = result.eloDiff?.let { "%+.0f".format(it) } ?: "off the scale"
            Fact("Elo difference:", "$diff (95%% CI $low to $high, ${result.ciMethod})".replace("%%", "%"))
            val significant = (result.eloCiLow ?: Double.NEGATIVE_INFINITY) > 0 || (result.eloCiHigh ?: Double.POSITIVE_INFINITY) < 0
            Text(
                if (significant) "The interval excludes zero." else "The interval includes zero: no measurable difference.",
                style = MaterialTheme.typography.labelMedium,
                color = if (significant) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text("The interval is not informative for this run.", style = MaterialTheme.typography.labelMedium)
        }
        Fact("Crashes / illegal / timeouts:", "${result.crashes} / ${result.illegalMoves} / ${result.timeouts}")
        result.baselineStats.avgNps?.let { Fact("Avg speed (baseline):", "${it / 1000}k nps") }
        result.candidateStats.avgNps?.let { Fact("Avg speed (candidate):", "${it / 1000}k nps") }
        Fact("Engine:", result.stockfishVersion)
        if (result.reproduce.isNotBlank()) {
            Text(result.reproduce, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun TestsCard(report: TestReport?, loaded: Boolean) {
    LabCard("Automated tests", "Last recorded run, from the generated test summary") {
        when {
            !loaded -> CircularProgressIndicator(Modifier.padding(4.dp))
            report == null -> Text(
                "No test summary is bundled. Run the tests, then python3 tools/export_dashboard_data.py.",
                style = MaterialTheme.typography.bodySmall,
            )
            else -> {
                Text("Recorded ${report.generatedAt}", style = MaterialTheme.typography.labelSmall)
                report.suites.forEach { suite ->
                    Fact("${suite.name}:", "${suite.total} tests, ${suite.failures} failed, ${suite.skipped} skipped")
                }
                Fact("Total:", "${report.suites.sumOf { it.total }} tests, ${report.suites.sumOf { it.failures }} failed")
            }
        }
    }
}

@Composable
private fun LicenseCard(onShowLicense: () -> Unit) {
    LabCard("Stockfish licence", "Free software, GNU GPL version 3") {
        Text(
            "This app bundles the unmodified official Stockfish 19 binary and talks to it over the UCI protocol. " +
                "Stockfish is copyright the Stockfish developers (see its AUTHORS file). The complete corresponding " +
                "source is included in this project under third_party/stockfish and at " +
                "https://github.com/official-stockfish/Stockfish (tag sf_19).",
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onShowLicense, modifier = Modifier.testTag("license_button")) { Text("View GPL-3.0 text") }
    }
}

@Composable
private fun LicenseDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = remember { readAsset(context, "licenses/stockfish-COPYING.txt") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GNU General Public License v3") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(text, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private fun readAsset(context: Context, path: String): String =
    runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }
        .getOrDefault("The licence text could not be loaded.")
