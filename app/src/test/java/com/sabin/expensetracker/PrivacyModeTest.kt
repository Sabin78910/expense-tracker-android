package com.sabin.expensetracker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyModeTest {
    private class MemoryStore(var value: Boolean = false) : FlagStore {
        override fun get() = value
        override fun set(value: Boolean) { this.value = value }
    }

    @Test fun defaultsToOff() {
        assertFalse(PrivacyMode(MemoryStore()).enabled)
    }

    @Test fun toggleFlipsState() {
        val mode = PrivacyMode(MemoryStore())
        assertTrue(mode.toggle())
        assertTrue(mode.enabled)
        assertFalse(mode.toggle())
        assertFalse(mode.enabled)
    }

    @Test fun persistsAcrossInstances() {
        val store = MemoryStore()
        PrivacyMode(store).setEnabled(true)
        assertTrue(PrivacyMode(store).enabled)
        PrivacyMode(store).setEnabled(false)
        assertFalse(PrivacyMode(store).enabled)
    }
}
