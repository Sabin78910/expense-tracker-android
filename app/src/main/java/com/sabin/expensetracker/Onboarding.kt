package com.sabin.expensetracker

interface FlagStore {
    fun get(): Boolean
    fun set(value: Boolean)
}

/** Tracks whether onboarding has been completed or skipped. */
class FirstRun(private val store: FlagStore) {
    fun shouldShow(): Boolean = !store.get()
    fun markDone() = store.set(true)
}

class OnboardingPage(val title: String, val benefit: String)

val ONBOARDING_PAGES = listOf(
    OnboardingPage("Log in seconds", "Add an expense with just an amount and a tap."),
    OnboardingPage("See where it goes", "Charts show your spending by category and week."),
    OnboardingPage("Stay on budget", "Set a monthly budget and keep your streak going.")
)

class OnboardingFlow(private val firstRun: FirstRun, private val pageCount: Int = ONBOARDING_PAGES.size) {
    var page = 0
        private set
    var finished = false
        private set
    val isLast get() = page == pageCount - 1

    /** Advances; returns true when finishing from the last page (caller opens the add-expense form). */
    fun next(): Boolean {
        if (!isLast) { page++; return false }
        finish()
        return true
    }

    fun skip() = finish()

    private fun finish() { finished = true; firstRun.markDone() }
}
