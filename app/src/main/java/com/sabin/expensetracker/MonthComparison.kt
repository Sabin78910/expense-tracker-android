package com.sabin.expensetracker

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

data class CategoryChange(val category: String, val delta: Double)

data class MonthComparison(
    val current: Double,
    val previous: Double,
    /** Rounded % change versus [previous]; null when [previous] is zero. */
    val percentChange: Int?,
    /** Category with the largest absolute change; null when nothing changed. */
    val topCategory: CategoryChange?
)

/**
 * Compares [month]'s spending up to a cutoff day with the previous month up to the same day
 * (capped at that month's length). The cutoff is [today]'s day for the current month, else the
 * month's end. Returns null when there is nothing to compare.
 */
fun compareWithPreviousMonth(
    expenses: List<Expense>,
    month: YearMonth,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
): MonthComparison? {
    val prevMonth = month.minusMonths(1)
    fun date(e: Expense) = Instant.ofEpochMilli(e.timestamp).atZone(zone).toLocalDate()
    val prevAll = expenses.filter { YearMonth.from(date(it)) == prevMonth }
    if (prevAll.isEmpty()) return null
    val cutoff = if (month == YearMonth.from(today)) today.dayOfMonth else month.lengthOfMonth()
    val cur = expenses.filter { val d = date(it); YearMonth.from(d) == month && d.dayOfMonth <= cutoff }
    val prev = prevAll.filter { date(it).dayOfMonth <= cutoff }
    val current = cur.sumOf { it.amount }
    val previous = prev.sumOf { it.amount }
    if (current == 0.0 && previous == 0.0) return null
    val percent = if (previous > 0) ((current - previous) / previous * 100).roundToInt() else null
    val curBy = cur.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }
    val prevBy = prev.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }
    val top = (curBy.keys + prevBy.keys)
        .map { CategoryChange(it, (curBy[it] ?: 0.0) - (prevBy[it] ?: 0.0)) }
        .filter { it.delta != 0.0 }
        .maxByOrNull { abs(it.delta) }
    return MonthComparison(current, previous, percent, top)
}
