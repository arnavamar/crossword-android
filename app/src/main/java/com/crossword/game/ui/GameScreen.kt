package com.crossword.game.ui

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.crossword.game.AppViewModel
import com.crossword.game.WordFeedback
import com.crossword.game.data.Difficulty
import com.crossword.game.model.Direction
import com.crossword.game.model.Entry
import com.crossword.game.model.GameState
import com.crossword.game.model.Hints
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// Strong ease-out: starts fast so feedback feels immediate.
private val EaseOutStrong = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

// Word feedback timings (ms). Typing itself never animates; only these check moments do.
private const val SOLVE_STAGGER = 45
private const val SOLVE_CELL = 240
private const val WRONG_STAGGER = 25
private const val WRONG_CELL = 320
private const val SHAKE = 360

private fun WordFeedback.durationMs(): Int {
    val solvedMs = solved.maxOfOrNull { (it.size - 1) * SOLVE_STAGGER + SOLVE_CELL } ?: 0
    val wrongMs = wrong.maxOfOrNull { maxOf((it.size - 1) * WRONG_STAGGER + WRONG_CELL, SHAKE) } ?: 0
    return maxOf(solvedMs, wrongMs)
}

@Composable
fun GameScreen(vm: AppViewModel, d: Difficulty, level: Int) {
    val state = vm.game ?: return
    LifecycleResumeEffect(d, level) {
        vm.resumeClock()
        onPauseOrDispose { vm.pauseClock() }
    }
    // Leaving the app mid-level opens the pause menu, so play restarts only on Resume.
    // A configuration change (screen size, dark mode) also stops the activity; that is not leaving.
    val activity = LocalContext.current as? Activity
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (activity?.isChangingConfigurations != true) vm.pause()
    }
    val snackbar = remember { SnackbarHostState() }
    val eventsAtOpen = remember { vm.notQuiteEvents }
    val notQuite = vm.notQuiteEvents
    LaunchedEffect(notQuite) {
        if (notQuite > eventsAtOpen) snackbar.showSnackbar("Every square is filled. Words that are not green still need fixing.")
    }
    val celebrationsAtOpen = remember { vm.celebrations }
    val celebrate = vm.celebrations > celebrationsAtOpen
    val solved = state.isSolved
    val total = remember(d) { vm.repo.levelCount(d) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                title = "Level ${level.grouped()}",
                subtitle = "${d.label}, ${state.solvedWords} of ${state.puzzle.entries.size} words",
                onBack = if (solved) vm::back else vm::pause,
                icon = if (solved) Glyphs.Back else Glyphs.Pause,
                iconLabel = if (solved) "Back" else "Pause",
                trailing = { if (!solved) HintButton(state, onClick = vm::onHint) },
            )
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val p = state.puzzle
                // Keep ~150dp for the clue list while squares can stay 30dp or more; on short screens
                // the grid takes up to 85% and the list shows only if enough height is left to read it.
                val byWidth = min((maxWidth - 24.dp) / p.width, 46.dp)
                val leavingClues = (maxHeight - 150.dp) / p.height
                val cell = min(byWidth, if (leavingClues >= 30.dp) leavingClues else maxHeight * 0.85f / p.height)
                val showClues = maxHeight - cell * p.height - 16.dp >= 72.dp
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(6.dp))
                    CrosswordGrid(state, cell, vm.feedback, onCell = vm::onCell)
                    Spacer(Modifier.height(10.dp))
                    if (showClues) {
                        HorizontalDivider(color = Palette.Line)
                        ClueLists(state, onEntry = vm::onEntry, modifier = Modifier.weight(1f))
                    }
                }
            }
            AnimatedContent(
                targetState = solved,
                transitionSpec = {
                    // The panel starts as the keyboard finishes fading, after the last word's green sweep has begun.
                    (slideInVertically(tween(340, delayMillis = 140, easing = EaseOutStrong)) { it / 2 } +
                        fadeIn(tween(200, delayMillis = 140)))
                        .togetherWith(fadeOut(tween(140)))
                        .using(SizeTransform(clip = false))
                },
                label = "bottom",
            ) { done ->
                if (done) {
                    val solvedCount = remember(vm.progressTick) { vm.store.solvedCount(d) }
                    ResultsPanel(
                        d = d,
                        level = level,
                        state = state,
                        solvedCount = solvedCount,
                        total = total,
                        onNext = if (level < total) vm::nextLevel else null,
                        onLevels = vm::showAllLevels,
                    )
                } else {
                    Column {
                        ClueBar(
                            entry = state.currentEntry,
                            solved = state.isCorrect(state.currentEntry),
                            onStep = vm::onStep,
                            onToggle = { vm.onCell(state.selected) },
                        )
                        LetterKeyboard(onKey = vm::onKey, onBackspace = vm::onBackspace)
                    }
                }
            }
        }
        if (celebrate) Confetti(Modifier.fillMaxSize())
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 256.dp))
        PauseMenu(
            visible = vm.paused && !solved,
            d = d,
            level = level,
            state = state,
            onResume = vm::resume,
            onLevels = vm::showAllLevels,
            onHome = vm::goHome,
        )
    }
}

@Composable
private fun HintButton(state: GameState, onClick: () -> Unit) {
    val left = Hints.left(state.hintsUsed)
    Button(
        onClick = onClick,
        enabled = left > 0,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Palette.Accent),
        modifier = Modifier.padding(end = 8.dp),
    ) {
        Text("Hint")
        Spacer(Modifier.width(8.dp))
        Text("$left", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CrosswordGrid(state: GameState, cell: Dp, feedback: WordFeedback?, onCell: (Int) -> Unit) {
    val p = state.puzzle
    val measurer = rememberTextMeasurer()
    val tap by rememberUpdatedState(onCell)
    val done = state.isSolved
    val word = if (done) emptySet() else state.currentEntry.cells(p.width).toSet()

    // One clock drives every square of the latest check; each square reads its own slice of it.
    val clock = remember { Animatable(1f) }
    var playing by remember { mutableStateOf<WordFeedback?>(null) }
    LaunchedEffect(feedback?.id) {
        val f = feedback ?: return@LaunchedEffect
        playing = f
        clock.snapTo(0f)
        clock.animateTo(1f, tween(f.durationMs(), easing = LinearEasing))
        playing = null
    }

    Canvas(
        Modifier
            .size(cell * p.width, cell * p.height)
            .pointerInput(p) {
                detectTapGestures { o ->
                    val cs = size.width.toFloat() / p.width
                    val c = (o.x / cs).toInt().coerceIn(0, p.width - 1)
                    val r = (o.y / cs).toInt().coerceIn(0, p.height - 1)
                    tap(r * p.width + c)
                }
            },
    ) {
        val stroke = 1.dp.toPx()
        val cs = (size.width - stroke) / p.width
        val inset = stroke / 2

        val greenAt = HashMap<Int, Float>()
        val redAt = HashMap<Int, Float>()
        val shakeAt = HashMap<Int, Float>()
        val f = playing
        if (f != null) {
            val elapsed = clock.value * f.durationMs()
            for (cells in f.solved) cells.forEachIndexed { i, c ->
                greenAt[c] = EaseOutStrong.transform(((elapsed - i * SOLVE_STAGGER) / SOLVE_CELL).coerceIn(0f, 1f))
            }
            val shakeT = (elapsed / SHAKE).coerceIn(0f, 1f)
            val dx = 5.dp.toPx() * sin(shakeT * 4 * PI.toFloat()) * (1 - shakeT)
            for (cells in f.wrong) cells.forEachIndexed { i, c ->
                val t = ((elapsed - i * WRONG_STAGGER) / WRONG_CELL).coerceIn(0f, 1f)
                redAt[c] = maxOf(redAt[c] ?: 0f, 0.6f * sin(PI.toFloat() * t))
                shakeAt[c] = dx
            }
        }

        fun topLeftOf(i: Int) = Offset(inset + (i % p.width) * cs + (shakeAt[i] ?: 0f), inset + (i / p.width) * cs)

        for (i in 0 until p.size) {
            if (!p.isOpen(i)) continue
            val isCursor = !done && i == state.selected
            val green = i in state.solvedCells
            val base = when {
                isCursor -> if (green) Palette.SolvedCursor else Palette.Cursor
                green -> Palette.Solved
                i in word -> Palette.WordTint
                else -> Palette.Square
            }
            val sweep = greenAt[i]
            val fill = if (sweep != null && !isCursor) lerp(Palette.WordTint, Palette.Solved, sweep) else base
            drawRect(fill, topLeftOf(i), Size(cs, cs))
            val red = redAt[i] ?: 0f
            if (red > 0f) drawRect(Palette.Wrong.copy(alpha = red), topLeftOf(i), Size(cs, cs))
        }

        val numberStyle = TextStyle(fontSize = (cs * 0.27f).toSp(), color = Palette.Ink)
        for (i in 0 until p.size) {
            if (!p.isOpen(i)) continue
            val topLeft = topLeftOf(i)
            drawRect(Palette.Ink, topLeft, Size(cs, cs), style = Stroke(stroke))
            val n = p.numbers[i]
            if (n > 0) drawText(measurer, n.toString(), topLeft + Offset(cs * 0.07f, cs * 0.02f), style = numberStyle)
            val letter = state.letterAt(i) ?: continue
            val layout = measurer.measure(
                letter.toString(),
                TextStyle(
                    fontSize = (cs * 0.56f).toSp(),
                    fontWeight = FontWeight.SemiBold,
                    color = if (i in state.revealed) Palette.Revealed else Palette.Ink,
                ),
            )
            drawText(
                layout,
                topLeft = topLeft + Offset((cs - layout.size.width) / 2, (cs - layout.size.height) / 2 + cs * 0.07f),
            )
        }
    }
}

@Composable
private fun ClueLists(state: GameState, onEntry: (Entry) -> Unit, modifier: Modifier) {
    val current = if (state.isSolved) null else state.currentEntry
    Row(modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        ClueColumn("Across", state.puzzle.across, state, current, onEntry, Modifier.weight(1f))
        ClueColumn("Down", state.puzzle.down, state, current, onEntry, Modifier.weight(1f))
    }
}

@Composable
private fun ClueColumn(
    label: String,
    entries: List<Entry>,
    state: GameState,
    current: Entry?,
    onEntry: (Entry) -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    val index = entries.indexOf(current)
    // Bring the current clue into view only when it is not already fully visible.
    LaunchedEffect(index) {
        if (index < 0) return@LaunchedEffect
        val item = index + 1 // item 0 is the heading
        val info = listState.layoutInfo
        val shown = info.visibleItemsInfo.firstOrNull { it.index == item }
        val fully = shown != null && shown.offset >= info.viewportStartOffset &&
            shown.offset + shown.size <= info.viewportEndOffset
        if (!fully) listState.animateScrollToItem(item)
    }
    LazyColumn(modifier.fillMaxHeight(), state = listState, contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Palette.InkSoft,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
        items(entries) { entry ->
            ClueRow(entry, selected = entry == current, solved = state.isCorrect(entry)) { onEntry(entry) }
        }
    }
}

@Composable
private fun ClueRow(entry: Entry, selected: Boolean, solved: Boolean, onClick: () -> Unit) {
    val ink = if (solved) Palette.InkSoft else Palette.Ink
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Palette.WordTint else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
    ) {
        Text("${entry.number}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ink, modifier = Modifier.width(22.dp))
        Text(entry.clue, fontSize = 13.sp, lineHeight = 17.sp, color = ink, modifier = Modifier.weight(1f))
        if (solved) {
            Icon(
                Glyphs.Check,
                contentDescription = "Solved",
                tint = Palette.SolvedInk,
                modifier = Modifier.padding(start = 4.dp).size(16.dp),
            )
        }
    }
}

@Composable
private fun ClueBar(entry: Entry, solved: Boolean, onStep: (Int) -> Unit, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(if (solved) Palette.Solved else Palette.WordTint),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onStep(-1) }) {
            Icon(Glyphs.Previous, contentDescription = "Previous clue", tint = Palette.Ink)
        }
        Column(
            Modifier
                .weight(1f)
                .clickable(onClick = onToggle)
                .padding(vertical = 8.dp),
        ) {
            val dir = if (entry.direction == Direction.ACROSS) "Across" else "Down"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${entry.number} $dir, ${entry.answer.length} letters",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (solved) Palette.SolvedInk else Palette.Accent,
                )
                if (solved) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Glyphs.Check, contentDescription = "Solved", tint = Palette.SolvedInk, modifier = Modifier.size(14.dp))
                }
            }
            Text(entry.clue, fontSize = 17.sp, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = { onStep(1) }) {
            Icon(Glyphs.Next, contentDescription = "Next clue", tint = Palette.Ink)
        }
    }
}

@Composable
private fun LetterKeyboard(onKey: (Char) -> Unit, onBackspace: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Palette.KeyboardBed)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (ch in "QWERTYUIOP") Key(Modifier.weight(1f), onClick = { onKey(ch) }) { KeyLabel(ch) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Spacer(Modifier.weight(0.5f))
            for (ch in "ASDFGHJKL") Key(Modifier.weight(1f), onClick = { onKey(ch) }) { KeyLabel(ch) }
            Spacer(Modifier.weight(0.5f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Spacer(Modifier.weight(1.5f))
            for (ch in "ZXCVBNM") Key(Modifier.weight(1f), onClick = { onKey(ch) }) { KeyLabel(ch) }
            Key(Modifier.weight(1.5f), onClick = onBackspace) {
                Icon(Glyphs.Backspace, contentDescription = "Delete", tint = Palette.Ink, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun KeyLabel(ch: Char) {
    Text(ch.toString(), fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Palette.Ink)
}

@Composable
private fun Key(modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Palette.Key)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Covers the grid while paused, so the clock and the puzzle stop together. */
@Composable
private fun PauseMenu(
    visible: Boolean,
    d: Difficulty,
    level: Int,
    state: GameState,
    onResume: () -> Unit,
    onLevels: () -> Unit,
    onHome: () -> Unit,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(140))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Palette.Paper)
                // Nothing behind the menu takes taps.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .animateEnterExit(
                        enter = slideInVertically(tween(280, easing = EaseOutStrong)) { it / 24 },
                        exit = ExitTransition.None,
                    )
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))
                MiniGrid(state, 156.dp)
                Spacer(Modifier.height(28.dp))
                Text("Paused", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 40.sp)
                Text("${d.label}, level ${level.grouped()}", color = Palette.InkSoft, fontSize = 16.sp)
                Spacer(Modifier.height(26.dp))
                val left = Hints.left(state.hintsUsed)
                Row(Modifier.fillMaxWidth(0.8f)) {
                    PauseFigure("${state.solvedWords} of ${state.puzzle.entries.size}", "words solved", Modifier.weight(1f))
                    PauseFigure("$left", if (left == 1) "hint left" else "hints left", Modifier.weight(1f))
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Accent),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) { Text("Resume", fontSize = 17.sp) }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onLevels,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text("All ${d.label.lowercase()} levels", color = Palette.Ink, fontSize = 15.sp) }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Home", color = Palette.Ink, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun PauseFigure(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp)
        Text(label, color = Palette.InkSoft, fontSize = 13.sp)
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "-"
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
private fun ResultsPanel(
    d: Difficulty,
    level: Int,
    state: GameState,
    solvedCount: Int,
    total: Int,
    onNext: (() -> Unit)?,
    onLevels: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Palette.Card)
            .drawBehind { drawLine(Palette.Line, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Solved", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp)
            Spacer(Modifier.weight(1f))
            Text(
                "${d.label} level ${level.grouped()}",
                color = Palette.InkSoft,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 7.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat(formatTime(state.elapsedMs), "time", 0)
            Stat("${state.hintsUsed}", if (state.hintsUsed == 1) "hint" else "hints", 1)
            Stat("${state.puzzle.entries.size}", "words", 2)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { solvedCount.toFloat() / total },
                modifier = Modifier.weight(1f).height(5.dp),
                color = Palette.of(d),
                trackColor = Palette.Line,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Spacer(Modifier.width(12.dp))
            Text("${solvedCount.grouped()} / ${total.grouped()}", color = Palette.InkSoft, fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))
        if (onNext != null) {
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Accent),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Next level", fontSize = 17.sp) }
        }
        TextButton(onClick = onLevels, modifier = Modifier.fillMaxWidth()) {
            Text("All levels", color = Palette.Ink, fontSize = 15.sp)
        }
    }
}

/** One figure in the results row; the three arrive one after another. */
@Composable
private fun RowScope.Stat(value: String, label: String, index: Int) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(380L + index * 70L)
        appear.animateTo(1f, tween(260, easing = EaseOutStrong))
    }
    Column(
        Modifier
            .weight(1f)
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 8.dp.toPx()
            },
    ) {
        Text(value, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
        Text(label, color = Palette.InkSoft, fontSize = 13.sp)
    }
}

private class Piece(
    val x: Float,
    val vx: Float,
    val vy: Float,
    val spin: Float,
    val w: Float,
    val h: Float,
    val color: Color,
    val delay: Float,
)

/** A one-off burst over the grid when a level is solved. */
@Composable
private fun Confetti(modifier: Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(300)
        progress.animateTo(1f, tween(2200, easing = LinearEasing))
    }
    val pieces = remember {
        val r = Random(11)
        val colors = listOf(Palette.Cursor, Palette.Solved, Palette.SolvedCursor, Palette.Accent, Palette.Revealed, Palette.Wrong)
        List(90) {
            Piece(
                x = 0.1f + r.nextFloat() * 0.8f,
                vx = (r.nextFloat() - 0.5f) * 0.5f,
                vy = -(0.7f + r.nextFloat() * 0.9f),
                spin = (r.nextFloat() - 0.5f) * 3f,
                w = 5f + r.nextFloat() * 4f,
                h = 9f + r.nextFloat() * 6f,
                color = colors[r.nextInt(colors.size)],
                delay = r.nextFloat() * 0.12f,
            )
        }
    }
    Canvas(modifier) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        for (piece in pieces) {
            val local = ((t - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val secs = local * 2.2f
            val x = size.width * (piece.x + piece.vx * secs)
            val y = size.height * (0.3f + (piece.vy * secs + 0.9f * secs * secs) * 0.5f)
            val alpha = if (local > 0.75f) (1f - local) / 0.25f else 1f
            val w = piece.w.dp.toPx()
            val h = piece.h.dp.toPx()
            rotate(degrees = piece.spin * secs * 360f, pivot = Offset(x, y)) {
                drawRect(piece.color.copy(alpha = alpha), Offset(x - w / 2, y - h / 2), Size(w, h))
            }
        }
    }
}
