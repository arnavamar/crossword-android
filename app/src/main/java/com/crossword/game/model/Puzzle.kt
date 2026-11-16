package com.crossword.game.model

enum class Direction {
    ACROSS, DOWN;

    fun other(): Direction = if (this == ACROSS) DOWN else ACROSS
}

/** An answer and the clues that can be shown for it. */
class Word(val answer: String, val clues: List<String>)

data class Entry(
    val number: Int,
    val row: Int,
    val col: Int,
    val direction: Direction,
    val answer: String,
    val clue: String,
) {
    /** Indices (row * width + col) of the squares this entry covers, in reading order. */
    fun cells(width: Int): List<Int> = List(answer.length) { i ->
        if (direction == Direction.ACROSS) row * width + col + i else (row + i) * width + col
    }
}

class Puzzle(val width: Int, val height: Int, val entries: List<Entry>) {
    val size = width * height

    /** Correct letter per square, or null where the grid has no square. */
    val solution: Array<Char?> = arrayOfNulls(size)

    /** Clue number printed in each square, 0 for none. */
    val numbers = IntArray(size)

    private val byCell: Array<MutableList<Entry>> = Array(size) { mutableListOf() }

    init {
        for (entry in entries) {
            entry.cells(width).forEachIndexed { i, cell ->
                val letter = entry.answer[i]
                require(solution[cell] == null || solution[cell] == letter) {
                    "Crossing letters disagree at square $cell"
                }
                solution[cell] = letter
                byCell[cell].add(entry)
            }
            numbers[entry.row * width + entry.col] = entry.number
        }
    }

    val across: List<Entry> get() = entries.filter { it.direction == Direction.ACROSS }
    val down: List<Entry> get() = entries.filter { it.direction == Direction.DOWN }

    fun isOpen(cell: Int): Boolean = solution[cell] != null

    fun entryAt(cell: Int, direction: Direction): Entry? =
        byCell[cell].firstOrNull { it.direction == direction }

    /** The one or two entries that pass through [cell]. */
    fun entriesThrough(cell: Int): List<Entry> = byCell[cell]

    companion object {
        /**
         * Builds a puzzle from one line of a levels file, "W,H;id,row,col,dir[,k];...",
         * where id indexes [words], dir is 0 across, 1 down, and k picks which of the
         * word's clues to show (the first when left out; one the word lacks is rejected).
         * Numbers are assigned the standard way: start squares in reading order.
         */
        fun parse(line: String, words: List<Word>): Puzzle {
            val parts = line.trim().split(';')
            val (width, height) = parts[0].split(',').map { it.toInt() }
            data class Raw(val row: Int, val col: Int, val dir: Direction, val word: Word, val clue: String)
            val raws = parts.drop(1).map { part ->
                val fields = part.split(',').map { it.toInt() }
                require(fields.size in 4..5) { "A level entry has 4 or 5 fields: $part" }
                val word = words[fields[0]]
                val k = fields.getOrElse(4) { 0 }
                require(k in word.clues.indices) { "${word.answer} has no clue $k: $part" }
                Raw(fields[1], fields[2], if (fields[3] == 0) Direction.ACROSS else Direction.DOWN, word, word.clues[k])
            }
            val starts = raws.map { it.row * width + it.col }.distinct().sorted()
            val numberOf = starts.withIndex().associate { (i, cell) -> cell to i + 1 }
            val entries = raws.map { raw ->
                Entry(
                    number = numberOf.getValue(raw.row * width + raw.col),
                    row = raw.row,
                    col = raw.col,
                    direction = raw.dir,
                    answer = raw.word.answer,
                    clue = raw.clue,
                )
            }.sortedWith(compareBy({ it.direction }, { it.number }))
            return Puzzle(width, height, entries)
        }
    }
}
