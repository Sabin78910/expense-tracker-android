package com.sabin.expensetracker

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartDataTest {
    private val zone = ZoneOffset.UTC
    private fun ts(y: Int, m: Int, d: Int) =
        LocalDate.of(y, m, d).atTime(12, 0).toInstant(zone).toEpochMilli()
    private fun e(id: Long, amount: Double, cat: String, t: Long) = Expense(id, "x", amount, cat, t)

    @Test fun sharesSumTo100WithRounding() {
        val shares = categoryShares(listOf("A" to 1.0, "B" to 1.0, "C" to 1.0))
        assertEquals(100, shares.sumOf { it.percent })
        assertEquals(listOf(34, 33, 33), shares.map { it.percent })
    }

    @Test fun sharesEmptyOrZeroGivesEmpty() {
        assertTrue(categoryShares(emptyList()).isEmpty())
        assertTrue(categoryShares(listOf("A" to 0.0)).isEmpty())
    }

    @Test fun sharesSingleCategoryIs100() {
        assertEquals(100, categoryShares(listOf("A" to 5.0)).single().percent)
    }

    @Test fun monthCategoryTotalsOnlyCountsThatMonthSortedDesc() {
        val list = listOf(
            e(1, 10.0, "Food", ts(2026, 10, 1)),
            e(2, 30.0, "Bills", ts(2026, 10, 5)),
            e(3, 99.0, "Food", ts(2026, 9, 30)),
            e(4, 5.0, "Food", ts(2026, 10, 9))
        )
        assertEquals(listOf("Bills" to 30.0, "Food" to 15.0), monthCategoryTotals(list, 2026, 10, zone))
    }

    @Test fun dailyTotalsCoversSevenDaysEndingToday() {
        val today = LocalDate.of(2026, 10, 9)
        val list = listOf(
            e(1, 10.0, "Food", ts(2026, 10, 9)),
            e(2, 5.0, "Food", ts(2026, 10, 9)),
            e(3, 7.0, "Food", ts(2026, 10, 3)),
            e(4, 100.0, "Food", ts(2026, 10, 2)),
            e(5, 1.0, "Food", 0L)
        )
        val days = dailyTotals(list, today, zone)
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 10, 3), days.first().date)
        assertEquals(today, days.last().date)
        assertEquals(7.0, days.first().total, 0.0)
        assertEquals(15.0, days.last().total, 0.0)
        assertEquals(22.0, days.sumOf { it.total }, 0.0)
    }

    @Test fun dailyTotalsEmptyIsSevenZeros() {
        val days = dailyTotals(emptyList(), LocalDate.of(2026, 10, 9), zone)
        assertEquals(7, days.size)
        assertTrue(days.all { it.total == 0.0 })
    }

    @Test fun chartDescriptions() {
        assertEquals("Spending by category: Food 60%, Bills 40%",
            donutDescription(listOf(CategoryShare("Food", 6.0, 60), CategoryShare("Bills", 4.0, 40))))
        assertEquals("No spending this month", donutDescription(emptyList()))
        assertEquals("Last 7 days spending: 2026-10-09 NPR 15.00",
            barsDescription(listOf(DayTotal(LocalDate.of(2026, 10, 9), 15.0))))
    }
}
