package com.sabin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryListTest {
    @Test fun defaultsAreTheFiveBuiltIns() {
        assertEquals(listOf("Food", "Transport", "Bills", "Shopping", "Other"), CategoryList.EMPTY.all)
    }

    @Test fun addTrimsAndAppendsAfterDefaults() {
        val l = CategoryList.EMPTY.add("  Rent ")!!
        assertEquals(CategoryList.DEFAULTS + "Rent", l.all)
        assertEquals(listOf("Rent"), l.custom)
    }

    @Test fun blankIsRejected() {
        assertEquals(null, CategoryList.EMPTY.add("   "))
    }

    @Test fun duplicatesAreRejectedCaseInsensitivelyIncludingDefaults() {
        val l = CategoryList.EMPTY.add("Rent")!!
        assertEquals(null, l.add("rent"))
        assertEquals(null, l.add("FOOD"))
    }

    @Test fun tooLongIsRejected() {
        assertEquals(null, CategoryList.EMPTY.add("x".repeat(MAX_CATEGORY_LENGTH + 1)))
        assertTrue(CategoryList.EMPTY.add("x".repeat(MAX_CATEGORY_LENGTH)) != null)
    }

    @Test fun removeDropsCustomButNeverDefaults() {
        val l = CategoryList.EMPTY.add("Rent")!!
        assertEquals(CategoryList.DEFAULTS, l.remove("Rent").all)
        assertEquals(l, l.remove("Food"))
        assertFalse(l.isCustom("Food"))
        assertTrue(l.isCustom("Rent"))
    }

    @Test fun serializeRoundTrips() {
        val l = CategoryList.EMPTY.add("Rent")!!.add("Health; care")!!
        assertEquals(l, CategoryList.deserialize(l.serialize()))
        assertEquals(CategoryList.EMPTY, CategoryList.deserialize(""))
    }

    @Test fun withAllKeepsValidNewNamesOnly() {
        val l = CategoryList.EMPTY.withAll(listOf("Rent", "food", " ", "Rent", "Health"))
        assertEquals(listOf("Rent", "Health"), l.custom)
    }

    @Test fun reassignMovesExpensesToOther() {
        val book = ExpenseBook(listOf(Expense(1, "a", 5.0, "Rent"), Expense(2, "b", 6.0, "Food")))
        assertEquals(1, book.reassignCategory("Rent", "Other"))
        assertEquals(listOf("Other", "Food"), book.expenses.map { it.category })
        assertEquals(11.0, book.total(), 0.0)
    }

    @Test fun backupRoundTripsCategories() {
        val cats = CategoryList.EMPTY.add("Rent")!!
        val data = Backup.parseFull(Backup.exportFull(emptyList(), CategoryLimits.EMPTY, cats))
        assertEquals(listOf("Rent"), data.categories)
    }

    @Test fun oldBackupWithoutCategoriesStillRestores() {
        val data = Backup.parseFull(Backup.export(listOf(Expense(1, "a", 5.0, "Food", 0))))
        assertEquals(emptyList<String>(), data.categories)
        assertEquals(1, data.expenses.size)
    }
}
