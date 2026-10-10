package com.sabin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatTest {
    @Test fun formatsWithGroupingAndTwoDecimals() {
        assertEquals("NPR 1,850.00", formatNpr(1850.0, java.util.Locale.US))
        assertEquals("NPR 50.00", formatNpr(50.0, java.util.Locale.US))
        assertEquals("NPR 1,234,567.50", formatNpr(1234567.5, java.util.Locale.US))
    }
}
