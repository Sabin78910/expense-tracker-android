package com.sabin.expensetracker

/** Shows the in-app review flow. The Play-backed implementation lives in the UI layer. */
interface ReviewLauncher {
    fun launch()
}

interface ReviewStore {
    fun lastAskedMillis(): Long?
    fun setLastAskedMillis(value: Long)
}

const val REVIEW_MIN_GAP_DAYS = 60L

/**
 * Asks for a Play Store rating only after a positive moment (a badge unlock, which
 * includes the 7-day streak), at most once per [REVIEW_MIN_GAP_DAYS], never on the
 * first launch and never right after an error.
 */
class ReviewPrompt(private val launcher: ReviewLauncher, private val store: ReviewStore) {
    fun onPositiveMoment(newBadges: Set<Badge>, nowMillis: Long, launchCount: Int, hasError: Boolean): Boolean {
        if (newBadges.isEmpty() || hasError || launchCount < 2) return false
        val last = store.lastAskedMillis()
        if (last != null && nowMillis - last < REVIEW_MIN_GAP_DAYS * 24L * 60 * 60 * 1000) return false
        store.setLastAskedMillis(nowMillis)
        launcher.launch()
        return true
    }
}
