package com.crossword.game

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.crossword.game.data.Difficulty
import com.crossword.game.data.ProgressStore
import com.crossword.game.data.PuzzleRepository
import com.crossword.game.model.Entry
import com.crossword.game.model.GameState
import com.crossword.game.model.Hints

sealed interface Screen {
    data object Home : Screen
    data class Levels(val difficulty: Difficulty) : Screen
    data class Game(val difficulty: Difficulty, val level: Int) : Screen
}

/** Words to animate after one change: each list is the squares of one word, in reading order. */
data class WordFeedback(
    val id: Long,
    val solved: List<List<Int>>,
    val wrong: List<List<Int>>,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repo = PuzzleRepository(app.assets)
    val store = ProgressStore(app)

    private val stack = mutableStateListOf<Screen>(Screen.Home)
    val screen: Screen get() = stack.last()

    /** Bumped whenever a level is solved, so progress readouts recompose. */
    var progressTick by mutableIntStateOf(0)
        private set

    var game by mutableStateOf<GameState?>(null)
        private set

    /** The pause menu is open over an unsolved level; the clock is stopped. */
    var paused by mutableStateOf(false)
        private set

    /** The latest words checked, for the grid to animate. */
    var feedback by mutableStateOf<WordFeedback?>(null)
        private set
    private var feedbackId = 0L

    /** Bumped when a level is solved while playing it (not when reopening a solved level). */
    var celebrations by mutableIntStateOf(0)
        private set

    /** Bumped when every square is filled but something is wrong. */
    var notQuiteEvents by mutableIntStateOf(0)
        private set

    private var active: Screen.Game? = null
    private var clockStartedAt: Long? = null

    fun openLevels(d: Difficulty) {
        stack.add(Screen.Levels(d))
    }

    fun openGame(d: Difficulty, level: Int) {
        pauseClock()
        val puzzle = repo.puzzle(d, level)
        game = store.loadGame(d, level, repo.signature(d, level), puzzle) ?: GameState.start(puzzle)
        paused = false
        feedback = null
        val target = Screen.Game(d, level)
        active = target
        stack.add(target)
    }

    fun back() {
        pauseClock()
        paused = false
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    /** System back: in an unsolved level it opens the pause menu, and closes it again. */
    fun onSystemBack() {
        val inPlay = screen is Screen.Game && game?.isSolved == false
        when {
            inPlay && paused -> resume()
            inPlay -> pause()
            else -> back()
        }
    }

    fun pause() {
        if (screen !is Screen.Game || game?.isSolved != false) return
        pauseClock()
        paused = true
    }

    fun resume() {
        paused = false
        resumeClock()
    }

    fun goHome() {
        pauseClock()
        paused = false
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun nextLevel() {
        val current = active ?: return
        pauseClock()
        stack.removeAt(stack.lastIndex)
        if (current.level < repo.levelCount(current.difficulty)) openGame(current.difficulty, current.level + 1)
    }

    /** From a level to that difficulty's level list. */
    fun showAllLevels() {
        val current = active ?: return
        pauseClock()
        paused = false
        stack.removeAt(stack.lastIndex)
        val levels = Screen.Levels(current.difficulty)
        if (stack.last() != levels) stack.add(levels)
    }

    /** The level timer runs only while its screen is in front, unpaused and unsolved. */
    fun resumeClock() {
        val s = game ?: return
        if (active != null && !paused && !s.isSolved && clockStartedAt == null) clockStartedAt = SystemClock.elapsedRealtime()
    }

    fun pauseClock() {
        val started = clockStartedAt ?: return
        clockStartedAt = null
        val s = game ?: return
        val level = active ?: return
        val stopped = s.copy(elapsedMs = s.elapsedMs + (SystemClock.elapsedRealtime() - started))
        game = stopped
        save(level, stopped)
    }

    fun onCell(cell: Int) = update { it.select(cell) }
    fun onEntry(entry: Entry) = update { it.selectEntry(entry) }
    fun onStep(step: Int) = update { it.moveEntry(step) }

    fun onKey(ch: Char) {
        if (game?.isSolved == true) return
        update { it.type(ch) }
    }

    fun onBackspace() {
        if (game?.isSolved == true) return
        update { it.backspace() }
    }

    fun onHint() {
        val s = game ?: return
        if (Hints.left(s.hintsUsed) == 0) return
        val target = s.hintTarget() ?: return
        update { it.reveal(target) }
    }

    private fun update(action: (GameState) -> GameState) {
        val before = game ?: return
        val level = active ?: return
        val changed = action(before)
        if (changed == before) return
        val (after, check) = changed.afterChange(before)
        game = after
        if (!check.isEmpty) {
            val width = after.puzzle.width
            feedback = WordFeedback(
                id = ++feedbackId,
                solved = check.solved.map { it.cells(width) },
                wrong = check.wrong.map { it.cells(width) },
            )
        }
        if (after.isSolved && !before.isSolved) {
            pauseClock() // stops the time at the moment of solving and saves it
            store.markSolved(level.difficulty, level.level)
            progressTick++
            celebrations++
        } else if (after.isFilled && !before.isFilled && !after.isSolved) {
            notQuiteEvents++
        }
        save(level, game!!)
    }

    private fun save(level: Screen.Game, s: GameState) {
        store.saveGame(level.difficulty, level.level, repo.signature(level.difficulty, level.level), s)
    }
}
