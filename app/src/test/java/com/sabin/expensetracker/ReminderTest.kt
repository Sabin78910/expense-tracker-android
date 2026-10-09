package com.sabin.expensetracker

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderTest {
    private val zone = ZoneOffset.UTC
    private val nine = LocalTime.of(21, 0)
    private fun at(d: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 3, d, h, m)
    private fun e(d: Int) =
        Expense(1, "x", 1.0, "Food", at(d, 9).toInstant(zone).toEpochMilli())

    @Test fun defaultIsNinePm() {
        assertEquals(LocalTime.of(21, 0), DEFAULT_REMINDER_TIME)
    }

    @Test fun beforeTimeSchedulesToday() {
        assertEquals(at(10, 21), nextReminder(at(10, 8), nine, emptyList(), zone))
    }

    @Test fun afterTimeSchedulesTomorrow() {
        assertEquals(at(11, 21), nextReminder(at(10, 22), nine, emptyList(), zone))
    }

    @Test fun exactlyAtTimeSchedulesTomorrow() {
        assertEquals(at(11, 21), nextReminder(at(10, 21), nine, emptyList(), zone))
    }

    @Test fun loggedTodaySkipsToTomorrow() {
        assertEquals(at(11, 21), nextReminder(at(10, 8), nine, listOf(e(10)), zone))
    }

    @Test fun loggedYesterdayDoesNotSkip() {
        assertEquals(at(10, 21), nextReminder(at(10, 8), nine, listOf(e(9)), zone))
    }

    @Test fun shouldRemindOnlyWhenNothingLoggedToday() {
        val today = LocalDate.of(2026, 3, 10)
        assertTrue(shouldRemind(emptyList(), today, zone))
        assertTrue(shouldRemind(listOf(e(9)), today, zone))
        assertFalse(shouldRemind(listOf(e(10)), today, zone))
    }

    @Test fun legacyExpensesWithoutTimestampAreIgnored() {
        assertTrue(shouldRemind(listOf(Expense(1, "x", 1.0, "Food", 0L)), LocalDate.of(2026, 3, 10), zone))
    }
}
