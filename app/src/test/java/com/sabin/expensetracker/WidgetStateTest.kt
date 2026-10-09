package com.sabin.expensetracker

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStateTest {
    private val today = LocalDate.of(2026, 10, 15)
    private fun ts(date: LocalDate) = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun exp(id: Long, amount: Double, date: LocalDate) = Expense(id, "x", amount, "Food", ts(date))

    @Test fun sumsOnlyCurrentMonth() {
        val s = widgetState(
            listOf(exp(1, 100.0, today), exp(2, 50.0, today.minusDays(3)), exp(3, 999.0, today.minusMonths(1))),
            0.0, today
        )
        assertEquals("NPR 150.00", s.totalLabel)
    }

    @Test fun noBudgetHidesProgress() {
        val s = widgetState(listOf(exp(1, 10.0, today)), 0.0, today)
        assertFalse(s.hasBudget)
        assertEquals(0f, s.progress, 0f)
        assertEquals("", s.remainingLabel)
        assertEquals(BudgetBand.NONE, s.band)
    }

    @Test fun budgetProgressAndLabel() {
        val s = widgetState(listOf(exp(1, 250.0, today)), 1000.0, today)
        assertTrue(s.hasBudget)
        assertEquals(0.25f, s.progress, 0.0001f)
        assertEquals("NPR 750.00 left", s.remainingLabel)
        assertEquals(BudgetBand.GREEN, s.band)
    }

    @Test fun overBudgetClampsAndTurnsRed() {
        val s = widgetState(listOf(exp(1, 1500.0, today)), 1000.0, today)
        assertEquals(1f, s.progress, 0f)
        assertEquals("NPR 500.00 over", s.remainingLabel)
        assertEquals(BudgetBand.RED, s.band)
    }

    @Test fun emptyBookIsZero() {
        assertEquals("NPR 0.00", widgetState(emptyList(), 500.0, today).totalLabel)
    }
}
