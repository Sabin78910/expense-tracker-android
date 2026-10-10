package com.sabin.expensetracker

import kotlin.math.roundToInt

/** Value shown during the count-up animation; [fraction] is clamped to 0..1. */
fun countUpValue(target: Double, fraction: Float): Double = target * fraction.coerceIn(0f, 1f)

fun categoryGlyph(category: String): String = when (category) {
    "Food" -> "🍔"
    "Transport" -> "🚌"
    "Bills" -> "🧾"
    "Shopping" -> "🛍"
    else -> "✨"
}

fun budgetPercentLabel(progress: Float, hasBudget: Boolean): String =
    if (hasBudget) "${(progress * 100).roundToInt()}% of budget" else ""

/** Share of the budget still unspent, 0..1 (0 when no budget is set). */
fun budgetLeftFraction(spent: Double, budget: Double): Float =
    if (budget <= 0.0) 0f else (1.0 - spent / budget).coerceIn(0.0, 1.0).toFloat()

/** Ring colour band: green under 80%, amber 80-100%, red over 100%. */
fun ringBand(spent: Double, budget: Double): BudgetBand =
    budgetStatus(spent, budget, 1, 1).band

/** Arrow glyph so the month-over-month delta is not conveyed by colour alone. */
fun deltaArrow(percentChange: Int?): String = when {
    percentChange == null -> ""
    percentChange > 0 -> "↑"
    percentChange < 0 -> "↓"
    else -> "→"
}

/** TalkBack description of the budget ring, stating the band in words. */
fun ringDescription(band: BudgetBand, leftFraction: Float): String {
    val state = when (band) {
        BudgetBand.NONE -> return ""
        BudgetBand.GREEN -> "on track"
        BudgetBand.AMBER -> "nearing limit"
        BudgetBand.RED -> "over budget"
    }
    return "Budget: ${(leftFraction * 100).roundToInt()}% left, $state"
}
