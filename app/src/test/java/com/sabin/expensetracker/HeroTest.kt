package com.sabin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Test

class HeroTest {
    @Test fun countUpInterpolatesAndClamps() {
        assertEquals(0.0, countUpValue(1000.0, 0f), 0.0001)
        assertEquals(500.0, countUpValue(1000.0, 0.5f), 0.0001)
        assertEquals(1000.0, countUpValue(1000.0, 1f), 0.0001)
        assertEquals(1000.0, countUpValue(1000.0, 2f), 0.0001)
        assertEquals(0.0, countUpValue(1000.0, -1f), 0.0001)
    }

    @Test fun categoryGlyphsAreDistinctWithFallback() {
        assertEquals("🍔", categoryGlyph("Food"))
        assertEquals("🚌", categoryGlyph("Transport"))
        assertEquals("🧾", categoryGlyph("Bills"))
        assertEquals("🛍", categoryGlyph("Shopping"))
        assertEquals("✨", categoryGlyph("Other"))
        assertEquals("✨", categoryGlyph("Unknown"))
    }

    @Test fun budgetPercentLabel() {
        assertEquals("", budgetPercentLabel(0f, false))
        assertEquals("42% of budget", budgetPercentLabel(0.42f, true))
        assertEquals("100% of budget", budgetPercentLabel(1f, true))
    }

    @Test fun ringFractionIsBudgetLeftClamped() {
        assertEquals(0f, budgetLeftFraction(100.0, 0.0), 0f)
        assertEquals(1f, budgetLeftFraction(0.0, 1000.0), 0f)
        assertEquals(0.75f, budgetLeftFraction(250.0, 1000.0), 0.0001f)
        assertEquals(0f, budgetLeftFraction(1500.0, 1000.0), 0f)
    }

    @Test fun ringThresholdsAmberAt80RedOver100() {
        assertEquals(BudgetBand.NONE, ringBand(500.0, 0.0))
        assertEquals(BudgetBand.GREEN, ringBand(799.0, 1000.0))
        assertEquals(BudgetBand.AMBER, ringBand(800.0, 1000.0))
        assertEquals(BudgetBand.AMBER, ringBand(1000.0, 1000.0))
        assertEquals(BudgetBand.RED, ringBand(1001.0, 1000.0))
    }

    @Test fun deltaArrowMatchesDirection() {
        assertEquals("↑", deltaArrow(12))
        assertEquals("↓", deltaArrow(-5))
        assertEquals("→", deltaArrow(0))
        assertEquals("", deltaArrow(null))
    }

    @Test fun ringDescriptionStatesBandInWords() {
        assertEquals("", ringDescription(BudgetBand.NONE, 0f))
        assertEquals("Budget: 75% left, on track", ringDescription(BudgetBand.GREEN, 0.75f))
        assertEquals("Budget: 10% left, nearing limit", ringDescription(BudgetBand.AMBER, 0.1f))
        assertEquals("Budget: 0% left, over budget", ringDescription(BudgetBand.RED, 0f))
    }
}
