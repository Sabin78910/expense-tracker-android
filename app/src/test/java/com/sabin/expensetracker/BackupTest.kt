package com.sabin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupTest {
    private val sample = listOf(
        Expense(2, "Tea \"quoted\" \\ \n tab\t é 😀", 12.5, "Food", 1_700_000_000_000),
        Expense(1, "Rent", 15000.0, "Bills", 0)
    )

    @Test fun roundTripGivesIdenticalData() {
        assertEquals(sample, Backup.parse(Backup.export(sample)))
    }

    @Test fun emptyListRoundTrips() {
        assertEquals(emptyList<Expense>(), Backup.parse(Backup.export(emptyList())))
    }

    @Test fun malformedFilesAreRejected() {
        listOf(
            "", "not json", "{", "[]", "{\"version\":1}",
            "{\"version\":1,\"expenses\":[{\"id\":1}]}",
            "{\"version\":1,\"expenses\":[{\"id\":1,\"title\":\"a\",\"amount\":\"x\",\"category\":\"c\",\"timestamp\":0}]}",
            Backup.export(sample) + "junk",
            "{\"version\":1,\"expenses\":[{\"id\":1,\"title\":\"a\",\"amount\":-5,\"category\":\"c\",\"timestamp\":0}]}"
        ).forEach {
            try {
                Backup.parse(it)
                fail("Expected rejection of: $it")
            } catch (_: BackupException) {
            }
        }
    }

    @Test fun mergeSkipsDuplicatesAndKeepsExisting() {
        val book = ExpenseBook(listOf(Expense(1, "Rent", 15000.0, "Bills", 0)))
        val added = book.merge(sample)
        assertEquals(1, added)
        assertEquals(2, book.expenses.size)
        assertEquals(0, book.merge(sample))
        assertEquals(2, book.expenses.map { it.id }.toSet().size)
    }

    @Test fun mergeAssignsFreshIdOnCollision() {
        val book = ExpenseBook(listOf(Expense(1, "Coffee", 5.0, "Food", 10)))
        book.merge(listOf(Expense(1, "Taxi", 9.0, "Transport", 20)))
        assertEquals(2, book.expenses.size)
        assertTrue(book.expenses.map { it.id }.toSet().size == 2)
    }

    @Test fun exportThenImportIntoEmptyBookMatches() {
        val book = ExpenseBook()
        book.merge(Backup.parse(Backup.export(sample)))
        assertEquals(sample.toSet(), book.expenses.map { it.copy(id = sample.first { s -> s.title == it.title }.id) }.toSet())
    }
}
