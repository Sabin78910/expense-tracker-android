package com.sabin.expensetracker

enum class BudgetBand { NONE, GREEN, AMBER, RED }

data class BudgetStatus(
    val band: BudgetBand,
    /** Fraction spent, clamped to 0..1 for a progress bar. */
    val progress: Float,
    /** "NPR X left" or "NPR X over"; empty when no budget is set. */
    val remainingLabel: String,
    /** Encouraging note when on track; null otherwise. */
    val message: String?
)

/** Pure budget logic: green under 80%, amber 80-100%, red over 100%. */
fun budgetStatus(spent: Double, budget: Double, dayOfMonth: Int, daysInMonth: Int): BudgetStatus {
    if (budget <= 0.0) return BudgetStatus(BudgetBand.NONE, 0f, "", null)
    val ratio = spent / budget
    val band = when {
        ratio > 1.0 -> BudgetBand.RED
        ratio >= 0.8 -> BudgetBand.AMBER
        else -> BudgetBand.GREEN
    }
    val label = if (spent > budget) formatNpr(spent - budget) + " over" else formatNpr(budget - spent) + " left"
    val onTrack = ratio < 0.5 && dayOfMonth * 2 >= daysInMonth
    return BudgetStatus(
        band,
        ratio.coerceIn(0.0, 1.0).toFloat(),
        label,
        if (onTrack) "You're on track this month. Keep it up!" else null
    )
}
