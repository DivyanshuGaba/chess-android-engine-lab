package com.divyanshu.chess.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Small outline icons drawn here so the app does not need the large extended icon set. */
object ChessIcons {

    private fun outline(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
        }.build()

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcToRelative(r, r, 0f, true, false, 2 * r, 0f)
        arcToRelative(r, r, 0f, true, false, -2 * r, 0f)
        close()
    }

    /** A knight's head, used for the Play tab. */
    val Play: ImageVector by lazy {
        outline("play") {
            circle(12f, 6.8f, 2.8f)
            moveTo(9.6f, 11f); lineTo(14.4f, 11f)
            moveTo(10.2f, 11f)
            curveTo(10.2f, 14.5f, 8.6f, 15.5f, 7.6f, 20f)
            lineTo(16.4f, 20f)
            curveTo(15.4f, 15.5f, 13.8f, 14.5f, 13.8f, 11f)
            moveTo(6.5f, 20f); lineTo(17.5f, 20f)
        }
    }

    val Watch: ImageVector by lazy {
        outline("watch") {
            moveTo(2.5f, 12f)
            curveTo(5f, 7.5f, 8.2f, 5.5f, 12f, 5.5f)
            curveTo(15.8f, 5.5f, 19f, 7.5f, 21.5f, 12f)
            curveTo(19f, 16.5f, 15.8f, 18.5f, 12f, 18.5f)
            curveTo(8.2f, 18.5f, 5f, 16.5f, 2.5f, 12f)
            close()
            circle(12f, 12f, 3f)
        }
    }

    val Learn: ImageVector by lazy {
        outline("learn") {
            moveTo(3.5f, 5.5f); lineTo(9.5f, 5.5f)
            curveTo(10.9f, 5.5f, 12f, 6.6f, 12f, 8f)
            lineTo(12f, 19f)
            curveTo(12f, 17.9f, 11.1f, 17f, 10f, 17f)
            lineTo(3.5f, 17f)
            close()
            moveTo(20.5f, 5.5f); lineTo(14.5f, 5.5f)
            curveTo(13.1f, 5.5f, 12f, 6.6f, 12f, 8f)
            lineTo(12f, 19f)
            curveTo(12f, 17.9f, 12.9f, 17f, 14f, 17f)
            lineTo(20.5f, 17f)
            close()
        }
    }

    val History: ImageVector by lazy {
        outline("history") {
            circle(12f, 12f, 8.5f)
            moveTo(12f, 7.5f); lineTo(12f, 12f); lineTo(15f, 14f)
        }
    }

    val Lab: ImageVector by lazy {
        outline("lab") {
            moveTo(9.5f, 3.5f); lineTo(14.5f, 3.5f)
            moveTo(10.5f, 3.5f); lineTo(10.5f, 9.5f); lineTo(5.2f, 18.2f)
            curveTo(4.5f, 19.5f, 5.3f, 20.5f, 6.6f, 20.5f)
            lineTo(17.4f, 20.5f)
            curveTo(18.7f, 20.5f, 19.5f, 19.5f, 18.8f, 18.2f)
            lineTo(13.5f, 9.5f); lineTo(13.5f, 3.5f)
            moveTo(7.5f, 15f); lineTo(16.5f, 15f)
        }
    }

    val Undo: ImageVector by lazy {
        outline("undo") {
            moveTo(9f, 6.5f); lineTo(4.5f, 11f); lineTo(9f, 15.5f)
            moveTo(5f, 11f); lineTo(14f, 11f)
            curveTo(17.3f, 11f, 19.5f, 13f, 19.5f, 16f)
            curveTo(19.5f, 17f, 19.3f, 17.8f, 19f, 18.5f)
        }
    }

    val Flag: ImageVector by lazy {
        outline("flag") {
            moveTo(5.5f, 21f); lineTo(5.5f, 3.5f)
            moveTo(5.5f, 4.5f); lineTo(18f, 4.5f); lineTo(15.5f, 8.5f); lineTo(18f, 12.5f); lineTo(5.5f, 12.5f)
        }
    }

    val Vibrate: ImageVector by lazy {
        outline("vibrate") {
            moveTo(8.5f, 4f); lineTo(15.5f, 4f)
            curveTo(16.3f, 4f, 17f, 4.7f, 17f, 5.5f)
            lineTo(17f, 18.5f)
            curveTo(17f, 19.3f, 16.3f, 20f, 15.5f, 20f)
            lineTo(8.5f, 20f)
            curveTo(7.7f, 20f, 7f, 19.3f, 7f, 18.5f)
            lineTo(7f, 5.5f)
            curveTo(7f, 4.7f, 7.7f, 4f, 8.5f, 4f)
            close()
            moveTo(3.5f, 9f); lineTo(3.5f, 15f)
            moveTo(20.5f, 9f); lineTo(20.5f, 15f)
        }
    }

    val Gear: ImageVector by lazy {
        outline("settings") {
            moveTo(3f, 7f); lineTo(21f, 7f)
            moveTo(3f, 17f); lineTo(21f, 17f)
            circle(9f, 7f, 2.6f)
            circle(15f, 17f, 2.6f)
        }
    }

    val Trash: ImageVector by lazy {
        outline("trash") {
            moveTo(4.5f, 7f); lineTo(19.5f, 7f)
            moveTo(9f, 7f); lineTo(9f, 4.5f); lineTo(15f, 4.5f); lineTo(15f, 7f)
            moveTo(6.5f, 7f); lineTo(7.4f, 19.5f); lineTo(16.6f, 19.5f); lineTo(17.5f, 7f)
            moveTo(10f, 11f); lineTo(10f, 16f)
            moveTo(14f, 11f); lineTo(14f, 16f)
        }
    }

    val Computer: ImageVector by lazy {
        outline("computer") {
            moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 15f); lineTo(4f, 15f); close()
            moveTo(9f, 19f); lineTo(15f, 19f)
            moveTo(12f, 15f); lineTo(12f, 19f)
        }
    }

    val People: ImageVector by lazy {
        outline("people") {
            circle(9f, 8.5f, 3f)
            moveTo(3.5f, 19f)
            curveTo(3.5f, 15.8f, 6f, 14f, 9f, 14f)
            curveTo(12f, 14f, 14.5f, 15.8f, 14.5f, 19f)
            moveTo(15.5f, 5.8f)
            curveTo(17.8f, 6.2f, 18.8f, 8.2f, 17.8f, 10.2f)
            moveTo(16.5f, 14.2f)
            curveTo(19f, 14.8f, 20.5f, 16.3f, 20.5f, 19f)
        }
    }

    val Chevron: ImageVector by lazy {
        outline("chevron") {
            moveTo(9f, 6f); lineTo(15f, 12f); lineTo(9f, 18f)
        }
    }
}
