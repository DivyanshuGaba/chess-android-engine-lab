package com.divyanshu.chess.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.divyanshu.chess.domain.model.Difficulty
import com.divyanshu.chess.domain.model.EngineType
import com.divyanshu.chess.domain.model.Game
import com.divyanshu.chess.domain.model.GameMode
import com.divyanshu.chess.domain.model.GameRecord
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.ui.components.ChessIcons
import com.divyanshu.chess.ui.components.IconAction
import com.divyanshu.chess.ui.components.ScreenHeader
import com.divyanshu.chess.ui.components.SectionLabel
import com.divyanshu.chess.ui.components.StaticBoard
import com.divyanshu.chess.ui.components.SurfaceCard
import com.divyanshu.chess.ui.settings.SettingsSheet
import com.divyanshu.chess.ui.settings.SettingsViewModel
import com.divyanshu.chess.ui.theme.Spacing

@Composable
fun HomeScreen(
    onOpenGame: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.OpenGame -> onOpenGame(event.gameId)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                Button(
                    onClick = viewModel::onStartGame,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.gap).height(56.dp).testTag("start_button"),
                ) { Text(startLabel(state.settings), style = MaterialTheme.typography.titleMedium) }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.gap),
        ) {
            ScreenHeader(
                title = "Play",
                subtitle = "Stockfish, the built-in engine, or a friend",
                actions = { IconAction(ChessIcons.Gear, "Settings", { showSettings = true }, Modifier.testTag("settings_button")) },
            )

            Column(Modifier.padding(horizontal = Spacing.screen), verticalArrangement = Arrangement.spacedBy(Spacing.gap)) {
                state.inProgress?.let { ContinueCard(it, onContinue = viewModel::onResumeGame) }

                SectionLabel("New game", Modifier.padding(top = Spacing.tight))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.gap)) {
                    ModeTile(
                        icon = ChessIcons.Computer,
                        title = "Computer",
                        subtitle = "Play an engine",
                        selected = state.settings.mode == GameMode.VS_COMPUTER,
                        onClick = { viewModel.onModeSelected(GameMode.VS_COMPUTER) },
                        modifier = Modifier.weight(1f).testTag("mode_computer"),
                    )
                    ModeTile(
                        icon = ChessIcons.People,
                        title = "Two players",
                        subtitle = "Share this phone",
                        selected = state.settings.mode == GameMode.LOCAL_TWO_PLAYER,
                        onClick = { viewModel.onModeSelected(GameMode.LOCAL_TWO_PLAYER) },
                        modifier = Modifier.weight(1f).testTag("mode_two_players"),
                    )
                }

                if (state.settings.mode == GameMode.VS_COMPUTER) {
                    SurfaceCard {
                        SettingRow("You play") {
                            Options(
                                options = Side.entries,
                                selected = state.settings.humanSide,
                                label = { if (it == Side.WHITE) "White" else "Black" },
                                onSelected = viewModel::onSideSelected,
                            )
                        }
                        SettingRow("Opponent") {
                            Options(
                                options = EngineType.entries.reversed(),
                                selected = state.settings.engine,
                                label = { it.label },
                                onSelected = viewModel::onEngineSelected,
                                isEnabled = { it != EngineType.STOCKFISH || state.stockfishAvailable },
                            )
                            if (!state.stockfishAvailable) {
                                Hint("Stockfish needs Android 10 or newer on a 64-bit ARM phone, so the built-in engine is used here.")
                            }
                        }
                        SettingRow("Level") {
                            Options(
                                options = Difficulty.entries,
                                selected = state.settings.difficulty,
                                label = { it.name.lowercase().replaceFirstChar(Char::titlecase) },
                                onSelected = viewModel::onDifficultySelected,
                            )
                            Hint(difficultyHint(state.settings.engine, state.settings.difficulty), Modifier.testTag("difficulty_hint"))
                        }
                    }
                } else {
                    SurfaceCard {
                        Text("How it works", style = MaterialTheme.typography.titleMedium)
                        Hint("White sits at the bottom. Black's panel faces the other player, so you can sit opposite each other. Each side has 10 minutes.")
                    }
                }
                Spacer(Modifier.height(Spacing.tight))
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            state = settings,
            onVibrationToggled = settingsViewModel::onVibrationToggled,
            onThemeSelected = settingsViewModel::onThemeSelected,
            onDismiss = { showSettings = false },
        )
    }
}

private fun startLabel(settings: NewGameSettings) = if (settings.mode == GameMode.VS_COMPUTER) "Start game" else "Start two-player game"

@Composable
private fun ContinueCard(record: GameRecord, onContinue: () -> Unit) {
    val game = remember(record.moves) { Game.fromMoves(record.moves) }
    SurfaceCard(modifier = Modifier.testTag("continue_card")) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.card), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            StaticBoard(game.state, Modifier.size(104.dp), lastMove = game.lastMove)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionLabel("Continue")
                Text(opponentLabel(record), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${record.moves.size} moves, ${if (game.state.sideToMove == Side.WHITE) "White" else "Black"} to move",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.tight))
                Button(onClick = onContinue, modifier = Modifier.testTag("resume_button")) { Text("Continue") }
            }
        }
    }
}

private fun opponentLabel(record: GameRecord): String = when (record.mode) {
    GameMode.LOCAL_TWO_PLAYER -> "Two players"
    GameMode.VS_COMPUTER -> "${record.engine.label}, ${record.difficulty.name.lowercase()}"
}

@Composable
private fun ModeTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) scheme.primaryContainer else scheme.surfaceContainer)
            .border(2.dp, if (selected) scheme.primary else Color.Transparent, shape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(Spacing.card),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) scheme.onPrimaryContainer else scheme.primary, modifier = Modifier.size(28.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = if (selected) scheme.onPrimaryContainer else scheme.onSurface)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingRow(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
private fun <T> Options(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    isEnabled: (T) -> Boolean = { true },
) {
    val scheme = MaterialTheme.colorScheme
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelected(option) },
                enabled = isEnabled(option),
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = scheme.primary,
                    activeContentColor = scheme.onPrimary,
                    activeBorderColor = scheme.primary,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = scheme.onSurface,
                    inactiveBorderColor = scheme.outlineVariant,
                ),
            ) { Text(label(option), fontWeight = FontWeight.Medium) }
        }
    }
}

private fun difficultyHint(engine: EngineType, difficulty: Difficulty): String = when (engine) {
    EngineType.STOCKFISH -> when (difficulty) {
        Difficulty.EASY -> "Stockfish limited to about 1350 Elo, 0.1 s per move"
        Difficulty.MEDIUM -> "Stockfish limited to about 1800 Elo, 0.3 s per move"
        Difficulty.HARD -> "Stockfish at full strength, 1 s per move"
    }
    EngineType.BUILT_IN -> when (difficulty) {
        Difficulty.EASY -> "Built-in engine, searches 1 move ahead and plays loosely"
        Difficulty.MEDIUM -> "Built-in engine, searches 3 moves ahead"
        Difficulty.HARD -> "Built-in engine, improved search, 0.3 s per move"
    }
}
