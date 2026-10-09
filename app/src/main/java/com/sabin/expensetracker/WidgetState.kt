package com.sabin.expensetracker

import java.time.LocalDate

/** Everything the home-screen widget renders, derived from stored data. */
data class WidgetState(
    val totalLabel: String,
    val hasBudget: Boolean,
    val progress: Float,
    val remainingLabel: String,
    val band: BudgetBand
)

fun widgetState(expenses: List<Expense>, budget: Double, today: LocalDate): WidgetState {
    val spent = ExpenseBook(expenses).totalForMonth(today.year, today.monthValue)
    val status = budgetStatus(spent, budget, today.dayOfMonth, today.lengthOfMonth())
    return WidgetState(formatNpr(spent), budget > 0.0, status.progress, status.remainingLabel, status.band)
}
