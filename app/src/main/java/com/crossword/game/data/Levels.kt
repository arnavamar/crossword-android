package com.crossword.game.data

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import com.crossword.game.model.Direction
import com.crossword.game.model.GameState
import com.crossword.game.model.Puzzle
import com.crossword.game.model.Word

/** [words] is how many words each level of this difficulty holds. */
enum class Difficulty(val label: String, val key: String, val words: IntRange) {
    EASY("Easy", "easy", 8..10),
    MEDIUM("Medium", "medium", 12..14),
    HARD("Hard", "hard", 16..18),
}

/** Reads the generated level files from assets, one difficulty at a time, on first use. */
class PuzzleRepository(private val assets: AssetManager) {
    private class Pack(val words: List<Word>, val levels: List<String>)

    private val packs = mutableMapOf<Difficulty, Pack>()

    private fun pack(d: Difficulty): Pack = packs.getOrPut(d) {
        val words = readLines("${d.key}_words.tsv").map(Word::parse)
        Pack(words, readLines("${d.key}_levels.txt"))
    }

    private fun readLines(name: String): List<String> =
        assets.open(name).bufferedReader().use { r -> r.readLines().filter { it.isNotBlank() } }

    fun levelCount(d: Difficulty): Int = pack(d).levels.size

    /** [level] is 1-based. */
    fun puzzle(d: Difficulty, level: Int): Puzzle = Puzzle.parse(pack(d).levels[level - 1], pack(d).words)

    /** Changes if a level's content changes, so stale saved progress is ignored. */
    fun signature(d: Difficulty, level: Int): Int = pack(d).levels[level - 1].hashCode()
}

/** Solved levels and in-progress grids, kept in SharedPreferences. */
class ProgressStore(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("progress", Context.MODE_PRIVATE))

    /** Index i is true when level i + 1 is solved. */
    fun solvedFlags(d: Difficulty, total: Int): BooleanArray {
        val s = prefs.getString("solved_${d.key}", "") ?: ""
        return BooleanArray(total) { i -> i < s.length && s[i] == '1' }
    }

    fun solvedCount(d: Difficulty): Int = (prefs.getString("solved_${d.key}", "") ?: "").count { it == '1' }

    /** First unsolved level (1-based), or the last level when all are solved. */
    fun nextLevel(d: Difficulty, total: Int): Int {
        val i = solvedFlags(d, total).indexOfFirst { !it }
        return if (i < 0) total else i + 1
    }

    fun markSolved(d: Difficulty, level: Int) {
        val sb = StringBuilder(prefs.getString("solved_${d.key}", "") ?: "")
        while (sb.length < level) sb.append('0')
        sb.setCharAt(level - 1, '1')
        prefs.edit().putString("solved_${d.key}", sb.toString()).apply()
    }

    /**
     * Levels (1-based) with at least one letter saved in the grid, solved or not. A save counts only
     * when it carries the level's current [signature], the way loadGame accepts it: after an update
     * that changed a level, openGame drops the old save, so the level is not started.
     */
    fun startedLevels(d: Difficulty, signature: (level: Int) -> Int): Set<Int> {
        val prefix = "game_${d.key}_"
        val started = HashSet<Int>()
        for ((key, value) in prefs.all) {
            if (!key.startsWith(prefix)) continue
            val level = key.removePrefix(prefix).toIntOrNull() ?: continue
            val parts = savedParts(value as? String ?: continue, signature(level)) ?: continue
            if (parts[1].isNotBlank()) started.add(level)
        }
        return started
    }

    /** The fields of a saved game, or null when it is not a save of the level with this [signature]. */
    private fun savedParts(saved: String, signature: Int): List<String>? {
        val parts = saved.split("|")
        // Six fields were saved before the timer existed; those load with no time.
        return parts.takeIf { it.size in 6..7 && it[0] == signature.toString() }
    }

    fun saveGame(d: Difficulty, level: Int, signature: Int, s: GameState) {
        val value = listOf(
            signature.toString(),
            s.letters,
            s.revealed.joinToString(","),
            s.hintsUsed.toString(),
            s.selected.toString(),
            s.direction.name,
            s.elapsedMs.toString(),
        ).joinToString("|")
        prefs.edit().putString("game_${d.key}_$level", value).apply()
    }

    fun loadGame(d: Difficulty, level: Int, signature: Int, puzzle: Puzzle): GameState? {
        val parts = prefs.getString("game_${d.key}_$level", null)?.let { savedParts(it, signature) } ?: return null
        if (parts[1].length != puzzle.size) return null
        val start = GameState.start(puzzle)
        return start.copy(
            letters = parts[1],
            revealed = parts[2].split(",").filter { it.isNotEmpty() }.map { it.toInt() }.toSet(),
            hintsUsed = parts[3].toInt(),
            selected = parts[4].toInt(),
            direction = Direction.valueOf(parts[5]),
            elapsedMs = parts.getOrNull(6)?.toLongOrNull() ?: 0L,
        ).let { if (it.puzzle.isOpen(it.selected)) it else it.copy(selected = start.selected) }
    }
}
