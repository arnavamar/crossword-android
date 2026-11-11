package com.crossword.game.model

/** Words checked by one change: [solved] just turned correct, [wrong] are full but incorrect. */
data class WordCheck(val solved: List<Entry>, val wrong: List<Entry>) {
    val isEmpty: Boolean get() = solved.isEmpty() && wrong.isEmpty()

    companion object {
        val NONE = WordCheck(emptyList(), emptyList())
    }
}

/**
 * Everything about one level in play. Immutable: every action returns a new state.
 * [letters] holds one character per square, ' ' when empty. [elapsedMs] is time spent playing.
 */
data class GameState(
    val puzzle: Puzzle,
    val letters: String,
    val revealed: Set<Int>,
    val hintsUsed: Int,
    val selected: Int,
    val direction: Direction,
    val elapsedMs: Long = 0,
) {
    /** Squares of every word that is fully correct. These show green. */
    val solvedCells: Set<Int> by lazy {
        puzzle.entries.filter(::isCorrect).flatMap { it.cells(puzzle.width) }.toSet()
    }

    /** Squares that can no longer change: correct words and hinted letters. */
    val locked: Set<Int> by lazy { solvedCells + revealed }

    val currentEntry: Entry
        get() = puzzle.entryAt(selected, direction) ?: puzzle.entryAt(selected, direction.other())!!

    val isFilled: Boolean
        get() = (0 until puzzle.size).all { !puzzle.isOpen(it) || letters[it] != ' ' }

    val isSolved: Boolean
        get() = (0 until puzzle.size).all { !puzzle.isOpen(it) || letters[it] == puzzle.solution[it] }

    val solvedWords: Int get() = puzzle.entries.count(::isCorrect)

    fun isFull(entry: Entry): Boolean = entry.cells(puzzle.width).all { letters[it] != ' ' }

    fun isCorrect(entry: Entry): Boolean = entry.cells(puzzle.width).all { letters[it] == puzzle.solution[it] }

    fun letterAt(cell: Int): Char? = letters[cell].takeIf { it != ' ' }

    /**
     * Tapping a new square selects it, preferring the direction whose word is unsolved.
     * Tapping the selected square flips across/down.
     */
    fun select(cell: Int): GameState {
        if (cell !in 0 until puzzle.size || !puzzle.isOpen(cell)) return this
        if (cell == selected) {
            val other = direction.other()
            return if (puzzle.entryAt(cell, other) != null) copy(direction = other) else this
        }
        val here = puzzle.entryAt(cell, direction)
        val there = puzzle.entryAt(cell, direction.other())
        val dir = when {
            here == null -> direction.other()
            there != null && isCorrect(here) && !isCorrect(there) -> direction.other()
            else -> direction
        }
        return copy(selected = cell, direction = dir)
    }

    /** Jumps to an entry, landing on its first empty square. */
    fun selectEntry(entry: Entry): GameState {
        val cells = entry.cells(puzzle.width)
        val target = cells.firstOrNull { letters[it] == ' ' } ?: cells.first()
        return copy(selected = target, direction = entry.direction)
    }

    /**
     * Moves to the next (+1) or previous (-1) clue that is not yet correct, across clues first,
     * then down. Solved words are skipped.
     */
    fun moveEntry(step: Int): GameState {
        val all = puzzle.entries
        val start = all.indexOf(currentEntry)
        for (k in 1..all.size) {
            val entry = all[Math.floorMod(start + step * k, all.size)]
            if (!isCorrect(entry)) return selectEntry(entry)
        }
        return this
    }

    /** Selects the next word after the current one that is not yet correct. */
    fun moveToNextUnsolved(): GameState = moveEntry(1)

    /** Writes a letter in the selected square and steps forward. Locked squares never change. */
    fun type(ch: Char): GameState {
        val entry = currentEntry
        val cells = entry.cells(puzzle.width)
        val newLetters = if (selected in locked) letters else letters.replaceAt(selected, ch.uppercaseChar())
        val next = cells.drop(cells.indexOf(selected) + 1).firstOrNull { it !in locked } ?: selected
        return copy(letters = newLetters, selected = next, direction = entry.direction)
    }

    /** Clears the selected square, or if it is empty or locked, steps back to the previous unlocked one. */
    fun backspace(): GameState {
        val entry = currentEntry
        if (letters[selected] != ' ' && selected !in locked) {
            return copy(letters = letters.replaceAt(selected, ' '), direction = entry.direction)
        }
        val cells = entry.cells(puzzle.width)
        val prev = cells.take(cells.indexOf(selected)).lastOrNull { it !in locked } ?: return this
        return copy(letters = letters.replaceAt(prev, ' '), selected = prev, direction = entry.direction)
    }

    /**
     * Call on the new state with the state before one action. Checks every word through the
     * square that changed once that word is full, and moves on if the current word just locked.
     */
    fun afterChange(before: GameState): Pair<GameState, WordCheck> {
        val cell = (0 until puzzle.size).firstOrNull { letters[it] != before.letters[it] }
            ?: return this to WordCheck.NONE
        val full = puzzle.entriesThrough(cell).filter(::isFull)
        val check = WordCheck(
            solved = full.filter { isCorrect(it) && !before.isCorrect(it) },
            wrong = full.filterNot(::isCorrect),
        )
        val next = if (!isSolved && currentEntry in check.solved) moveToNextUnsolved() else this
        return next to check
    }

    /**
     * The square a hint fills: the selected square if it is wrong or empty, else the first
     * wrong or empty square of the current word, else the first one in the grid. Null when solved.
     */
    fun hintTarget(): Int? {
        fun wrong(cell: Int) = puzzle.isOpen(cell) && letters[cell] != puzzle.solution[cell]
        if (wrong(selected)) return selected
        currentEntry.cells(puzzle.width).firstOrNull(::wrong)?.let { return it }
        return (0 until puzzle.size).firstOrNull(::wrong)
    }

    /** Fills [cell] with its correct letter, locks it, and counts one hint. */
    fun reveal(cell: Int): GameState {
        val dir = if (puzzle.entryAt(cell, direction) != null) direction else direction.other()
        return copy(
            letters = letters.replaceAt(cell, puzzle.solution[cell]!!),
            revealed = revealed + cell,
            hintsUsed = hintsUsed + 1,
            selected = cell,
            direction = dir,
        )
    }

    companion object {
        fun start(puzzle: Puzzle): GameState {
            val first = puzzle.entries.first()
            return GameState(
                puzzle = puzzle,
                letters = " ".repeat(puzzle.size),
                revealed = emptySet(),
                hintsUsed = 0,
                selected = first.cells(puzzle.width).first(),
                direction = first.direction,
            )
        }
    }
}

private fun String.replaceAt(index: Int, ch: Char): String =
    StringBuilder(this).also { it.setCharAt(index, ch) }.toString()
