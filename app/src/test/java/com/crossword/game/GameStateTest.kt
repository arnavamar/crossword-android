package com.crossword.game

import com.crossword.game.model.Direction
import com.crossword.game.model.GameState
import com.crossword.game.model.Hints
import com.crossword.game.model.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {
    // C A T      CAT across at (0,0), CAR down at (0,0), TEA down at (0,2)
    // A . E
    // R . A
    private val words = listOf("CAT" to "Pet that purrs", "CAR" to "Vehicle", "TEA" to "Hot drink")
    private val puzzle = Puzzle.parse("3,3;0,0,0,0;1,0,0,1;2,0,2,1", words)

    @Test
    fun numbersFollowReadingOrder() {
        val cat = puzzle.across.single()
        assertEquals(1, cat.number)
        assertEquals(listOf(1, 2), puzzle.down.map { it.number })
        assertEquals("TEA", puzzle.down.single { it.number == 2 }.answer)
        assertFalse(puzzle.isOpen(4))
    }

    @Test
    fun typingFillsAndAdvancesWithinTheWord() {
        var s = GameState.start(puzzle)
        assertEquals(Direction.ACROSS, s.direction)
        s = s.type('c').type('a').type('t')
        assertEquals("CAT", s.letters.substring(0, 3))
        assertEquals(2, s.selected) // stays on the last square of the word
    }

    @Test
    fun tappingTheSelectedSquareFlipsDirection() {
        var s = GameState.start(puzzle)
        s = s.select(0)
        assertEquals(Direction.DOWN, s.direction)
        assertEquals("CAR", s.currentEntry.answer)
        // A square with only a down word stays down.
        s = s.select(5)
        assertEquals("TEA", s.currentEntry.answer)
        assertEquals(s, s.select(4)) // gap square: no change
    }

    @Test
    fun backspaceClearsThenStepsBack() {
        var s = GameState.start(puzzle).type('C').type('A')
        assertEquals(2, s.selected)
        s = s.backspace() // square 2 empty: step back to 1 and clear it
        assertEquals(1, s.selected)
        assertEquals(' ', s.letters[1])
        s = s.type('X').select(1).select(1).select(1) // back on square 1, across
        s = s.backspace()
        assertEquals(' ', s.letters[1])
    }

    @Test
    fun hintRevealsSelectedSquareAndLocksIt() {
        var s = GameState.start(puzzle)
        val target = s.hintTarget()!!
        assertEquals(0, target)
        s = s.reveal(target)
        assertEquals('C', s.letters[0])
        assertEquals(1, s.hintsUsed)
        s = s.select(0).select(0) // still on square 0
        val typed = s.type('Z')
        assertEquals('C', typed.letters[0]) // locked
    }

    @Test
    fun hintSkipsCorrectSquares() {
        val s = GameState.start(puzzle).type('C').type('A')
        // Selected square 2 is empty, so it is the target.
        assertEquals(2, s.hintTarget())
        val onCorrect = s.select(0).select(0) // square 0 holds a correct C; second tap flips to CAR down
        assertEquals(Direction.DOWN, onCorrect.direction)
        assertEquals(3, onCorrect.hintTarget()) // first empty square of CAR
    }

    @Test
    fun solvedOnlyWhenEveryLetterIsRight() {
        var s = GameState.start(puzzle)
        s = s.type('C').type('A').type('T')
        s = s.select(3).type('A').type('R') // CAR down: squares 3, 6
        val almost = s.select(5).type('E') // TEA down: squares 5, 8; cursor now on 8
        val wrong = almost.type('X')
        assertTrue(wrong.isFilled)
        assertFalse(wrong.isSolved)
        val right = almost.type('A')
        assertTrue(right.isFilled)
        assertTrue(right.isSolved)
        assertNull(right.hintTarget())
    }

    @Test
    fun correctWordLocksAndCannotBeEdited() {
        val cat = puzzle.across.single()
        var s = GameState.start(puzzle).type('C').type('A').type('T')
        assertTrue(s.isCorrect(cat))
        assertEquals(setOf(0, 1, 2), s.solvedCells)
        assertTrue(s.locked.containsAll(listOf(0, 1, 2)))
        s = s.select(1)
        assertEquals("CAT", s.backspace().letters.substring(0, 3)) // nothing to clear
        assertEquals("CAT", s.type('Z').letters.substring(0, 3)) // typing skips locked squares
    }

    @Test
    fun crossingWordSkipsLockedSquares() {
        var s = GameState.start(puzzle).type('C').type('A').type('T') // CAT locks squares 0, 1, 2
        s = s.selectEntry(puzzle.down.first { it.answer == "CAR" })
        assertEquals(3, s.selected) // first empty square; C is already locked
        s = s.type('A')
        assertEquals(6, s.selected)
        s = s.backspace() // 6 empty: steps back to 3 and clears it
        assertEquals(3, s.selected)
        s = s.backspace() // 3 empty and only a locked square before it: no change
        assertEquals(3, s.selected)
        assertEquals('C', s.letters[0])
        val onLocked = s.copy(selected = 0, direction = Direction.DOWN).type('Z')
        assertEquals('C', onLocked.letters[0]) // locked square unchanged
        assertEquals(3, onLocked.selected) // and the cursor moves past it
    }

    @Test
    fun wrongWordIsReportedWhenItsLastSquareFills() {
        val start = GameState.start(puzzle)
        val before = start.type('C').type('A')
        val after = before.type('X')
        val (next, check) = after.afterChange(before)
        assertEquals(listOf("CAT"), check.wrong.map { it.answer })
        assertTrue(check.solved.isEmpty())
        assertEquals(after.selected, next.selected) // stays put so it can be fixed
        assertTrue(after.locked.isEmpty())
    }

    @Test
    fun correctWordIsReportedAndCursorMovesToNextUnsolvedWord() {
        val before = GameState.start(puzzle).type('C').type('A')
        val (next, check) = before.type('T').afterChange(before)
        assertEquals(listOf("CAT"), check.solved.map { it.answer })
        assertTrue(check.wrong.isEmpty())
        assertEquals("CAR", next.currentEntry.answer)
        assertEquals(3, next.selected)
    }

    @Test
    fun oneLetterCanCompleteTwoWords() {
        // Fill TEA's E and A first, then T completes both CAT and TEA.
        var s = GameState.start(puzzle).type('C').type('A')
        s = s.select(5).type('E').type('A').select(2)
        val (_, check) = s.type('T').afterChange(s)
        assertEquals(setOf("CAT", "TEA"), check.solved.map { it.answer }.toSet())
    }

    @Test
    fun retypingTheSameLetterChecksNothing() {
        val s = GameState.start(puzzle).type('C').type('A').type('X')
        assertEquals(2, s.selected)
        val (_, check) = s.type('X').afterChange(s)
        assertTrue(check.wrong.isEmpty() && check.solved.isEmpty())
    }

    @Test
    fun arrowsSkipSolvedWords() {
        val car = puzzle.down.first { it.answer == "CAR" }
        var s = GameState.start(puzzle).selectEntry(car).type('C').type('A').type('R')
        assertTrue(s.isCorrect(car))
        s = s.selectEntry(puzzle.across.single()) // on CAT; clue order is CAT, CAR, TEA
        val next = s.moveEntry(1)
        assertEquals("TEA", next.currentEntry.answer) // CAR is solved, so it is skipped
        assertEquals("CAT", next.moveEntry(-1).currentEntry.answer) // and skipped going back
    }

    @Test
    fun threeHintsPerLevel() {
        assertEquals(3, Hints.left(0))
        assertEquals(1, Hints.left(2))
        assertEquals(0, Hints.left(3))
        assertEquals(0, Hints.left(5))
    }
}
