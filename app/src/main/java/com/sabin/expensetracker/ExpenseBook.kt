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

    fun total(): Double = items.sumOf { it.amount }

    fun totalsByCategory(): Map<String, Double> =
        items.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }
}
