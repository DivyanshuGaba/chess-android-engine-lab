package com.divyanshu.chess.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.divyanshu.chess.domain.model.Piece
import com.divyanshu.chess.domain.model.PieceType
import com.divyanshu.chess.domain.model.Side

/** Pieces are drawn as layered vector shapes in a 100 x 100 design space. */
@Composable
fun PieceView(piece: Piece, modifier: Modifier = Modifier) {
    val model = remember(piece.type) { PieceModels.of(piece.type) }
    Canvas(modifier) {
        val scale = size.minDimension / DESIGN_SIZE
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) { drawPiece(model, piece.side) }
    }
}

fun describe(piece: Piece): String =
    "${if (piece.side == Side.WHITE) "white" else "black"} ${piece.type.name.lowercase()}"

private const val DESIGN_SIZE = 100f

private class Detail(val path: Path, val width: Float)

private class PieceModel(val parts: List<Path>, val details: List<Detail> = emptyList())

private val WhiteFill = Brush.linearGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF3EBDA), Color(0xFFCDC0A6)),
    start = Offset(20f, 10f),
    end = Offset(80f, 95f),
)

private val BlackFill = Brush.linearGradient(
    colors = listOf(Color(0xFF5B5C66), Color(0xFF2B2C33), Color(0xFF0F0F13)),
    start = Offset(20f, 10f),
    end = Offset(80f, 95f),
)

private fun shape(block: Path.() -> Unit): Path = Path().apply(block)

private fun Path.oval(left: Float, top: Float, right: Float, bottom: Float) =
    addOval(Rect(left, top, right, bottom))

private fun Path.pill(left: Float, top: Float, right: Float, bottom: Float, radius: Float) =
    addRoundRect(RoundRect(left, top, right, bottom, CornerRadius(radius)))

private fun plinth(): List<Path> = listOf(
    shape { pill(21f, 83f, 79f, 92f, 3.5f) },
    shape { pill(27f, 76f, 73f, 84f, 3f) },
)

private fun dot(x: Float, y: Float, width: Float) =
    Detail(shape { moveTo(x, y); lineTo(x + 0.2f, y) }, width)

private object PieceModels {
    private val models: Map<PieceType, PieceModel> by lazy {
        mapOf(
            PieceType.PAWN to pawn(),
            PieceType.ROOK to rook(),
            PieceType.KNIGHT to knight(),
            PieceType.BISHOP to bishop(),
            PieceType.QUEEN to queen(),
            PieceType.KING to king(),
        )
    }

    fun of(type: PieceType): PieceModel = models.getValue(type)

    private fun pawn() = PieceModel(
        listOf(
            shape {
                moveTo(41f, 49f)
                cubicTo(40f, 60f, 35f, 68f, 31f, 77f)
                lineTo(69f, 77f)
                cubicTo(65f, 68f, 60f, 60f, 59f, 49f)
                close()
            },
            shape { pill(34f, 43f, 66f, 50f, 3.5f) },
            shape { oval(38f, 20f, 62f, 44f) },
        ) + plinth(),
    )

    private fun rook() = PieceModel(
        listOf(
            shape {
                moveTo(36f, 40f)
                lineTo(64f, 40f)
                lineTo(68f, 77f)
                lineTo(32f, 77f)
                close()
            },
            shape {
                moveTo(32f, 40f); lineTo(68f, 40f); lineTo(65f, 33f); lineTo(35f, 33f); close()
            },
            shape {
                moveTo(29f, 33f)
                lineTo(29f, 15f); lineTo(38f, 15f); lineTo(38f, 22f); lineTo(44f, 22f); lineTo(44f, 15f)
                lineTo(56f, 15f); lineTo(56f, 22f); lineTo(62f, 22f); lineTo(62f, 15f); lineTo(71f, 15f)
                lineTo(71f, 33f)
                close()
            },
        ) + plinth(),
    )

    private fun bishop() = PieceModel(
        parts = listOf(
            shape {
                moveTo(42f, 55f)
                cubicTo(41f, 65f, 35f, 70f, 31f, 77f)
                lineTo(69f, 77f)
                cubicTo(65f, 70f, 59f, 65f, 58f, 55f)
                close()
            },
            shape { pill(33f, 50f, 67f, 57f, 3.5f) },
            shape {
                moveTo(50f, 16f)
                cubicTo(64f, 25f, 68f, 38f, 62f, 48f)
                cubicTo(59f, 53f, 41f, 53f, 38f, 48f)
                cubicTo(32f, 38f, 36f, 25f, 50f, 16f)
                close()
            },
            shape { oval(45.5f, 7f, 54.5f, 16f) },
        ) + plinth(),
        details = listOf(Detail(shape { moveTo(47f, 29f); lineTo(58f, 41f) }, 2.4f)),
    )

    private fun knight() = PieceModel(
        parts = listOf(
            shape {
                moveTo(27f, 80f)
                cubicTo(27f, 68f, 32f, 60f, 40f, 54f)
                cubicTo(34f, 54f, 28f, 54f, 22f, 50f)
                cubicTo(17f, 47f, 17f, 42f, 21f, 38f)
                lineTo(34f, 26f)
                cubicTo(35f, 20f, 37f, 16f, 40f, 11f)
                lineTo(45f, 17f)
                cubicTo(53f, 13f, 62f, 17f, 68f, 28f)
                cubicTo(76f, 41f, 75f, 62f, 73f, 80f)
                close()
            },
        ) + plinth(),
        details = listOf(
            dot(37f, 31f, 3.6f),
            dot(21.5f, 43f, 2.6f),
            Detail(shape { moveTo(47f, 21f); cubicTo(58f, 26f, 64f, 38f, 64f, 52f) }, 1.6f),
        ),
    )

    private fun queen(): PieceModel {
        val peaks = listOf(24f to 27f, 37f to 21f, 50f to 17f, 63f to 21f, 76f to 27f)
        return PieceModel(
            listOf(
                shape {
                    moveTo(37f, 64f)
                    cubicTo(37f, 71f, 33f, 73f, 31f, 77f)
                    lineTo(69f, 77f)
                    cubicTo(67f, 73f, 63f, 71f, 63f, 64f)
                    close()
                },
                shape {
                    moveTo(30f, 60f)
                    lineTo(24f, 27f); lineTo(30f, 46f); lineTo(37f, 21f); lineTo(43f, 44f)
                    lineTo(50f, 17f); lineTo(57f, 44f); lineTo(63f, 21f); lineTo(70f, 46f)
                    lineTo(76f, 27f); lineTo(70f, 60f)
                    close()
                },
                shape { pill(30f, 57f, 70f, 65f, 3.5f) },
            ) + peaks.map { (x, y) -> shape { oval(x - 3.8f, y - 3.8f, x + 3.8f, y + 3.8f) } } + plinth(),
        )
    }

    private fun king() = PieceModel(
        listOf(
            shape {
                moveTo(36f, 62f)
                cubicTo(36f, 70f, 33f, 73f, 31f, 77f)
                lineTo(69f, 77f)
                cubicTo(67f, 73f, 64f, 70f, 64f, 62f)
                close()
            },
            shape {
                moveTo(30f, 56f)
                cubicTo(24f, 40f, 32f, 27f, 50f, 27f)
                cubicTo(68f, 27f, 76f, 40f, 70f, 56f)
                close()
            },
            shape { pill(29f, 54f, 71f, 62f, 3.5f) },
            shape {
                moveTo(46.5f, 5f); lineTo(53.5f, 5f); lineTo(53.5f, 10f); lineTo(58.5f, 10f)
                lineTo(58.5f, 16.5f); lineTo(53.5f, 16.5f); lineTo(53.5f, 28f); lineTo(46.5f, 28f)
                lineTo(46.5f, 16.5f); lineTo(41.5f, 16.5f); lineTo(41.5f, 10f); lineTo(46.5f, 10f)
                close()
            },
        ) + plinth(),
    )
}

private fun DrawScope.drawPiece(model: PieceModel, side: Side) {
    val white = side == Side.WHITE
    val fill = if (white) WhiteFill else BlackFill
    val outline = if (white) Color(0xFF3A2F22) else Color(0xFF050506)
    val gloss = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = if (white) 0.55f else 0.2f), Color.Transparent),
        start = Offset(18f, 8f),
        end = Offset(62f, 70f),
    )
    val edge = Stroke(width = 2f, join = StrokeJoin.Round)

    drawOval(Color.Black.copy(alpha = 0.3f), topLeft = Offset(20f, 88f), size = Size(60f, 8f))
    model.parts.forEach { part ->
        drawPath(part, fill)
        drawPath(part, gloss)
        drawPath(part, outline, style = edge)
    }
    model.details.forEach { detail ->
        drawPath(
            detail.path,
            outline,
            style = Stroke(width = detail.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
