package com.sabin.expensetracker

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgesTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 3, 10)
    private fun e(date: LocalDate, amount: Double = 1.0) =
        Expense(1, "x", amount, "Food", date.atTime(9, 0).toInstant(zone).toEpochMilli())
    private fun earned(
        expenses: List<Expense>,
        budget: Double = 0.0,
        backedUp: Boolean = false
    ) = earnedBadges(expenses, budget, backedUp, today, zone)

    @Test fun nothingWhenEmpty() = assertEquals(emptySet<Badge>(), earned(emptyList()))

    @Test fun firstExpense() = assertTrue(Badge.FIRST_EXPENSE in earned(listOf(e(today))))

    @Test fun sevenDayStreak() {
        val six = (0..5).map { e(today.minusDays(it.toLong())) }
        assertFalse(Badge.STREAK_7 in earned(six))
        assertTrue(Badge.STREAK_7 in earned(six + e(today.minusDays(6))))
    }

    @Test fun sevenDayStreakStaysEarnedAfterItBreaks() {
        val old = (20..26).map { e(today.minusDays(it.toLong())) }
        assertTrue(Badge.STREAK_7 in earned(old))
    }

    @Test fun noSpendDayNeedsAGapAfterFirstExpense() {
        assertFalse(Badge.NO_SPEND_DAY in earned(listOf(e(today.minusDays(1)), e(today))))
        assertTrue(Badge.NO_SPEND_DAY in earned(listOf(e(today.minusDays(2)), e(today))))
    }

    @Test fun todayWithoutExpenseIsNotYetNoSpendDay() {
        assertFalse(Badge.NO_SPEND_DAY in earned(listOf(e(today.minusDays(1)))))
    }

    @Test fun budgetKeptForCompletedMonth() {
        val feb = e(LocalDate.of(2026, 2, 5), 400.0)
        assertTrue(Badge.BUDGET_KEPT_MONTH in earned(listOf(feb), budget = 500.0))
    }

    @Test fun budgetExceededOrUnsetOrCurrentMonthDoesNotCount() {
        val feb = e(LocalDate.of(2026, 2, 5), 600.0)
        assertFalse(Badge.BUDGET_KEPT_MONTH in earned(listOf(feb), budget = 500.0))
        assertFalse(Badge.BUDGET_KEPT_MONTH in earned(listOf(e(LocalDate.of(2026, 2, 5), 1.0)), budget = 0.0))
        assertFalse(Badge.BUDGET_KEPT_MONTH in earned(listOf(e(today, 1.0)), budget = 500.0))
    }

    @Test fun firstBackup() {
        assertFalse(Badge.FIRST_BACKUP in earned(emptyList()))
        assertTrue(Badge.FIRST_BACKUP in earned(emptyList(), backedUp = true))
    }

    @Test fun storeRoundTripAndNewlyUnlocked() {
        val s = BadgeStore.parse(BadgeStore.serialize(setOf(Badge.FIRST_EXPENSE, Badge.FIRST_BACKUP)))
        assertEquals(setOf(Badge.FIRST_EXPENSE, Badge.FIRST_BACKUP), s)
        assertEquals(setOf(Badge.STREAK_7), newlyUnlocked(s, s + Badge.STREAK_7))
        assertEquals(emptySet<Badge>(), BadgeStore.parse("bogus,"))
    }
}
