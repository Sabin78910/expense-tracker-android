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

    @Test fun sortedTotalsOrdersHighestFirst() {
        val book = ExpenseBook()
        book.add("Bus", 50.0, "Transport")
        book.add("Rent", 10000.0, "Bills")
        book.add("Momo", 250.0, "Food")
        book.add("Tea", 30.0, "Food")
        assertEquals(
            listOf("Bills" to 10000.0, "Food" to 280.0, "Transport" to 50.0).sortedByDescending { it.second },
            book.sortedTotalsByCategory()
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
}
