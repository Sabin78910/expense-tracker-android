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

/** Operator keys on the number pad; each is only accepted straight after a number. */
const val KEYPAD_OPERATORS = "+-×÷"

/** Applies one number-pad [key] (digit, '.', operator, or [KEYPAD_BACKSPACE]) to [current]; limits apply per number. */
fun keypadPress(current: String, key: Char): String {
    val number = current.takeLastWhile { it.isDigit() || it == '.' }
    return when {
        key == KEYPAD_BACKSPACE -> current.dropLast(1)
        key in KEYPAD_OPERATORS -> when {
            number == "." -> current
            number.isNotEmpty() -> current + key
            current.isEmpty() -> current
            else -> current.dropLast(1) + key
        }
        key == '.' -> when {
            '.' in number -> current
            number.isEmpty() -> current + "0."
            number.length >= MAX_KEYPAD_DIGITS -> current
            else -> "$current."
        }
        key in '0'..'9' -> when {
            number.length >= MAX_KEYPAD_DIGITS -> current
            '.' in number && number.substringAfter('.').length >= MAX_DECIMALS -> current
            else -> current + key
        }
        else -> current
    }
}
