package com.sabin.expensetracker

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Streak(val current: Int, val best: Int)

/**
 * Consecutive days with at least one expense. The current streak stays alive if
 * today is not logged yet (it ends only when a whole day is missed).
 */
fun loggingStreak(
    expenses: List<Expense>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): Streak {
    val days = expenses.filter { it.timestamp > 0 }
        .map { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        .toSortedSet()
    var best = 0
    var run = 0
    var prev: LocalDate? = null
    var lastRun = 0
    for (d in days) {
        run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
        best = maxOf(best, run)
        prev = d
        lastRun = run
    }
    val alive = prev != null && (prev == today || prev == today.minusDays(1))
    return Streak(if (alive) lastRun else 0, best)
}

fun streakLabel(streak: Streak): String = "🔥 ${streak.current}-day streak"
