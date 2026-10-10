package com.sabin.expensetracker

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthSelectionTest {
    private val zone = ZoneOffset.UTC
    private val current = YearMonth.of(2026, 10)
    private val earliest = YearMonth.of(2025, 11)

    private fun expense(id: Long, date: String) =
        Expense(id, "t$id", 10.0, "Food", LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli())

    @Test fun previousCrossesYearBoundary() {
        assertEquals(YearMonth.of(2025, 12), MonthSelection.previous(YearMonth.of(2026, 1), YearMonth.of(2020, 1)))
    }

    @Test fun nextCrossesYearBoundary() {
        assertEquals(YearMonth.of(2026, 1), MonthSelection.next(YearMonth.of(2025, 12), YearMonth.of(2026, 6)))
    }

    @Test fun previousStopsAtEarliest() {
        assertNull(MonthSelection.previous(earliest, earliest))
        assertFalse(MonthSelection.canGoPrevious(earliest, earliest))
        assertTrue(MonthSelection.canGoPrevious(current, earliest))
    }

    @Test fun nextDisabledAtCurrentMonth() {
        assertNull(MonthSelection.next(current, current))
        assertFalse(MonthSelection.canGoNext(current, current))
        assertTrue(MonthSelection.canGoNext(earliest, current))
    }

    @Test fun clampKeepsSelectionInRange() {
        assertEquals(current, MonthSelection.clamp(YearMonth.of(2027, 3), earliest, current))
        assertEquals(earliest, MonthSelection.clamp(YearMonth.of(2020, 3), earliest, current))
        assertEquals(YearMonth.of(2026, 2), MonthSelection.clamp(YearMonth.of(2026, 2), earliest, current))
    }

    @Test fun earliestIsOldestExpenseOrCurrentWhenEmpty() {
        val list = listOf(expense(1, "2026-03-05"), expense(2, "2025-11-30"), expense(3, "2026-09-01"))
        assertEquals(earliest, MonthSelection.earliest(list, current, zone))
        assertEquals(current, MonthSelection.earliest(emptyList(), current, zone))
    }

    @Test fun earliestNeverAfterCurrent() {
        assertEquals(current, MonthSelection.earliest(listOf(expense(1, "2027-01-01")), current, zone))
    }

    @Test fun filtersByYearMonthIncludingBoundaries() {
        val list = listOf(
            expense(1, "2025-12-31"), expense(2, "2026-01-01"), expense(3, "2026-01-31"), expense(4, "2026-02-01")
        )
        val jan = MonthSelection.filter(list, YearMonth.of(2026, 1), zone)
        assertEquals(listOf(2L, 3L), jan.map { it.id })
    }

    @Test fun emptyMonthYieldsEmptyList() {
        assertTrue(MonthSelection.filter(listOf(expense(1, "2026-01-10")), YearMonth.of(2026, 5), zone).isEmpty())
        assertTrue(MonthSelection.filter(emptyList(), current, zone).isEmpty())
    }

    @Test fun labelUsesLocaleMonthName() {
        assertEquals("January 2026", MonthSelection.label(YearMonth.of(2026, 1), Locale.ENGLISH))
        assertTrue(MonthSelection.label(YearMonth.of(2026, 1), Locale.forLanguageTag("ne")) != "January 2026")
    }
}
