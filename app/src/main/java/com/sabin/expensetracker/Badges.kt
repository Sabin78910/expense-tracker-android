package com.sabin.expensetracker

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class Badge(val title: String, val description: String) {
    FIRST_EXPENSE("First step", "Log your first expense"),
    STREAK_7("Week warrior", "Log expenses 7 days in a row"),
    NO_SPEND_DAY("No-spend day", "Have a day with no spending"),
    BUDGET_KEPT_MONTH("Budget keeper", "Stay within budget for a whole month"),
    FIRST_BACKUP("Safety net", "Back up your expenses")
}

/** Badges earned so far, derived from the data. Pure logic; persist with [BadgeStore] so they stay unlocked. */
fun earnedBadges(
    expenses: List<Expense>,
    budget: Double,
    backedUp: Boolean,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): Set<Badge> {
    val dated = expenses.filter { it.timestamp > 0 }
        .map { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() to it.amount }
    val days = dated.map { it.first }.toSet()
    val earned = mutableSetOf<Badge>()
    if (expenses.isNotEmpty()) earned += Badge.FIRST_EXPENSE
    if (loggingStreak(expenses, today, zone).best >= 7) earned += Badge.STREAK_7
    val first = days.minOrNull()
    if (first != null && generateSequence(first) { it.plusDays(1) }.takeWhile { it < today }.any { it !in days }) {
        earned += Badge.NO_SPEND_DAY
    }
    if (budget > 0) {
        val thisMonth = YearMonth.from(today)
        val byMonth = dated.groupBy({ YearMonth.from(it.first) }, { it.second })
        if (byMonth.any { (m, list) -> m < thisMonth && list.sum() <= budget }) earned += Badge.BUDGET_KEPT_MONTH
    }
    if (backedUp) earned += Badge.FIRST_BACKUP
    return earned
}

fun newlyUnlocked(before: Set<Badge>, after: Set<Badge>): Set<Badge> = after - before

object BadgeStore {
    fun serialize(badges: Set<Badge>): String = badges.joinToString(",") { it.name }

    fun parse(data: String): Set<Badge> =
        data.split(",").mapNotNull { n -> Badge.values().firstOrNull { it.name == n } }.toSet()
}
