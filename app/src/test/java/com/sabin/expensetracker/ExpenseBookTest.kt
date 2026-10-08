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
}
