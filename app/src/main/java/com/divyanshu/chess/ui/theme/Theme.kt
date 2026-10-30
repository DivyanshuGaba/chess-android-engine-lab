package com.divyanshu.chess.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divyanshu.chess.domain.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE0AE4C),
    onPrimary = Color(0xFF1A1405),
    primaryContainer = Color(0xFF4A3912),
    onPrimaryContainer = Color(0xFFFFE3A8),
    secondary = Color(0xFF9ED4AF),
    onSecondary = Color(0xFF00391E),
    secondaryContainer = Color(0xFF2E4636),
    onSecondaryContainer = Color(0xFFD7EEDD),
    tertiary = Color(0xFF8FC7E8),
    background = Color(0xFF0B120E),
    onBackground = Color(0xFFEDE9DD),
    surface = Color(0xFF101A14),
    onSurface = Color(0xFFEDE9DD),
    surfaceVariant = Color(0xFF22332A),
    onSurfaceVariant = Color(0xFFB9C2B8),
    surfaceContainerLowest = Color(0xFF0B120E),
    surfaceContainerLow = Color(0xFF111B15),
    surfaceContainer = Color(0xFF17241C),
    surfaceContainerHigh = Color(0xFF1E2E24),
    surfaceContainerHighest = Color(0xFF26382C),
    outline = Color(0xFF74877B),
    outlineVariant = Color(0xFF3A4D41),
    error = Color(0xFFFF8A80),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A5A00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDEA5),
    onPrimaryContainer = Color(0xFF2B1A00),
    secondary = Color(0xFF2E6B47),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE9D6),
    onSecondaryContainer = Color(0xFF0B2A18),
    tertiary = Color(0xFF1F5F85),
    background = Color(0xFFF5F2E9),
    onBackground = Color(0xFF1B1F1A),
    surface = Color(0xFFFBF8F0),
    onSurface = Color(0xFF1B1F1A),
    surfaceVariant = Color(0xFFE4E1D3),
    onSurfaceVariant = Color(0xFF4A564D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F2E8),
    surfaceContainer = Color(0xFFEEEADD),
    surfaceContainerHigh = Color(0xFFE6E1D2),
    surfaceContainerHighest = Color(0xFFDDD8C6),
    outline = Color(0xFF7A877D),
    outlineVariant = Color(0xFFC6C3B4),
    error = Color(0xFFB3261E),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val AppTypography = Typography().run {
    copy(
        displayMedium = displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.Medium),
        bodyLarge = bodyLarge.copy(lineHeight = 24.sp),
    )
}

/** Spacing scale used across the app, so screens line up with each other. */
object Spacing {
    val screen = 16.dp
    val card = 16.dp
    val gap = 12.dp
    val tight = 8.dp
}

object BoardColors {
    val frame = Color(0xFF4A2F1C)
    val light = Color(0xFFEBD8B4)
    val dark = Color(0xFFA9794C)
    val selected = Color(0xCCF5E663)
    val lastMove = Color(0x66FFD54A)
    val check = Color(0xD9E5483B)
    val target = Color(0x44000000)
}

/** A text style for tabular numbers such as clocks. */
val ClockTextStyle = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun ChessTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}

/** Shared backdrop for every screen: a soft vertical gradient behind transparent scaffolds. */
@Composable
fun AppBackground(content: @Composable () -> Unit) {
    val colors: ColorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent,
        contentColor = colors.onSurface,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(colors.surface, colors.background, colors.background))),
        ) {
            content()
        }
    }
}
