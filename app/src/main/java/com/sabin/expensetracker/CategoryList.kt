package com.sabin.expensetracker

const val MAX_CATEGORY_LENGTH = 20

/** The built-in categories plus user-defined ones (no Android dependencies). */
class CategoryList private constructor(val custom: List<String>) {
    val all: List<String> get() = DEFAULTS + custom

    fun isCustom(name: String) = custom.contains(name)

    /** Returns a copy with [name] added (trimmed), or null if blank, too long or a case-insensitive duplicate. */
    fun add(name: String): CategoryList? {
        val n = name.trim()
        if (n.isEmpty() || n.length > MAX_CATEGORY_LENGTH || '\n' in n) return null
        if (all.any { it.equals(n, ignoreCase = true) }) return null
        return CategoryList(custom + n)
    }

    /** Returns a copy without custom category [name]; default categories cannot be removed. */
    fun remove(name: String): CategoryList = if (isCustom(name)) CategoryList(custom - name) else this

    /** Adds each valid, new name from [names] (e.g. from a backup). */
    fun withAll(names: List<String>): CategoryList = names.fold(this) { acc, n -> acc.add(n) ?: acc }

    fun serialize(): String = custom.joinToString("\n")

    override fun equals(other: Any?) = other is CategoryList && other.custom == custom
    override fun hashCode() = custom.hashCode()

    companion object {
        val DEFAULTS = listOf("Food", "Transport", "Bills", "Shopping", "Other")
        const val OTHER = "Other"
        val EMPTY = CategoryList(emptyList())

        fun deserialize(s: String): CategoryList = EMPTY.withAll(s.lines())
    }
}
