package com.sabin.expensetracker

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {
    private val zone = ZoneOffset.UTC
    private fun e(y: Int, m: Int, d: Int) =
        Expense(1, "x", 1.0, "Food", LocalDate.of(y, m, d).atTime(9, 0).toInstant(zone).toEpochMilli())
    private fun streak(today: LocalDate, vararg days: Expense) = loggingStreak(days.toList(), today, zone)

    @Test fun emptyIsZero() {
        assertEquals(Streak(0, 0), streak(LocalDate.of(2026, 3, 10)))
    }

    @Test fun consecutiveDaysIncludingToday() {
        assertEquals(Streak(3, 3), streak(LocalDate.of(2026, 3, 10), e(2026, 3, 8), e(2026, 3, 9), e(2026, 3, 10)))
    }

    @Test fun multipleExpensesSameDayCountOnce() {
        assertEquals(Streak(1, 1), streak(LocalDate.of(2026, 3, 10), e(2026, 3, 10), e(2026, 3, 10)))
    }

    @Test fun todayNotLoggedKeepsStreakAlive() {
        assertEquals(Streak(2, 2), streak(LocalDate.of(2026, 3, 10), e(2026, 3, 8), e(2026, 3, 9)))
    }

    @Test fun gapResetsCurrentButKeepsBest() {
        val s = streak(LocalDate.of(2026, 3, 10), e(2026, 3, 1), e(2026, 3, 2), e(2026, 3, 3), e(2026, 3, 9), e(2026, 3, 10))
        assertEquals(Streak(2, 3), s)
    }

    @Test fun missedYesterdayAndTodayResetsCurrent() {
        assertEquals(Streak(0, 2), streak(LocalDate.of(2026, 3, 10), e(2026, 3, 6), e(2026, 3, 7)))
    }

    @Test fun monthBoundary() {
        assertEquals(Streak(3, 3), streak(LocalDate.of(2026, 3, 1), e(2026, 2, 27), e(2026, 2, 28), e(2026, 3, 1)))
    }

    @Test fun legacyZeroTimestampIgnored() {
        assertEquals(Streak(0, 0), loggingStreak(listOf(Expense(1, "x", 1.0, "Food", 0L)), LocalDate.of(2026, 3, 1), zone))
    }

    @Test fun labelPluralisation() {
        assertEquals("🔥 1-day streak", streakLabel(Streak(1, 1)))
        assertEquals("🔥 5-day streak", streakLabel(Streak(5, 7)))
    }
}
