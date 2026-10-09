package com.sabin.expensetracker

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val DECIMAL = Regex("""\d*\.?\d*""")

/** True when [text] is only digits with at most one decimal point. */
fun isValidDecimalInput(text: String): Boolean = DECIMAL.matches(text)

/** Returns [new] if valid, otherwise keeps [old]. */
fun filterDecimalInput(old: String, new: String): String = if (isValidDecimalInput(new)) new else old

/** Short weekday name (Mon, Tue...) in [locale]. */
fun weekdayLabel(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
