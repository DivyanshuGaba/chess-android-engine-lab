package com.divyanshu.chess.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.divyanshu.chess.domain.model.GameMode
import com.divyanshu.chess.domain.model.GameStatus
import com.divyanshu.chess.domain.model.Piece
import com.divyanshu.chess.domain.model.PieceType
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.domain.model.Square
import com.divyanshu.chess.ui.components.ChessIcons
import com.divyanshu.chess.ui.components.EvalBar
import com.divyanshu.chess.ui.theme.ClockTextStyle
import com.divyanshu.chess.ui.theme.Spacing
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    onBack: () -> Unit,
    onLearn: (String) -> Unit,
    onOpenGame: (Long) -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val vibration by viewModel.vibrationEnabled.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAppBackgrounded() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAppForegrounded() }
    LaunchedEffect(viewModel) {
        viewModel.navigation.collect { event ->
            when (event) {
                is GameNavigation.OpenGame -> onOpenGame(event.gameId)
            }
        }
    }

    GameContent(
        state = state,
        clock = clock,
        vibrationEnabled = vibration,
        onVibrationToggled = viewModel::onVibrationToggled,
        onBack = onBack,
        onLearn = onLearn,
        onSquareClick = viewModel::onSquareClick,
        onPromotionChosen = viewModel::onPromotionChosen,
        onPromotionDismissed = viewModel::onPromotionDismissed,
        onUndo = viewModel::onUndo,
        onResign = viewModel::onResign,
        onRematch = viewModel::onRematch,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameContent(
    state: GameUiState,
    clock: ClockUiState,
    vibrationEnabled: Boolean,
    onVibrationToggled: (Boolean) -> Unit,
    onBack: () -> Unit,
    onLearn: (String) -> Unit,
    onSquareClick: (Square) -> Unit,
    onPromotionChosen: (PieceType) -> Unit,
    onPromotionDismissed: () -> Unit,
    onUndo: () -> Unit,
    onResign: () -> Unit,
    onRematch: () -> Unit = {},
) {
    var confirmResign by rememberSaveable { mutableStateOf(false) }
    var dismissedResult by rememberSaveable { mutableStateOf(false) }
    val outcome = outcomeFor(state)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (state.mode == GameMode.LOCAL_TWO_PLAYER) "Two players" else "Against the computer", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconToggleButton(
                        checked = vibrationEnabled,
                        onCheckedChange = onVibrationToggled,
                        modifier = Modifier.testTag("vibration_switch").semantics {
                            contentDescription = if (vibrationEnabled) "Vibration on" else "Vibration off"
                        },
                    ) {
                        Icon(
                            ChessIcons.Vibrate,
                            contentDescription = null,
                            tint = if (vibrationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            if (!state.isLoading) {
                ActionBar(
                    canUndo = state.canUndo,
                    gameOver = state.isGameOver,
                    onUndo = onUndo,
                    onResign = { confirmResign = true },
                    onRematch = onRematch,
                )
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight),
        ) {
            Spacer(Modifier.weight(1f))
            PlayerPanel(state, clock, state.bottomSide.opposite)
            Box {
                ChessBoard(state = state, onSquareClick = onSquareClick)
                EventBanner(
                    event = state.lastEvent,
                    moveCount = state.moveCount,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp),
                )
            }
            PlayerPanel(state, clock, state.bottomSide)

            if (outcome != null && dismissedResult) {
                ResultCard(outcome, onRematch = onRematch, onLearn = { onLearn(outcome.topicId) })
            } else {
                state.engineLine?.let { AnalysisCard(it) }
            }

            Spacer(Modifier.weight(1f))
            MoveStrip(state.moveHistory)
        }
    }

    state.pendingPromotion?.let { pending ->
        PromotionDialog(
            side = pending.side,
            facingOpponent = state.mode == GameMode.LOCAL_TWO_PLAYER && pending.side == Side.BLACK,
            onChosen = onPromotionChosen,
            onDismiss = onPromotionDismissed,
        )
    }

    if (confirmResign) {
        Dialog(onDismissRequest = { confirmResign = false }) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(Spacing.gap)) {
                    Text("Resign this game?", style = MaterialTheme.typography.titleLarge)
                    Text("Your opponent will be awarded the win.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { confirmResign = false }) { Text("Keep playing") }
                        TextButton(onClick = { confirmResign = false; onResign() }, modifier = Modifier.testTag("confirm_resign")) { Text("Resign") }
                    }
                }
            }
        }
    }

    if (outcome != null && !dismissedResult) {
        GameOverDialog(
            outcome = outcome,
            onRematch = onRematch,
            onLearn = { onLearn(outcome.topicId) },
            onViewBoard = { dismissedResult = true },
            onMenu = onBack,
        )
    }
}

@Composable
private fun ActionBar(canUndo: Boolean, gameOver: Boolean, onUndo: () -> Unit, onResign: () -> Unit, onRematch: () -> Unit) {
    Surface(color = Color.Transparent) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.gap),
            horizontalArrangement = Arrangement.spacedBy(Spacing.gap),
        ) {
            FilledTonalButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.weight(1f).height(52.dp).testTag("undo_button"),
            ) {
                Icon(ChessIcons.Undo, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text("Undo")
            }
            if (gameOver) {
                Button(onClick = onRematch, modifier = Modifier.weight(1f).height(52.dp).testTag("rematch_button")) { Text("Play again") }
            } else {
                FilledTonalButton(
                    onClick = onResign,
                    modifier = Modifier.weight(1f).height(52.dp).testTag("resign_button"),
                ) {
                    Icon(ChessIcons.Flag, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Resign")
                }
            }
        }
    }
}

@Composable
private fun PlayerPanel(state: GameUiState, clock: ClockUiState, side: Side) {
    val scheme = MaterialTheme.colorScheme
    val capturedTypes = if (side == Side.WHITE) state.capturedByWhite else state.capturedByBlack
    val twoPlayer = state.mode == GameMode.LOCAL_TWO_PLAYER
    val active = state.sideToMove == side && !state.isGameOver
    val name = when {
        twoPlayer -> if (side == Side.WHITE) "White" else "Black"
        side == state.bottomSide -> "You"
        else -> "Computer"
    }
    val subtitle = when {
        !active -> ""
        state.isThinking -> "Thinking"
        state.status == GameStatus.CHECK -> "In check"
        else -> "To move"
    }
    val facing = if (twoPlayer && side == Side.BLACK) Modifier.rotate(180f) else Modifier
    val seconds = if (side == Side.WHITE) clock.whiteSeconds else clock.blackSeconds
    val shape = MaterialTheme.shapes.large

    Row(
        modifier = facing
            .fillMaxWidth()
            .clip(shape)
            .background(if (active) scheme.surfaceContainerHigh else scheme.surfaceContainer)
            .then(if (active) Modifier.border(1.5.dp, scheme.primary, shape) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        if (side == Side.WHITE) listOf(Color(0xFFF8F1E2), Color(0xFFC9BC9F)) else listOf(Color(0xFF55565E), Color(0xFF17171B)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            PieceView(Piece(PieceType.KNIGHT, side), Modifier.size(28.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                if (state.isThinking && active) CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp, color = scheme.primary)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.status == GameStatus.CHECK && active) Color(0xFFD9534F) else scheme.primary,
                )
            }
            Row(Modifier.height(20.dp), horizontalArrangement = Arrangement.spacedBy((-9).dp)) {
                capturedTypes.forEach { type -> PieceView(Piece(type, side.opposite), Modifier.size(20.dp)) }
            }
        }
        ClockChip(seconds = seconds, running = clock.running == side, side = side)
    }
}

@Composable
private fun ClockChip(seconds: Int, running: Boolean, side: Side) {
    val scheme = MaterialTheme.colorScheme
    val low = seconds <= LOW_TIME_SECONDS
    val (background, content) = when {
        running && low -> Color(0xFFB3261E) to Color.White
        running -> scheme.primary to scheme.onPrimary
        else -> scheme.surfaceContainerHighest to scheme.onSurface
    }
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("clock_${side.name.lowercase()}")
            .semantics(mergeDescendants = true) { contentDescription = "${side.name.lowercase()} clock ${formatClock(seconds)}" },
        contentAlignment = Alignment.Center,
    ) {
        Text(formatClock(seconds), style = MaterialTheme.typography.titleLarge.merge(ClockTextStyle), fontWeight = FontWeight.SemiBold, color = content)
    }
}

private const val LOW_TIME_SECONDS = 30

@Composable
private fun AnalysisCard(line: EngineLine) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("engine_line"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${line.engineName}  ·  depth ${line.depth}  ·  ${formatNps(line.nps)}  ·  ${"%.1f".format(line.timeMs / 1000.0)} s",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Text(line.score, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        EvalBar(line.scoreCp, line.mateIn)
        if (line.pv.isNotEmpty()) {
            Text(
                text = line.pv.joinToString(" "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

private fun formatNps(nps: Long): String = when {
    nps >= 1_000_000 -> "%.1fM nps".format(nps / 1_000_000.0)
    nps >= 1_000 -> "${nps / 1_000}k nps"
    else -> "$nps nps"
}

@Composable
private fun MoveStrip(moves: List<String>) {
    val listState = rememberLazyListState()
    LaunchedEffect(moves.size) {
        if (moves.isNotEmpty()) listState.animateScrollToItem(moves.lastIndex)
    }
    if (moves.isEmpty()) {
        Text(
            "Tap a piece to see where it can go",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().height(32.dp).padding(top = 6.dp),
        )
        return
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(moves.size) { index ->
            val last = index == moves.lastIndex
            val prefix = if (index % 2 == 0) "${index / 2 + 1}. " else ""
            Text(
                text = "$prefix${moves[index]}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (last) FontWeight.SemiBold else FontWeight.Normal,
                color = if (last) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (last) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ResultCard(outcome: Outcome, onRematch: () -> Unit, onLearn: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(14.dp)
            .testTag("status_text"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(outcome.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(outcome.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Row {
            TextButton(onClick = onLearn) { Text("Learn why") }
            TextButton(onClick = onRematch) { Text("Play again") }
        }
    }
}

@Composable
private fun GameOverDialog(outcome: Outcome, onRematch: () -> Unit, onLearn: () -> Unit, onViewBoard: () -> Unit, onMenu: () -> Unit) {
    Dialog(onDismissRequest = onViewBoard) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.testTag("game_over_dialog"),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(Spacing.gap)) {
                Text(outcome.title, style = MaterialTheme.typography.headlineSmall)
                Text(outcome.explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onRematch, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("dialog_rematch")) { Text("Play again") }
                FilledTonalButton(onClick = onLearn, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Learn why") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onViewBoard) { Text("View board") }
                    TextButton(onClick = onMenu) { Text("Menu") }
                }
            }
        }
    }
}

@Composable
private fun EventBanner(event: GameEvent?, moveCount: Int, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf<GameEvent?>(null) }

    LaunchedEffect(moveCount, event) {
        if (event != null) {
            shown = event
            visible = true
            delay(EVENT_BANNER_MS)
        }
        visible = false
    }

    AnimatedVisibility(visible = visible, modifier = modifier, enter = fadeIn() + scaleIn(initialScale = 0.8f), exit = fadeOut()) {
        val alert = shown == GameEvent.CHECK
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (alert) Color(0xFFB3261E) else MaterialTheme.colorScheme.primary,
            shadowElevation = 6.dp,
        ) {
            Text(
                text = when (shown) {
                    GameEvent.CHECK -> "Check!"
                    GameEvent.CASTLING -> "Castling"
                    GameEvent.EN_PASSANT -> "En passant"
                    GameEvent.PROMOTION -> "Promotion"
                    null -> ""
                },
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp).testTag("event_banner"),
                style = MaterialTheme.typography.titleMedium,
                color = if (alert) Color.White else MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

private const val EVENT_BANNER_MS = 1700L

@Composable
private fun PromotionDialog(side: Side, facingOpponent: Boolean, onChosen: (PieceType) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = if (facingOpponent) Modifier.rotate(180f) else Modifier,
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Promote pawn", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PieceType.promotions.forEach { type ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable { onChosen(type) }
                                .testTag("promote_${type.name}"),
                            contentAlignment = Alignment.Center,
                        ) { PieceView(Piece(type, side), Modifier.size(52.dp)) }
                    }
                }
            }
        }
    }
}
