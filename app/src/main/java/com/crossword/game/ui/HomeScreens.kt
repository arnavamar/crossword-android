package com.crossword.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.crossword.game.AppViewModel
import com.crossword.game.data.Difficulty
import com.crossword.game.model.GameState
import java.util.Locale

internal fun Int.grouped(): String = String.format(Locale.US, "%,d", this)

/** Levels in each block of the level list. */
private const val SECTION = 100

@Composable
fun HomeScreen(vm: AppViewModel) {
    val tick = vm.progressTick
    val total = remember { Difficulty.entries.sumOf { vm.repo.levelCount(it) } }
    val solved = remember(tick) { Difficulty.entries.sumOf { vm.store.solvedCount(it) } }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 36.dp, bottom = 24.dp),
    ) {
        TitleWord("CROSSWORD")
        Spacer(Modifier.height(14.dp))
        Text(
            if (solved == 0) "${total.grouped()} puzzles, all offline" else "${solved.grouped()} of ${total.grouped()} puzzles solved",
            color = Palette.InkSoft,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(28.dp))
        for (d in Difficulty.entries) {
            DifficultyCard(vm, d, tick)
            Spacer(Modifier.height(14.dp))
        }
    }
}

/** The app name set in a row of grid squares, drawn the way the game shows a selected word. */
@Composable
private fun TitleWord(word: String) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth().semantics { contentDescription = "Crossword" }) {
        val cell = min(maxWidth / word.length, 42.dp)
        Canvas(Modifier.size(cell * word.length, cell)) {
            val stroke = 1.5.dp.toPx()
            val cs = (size.width - stroke) / word.length
            val inset = stroke / 2
            fun topLeft(i: Int) = Offset(inset + i * cs, inset)
            word.indices.forEach { i ->
                drawRect(if (i == 0) Palette.Cursor else Palette.WordTint, topLeft(i), Size(cs, cs))
            }
            val letterStyle = TextStyle(
                fontSize = (cs * 0.56f).toSp(),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = Palette.Ink,
            )
            word.forEachIndexed { i, ch ->
                drawRect(Palette.Ink, topLeft(i), Size(cs, cs), style = Stroke(stroke))
                val layout = measurer.measure(ch.toString(), letterStyle)
                drawText(
                    layout,
                    topLeft = topLeft(i) + Offset((cs - layout.size.width) / 2, (cs - layout.size.height) / 2 + cs * 0.03f),
                )
            }
            drawText(
                measurer,
                "1",
                topLeft(0) + Offset(cs * 0.08f, cs * 0.02f),
                style = TextStyle(fontSize = (cs * 0.25f).toSp(), color = Palette.Ink),
            )
        }
    }
}

@Composable
private fun DifficultyCard(vm: AppViewModel, d: Difficulty, tick: Int) {
    val total = remember(d) { vm.repo.levelCount(d) }
    val solved = remember(d, tick) { vm.store.solvedCount(d) }
    val next = remember(d, tick) { vm.store.nextLevel(d, total) }
    // The next level's shape, with any letters already in it.
    val preview = remember(d, next, tick) {
        val puzzle = vm.repo.puzzle(d, next)
        vm.store.loadGame(d, next, vm.repo.signature(d, next), puzzle) ?: GameState.start(puzzle)
    }
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.Card)
            .border(1.dp, Palette.Line, shape)
            .padding(18.dp),
    ) {
        Row {
            Column(Modifier.weight(1f)) {
                Text(d.label, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp)
                Text("${d.words.first} to ${d.words.last} words a puzzle", color = Palette.InkSoft, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Text("${solved.grouped()} of ${total.grouped()} solved", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { solved.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = Palette.of(d),
                    trackColor = Palette.Line,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
            Spacer(Modifier.width(20.dp))
            MiniGrid(preview, 84.dp)
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { vm.openGame(d, next) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Accent),
                modifier = Modifier.weight(1f).height(48.dp),
            ) { Text("Play level ${next.grouped()}", fontSize = 15.sp) }
            OutlinedButton(
                onClick = { vm.openLevels(d) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp),
            ) { Text("All levels", color = Palette.Ink, fontSize = 15.sp) }
        }
    }
}

@Composable
fun TopBar(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    icon: ImageVector = Glyphs.Back,
    iconLabel: String = "Back",
    trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(icon, contentDescription = iconLabel, tint = Palette.Ink) }
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            if (subtitle != null) Text(subtitle, color = Palette.InkSoft, fontSize = 13.sp)
        }
        trailing()
    }
}

@Composable
fun LevelsScreen(vm: AppViewModel, d: Difficulty) {
    val tick = vm.progressTick
    val total = remember(d) { vm.repo.levelCount(d) }
    val solved = remember(d, tick) { vm.store.solvedFlags(d, total) }
    val started = remember(d, tick) { vm.store.startedLevels(d) }
    val next = remember(d, tick) { vm.store.nextLevel(d, total) }
    val solvedCount = solved.count { it }
    // Every block before the next level's is one heading plus SECTION tiles. Open on that block's
    // heading, or a little above the tile when it sits deep in its block.
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = run {
            val block = (next - 1) / SECTION
            val within = (next - 1) % SECTION
            val heading = block * (SECTION + 1)
            if (within < 30) heading else heading + 1 + within - 12
        },
    )
    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = d.label,
            subtitle = "${solvedCount.grouped()} of ${total.grouped()} solved",
            onBack = vm::back,
        )
        LinearProgressIndicator(
            progress = { solvedCount.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = Palette.of(d),
            trackColor = Palette.Line,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(54.dp),
            state = gridState,
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            for (block in 0 until (total + SECTION - 1) / SECTION) {
                val first = block * SECTION + 1
                val last = minOf(total, first + SECTION - 1)
                item(key = "block$block", span = { GridItemSpan(maxLineSpan) }) {
                    BlockHeading(first, last, solvedIn = (first..last).count { solved[it - 1] })
                }
                items(count = last - first + 1, key = { first + it }) { k ->
                    val level = first + k
                    LevelTile(
                        level = level,
                        solved = solved[level - 1],
                        started = level in started,
                        isNext = level == next,
                    ) { vm.openGame(d, level) }
                }
            }
        }
    }
}

@Composable
private fun BlockHeading(first: Int, last: Int, solvedIn: Int) {
    Row(
        Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            "${first.grouped()} to ${last.grouped()}",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
        )
        Spacer(Modifier.weight(1f))
        if (solvedIn > 0) {
            Text(
                if (solvedIn == last - first + 1) "All solved" else "$solvedIn solved",
                color = Palette.InkSoft,
                fontSize = 13.sp,
            )
        }
    }
}

/** Green when solved and blue when started, the same colours as words in the grid. */
@Composable
private fun LevelTile(level: Int, solved: Boolean, started: Boolean, isNext: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val fill = when {
        solved -> Palette.Solved
        started -> Palette.WordTint
        else -> Palette.Card
    }
    val edge = when {
        isNext -> Palette.Ink
        solved -> Palette.SolvedCursor
        started -> Color(0xFFA9C7E8)
        else -> Palette.Line
    }
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(fill)
            .border(if (isNext) 2.dp else 1.dp, edge, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            level.toString(),
            color = if (solved) Palette.SolvedInk else Palette.Ink,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
            fontSize = if (level >= 1000) 13.sp else 15.sp,
        )
        if (solved) {
            Icon(
                Glyphs.Check,
                contentDescription = "Solved",
                tint = Palette.SolvedInk,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(11.dp),
            )
        }
    }
}
