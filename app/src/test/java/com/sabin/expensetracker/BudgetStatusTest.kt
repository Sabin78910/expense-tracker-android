package com.sabin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetStatusTest {
    @Test fun greenUnder80Percent() {
        val s = budgetStatus(7_999.0, 10_000.0, 10, 30)
        assertEquals(BudgetBand.GREEN, s.band)
        assertEquals("NPR 2,001.00 left", s.remainingLabel)
    }

    @Test fun amberFrom80ToExactly100Percent() {
        assertEquals(BudgetBand.AMBER, budgetStatus(8_000.0, 10_000.0, 10, 30).band)
        val s = budgetStatus(10_000.0, 10_000.0, 10, 30)
        assertEquals(BudgetBand.AMBER, s.band)
        assertEquals("NPR 0.00 left", s.remainingLabel)
    }

    @Test fun redOver100Percent() {
        val s = budgetStatus(10_500.0, 10_000.0, 10, 30)
        assertEquals(BudgetBand.RED, s.band)
        assertEquals("NPR 500.00 over", s.remainingLabel)
        assertEquals(1f, s.progress, 0f)
    }

    @Test fun progressIsFractionClamped() {
        assertEquals(0.25f, budgetStatus(2_500.0, 10_000.0, 5, 30).progress, 0.0001f)
        assertEquals(0f, budgetStatus(0.0, 10_000.0, 5, 30).progress, 0f)
    }

    @Test fun encouragingWhenUnder50PercentAtMidMonth() {
        assertNotNull(budgetStatus(4_999.0, 10_000.0, 15, 30).message)
        assertNotNull(budgetStatus(1_000.0, 10_000.0, 28, 30).message)
    }

    @Test fun noEncouragementBeforeMidMonthOrAt50Percent() {
        assertNull(budgetStatus(1_000.0, 10_000.0, 14, 30).message)
        assertNull(budgetStatus(5_000.0, 10_000.0, 20, 30).message)
    }

    @Test fun noMessageWhenOver() {
        assertNull(budgetStatus(12_000.0, 10_000.0, 28, 30).message)
    }

    @Test fun nonPositiveBudgetMeansNotSet() {
        assertTrue(budgetStatus(100.0, 0.0, 5, 30).band == BudgetBand.NONE)
    }
}
