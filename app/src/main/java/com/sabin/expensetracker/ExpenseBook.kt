package com.sabin.expensetracker

data class Expense(val id: Long, val title: String, val amount: Double, val category: String)

/** Pure, testable expense logic (no Android dependencies). */
class ExpenseBook(initial: List<Expense> = emptyList()) {
    private val items = initial.toMutableList()
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    val expenses: List<Expense> get() = items.toList()

    fun add(title: String, amount: Double, category: String): Expense {
        require(title.isNotBlank()) { "Title is required" }
        require(amount > 0) { "Amount must be positive" }
        return Expense(nextId++, title.trim(), amount, category).also { items.add(0, it) }
    }

    fun remove(id: Long) {
        items.removeAll { it.id == id }
    }

    /** Expenses in [category], or all expenses when [category] is null. */
    fun filterByCategory(category: String?): List<Expense> =
        if (category == null) expenses else items.filter { it.category == category }

    fun total(): Double = items.sumOf { it.amount }

    fun totalsByCategory(): Map<String, Double> =
        items.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }

    /** Category totals, highest first. */
    fun sortedCategoryTotals(): List<Pair<String, Double>> =
        totalsByCategory().toList().sortedByDescending { it.second }

    /** One expense per line, tab-separated: id, title, amount, category (fields escaped). */
    fun serialize(): String = items.joinToString("\n") {
        listOf(it.id.toString(), escape(it.title), it.amount.toString(), escape(it.category))
            .joinToString("\t")
    }

    companion object {
        fun deserialize(data: String): ExpenseBook = ExpenseBook(
            data.lines().filter { it.isNotBlank() }.mapNotNull { line ->
                val f = line.split("\t")
                if (f.size != 4) return@mapNotNull null
                val id = f[0].toLongOrNull() ?: return@mapNotNull null
                val amount = f[2].toDoubleOrNull() ?: return@mapNotNull null
                Expense(id, unescape(f[1]), amount, unescape(f[3]))
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
