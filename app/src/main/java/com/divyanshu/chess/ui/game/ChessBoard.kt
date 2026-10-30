package com.divyanshu.chess.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.divyanshu.chess.domain.model.Piece
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.domain.model.Square
import com.divyanshu.chess.ui.theme.BoardColors
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private const val MOVE_ANIMATION_MS = 260

@Composable
fun ChessBoard(
    state: GameUiState,
    onSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier,
) {
    val frameShape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(16.dp, frameShape)
            .clip(frameShape)
            .background(BoardColors.frame)
            .padding(6.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
            val cellDp = maxWidth / 8
            val cellPx = with(LocalDensity.current) { cellDp.toPx() }
            val whiteAtBottom = state.bottomSide == Side.WHITE

            fun offsetOf(square: Square): Offset {
                val column = if (whiteAtBottom) square.file else 7 - square.file
                val row = if (whiteAtBottom) 7 - square.rank else square.rank
                return Offset(column * cellPx, row * cellPx)
            }

            Column(Modifier.fillMaxSize()) {
                for (row in 0..7) {
                    Row(Modifier.weight(1f)) {
                        for (col in 0..7) {
                            val square = if (whiteAtBottom) Square.of(col, 7 - row) else Square.of(7 - col, row)
                            Box(Modifier.weight(1f)) {
                                BoardCell(
                                    square = square,
                                    state = state,
                                    showFile = row == 7,
                                    showRank = col == 0,
                                    onClick = { onSquareClick(square) },
                                )
                            }
                        }
                    }
                }
            }

            state.lastCapture?.let { captured ->
                key(state.moveCount) {
                    CapturedGhost(captured.piece, offsetOf(captured.square), cellDp)
                }
            }

            state.pieces.forEach { tracked ->
                key(tracked.id) {
                    AnimatedPiece(
                        tracked = tracked,
                        target = offsetOf(tracked.square),
                        cellDp = cellDp,
                        selected = tracked.square == state.selected,
                        justMoved = tracked.id == state.lastMovedId,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardCell(
    square: Square,
    state: GameUiState,
    showFile: Boolean,
    showRank: Boolean,
    onClick: () -> Unit,
) {
    val isLight = (square.file + square.rank) % 2 == 1
    val base = if (isLight) BoardColors.light else BoardColors.dark
    val lastMove = state.lastMove
    val background = when {
        square == state.checkedKing -> BoardColors.check.compositeOver(base)
        square == state.selected -> BoardColors.selected.compositeOver(base)
        square == lastMove?.from || square == lastMove?.to -> BoardColors.lastMove.compositeOver(base)
        else -> base
    }
    val piece = state.board[square.index]
    val label = buildString {
        append(square)
        piece?.let { append(", ").append(describe(it)) }
        if (square in state.legalTargets) append(", legal move")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .testTag("square_$square")
            .semantics { contentDescription = label }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val labelColor = if (isLight) BoardColors.dark else BoardColors.light
        if (showRank) {
            Text(
                text = "${square.rank + 1}",
                color = labelColor,
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp, top = 1.dp),
            )
        }
        if (showFile) {
            Text(
                text = "${'a' + square.file}",
                color = labelColor,
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 1.dp),
            )
        }
        if (square in state.legalTargets) {
            Box(
                Modifier
                    .size(if (piece == null) 14.dp else 38.dp)
                    .clip(CircleShape)
                    .background(BoardColors.target),
            )
        }
    }
}

@Composable
private fun AnimatedPiece(
    tracked: TrackedPiece,
    target: Offset,
    cellDp: Dp,
    selected: Boolean,
    justMoved: Boolean,
) {
    val position by animateOffsetAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = MOVE_ANIMATION_MS, easing = FastOutSlowInEasing),
        label = "piece-position",
    )
    val lift by animateFloatAsState(
        targetValue = if (selected) 1.14f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "piece-lift",
    )
    PieceView(
        piece = tracked.piece,
        modifier = Modifier
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .size(cellDp)
            .zIndex(if (justMoved || selected) 2f else 1f)
            .graphicsLayer {
                scaleX = lift
                scaleY = lift
                translationY = -(lift - 1f) * size.height * 0.6f
            }
            .padding(cellDp * 0.03f),
    )
}

@Composable
private fun CapturedGhost(piece: Piece, offset: Offset, cellDp: Dp) {
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(110)
        fade.animateTo(1f, tween(durationMillis = 260))
    }
    PieceView(
        piece = piece,
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .size(cellDp)
            .graphicsLayer {
                alpha = 1f - fade.value
                val shrink = 1f - 0.4f * fade.value
                scaleX = shrink
                scaleY = shrink
            }
            .padding(cellDp * 0.03f),
    )
}
