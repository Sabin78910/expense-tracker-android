package com.sabin.expensetracker

import org.junit.Assert.assertEquals
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
}
