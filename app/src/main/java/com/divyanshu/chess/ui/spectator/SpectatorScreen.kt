package com.divyanshu.chess.ui.spectator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.EngineType
import com.divyanshu.chess.ui.components.ScreenHeader
import com.divyanshu.chess.ui.game.ChessBoard
import com.divyanshu.chess.ui.game.EngineLine

@Composable
fun SpectatorScreen(viewModel: SpectatorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SpectatorContent(
        state = state,
        onWhiteChanged = viewModel::onWhiteChanged,
        onBlackChanged = viewModel::onBlackChanged,
        onSpeedChanged = viewModel::onSpeedChanged,
        onStart = viewModel::onStart,
        onPauseToggled = viewModel::onPauseToggled,
        onReset = viewModel::onReset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpectatorContent(
    state: SpectatorUiState,
    onWhiteChanged: (Contestant) -> Unit,
    onBlackChanged: (Contestant) -> Unit,
    onSpeedChanged: (SpectatorSpeed) -> Unit,
    onStart: () -> Unit,
    onPauseToggled: () -> Unit,
    onReset: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f).height(52.dp).testTag("start_match_button"),
                    ) { Text(if (state.started) "Rematch" else "Start match") }
                    OutlinedButton(
                        onClick = onPauseToggled,
                        enabled = state.started && !state.finished,
                        modifier = Modifier.weight(1f).height(52.dp).testTag("pause_button"),
                    ) { Text(if (state.paused) "Resume" else "Pause") }
                }
                if (state.started) {
                    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth().testTag("reset_button")) { Text("Back to setup") }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader("Watch", subtitle = "Two engines play each other", modifier = Modifier.padding(horizontal = 0.dp))
            DemoNotice()
            ChessBoard(state = state.board, onSquareClick = {})

            state.error?.let { message ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x33E5483B))
                        .padding(14.dp)
                        .testTag("spectator_error"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("The match stopped", style = MaterialTheme.typography.titleMedium)
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }

            state.outcome?.let { outcome ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(14.dp)
                        .testTag("spectator_outcome"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        if (state.stoppedAtLimit) "Stopped at the demo's move limit" else outcome.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        if (state.stoppedAtLimit) "The engines were still playing after ${SpectatorViewModel.MAX_PLIES / 2} moves each, so the demo ended the game." else outcome.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            ContestantCard(
                title = "White",
                contestant = state.white,
                line = state.whiteLine,
                editable = !state.started,
                stockfishAvailable = state.stockfishAvailable,
                onChanged = onWhiteChanged,
                tag = "white_card",
            )
            ContestantCard(
                title = "Black",
                contestant = state.black,
                line = state.blackLine,
                editable = !state.started,
                stockfishAvailable = state.stockfishAvailable,
                onChanged = onBlackChanged,
                tag = "black_card",
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpectatorSpeed.entries.forEach { speed ->
                    FilterChip(
                        selected = state.speed == speed,
                        onClick = { onSpeedChanged(speed) },
                        label = { Text(speed.label) },
                        modifier = Modifier.testTag("speed_${speed.name}"),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DemoNotice() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(12.dp)
            .testTag("demo_notice"),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("Demo, not a benchmark", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text(
            "One game on a phone says nothing about which engine is stronger. Measured results are in the Engine lab.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ContestantCard(
    title: String,
    contestant: Contestant,
    line: EngineLine?,
    editable: Boolean,
    stockfishAvailable: Boolean,
    onChanged: (Contestant) -> Unit,
    tag: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("$title: ${contestant.label}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (editable) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EngineType.entries.reversed().forEach { engine ->
                    FilterChip(
                        selected = contestant.engine == engine,
                        onClick = { onChanged(contestant.copy(engine = engine)) },
                        enabled = engine != EngineType.STOCKFISH || stockfishAvailable,
                        label = { Text(engine.label) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { difficulty ->
                    FilterChip(
                        selected = contestant.difficulty == difficulty,
                        onClick = { onChanged(contestant.copy(difficulty = difficulty)) },
                        label = { Text(difficulty.name.lowercase().replaceFirstChar(Char::titlecase)) },
                    )
                }
            }
        }
        line?.let {
            Text(
                "${it.engineName}  ·  depth ${it.depth}  ·  ${it.score}  ·  ${it.nps / 1000}k nps",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (it.pv.isNotEmpty()) {
                Text(it.pv.joinToString(" "), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}
