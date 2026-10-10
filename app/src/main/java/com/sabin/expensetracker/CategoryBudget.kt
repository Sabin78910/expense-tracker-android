package com.sabin.expensetracker

import java.util.Locale

/** Optional monthly limit per category; a missing entry means no limit. */
class CategoryLimits private constructor(private val map: Map<String, Double>) {
    fun limitFor(category: String): Double = map[category] ?: 0.0

    fun isEmpty() = map.isEmpty()

    /** Returns a copy with [category] set to [limit]; a non-positive or non-finite limit clears it. */
    fun with(category: String, limit: Double): CategoryLimits =
        CategoryLimits(if (limit > 0 && limit.isFinite()) map + (category to limit) else map - category)

    fun entries(): Map<String, Double> = map

    fun serialize(): String = map.entries.joinToString(";") { "${it.key}=${it.value}" }

    override fun equals(other: Any?) = other is CategoryLimits && other.map == map
    override fun hashCode() = map.hashCode()

    companion object {
        val EMPTY = CategoryLimits(emptyMap())

        fun deserialize(s: String): CategoryLimits =
            s.split(';').fold(EMPTY) { acc, part ->
                val key = part.substringBefore('=', "")
                val v = part.substringAfter('=', "").toDoubleOrNull()
                if (key.isBlank() || v == null) acc else acc.with(key, v)
            }
    }
}

/** Same green/amber/red bands as the overall budget; NONE when [limit] is not set. */
fun categoryBudgetStatus(spent: Double, limit: Double): BudgetStatus = budgetStatus(spent, limit, 1, 31)

/** Amount left (negative when over); null when there is no limit. */
fun categoryRemaining(spent: Double, limit: Double): Double? = if (limit > 0) limit - spent else null

/** Spoken description of a category progress bar, e.g. "Food: 800 of 1,000 rupees used". */
fun categoryBudgetDescription(name: String, spent: Double, limit: Double, locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%s: %,.0f of %,.0f rupees used", name, spent, limit)
