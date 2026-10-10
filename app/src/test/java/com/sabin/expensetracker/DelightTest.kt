package com.sabin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DelightTest {
    @Test fun milestoneReachedWhenCrossingSeven() {
        assertEquals(7, streakMilestoneReached(6, 7))
    }

    @Test fun milestoneReachedAt30And100() {
        assertEquals(30, streakMilestoneReached(29, 30))
        assertEquals(100, streakMilestoneReached(99, 100))
    }

    @Test fun noMilestoneWhenNotCrossing() {
        assertNull(streakMilestoneReached(7, 8))
        assertNull(streakMilestoneReached(0, 1))
        assertNull(streakMilestoneReached(7, 7))
    }

    @Test fun noMilestoneWhenStreakFalls() {
        assertNull(streakMilestoneReached(10, 1))
    }

    @Test fun highestCrossedMilestoneWins() {
        assertEquals(30, streakMilestoneReached(0, 30))
    }

    @Test fun streakGrewOnlyWhenIncreased() {
        assertTrue(streakGrew(2, 3))
        assertFalse(streakGrew(3, 3))
        assertFalse(streakGrew(3, 0))
    }

    @Test fun keypadAppendsDigits() {
        assertEquals("12", keypadPress("1", '2'))
    }

    @Test fun keypadAllowsOneDecimalPoint() {
        assertEquals("1.", keypadPress("1", '.'))
        assertEquals("1.5", keypadPress("1.", '5'))
        assertEquals("1.5", keypadPress("1.5", '.'))
    }

    @Test fun keypadLeadingDecimalGetsZero() {
        assertEquals("0.", keypadPress("", '.'))
    }

    @Test fun keypadLimitsToTwoDecimals() {
        assertEquals("1.25", keypadPress("1.25", '9'))
    }

    @Test fun keypadLimitsLength() {
        val full = "1".repeat(MAX_KEYPAD_DIGITS)
        assertEquals(full, keypadPress(full, '2'))
    }

    @Test fun keypadBackspace() {
        assertEquals("1", keypadPress("12", KEYPAD_BACKSPACE))
        assertEquals("", keypadPress("", KEYPAD_BACKSPACE))
    }

    @Test fun keypadIgnoresUnknownKeys() {
        assertEquals("1", keypadPress("1", 'x'))
    }
}
