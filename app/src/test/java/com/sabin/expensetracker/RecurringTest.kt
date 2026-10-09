package com.sabin.expensetracker

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringTest {
    private val zone = ZoneOffset.UTC
    private fun rule(day: Int, last: String = "") =
        RecurringExpense(1, "Rent", 100.0, "Bills", day, last)

    @Test fun dueOnOrAfterDayOfMonth() {
        val r = RecurringList(listOf(rule(15)))
        assertEquals(0, r.due(LocalDate.of(2026, 3, 14)).size)
        assertEquals(1, r.due(LocalDate.of(2026, 3, 15)).size)
        assertEquals(1, r.due(LocalDate.of(2026, 3, 28)).size)
    }

    @Test fun day31ClampsToLastDayOfShortMonth() {
        val r = RecurringList(listOf(rule(31)))
        assertEquals(0, r.due(LocalDate.of(2026, 2, 27)).size)
        assertEquals(1, r.due(LocalDate.of(2026, 2, 28)).size)
        assertEquals(1, r.due(LocalDate.of(2026, 4, 30)).size)
    }

    @Test fun appliesOnceAndNeverDuplicates() {
        val book = ExpenseBook()
        val r = RecurringList(listOf(rule(10)))
        val today = LocalDate.of(2026, 3, 12)
        assertEquals(1, r.applyDue(book, today, zone))
        assertEquals(0, r.applyDue(book, today, zone))
        assertEquals(1, book.expenses.size)
        assertEquals(LocalDate.of(2026, 3, 10).atStartOfDay().toInstant(zone).toEpochMilli(), book.expenses[0].timestamp)
    }

    @Test fun noDuplicateAfterRestart() {
        val book = ExpenseBook()
        val r = RecurringList(listOf(rule(10)))
        r.applyDue(book, LocalDate.of(2026, 3, 12), zone)
        val restored = RecurringList.deserialize(r.serialize())
        assertEquals(0, restored.applyDue(book, LocalDate.of(2026, 3, 20), zone))
        assertEquals(1, restored.applyDue(book, LocalDate.of(2026, 4, 11), zone))
    }

    @Test fun addValidatesAndClampsDay() {
        val r = RecurringList()
        val added = r.add("Rent", 5.0, "Bills", 40)
        assertEquals(31, added.dayOfMonth)
        assertTrue(runCatching { r.add(" ", 5.0, "Bills", 1) }.isFailure)
        assertTrue(runCatching { r.add("x", 0.0, "Bills", 1) }.isFailure)
    }

    @Test fun serializeRoundTripEscapes() {
        val r = RecurringList()
        r.add("A\tb\nc", 2.5, "Food", 5)
        assertEquals(r.rules, RecurringList.deserialize(r.serialize()).rules)
    }

    @Test fun recentChipsLastThreeDistinct() {
        fun e(id: Long, cat: String, amt: Double, ts: Long) = Expense(id, "t$id", amt, cat, ts)
        val list = listOf(
            e(1, "Food", 5.0, 1), e(2, "Food", 5.0, 2), e(3, "Bills", 9.0, 3),
            e(4, "Food", 6.0, 4), e(5, "Other", 1.0, 5), e(6, "Food", 5.0, 0)
        )
        assertEquals(
            listOf(RecentChip("Other", 1.0), RecentChip("Food", 6.0), RecentChip("Bills", 9.0)),
            recentChips(list)
        )
    }
}
