package com.sabin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTest {
    private class MemoryStore(var seen: Boolean = false) : FlagStore {
        override fun get() = seen
        override fun set(value: Boolean) { seen = value }
    }

    @Test fun shownOnFirstRun() {
        assertTrue(FirstRun(MemoryStore()).shouldShow())
    }

    @Test fun notShownAfterCompleted() {
        val store = MemoryStore()
        FirstRun(store).markDone()
        assertFalse(FirstRun(store).shouldShow())
    }

    @Test fun skipAlsoMarksDone() {
        val store = MemoryStore()
        val flow = OnboardingFlow(FirstRun(store))
        flow.skip()
        assertTrue(flow.finished)
        assertFalse(FirstRun(store).shouldShow())
    }

    @Test fun threePagesAndLastOpensAddExpense() {
        assertEquals(3, ONBOARDING_PAGES.size)
        val flow = OnboardingFlow(FirstRun(MemoryStore()))
        assertFalse(flow.isLast)
        flow.next(); flow.next()
        assertTrue(flow.isLast)
        assertTrue(flow.next()) // true = open the add-expense form
        assertTrue(flow.finished)
    }

    @Test fun nextBeforeLastDoesNotOpenForm() {
        val flow = OnboardingFlow(FirstRun(MemoryStore()))
        assertFalse(flow.next())
        assertEquals(1, flow.page)
    }
}
