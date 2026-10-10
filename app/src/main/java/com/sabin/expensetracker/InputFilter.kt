package com.sabin.expensetracker

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val DECIMAL = Regex("""\d*\.?\d*""")

/** True when [text] is only digits with at most one decimal point. */
fun isValidDecimalInput(text: String): Boolean = DECIMAL.matches(text)

/** Returns [new] if valid, otherwise keeps [old]. */
fun filterDecimalInput(old: String, new: String): String = if (isValidDecimalInput(new)) new else old

/** Returns [new] if it is a valid single-line note within [MAX_NOTE_LENGTH], otherwise keeps [old]. */
fun filterNoteInput(old: String, new: String): String =
    if (new.length <= MAX_NOTE_LENGTH && '\n' !in new && '\r' !in new) new else old

/** Short weekday name (Mon, Tue...) in [locale]. */
fun weekdayLabel(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
