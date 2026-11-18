package com.crossword.game

import com.crossword.game.model.Puzzle
import com.crossword.game.model.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleParseTest {
    // C A T      CAT across at (0,0), CAR down at (0,0), TEA down at (0,2)
    // A . E
    // R . A
    private val words = listOf(
        Word("CAT", listOf("Pet that purrs", "Tom or tabby")),
        Word("CAR", listOf("Vehicle")),
        Word("TEA", listOf("Hot drink", "Leaves in hot water")),
    )

    @Test
    fun fifthFieldPicksTheClue() {
        val p = Puzzle.parse("3,3;0,0,0,0,1;1,0,0,1,0;2,0,2,1", words)
        assertEquals("Tom or tabby", p.across.single().clue)
        assertEquals("Vehicle", p.down.single { it.answer == "CAR" }.clue)
        assertEquals("Hot drink", p.down.single { it.answer == "TEA" }.clue) // no fifth field
    }

    @Test(expected = IllegalArgumentException::class)
    fun aClueIndexTheWordLacksIsRejected() {
        Puzzle.parse("3,3;0,0,0,0;1,0,0,1,1;2,0,2,1", words) // CAR has one clue
    }

    @Test
    fun anEntryWithThreeFieldsIsRejected() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            Puzzle.parse("3,3;0,0,0,0;1,0,0,1;2,0,2", words)
        }
        assertTrue(e.message, e.message!!.contains("4 or 5 fields") && e.message!!.endsWith("2,0,2"))
    }

    @Test
    fun anEntryWithSixFieldsIsRejected() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            Puzzle.parse("3,3;0,0,0,0,0,0;1,0,0,1;2,0,2,1", words)
        }
        assertTrue(e.message, e.message!!.contains("4 or 5 fields") && e.message!!.endsWith("0,0,0,0,0,0"))
    }

    @Test
    fun aWordLineHoldsTheAnswerThenOneFieldPerClue() {
        val two = Word.parse("CAT\tPet that purrs\tTom or tabby")
        assertEquals("CAT", two.answer)
        assertEquals(listOf("Pet that purrs", "Tom or tabby"), two.clues)
        assertEquals(listOf("Vehicle"), Word.parse("CAR\tVehicle").clues) // a v1.2 line: one clue
    }
}
