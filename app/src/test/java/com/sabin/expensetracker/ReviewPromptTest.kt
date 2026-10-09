package com.sabin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptTest {
    private class FakeReviewLauncher : ReviewLauncher {
        var launched = 0
        override fun launch() { launched++ }
    }

    private class MemoryStore(var last: Long? = null) : ReviewStore {
        override fun lastAskedMillis() = last
        override fun setLastAskedMillis(value: Long) { last = value }
    }

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_000 * day

    private fun prompt(store: MemoryStore = MemoryStore(), fake: FakeReviewLauncher = FakeReviewLauncher()) =
        ReviewPrompt(fake, store) to fake

    @Test fun asksAfterBadgeUnlock() {
        val (p, fake) = prompt()
        assertTrue(p.onPositiveMoment(setOf(Badge.STREAK_7), now, launchCount = 3, hasError = false))
        assertEquals(1, fake.launched)
    }

    @Test fun doesNotAskWithoutNewBadge() {
        val (p, fake) = prompt()
        assertFalse(p.onPositiveMoment(emptySet(), now, 3, false))
        assertEquals(0, fake.launched)
    }

    @Test fun neverOnFirstLaunch() {
        val (p, fake) = prompt()
        assertFalse(p.onPositiveMoment(setOf(Badge.FIRST_EXPENSE), now, 1, false))
        assertEquals(0, fake.launched)
    }

    @Test fun neverAfterError() {
        val (p, fake) = prompt()
        assertFalse(p.onPositiveMoment(setOf(Badge.STREAK_7), now, 3, true))
        assertEquals(0, fake.launched)
    }

    @Test fun atMostOnceEvery60Days() {
        val store = MemoryStore()
        val (p, fake) = prompt(store)
        assertTrue(p.onPositiveMoment(setOf(Badge.STREAK_7), now, 3, false))
        assertEquals(now, store.last)
        assertFalse(p.onPositiveMoment(setOf(Badge.NO_SPEND_DAY), now + 59 * day, 3, false))
        assertTrue(p.onPositiveMoment(setOf(Badge.NO_SPEND_DAY), now + 60 * day, 3, false))
        assertEquals(2, fake.launched)
    }
}
