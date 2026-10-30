package com.divyanshu.chess.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.divyanshu.chess.domain.model.Game
import com.divyanshu.chess.domain.model.GameMode
import com.divyanshu.chess.domain.model.GameRecord
import com.divyanshu.chess.ui.components.ChessIcons
import com.divyanshu.chess.ui.components.ResultBadge
import com.divyanshu.chess.ui.components.ScreenHeader
import com.divyanshu.chess.ui.components.StaticBoard
import com.divyanshu.chess.ui.components.SurfaceCard
import com.divyanshu.chess.ui.theme.Spacing

@Composable
fun HistoryScreen(onOpenGame: (Long) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDeleteAll by rememberSaveable { mutableStateOf(false) }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(
                title = "History",
                subtitle = if (state.games.isEmpty()) null else "${state.games.size} saved games",
                actions = {
                    if (state.games.isNotEmpty()) {
                        TextButton(onClick = { confirmDeleteAll = true }, modifier = Modifier.testTag("delete_all_button")) { Text("Delete all") }
                    }
                },
            )
            if (!state.isLoading && state.games.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                        Icon(ChessIcons.History, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No games yet", style = MaterialTheme.typography.titleMedium)
                        Text("Finished and unfinished games show up here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.tight),
                    verticalArrangement = Arrangement.spacedBy(Spacing.gap),
                ) {
                    items(state.games, key = { it.id }) { game ->
                        GameCard(game, onOpen = { onOpenGame(game.id) }, onDelete = { viewModel.onDelete(game.id) })
                    }
                }
            }
        }
    }

    if (confirmDeleteAll) {
        Dialog(onDismissRequest = { confirmDeleteAll = false }) {
            androidx.compose.material3.Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(Spacing.gap)) {
                    Text("Delete all games?", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "This removes ${state.games.size} saved games, including any in progress. It can't be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(Modifier.fillMaxSize().padding(0.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") }
                        TextButton(onClick = { confirmDeleteAll = false; viewModel.onDeleteAll() }) { Text("Delete all") }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCard(game: GameRecord, onOpen: () -> Unit, onDelete: () -> Unit) {
    val position = remember(game.moves) { Game.fromMoves(game.moves) }
    SurfaceCard(onClick = onOpen, modifier = Modifier.testTag("game_${game.id}")) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.card), verticalAlignment = Alignment.CenterVertically) {
            StaticBoard(position.state, Modifier.size(72.dp), lastMove = position.lastMove, cornerRadius = 8.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title(game), style = MaterialTheme.typography.titleMedium)
                Text("${game.moves.size} moves", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ResultBadge(game.result)
            }
            IconButton(onClick = onDelete) {
                Icon(ChessIcons.Trash, contentDescription = "Delete game", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun title(game: GameRecord): String = when (game.mode) {
    GameMode.LOCAL_TWO_PLAYER -> "Two players"
    GameMode.VS_COMPUTER -> "${game.engine.label}, ${game.difficulty.name.lowercase()}"
}
