package com.sabin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseBookTest {
    @Test fun addsAndTotals() {
        val book = ExpenseBook()
        book.add("Momo", 250.0, "Food")
        book.add("Bus", 50.0, "Transport")
        book.add("Tea", 30.0, "Food")
        assertEquals(330.0, book.total(), 0.001)
        assertEquals(280.0, book.totalsByCategory()["Food"]!!, 0.001)
    }

    @Test fun categoryTotalsSortedHighestFirst() {
        val book = ExpenseBook()
        book.add("Bus", 50.0, "Transport")
        book.add("Momo", 250.0, "Food")
        book.add("Rent", 10000.0, "Bills")
        book.add("Tea", 30.0, "Food")
        assertEquals(
            listOf("Bills" to 10000.0, "Food" to 280.0, "Transport" to 50.0),
            book.sortedCategoryTotals()
        )
    }

    @Test fun removeDeletesExpense() {
        val book = ExpenseBook()
        val e = book.add("Rent", 10000.0, "Bills")
        book.remove(e.id)
        assertEquals(0, book.expenses.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveAmount() { ExpenseBook().add("X", 0.0, "Other") }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankTitle() { ExpenseBook().add("  ", 10.0, "Other") }

    @Test fun serializeRoundTrips() {
        val book = ExpenseBook()
        book.add("Momo\tand\nchutney \\ more", 250.5, "Food")
        book.add("Bus", 50.0, "Transport")
        val restored = ExpenseBook.deserialize(book.serialize())
        assertEquals(book.expenses, restored.expenses)
        assertEquals(3L, restored.add("Tea", 30.0, "Food").id)
    }

    @Test fun deserializeEmptyOrCorruptGivesEmptyBook() {
        assertEquals(0, ExpenseBook.deserialize("").expenses.size)
        assertEquals(0, ExpenseBook.deserialize("garbage").expenses.size)
    }

    @Test fun filterByCategoryReturnsOnlyThatCategory() {
        val book = ExpenseBook()
        book.add("Momo", 250.0, "Food")
        book.add("Bus", 50.0, "Transport")
        book.add("Tea", 30.0, "Food")
        assertEquals(listOf("Tea", "Momo"), book.filterByCategory("Food").map { it.title })
        assertEquals(0, book.filterByCategory("Bills").size)
    }

    @Test fun filterByNullCategoryReturnsAll() {
        val book = ExpenseBook()
        book.add("Momo", 250.0, "Food")
        book.add("Bus", 50.0, "Transport")
        assertEquals(2, book.filterByCategory(null).size)
    }

    private fun millis(y: Int, m: Int, d: Int) =
        java.time.LocalDate.of(y, m, d).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test fun totalForMonthSumsOnlyThatMonth() {
        val book = ExpenseBook()
        book.add("A", 100.0, "Food", millis(2026, 10, 1))
        book.add("B", 50.0, "Food", millis(2026, 10, 31))
        book.add("C", 70.0, "Food", millis(2026, 9, 30))
        book.add("D", 20.0, "Food", millis(2025, 10, 5))
        assertEquals(150.0, book.totalForMonth(2026, 10, java.time.ZoneOffset.UTC), 0.001)
        assertEquals(0.0, book.totalForMonth(2026, 1, java.time.ZoneOffset.UTC), 0.001)
    }

    @Test fun timestampSurvivesSerialization() {
        val book = ExpenseBook()
        book.add("A", 100.0, "Food", millis(2026, 10, 1))
        assertEquals(book.expenses, ExpenseBook.deserialize(book.serialize()).expenses)
    }

    @Test fun deserializesLegacyFourFieldLines() {
        val book = ExpenseBook.deserialize("1\tMomo\t250.0\tFood")
        assertEquals(1, book.expenses.size)
        assertEquals(0L, book.expenses[0].timestamp)
        assertEquals(0.0, book.totalForMonth(2026, 10, java.time.ZoneOffset.UTC), 0.001)
    }

    @Test fun restoreAfterRemoveBringsBackSameItemAndTotals() {
        val book = ExpenseBook()
        book.add("Tea", 30.0, "Food", 1_700_000_000_000L)
        val e = book.add("Momo", 250.0, "Food", 1_700_000_100_000L)
        book.add("Bus", 50.0, "Transport", 1_700_000_200_000L)
        val before = book.expenses
        val total = book.total()
        val cats = book.totalsByCategory()
        val month = book.totalForMonth(2023, 11, java.time.ZoneOffset.UTC)
        book.remove(e.id)
        book.restore(e)
        assertEquals(before, book.expenses)
        assertEquals(total, book.total(), 0.001)
        assertEquals(cats, book.totalsByCategory())
        assertEquals(month, book.totalForMonth(2023, 11, java.time.ZoneOffset.UTC), 0.001)
    }

    @Test fun restoreExistingIdIsNoOp() {
        val book = ExpenseBook()
        val e = book.add("Tea", 30.0, "Food")
        book.restore(e)
        book.restore(e.copy(title = "Other"))
        assertEquals(listOf(e), book.expenses)
    }

    @Test fun restoreDoesNotReuseIds() {
        val book = ExpenseBook()
        val e = book.add("Tea", 30.0, "Food")
        book.remove(e.id)
        val n = book.add("Bus", 50.0, "Transport")
        book.restore(e)
        assertEquals(e.id + 1, n.id)
        assertEquals(e.id + 2, book.add("X", 1.0, "Other").id)
    }

    @Test fun updateChangesFieldsKeepingIdTimestampAndPosition() {
        val book = ExpenseBook()
        val a = book.add("Tea", 30.0, "Food", 111L)
        val b = book.add("Bus", 50.0, "Transport", 222L)
        book.update(a.id, " Momo ", 250.0, "Bills")
        assertEquals(listOf(b, Expense(a.id, "Momo", 250.0, "Bills", 111L)), book.expenses)
    }

    @Test fun updateWithBlankTitleThrowsAndKeepsExpense() {
        val book = ExpenseBook()
        val e = book.add("Tea", 30.0, "Food")
        assertTrue(runCatching { book.update(e.id, " ", 10.0, "Food") }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(listOf(e), book.expenses)
    }

    @Test fun updateWithNonPositiveAmountThrowsAndKeepsExpense() {
        val book = ExpenseBook()
        val e = book.add("Tea", 30.0, "Food")
        assertTrue(runCatching { book.update(e.id, "Tea", 0.0, "Food") }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(listOf(e), book.expenses)
    }

    @Test fun updateUnknownIdIsNoOp() {
        val book = ExpenseBook()
        val e = book.add("Tea", 30.0, "Food")
        book.update(999L, "X", 1.0, "Other")
        assertEquals(listOf(e), book.expenses)
    }

    private fun searchBook() = ExpenseBook().apply {
        add("Momo", 250.0, "Food")
        add("Bus ticket", 50.0, "Transport")
        add("Momo dinner", 300.0, "Bills")
    }

    @Test fun searchMatchesTitleSubstring() {
        assertEquals(listOf("Momo dinner", "Momo"), searchBook().search("mom").map { it.title })
    }

    @Test fun searchIgnoresCaseAndTrims() {
        assertEquals(1, searchBook().search("  BUS ").size)
    }

    @Test fun blankQueryReturnsAll() {
        assertEquals(3, searchBook().search("   ").size)
    }

    @Test fun searchCombinesWithCategory() {
        assertEquals(listOf("Momo"), searchBook().search("momo", "Food").map { it.title })
    }

    @Test fun searchWithNoMatchesIsEmpty() {
        assertTrue(searchBook().search("pizza").isEmpty())
    }
}
