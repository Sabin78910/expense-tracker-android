package com.sabin.expensetracker

import java.util.Locale

/** A repeat-purchase suggestion built from past expenses. */
data class ExpenseSuggestion(val title: String, val category: String, val amount: Double)

/**
 * Up to [limit] suggestions from [history] whose title contains [query] (case-insensitive; blank matches all).
 * De-duplicated by title, ordered by frequency then recency; category and amount come from the latest match.
 */
fun suggestExpenses(history: List<Expense>, query: String, limit: Int = 3): List<ExpenseSuggestion> {
    val q = query.trim()
    // Equal timestamps: earlier list position counts as newer (ExpenseBook keeps newest first).
    val newestFirst = history.withIndex()
        .sortedWith(compareByDescending<IndexedValue<Expense>> { it.value.timestamp }.thenBy { it.index })
        .map { it.value }
    return newestFirst
        .filter { it.title.contains(q, ignoreCase = true) }
        .groupBy { it.title.lowercase() }
        .values
        .withIndex()
        .sortedWith(compareByDescending<IndexedValue<List<Expense>>> { it.value.size }.thenBy { it.index })
        .take(limit)
        .map { (_, group) -> group.first().let { ExpenseSuggestion(it.title, it.category, it.amount) } }
}

/** Spoken label for a suggestion chip, e.g. "Tea, Food, NPR 40.00. Double tap to fill". */
fun suggestionLabel(s: ExpenseSuggestion, hint: String, locale: Locale = Locale.getDefault()): String =
    "${s.title}, ${s.category}, ${formatNpr(s.amount, locale)}. $hint"
