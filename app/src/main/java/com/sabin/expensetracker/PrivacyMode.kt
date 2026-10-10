package com.sabin.expensetracker

/** Opt-in "hide app content" preference (maps to FLAG_SECURE). Off by default. */
class PrivacyMode(private val store: FlagStore) {
    val enabled: Boolean get() = store.get()
    fun setEnabled(value: Boolean) = store.set(value)
    fun toggle(): Boolean = !store.get().also { store.set(!it) }
}
