package com.crossword.game.model

/** Three hints per level. */
object Hints {
    const val PER_LEVEL = 3

    fun left(hintsUsed: Int): Int = (PER_LEVEL - hintsUsed).coerceAtLeast(0)
}
