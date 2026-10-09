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
}
