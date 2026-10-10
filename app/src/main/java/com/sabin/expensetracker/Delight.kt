package com.sabin.expensetracker

/** Streak lengths (in days) that get a milestone sheet. */
val STREAK_MILESTONES = listOf(7, 30, 100)

/** The highest milestone crossed when the streak goes from [before] to [after], or null. */
fun streakMilestoneReached(before: Int, after: Int): Int? =
    STREAK_MILESTONES.filter { it in (before + 1)..after }.maxOrNull()

fun streakGrew(before: Int, after: Int): Boolean = after > before

const val KEYPAD_BACKSPACE = '<'
const val MAX_KEYPAD_DIGITS = 9
private const val MAX_DECIMALS = 2

/** Applies one number-pad [key] (digit, '.', or [KEYPAD_BACKSPACE]) to [current]. */
fun keypadPress(current: String, key: Char): String = when {
    key == KEYPAD_BACKSPACE -> current.dropLast(1)
    key == '.' -> when {
        '.' in current -> current
        current.isEmpty() -> "0."
        current.length >= MAX_KEYPAD_DIGITS -> current
        else -> "$current."
    }
    key in '0'..'9' -> when {
        current.length >= MAX_KEYPAD_DIGITS -> current
        '.' in current && current.substringAfter('.').length >= MAX_DECIMALS -> current
        else -> current + key
    }
    else -> current
}
