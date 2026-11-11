package com.crossword.game.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.crossword.game.data.Difficulty

object Palette {
    val Paper = Color(0xFFF6F1E7)
    val Ink = Color(0xFF1F1B16)
    val InkSoft = Color(0xFF6B6359)
    val Line = Color(0xFFDDD4C4)
    val Card = Color(0xFFFFFDF8)
    val Square = Color(0xFFFFFFFF)
    val WordTint = Color(0xFFCFE2F5)
    val Cursor = Color(0xFFFFCF4D)
    val Revealed = Color(0xFF1D5BAA)
    val Solved = Color(0xFFCBE8C0)
    val SolvedCursor = Color(0xFFA3D595)
    val SolvedInk = Color(0xFF2F6B2A)
    val Wrong = Color(0xFFE5484D)
    val Accent = Color(0xFF1E3A5F)
    val KeyboardBed = Color(0xFFE9E2D4)
    val Key = Color(0xFFFFFDF8)

    fun of(d: Difficulty): Color = when (d) {
        Difficulty.EASY -> Color(0xFF2E7D32)
        Difficulty.MEDIUM -> Color(0xFFB26A00)
        Difficulty.HARD -> Color(0xFFB3261E)
    }
}

@Composable
fun CrosswordTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.Accent,
            onPrimary = Color.White,
            background = Palette.Paper,
            onBackground = Palette.Ink,
            surface = Palette.Paper,
            onSurface = Palette.Ink,
            onSurfaceVariant = Palette.InkSoft,
            surfaceContainerHigh = Palette.Card,
            outline = Palette.InkSoft,
            error = Color(0xFFB3261E),
        ),
        content = content,
    )
}

/** Small line icons drawn on a 24 x 24 grid, so the app needs no icon library. */
object Glyphs {
    private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        ).build()

    val Back = icon("back") {
        moveTo(20f, 12f); lineTo(5f, 12f)
        moveTo(11f, 6f); lineTo(5f, 12f); lineTo(11f, 18f)
    }
    val Pause = icon("pause") {
        moveTo(9f, 6f); lineTo(9f, 18f)
        moveTo(15f, 6f); lineTo(15f, 18f)
    }
    val Previous = icon("previous") { moveTo(15f, 6f); lineTo(9f, 12f); lineTo(15f, 18f) }
    val Next = icon("next") { moveTo(9f, 6f); lineTo(15f, 12f); lineTo(9f, 18f) }
    val Backspace = icon("backspace") {
        moveTo(21f, 5f); lineTo(9f, 5f); lineTo(3f, 12f); lineTo(9f, 19f); lineTo(21f, 19f); close()
        moveTo(12f, 9f); lineTo(18f, 15f)
        moveTo(18f, 9f); lineTo(12f, 15f)
    }
    val Check = icon("check") { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }
}
