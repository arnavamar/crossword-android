package com.crossword.game

import android.content.SharedPreferences
import com.crossword.game.data.Difficulty
import com.crossword.game.data.ProgressStore
import com.crossword.game.model.GameState
import com.crossword.game.model.Puzzle
import com.crossword.game.model.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** SharedPreferences held in a map, so the store can run without a device. */
private class MapPrefs : SharedPreferences {
    val map = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = map
    override fun getString(key: String, defValue: String?): String? = map[key] as? String ?: defValue
    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        override fun putString(key: String, value: String?): SharedPreferences.Editor { pending[key] = value; return this }
        override fun apply() { map.putAll(pending) }
        override fun commit(): Boolean { apply(); return true }
        override fun putStringSet(key: String, values: MutableSet<String>?) = TODO()
        override fun putInt(key: String, value: Int) = TODO()
        override fun putLong(key: String, value: Long) = TODO()
        override fun putFloat(key: String, value: Float) = TODO()
        override fun putBoolean(key: String, value: Boolean) = TODO()
        override fun remove(key: String) = TODO()
        override fun clear() = TODO()
    }

    override fun getStringSet(key: String, defValues: MutableSet<String>?) = TODO()
    override fun getInt(key: String, defValue: Int) = TODO()
    override fun getLong(key: String, defValue: Long) = TODO()
    override fun getFloat(key: String, defValue: Float) = TODO()
    override fun getBoolean(key: String, defValue: Boolean) = TODO()
    override fun contains(key: String) = TODO()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = TODO()
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = TODO()
}

class ProgressStoreTest {
    private val puzzle = Puzzle.parse(
        "3,3;0,0,0,0,0;1,0,0,1,0;2,0,2,1,0",
        listOf(Word("CAT", listOf("Pet that purrs")), Word("CAR", listOf("Vehicle")), Word("TEA", listOf("Hot drink"))),
    )
    private val oneLetter = GameState.start(puzzle).copy(letters = "C" + " ".repeat(puzzle.size - 1))
    private val noLetters = GameState.start(puzzle).copy(letters = " ".repeat(puzzle.size))

    private val store = ProgressStore(MapPrefs())

    @Test
    fun aSaveWithTheLevelsCurrentSignatureIsStarted() {
        store.saveGame(Difficulty.EASY, 3, signature = 111, s = oneLetter)
        assertEquals(setOf(3), store.startedLevels(Difficulty.EASY) { 111 })
    }

    @Test
    fun aSaveFromBeforeTheLevelChangedIsNotStarted() {
        // A v1.2 grid the player half filled: the level has a new signature in 1.3.
        store.saveGame(Difficulty.EASY, 3, signature = 111, s = oneLetter)
        assertEquals(emptySet<Int>(), store.startedLevels(Difficulty.EASY) { 222 })
        // openGame drops the same save, so the tile and the game agree.
        assertNull(store.loadGame(Difficulty.EASY, 3, signature = 222, puzzle = puzzle))
        assertNotNull(store.loadGame(Difficulty.EASY, 3, signature = 111, puzzle = puzzle))
    }

    @Test
    fun eachLevelIsComparedWithItsOwnSignature() {
        store.saveGame(Difficulty.EASY, 1, signature = 10, s = oneLetter)
        store.saveGame(Difficulty.EASY, 2, signature = 20, s = oneLetter)
        store.saveGame(Difficulty.HARD, 1, signature = 10, s = oneLetter)
        assertEquals(setOf(1), store.startedLevels(Difficulty.EASY) { level -> if (level == 1) 10 else 99 })
    }

    @Test
    fun aSaveWithNoLettersIsNotStarted() {
        store.saveGame(Difficulty.EASY, 3, signature = 111, s = noLetters)
        assertEquals(emptySet<Int>(), store.startedLevels(Difficulty.EASY) { 111 })
    }
}
