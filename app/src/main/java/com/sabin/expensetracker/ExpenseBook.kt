package com.sabin.expensetracker

import java.time.Instant
import java.time.ZoneId

data class Expense(
    val id: Long,
    val title: String,
    val amount: Double,
    val category: String,
    /** Epoch millis; 0 for expenses saved before timestamps existed. */
    val timestamp: Long = 0L,
    /** Optional short note; empty when none. */
    val note: String = ""
)

const val MAX_NOTE_LENGTH = 100

private fun cleanNote(note: String) = note.trim().take(MAX_NOTE_LENGTH)

/** Pure, testable expense logic (no Android dependencies). */
class ExpenseBook(initial: List<Expense> = emptyList()) {
    private val items = initial.toMutableList()
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    val expenses: List<Expense> get() = items.toList()

    fun add(
        title: String,
        amount: Double,
        category: String,
        timestamp: Long = System.currentTimeMillis(),
        note: String = ""
    ): Expense {
        require(title.isNotBlank()) { "Title is required" }
        require(amount > 0) { "Amount must be positive" }
        return Expense(nextId++, title.trim(), amount, category, timestamp, cleanNote(note)).also { items.add(0, it) }
    }

    /**
     * Replaces title, amount and category of expense [id], keeping its id and position.
     * The timestamp is kept unless [timestamp] is given. [note] replaces the old note (blank clears it).
     * Validates like [add]; an unknown [id] is a no-op.
     */
    fun update(id: Long, title: String, amount: Double, category: String, timestamp: Long? = null, note: String = "") {
        require(title.isNotBlank()) { "Title is required" }
        require(amount > 0) { "Amount must be positive" }
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0) items[i] = items[i].copy(title = title.trim(), amount = amount, category = category,
            timestamp = timestamp ?: items[i].timestamp, note = cleanNote(note))
    }

    /**
     * Adds [imported] expenses not already present (same title, amount, category and timestamp),
     * giving each a fresh id. Returns how many were added.
     */
    fun merge(imported: List<Expense>): Int {
        fun key(e: Expense) = listOf(e.title, e.amount, e.category, e.timestamp)
        val seen = items.map(::key).toMutableSet()
        var added = 0
        imported.forEach { e ->
            if (seen.add(key(e))) {
                items.add(e.copy(id = nextId++))
                added++
            }
        }
        return added
    }

    fun remove(id: Long) {
        items.removeAll { it.id == id }
    }

    /** Moves every expense in category [from] to [to]; returns how many changed. */
    fun reassignCategory(from: String, to: String): Int {
        var n = 0
        items.indices.forEach { i -> if (items[i].category == from) { items[i] = items[i].copy(category = to); n++ } }
        return n
    }

    /** Re-inserts a removed [expense] with its original id; no-op if that id is present. */
    fun restore(expense: Expense) {
        if (items.any { it.id == expense.id }) return
        val at = items.indexOfFirst { it.id < expense.id }
        items.add(if (at < 0) items.size else at, expense)
        if (expense.id >= nextId) nextId = expense.id + 1
    }

    /** Expenses in [category], or all expenses when [category] is null. */
    fun filterByCategory(category: String?): List<Expense> =
        if (category == null) expenses else items.filter { it.category == category }

    /** Expenses whose title or note contains [query] (trimmed, case-insensitive), within [category] if given. */
    fun search(query: String, category: String? = null): List<Expense> {
        val q = query.trim()
        return filterByCategory(category).filter {
            q.isEmpty() || it.title.contains(q, ignoreCase = true) || it.note.contains(q, ignoreCase = true)
        }
    }

    fun total(): Double = items.sumOf { it.amount }

    /** Sum of expenses dated in [year]/[month] (1-12) in [zone]. */
    fun totalForMonth(year: Int, month: Int, zone: ZoneId = ZoneId.systemDefault()): Double =
        items.filter {
            val d = Instant.ofEpochMilli(it.timestamp).atZone(zone)
            d.year == year && d.monthValue == month
        }.sumOf { it.amount }

    fun totalsByCategory(): Map<String, Double> =
        items.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }

    /** Category totals, highest first. */
    fun sortedCategoryTotals(): List<Pair<String, Double>> =
        totalsByCategory().toList().sortedByDescending { it.second }

    /** One expense per line, tab-separated: id, title, amount, category, timestamp, note (fields escaped). */
    fun serialize(): String = items.joinToString("\n") {
        listOf(it.id.toString(), escape(it.title), it.amount.toString(), escape(it.category), it.timestamp.toString(), escape(it.note))
            .joinToString("\t")
    }

    companion object {
        fun deserialize(data: String): ExpenseBook = ExpenseBook(
            data.lines().filter { it.isNotBlank() }.mapNotNull { line ->
                val f = line.split("\t")
                if (f.size !in 4..6) return@mapNotNull null
                val id = f[0].toLongOrNull() ?: return@mapNotNull null
                val amount = f[2].toDoubleOrNull() ?: return@mapNotNull null
                val ts = if (f.size >= 5) f[4].toLongOrNull() ?: return@mapNotNull null else 0L
                Expense(id, unescape(f[1]), amount, unescape(f[3]), ts, if (f.size == 6) unescape(f[5]) else "")
            }
        )

        private fun escape(s: String) =
            s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")

        private fun unescape(s: String): String {
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
