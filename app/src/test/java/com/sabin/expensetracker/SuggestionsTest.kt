package com.sabin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionsTest {
    private fun e(id: Long, title: String, amount: Double, category: String = "Food", ts: Long = id) =
        Expense(id, title, amount, category, ts)

    @Test fun emptyHistoryGivesNoSuggestions() {
        assertTrue(suggestExpenses(emptyList(), "tea").isEmpty())
        assertTrue(suggestExpenses(emptyList(), "").isEmpty())
    }

    @Test fun blankQueryReturnsMostFrequentTitles() {
        val history = listOf(e(1, "Tea", 40.0), e(2, "Bus", 20.0), e(3, "Bus", 20.0), e(4, "Lunch", 150.0), e(5, "Tea", 40.0), e(6, "Bus", 20.0), e(7, "Rent", 9000.0))
        assertEquals(listOf("Bus", "Tea", "Rent"), suggestExpenses(history, "  ").map { it.title })
    }

    @Test fun matchingIgnoresCase() {
        val history = listOf(e(1, "Tea", 40.0), e(2, "Bus", 20.0))
        assertEquals(listOf("Tea"), suggestExpenses(history, "TE").map { it.title })
    }

    @Test fun matchesContainedText() {
        assertEquals(listOf("Milk tea"), suggestExpenses(listOf(e(1, "Milk tea", 30.0)), "tea").map { it.title })
    }

    @Test fun deduplicatesByTitleIgnoringCase() {
        val history = listOf(e(1, "tea", 40.0), e(2, "Tea", 45.0))
        assertEquals(1, suggestExpenses(history, "").size)
    }

    @Test fun ordersByFrequencyThenRecency() {
        val history = listOf(e(1, "Tea", 40.0), e(2, "Taxi", 300.0), e(3, "Tea", 40.0), e(4, "Tofu", 80.0))
        assertEquals(listOf("Tea", "Tofu", "Taxi"), suggestExpenses(history, "t").map { it.title })
    }

    @Test fun respectsLimit() {
        val history = (1..6L).map { e(it, "Item$it", 10.0) }
        assertEquals(3, suggestExpenses(history, "").size)
        assertEquals(2, suggestExpenses(history, "item", limit = 2).size)
    }

    @Test fun latestAmountAndCategoryWin() {
        val history = listOf(e(1, "Tea", 40.0, "Food", ts = 100), e(2, "Tea", 55.0, "Other", ts = 300), e(3, "Tea", 50.0, "Food", ts = 200))
        val s = suggestExpenses(history, "tea").single()
        assertEquals(55.0, s.amount, 0.0)
        assertEquals("Other", s.category)
    }

    @Test fun suggestionLabelReadsAllPartsAndHint() {
        assertEquals("Tea, Food, NPR 40.00. Double tap to fill",
            suggestionLabel(ExpenseSuggestion("Tea", "Food", 40.0), "Double tap to fill", java.util.Locale.US))
    }
}
