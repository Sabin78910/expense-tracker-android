package com.sabin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityLabelsTest {
    @Test
    fun deleteLabelNamesTheExpense() {
        assertEquals("Delete Momo lunch", deleteLabel("Momo lunch"))
    }

    @Test
    fun expenseSummaryReadsTitleCategoryAndAmount() {
        assertEquals("Momo lunch, Food, NPR 150.00", expenseSummary("Momo lunch", "Food", 150.0))
    }

    @Test
    fun expenseSummaryIncludesNoteWhenPresent() {
        assertEquals("Momo, Food, NPR 150.00, with Ram", expenseSummary("Momo", "Food", 150.0, "with Ram"))
    }
}
