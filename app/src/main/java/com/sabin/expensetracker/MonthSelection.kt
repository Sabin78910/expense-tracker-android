package com.sabin.expensetracker

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Pure helpers for browsing months on the summary (no Android dependencies). */
object MonthSelection {
    /** Month before [selected], or null when already at [earliest]. */
    fun previous(selected: YearMonth, earliest: YearMonth): YearMonth? =
        selected.minusMonths(1).takeIf { !it.isBefore(earliest) }

    /** Month after [selected], or null when already at [latest]. */
    fun next(selected: YearMonth, latest: YearMonth): YearMonth? =
        selected.plusMonths(1).takeIf { !it.isAfter(latest) }

    fun canGoPrevious(selected: YearMonth, earliest: YearMonth) = previous(selected, earliest) != null

    fun canGoNext(selected: YearMonth, latest: YearMonth) = next(selected, latest) != null

    fun clamp(selected: YearMonth, earliest: YearMonth, latest: YearMonth): YearMonth =
        if (selected.isBefore(earliest)) earliest else if (selected.isAfter(latest)) latest else selected

    /** Month of the oldest expense, or [current] when there are none (or all are in the future). */
    fun earliest(expenses: List<Expense>, current: YearMonth, zone: ZoneId = ZoneId.systemDefault()): YearMonth =
        expenses.minOfOrNull { monthOf(it, zone) }?.takeIf { it.isBefore(current) } ?: current

    fun filter(expenses: List<Expense>, month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): List<Expense> =
        expenses.filter { monthOf(it, zone) == month }

    /** Localized "Month Year" label. */
    fun label(month: YearMonth, locale: Locale = Locale.getDefault()): String =
        month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale) + " " + month.year

    private fun monthOf(e: Expense, zone: ZoneId) = YearMonth.from(Instant.ofEpochMilli(e.timestamp).atZone(zone))
}
