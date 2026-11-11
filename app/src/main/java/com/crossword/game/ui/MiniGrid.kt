package com.crossword.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.crossword.game.model.GameState

/**
 * A small picture of a level: its shape, squares with a letter tinted and solved words green.
 * Never shows letters, so it can stand in for the grid while the game is paused.
 */
@Composable
fun MiniGrid(state: GameState, box: Dp, modifier: Modifier = Modifier) {
    val p = state.puzzle
    val solved = state.solvedCells
    Canvas(modifier.size(box)) {
        val cs = size.minDimension / maxOf(p.width, p.height)
        val ox = (size.width - cs * p.width) / 2
        val oy = (size.height - cs * p.height) / 2
        val line = maxOf(1.5f, cs * 0.09f)
        for (i in 0 until p.size) {
            if (!p.isOpen(i)) continue
            val topLeft = Offset(ox + (i % p.width) * cs, oy + (i / p.width) * cs)
            val fill = when {
                i in solved -> Palette.Solved
                state.letters[i] != ' ' -> Palette.WordTint
                else -> Palette.Square
            }
            drawRect(fill, topLeft, Size(cs, cs))
            drawRect(Palette.Ink, topLeft, Size(cs, cs), style = Stroke(line))
        }
    }
}
