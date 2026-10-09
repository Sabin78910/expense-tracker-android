package com.sabin.expensetracker

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CategoryShare(val category: String, val amount: Double, val percent: Int)

data class DayTotal(val date: LocalDate, val total: Double)

/** Category totals for [year]/[month] in [zone], highest first. */
fun monthCategoryTotals(
    expenses: List<Expense>,
    year: Int,
    month: Int,
    zone: ZoneId = ZoneId.systemDefault()
): List<Pair<String, Double>> =
    expenses.filter {
        val d = Instant.ofEpochMilli(it.timestamp).atZone(zone)
        d.year == year && d.monthValue == month
    }.groupBy { it.category }
        .map { (c, l) -> c to l.sumOf { it.amount } }
        .sortedByDescending { it.second }

/** Whole-number percentages that always sum to 100 (largest-remainder); empty when nothing was spent. */
fun categoryShares(totals: List<Pair<String, Double>>): List<CategoryShare> {
    val sum = totals.sumOf { it.second }
    if (totals.isEmpty() || sum <= 0.0) return emptyList()
    val exact = totals.map { it.second / sum * 100 }
    val percents = exact.map { it.toInt() }.toMutableList()
    val leftover = 100 - percents.sum()
    exact.indices.sortedByDescending { exact[it] - percents[it] }.take(leftover).forEach { percents[it]++ }
    return totals.mapIndexed { i, (c, a) -> CategoryShare(c, a, percents[i]) }
}

/** Daily totals for the [days] days ending on [today], oldest first. */
fun dailyTotals(
    expenses: List<Expense>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
    days: Int = 7
): List<DayTotal> {
    val byDate = expenses.filter { it.timestamp > 0 }
        .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
    return (days - 1 downTo 0).map { back ->
        val d = today.minusDays(back.toLong())
        DayTotal(d, byDate[d].orEmpty().sumOf { it.amount })
    }
}

fun donutDescription(shares: List<CategoryShare>): String =
    if (shares.isEmpty()) "No spending this month"
    else "Spending by category: " + shares.joinToString(", ") { "${it.category} ${it.percent}%" }

fun barsDescription(days: List<DayTotal>): String =
    "Last 7 days spending: " + days.joinToString(", ") { "${it.date} ${formatNpr(it.total)}" }
