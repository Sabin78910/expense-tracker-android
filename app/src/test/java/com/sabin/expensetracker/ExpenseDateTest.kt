package com.sabin.expensetracker

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ExpenseDateTest {
    private val zone = ZoneId.of("Asia/Kathmandu")
    private val today = LocalDate.of(2026, 10, 10)
    private val noon = LocalTime.of(12, 30)

    private fun local(ts: Long) = Instant.ofEpochMilli(ts).atZone(zone)

    @Test fun todayKeepsTimeOfDay() {
        val ts = ExpenseDate.timestampFor(today, noon, today, zone)
        assertEquals(today, local(ts).toLocalDate())
        assertEquals(noon, local(ts).toLocalTime())
    }

    @Test fun pastDateKeepsTimeOfDay() {
        val ts = ExpenseDate.timestampFor(LocalDate.of(2026, 10, 3), noon, today, zone)
        assertEquals(LocalDate.of(2026, 10, 3), local(ts).toLocalDate())
        assertEquals(noon, local(ts).toLocalTime())
    }

    @Test fun futureDateRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ExpenseDate.timestampFor(today.plusDays(1), noon, today, zone)
        }
    }

    @Test fun monthAndYearBoundaries() {
        val prevMonth = ExpenseDate.timestampFor(LocalDate.of(2026, 9, 30), noon, today, zone)
        assertEquals(LocalDate.of(2026, 9, 30), local(prevMonth).toLocalDate())
        val newYear = LocalDate.of(2026, 1, 1)
        val prevYear = ExpenseDate.timestampFor(LocalDate.of(2025, 12, 31), noon, newYear, zone)
        assertEquals(LocalDate.of(2025, 12, 31), local(prevYear).toLocalDate())
    }

    @Test fun pickerMillisRoundTrip() {
        val d = LocalDate.of(2026, 2, 28)
        val utc = d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(utc, ExpenseDate.toPickerMillis(d))
        assertEquals(d, ExpenseDate.fromPickerMillis(utc))
    }

    @Test fun selectableOnlyUpToToday() {
        assertEquals(true, ExpenseDate.isSelectable(ExpenseDate.toPickerMillis(today), today))
        assertEquals(false, ExpenseDate.isSelectable(ExpenseDate.toPickerMillis(today.plusDays(1)), today))
    }
}
