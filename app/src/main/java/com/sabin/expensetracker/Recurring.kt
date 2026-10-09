package com.sabin.expensetracker

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** A monthly expense; [lastAdded] is the "yyyy-MM" it was last added for ("" if never). */
data class RecurringExpense(
    val id: Long,
    val title: String,
    val amount: Double,
    val category: String,
    val dayOfMonth: Int,
    val lastAdded: String = ""
)

data class RecentChip(val category: String, val amount: Double)

/** Last 3 distinct category + amount pairs, newest first. */
fun recentChips(expenses: List<Expense>, limit: Int = 3): List<RecentChip> =
    expenses.sortedByDescending { it.timestamp }
        .map { RecentChip(it.category, it.amount) }
        .distinct()
        .take(limit)

/** Pure, testable recurring-expense scheduling (no Android dependencies). */
class RecurringList(initial: List<RecurringExpense> = emptyList()) {
    private val items = initial.toMutableList()
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    val rules: List<RecurringExpense> get() = items.toList()

    fun add(title: String, amount: Double, category: String, dayOfMonth: Int): RecurringExpense {
        require(title.isNotBlank()) { "Title is required" }
        require(amount > 0) { "Amount must be positive" }
        return RecurringExpense(nextId++, title.trim(), amount, category, dayOfMonth.coerceIn(1, 31))
            .also { items.add(it) }
    }

    fun remove(id: Long) {
        items.removeAll { it.id == id }
    }

    /** Rules whose day (clamped to the month length) has arrived and not yet added this month. */
    fun due(today: LocalDate): List<RecurringExpense> {
        val month = YearMonth.from(today)
        return items.filter { it.lastAdded != month.toString() && dueDay(it, month) <= today.dayOfMonth }
    }

    /** Adds each due rule to [book] once for today's month; returns how many were added. */
    fun applyDue(book: ExpenseBook, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int {
        val month = YearMonth.from(today)
        val due = due(today)
        due.forEach { r ->
            val ts = month.atDay(dueDay(r, month)).atStartOfDay(zone).toInstant().toEpochMilli()
            book.add(r.title, r.amount, r.category, ts)
            items[items.indexOfFirst { it.id == r.id }] = r.copy(lastAdded = month.toString())
        }
        return due.size
    }

    fun serialize(): String = items.joinToString("\n") {
        listOf(it.id, esc(it.title), it.amount, esc(it.category), it.dayOfMonth, it.lastAdded)
            .joinToString("\t")
    }

    companion object {
        private fun dueDay(r: RecurringExpense, month: YearMonth) = minOf(r.dayOfMonth, month.lengthOfMonth())

        fun deserialize(data: String): RecurringList = RecurringList(
            data.lines().filter { it.isNotBlank() }.mapNotNull { line ->
                val f = line.split("\t")
                if (f.size != 6) return@mapNotNull null
                RecurringExpense(
                    f[0].toLongOrNull() ?: return@mapNotNull null,
                    unesc(f[1]),
                    f[2].toDoubleOrNull() ?: return@mapNotNull null,
                    unesc(f[3]),
                    f[4].toIntOrNull() ?: return@mapNotNull null,
                    f[5]
                )
            }
        )

        private fun esc(s: String) =
            s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")

        private fun unesc(s: String): String {
            val sb = StringBuilder()
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '\\' && i + 1 < s.length) {
                    i++
                    sb.append(when (s[i]) { 't' -> '\t'; 'n' -> '\n'; 'r' -> '\r'; else -> s[i] })
                } else sb.append(c)
                i++
            }
            return sb.toString()
        }
    }
}
