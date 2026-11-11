package com.crossword.game

import com.crossword.game.model.Direction
import com.crossword.game.model.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Loads every shipped level through the app's parser and checks it is a clean crossword. */
class LevelAssetsTest {
    private val assets = File("src/main/assets")

    private fun words(tier: String) = File(assets, "${tier}_words.tsv").readLines().filter { it.isNotBlank() }.map {
        val (w, c) = it.split('\t')
        w to c
    }

    private fun levels(tier: String) = File(assets, "${tier}_levels.txt").readLines().filter { it.isNotBlank() }

    @Test
    fun atLeastTenThousandLevelsSplitIntoThreeDifficulties() {
        val counts = listOf("easy", "medium", "hard").associateWith { levels(it).size }
        assertEquals(mapOf("easy" to 3500, "medium" to 3500, "hard" to 3500), counts)
        assertTrue(counts.values.sum() >= 10_000)
    }

    @Test
    fun everyLevelIsAValidCrossword() {
        val limits = mapOf("easy" to (5..7 to 8), "medium" to (8..10 to 10), "hard" to (11..13 to 12))
        for ((tier, limit) in limits) {
            val (wordCount, maxSize) = limit
            val words = words(tier)
            val seen = HashSet<List<String>>()
            levels(tier).forEachIndexed { i, line ->
                val where = "$tier level ${i + 1}"
                val p = Puzzle.parse(line, words)
                assertTrue("$where size ${p.width}x${p.height}", p.width <= maxSize && p.height <= maxSize)
                assertTrue("$where has ${p.entries.size} words", p.entries.size in wordCount)
                assertTrue("$where repeats a word set", seen.add(p.entries.map { it.answer }.sorted()))
                assertTrue("$where has a duplicate word", p.entries.map { it.answer }.toSet().size == p.entries.size)
                assertTrue("$where has a blank clue", p.entries.all { it.clue.isNotBlank() })

                // Every run of 2+ squares is exactly one entry, so no accidental words.
                val expected = p.entries.map { Triple(it.row * p.width + it.col, it.direction, it.answer) }.toSet()
                val found = mutableSetOf<Triple<Int, Direction, String>>()
                for (cell in 0 until p.size) {
                    if (!p.isOpen(cell)) continue
                    val r = cell / p.width
                    val c = cell % p.width
                    if (c == 0 || !p.isOpen(cell - 1)) {
                        val run = StringBuilder()
                        var cc = c
                        while (cc < p.width && p.isOpen(r * p.width + cc)) run.append(p.solution[r * p.width + cc++])
                        if (run.length >= 2) found.add(Triple(cell, Direction.ACROSS, run.toString()))
                    }
                    if (r == 0 || !p.isOpen(cell - p.width)) {
                        val run = StringBuilder()
                        var rr = r
                        while (rr < p.height && p.isOpen(rr * p.width + c)) run.append(p.solution[rr++ * p.width + c])
                        if (run.length >= 2) found.add(Triple(cell, Direction.DOWN, run.toString()))
                    }
                }
                assertEquals("$where stray or missing words", expected, found)

                // All squares connected.
                val open = (0 until p.size).filter { p.isOpen(it) }
                val reached = mutableSetOf(open.first())
                val queue = ArrayDeque(listOf(open.first()))
                while (queue.isNotEmpty()) {
                    val x = queue.removeFirst()
                    val nbrs = listOfNotNull(
                        (x - p.width).takeIf { it >= 0 },
                        (x + p.width).takeIf { it < p.size },
                        (x - 1).takeIf { x % p.width != 0 },
                        (x + 1).takeIf { (x + 1) % p.width != 0 },
                    )
                    for (n in nbrs) if (p.isOpen(n) && reached.add(n)) queue.add(n)
                }
                assertEquals("$where is not connected", open.size, reached.size)
            }
        }
    }
}
