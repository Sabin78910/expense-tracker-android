package com.sabin.expensetracker

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryBudgetTest {
    @Test fun nearLimitIsAmberWithRemaining() {
        val s = categoryBudgetStatus(800.0, 1000.0)
        assertEquals(BudgetBand.AMBER, s.band)
        assertEquals(0.8f, s.progress, 0.001f)
        assertEquals(200.0, categoryRemaining(800.0, 1000.0)!!, 0.001)
    }

    @Test fun lowSpendIsGreen() {
        assertEquals(BudgetBand.GREEN, categoryBudgetStatus(100.0, 1000.0).band)
    }

    @Test fun overLimitIsRedAndRemainingNegative() {
        val s = categoryBudgetStatus(1200.0, 1000.0)
        assertEquals(BudgetBand.RED, s.band)
        assertEquals(1f, s.progress, 0.001f)
        assertEquals(-200.0, categoryRemaining(1200.0, 1000.0)!!, 0.001)
    }

    @Test fun noLimitMeansNoStatus() {
        assertEquals(BudgetBand.NONE, categoryBudgetStatus(500.0, 0.0).band)
        assertEquals(null, categoryRemaining(500.0, 0.0))
    }

    @Test fun setChangeAndClearLimit() {
        var l = CategoryLimits.EMPTY.with("Food", 1000.0)
        assertEquals(1000.0, l.limitFor("Food"), 0.0)
        l = l.with("Food", 1500.0)
        assertEquals(1500.0, l.limitFor("Food"), 0.0)
        l = l.with("Food", 0.0)
        assertEquals(0.0, l.limitFor("Food"), 0.0)
        assertTrue(l.isEmpty())
    }

    @Test fun serializeRoundTripsAndIgnoresGarbage() {
        val l = CategoryLimits.EMPTY.with("Food", 1000.5).with("Bills", 20.0)
        assertEquals(l, CategoryLimits.deserialize(l.serialize()))
        assertTrue(CategoryLimits.deserialize("junk;=;Food=-3;Food=x").isEmpty())
        assertTrue(CategoryLimits.deserialize("").isEmpty())
    }

    @Test fun contentDescriptionMentionsUsedAndLimit() {
        assertEquals(
            "Food: 800 of 1,000 rupees used",
            categoryBudgetDescription("Food", 800.0, 1000.0, Locale.US)
        )
    }

    @Test fun backupRoundTripWithLimits() {
        val expenses = listOf(Expense(1, "Tea", 10.0, "Food", 5))
        val limits = CategoryLimits.EMPTY.with("Food", 1000.0)
        val data = Backup.parseFull(Backup.exportFull(expenses, limits))
        assertEquals(expenses, data.expenses)
        assertEquals(limits, data.limits)
    }

    @Test fun backupRoundTripWithoutLimits() {
        val expenses = listOf(Expense(1, "Tea", 10.0, "Food", 5))
        val data = Backup.parseFull(Backup.exportFull(expenses, CategoryLimits.EMPTY))
        assertEquals(expenses, data.expenses)
        assertTrue(data.limits.isEmpty())
    }

    @Test fun oldBackupWithoutLimitsStillRestores() {
        val old = Backup.export(listOf(Expense(1, "Tea", 10.0, "Food", 5)))
        val data = Backup.parseFull(old)
        assertEquals(1, data.expenses.size)
        assertTrue(data.limits.isEmpty())
        assertEquals(1, Backup.parse(Backup.exportFull(data.expenses, CategoryLimits.EMPTY.with("Food", 5.0))).size)
    }
}
