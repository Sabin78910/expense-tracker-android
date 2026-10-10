package com.sabin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class MonthComparisonTest {
    private val zone = ZoneOffset.UTC
    private fun e(date: String, amount: Double, cat: String = "Food") =
        Expense(1, "x", amount, cat, LocalDate.parse(date).atTime(12, 0).toInstant(zone).toEpochMilli())

    private fun cmp(list: List<Expense>, month: String, today: String) =
        compareWithPreviousMonth(list, YearMonth.parse(month), LocalDate.parse(today), zone)

    @Test fun comparesUpToSameDayOfMonth() {
        val list = listOf(e("2026-09-05", 100.0), e("2026-09-25", 900.0), e("2026-10-05", 50.0))
        val c = cmp(list, "2026-10", "2026-10-10")!!
        assertEquals(50.0, c.current, 0.0)
        assertEquals(100.0, c.previous, 0.0)
        assertEquals(-50, c.percentChange)
    }

    @Test fun pastMonthUsesFullLengthAndMoreSpendIsPositive() {
        val list = listOf(e("2026-08-30", 100.0), e("2026-08-31", 500.0), e("2026-09-30", 150.0))
        val c = cmp(list, "2026-09", "2026-10-10")!!
        assertEquals(150.0, c.current, 0.0)
        assertEquals(100.0, c.previous, 0.0)
        assertEquals(50, c.percentChange)
    }

    @Test fun noPreviousDataReturnsNull() {
        assertNull(cmp(listOf(e("2026-10-05", 50.0)), "2026-10", "2026-10-10"))
    }

    @Test fun zeroPreviousSpendHasNoPercent() {
        val list = listOf(e("2026-09-20", 100.0), e("2026-10-05", 50.0))
        val c = cmp(list, "2026-10", "2026-10-10")!!
        assertEquals(0.0, c.previous, 0.0)
        assertNull(c.percentChange)
    }

    @Test fun bothZeroReturnsNull() {
        assertNull(cmp(listOf(e("2026-09-20", 100.0)), "2026-10", "2026-10-10"))
    }

    @Test fun shorterPreviousMonthIsCappedAtItsLength() {
        val list = listOf(e("2026-02-28", 100.0), e("2026-03-31", 100.0))
        val c = cmp(list, "2026-03", "2026-03-31")!!
        assertEquals(100.0, c.previous, 0.0)
        assertEquals(0, c.percentChange)
    }

    @Test fun topCategoryIsLargestAbsoluteChange() {
        val list = listOf(
            e("2026-09-02", 100.0, "Food"), e("2026-10-02", 130.0, "Food"),
            e("2026-09-03", 300.0, "Bills"), e("2026-10-03", 100.0, "Bills"),
            e("2026-10-04", 20.0, "Transport")
        )
        val top = cmp(list, "2026-10", "2026-10-10")!!.topCategory!!
        assertEquals("Bills", top.category)
        assertEquals(-200.0, top.delta, 0.0)
    }

    @Test fun noTopCategoryWhenNothingChanged() {
        val list = listOf(e("2026-09-02", 100.0), e("2026-10-02", 100.0))
        assertNull(cmp(list, "2026-10", "2026-10-10")!!.topCategory)
    }
}
