package com.divyanshu.chess.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.divyanshu.chess.domain.model.GameResult
import com.divyanshu.chess.domain.model.GameState
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.Square
import com.divyanshu.chess.ui.game.PieceView
import com.divyanshu.chess.ui.theme.BoardColors
import com.divyanshu.chess.ui.theme.Spacing
import kotlin.math.tanh

/** A rounded card on the theme's surface colour; clickable when [onClick] is given. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(Spacing.card),
        verticalArrangement = Arrangement.spacedBy(Spacing.gap),
        content = content,
    )
}

/** Big screen title with optional actions on the right. */
@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = Spacing.screen + 4.dp, end = Spacing.tight, top = Spacing.gap, bottom = Spacing.tight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        actions()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
fun IconAction(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = description)
    }
}

/** A read-only board for thumbnails: shows the pieces of [state] and tints the squares of [lastMove]. */
@Composable
fun StaticBoard(state: GameState, modifier: Modifier = Modifier, lastMove: Move? = null, cornerRadius: Dp = 10.dp) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(BoardColors.frame)
            .padding(2.dp)
            .clip(RoundedCornerShape(cornerRadius / 2)),
    ) {
        for (row in 0..7) {
            Row(Modifier.weight(1f)) {
                for (col in 0..7) {
                    val square = Square.of(col, 7 - row)
                    val light = (square.file + square.rank) % 2 == 1
                    val base = if (light) BoardColors.light else BoardColors.dark
                    val tinted = if (square == lastMove?.from || square == lastMove?.to) BoardColors.lastMove.compositeOver(base) else base
                    Box(Modifier.weight(1f).fillMaxSize().background(tinted)) {
                        state.board[square]?.let { PieceView(it, Modifier.fillMaxSize()) }
                    }
                }
            }
        }
    }
}

/** A thin bar showing who is ahead: White's share fills from the left. Scores are from White's point of view. */
@Composable
fun EvalBar(scoreCp: Int?, mateIn: Int?, modifier: Modifier = Modifier) {
    val target = when {
        mateIn != null -> if (mateIn > 0) 1f else 0f
        scoreCp != null -> (0.5 + tanh(scoreCp / 400.0) / 2).toFloat()
        else -> 0.5f
    }
    val share by animateFloatAsState(target, label = "eval")
    val description = when {
        mateIn != null -> "Evaluation: mate in ${kotlin.math.abs(mateIn)} for ${if (mateIn > 0) "White" else "Black"}"
        scoreCp != null -> "Evaluation: ${if (scoreCp >= 0) "White" else "Black"} ahead by ${"%.1f".format(kotlin.math.abs(scoreCp) / 100.0)} pawns"
        else -> "Evaluation unavailable"
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(Color(0xFF2A2D2B))
            .semantics { contentDescription = description },
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(share).background(Color(0xFFF1EBDD)))
    }
}

/** A small pill that states a game's result. */
@Composable
fun ResultBadge(result: GameResult?, modifier: Modifier = Modifier) {
    val (label, container, content) = when (result) {
        GameResult.WHITE_WINS -> Triple("White won", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        GameResult.BLACK_WINS -> Triple("Black won", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        GameResult.DRAW -> Triple("Draw", MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurface)
        null -> Triple("In progress", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = content,
        modifier = modifier.clip(CircleShape).background(container).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
