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
