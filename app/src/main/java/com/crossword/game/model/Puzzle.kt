package com.crossword.game.model

enum class Direction {
    ACROSS, DOWN;

    fun other(): Direction = if (this == ACROSS) DOWN else ACROSS
}

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
         * Builds a puzzle from one line of a levels file, "W,H;id,row,col,dir;...",
         * where id indexes [words] (answer to clue) and dir is 0 across, 1 down.
         * Numbers are assigned the standard way: start squares in reading order.
         */
        fun parse(line: String, words: List<Pair<String, String>>): Puzzle {
            val parts = line.trim().split(';')
            val (width, height) = parts[0].split(',').map { it.toInt() }
            data class Raw(val row: Int, val col: Int, val dir: Direction, val word: Pair<String, String>)
            val raws = parts.drop(1).map { part ->
                val (id, row, col, dir) = part.split(',').map { it.toInt() }
                Raw(row, col, if (dir == 0) Direction.ACROSS else Direction.DOWN, words[id])
            }
            val starts = raws.map { it.row * width + it.col }.distinct().sorted()
            val numberOf = starts.withIndex().associate { (i, cell) -> cell to i + 1 }
            val entries = raws.map { raw ->
                Entry(
                    number = numberOf.getValue(raw.row * width + raw.col),
                    row = raw.row,
                    col = raw.col,
                    direction = raw.dir,
                    answer = raw.word.first,
                    clue = raw.word.second,
                )
            }.sortedWith(compareBy({ it.direction }, { it.number }))
            return Puzzle(width, height, entries)
        }
    }
}
