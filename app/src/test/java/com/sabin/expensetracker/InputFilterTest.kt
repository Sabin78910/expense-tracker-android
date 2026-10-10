package com.sabin.expensetracker

import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputFilterTest {
    @Test fun acceptsDigitsAndOneDecimalPoint() {
        listOf("", "0", "12", "12.", "12.50", ".5").forEach { assertTrue(it, isValidDecimalInput(it)) }
    }

    @Test fun rejectsEverythingElse() {
        listOf(",9=11", "1.2.3", "-5", "1e5", "a", " 1", "1,5").forEach { assertFalse(it, isValidDecimalInput(it)) }
    }

    @Test fun filterKeepsPreviousTextWhenInvalid() {
        assertEquals("12", filterDecimalInput("12", "12x"))
        assertEquals("12.5", filterDecimalInput("12", "12.5"))
    }

    @Test fun weekdayLabelIsShortAndLocalized() {
        val monday = LocalDate.of(2026, 10, 5)
        assertEquals("Mon", weekdayLabel(monday, Locale.ENGLISH))
        assertEquals("lun", weekdayLabel(monday, Locale.FRENCH).lowercase().take(3))
    }

    @Test
    fun noteInputRejectsTooLongAndMultiline() {
        assertEquals("ab", filterNoteInput("a", "ab"))
        assertEquals("a", filterNoteInput("a", "a".repeat(MAX_NOTE_LENGTH + 1)))
        assertEquals("a", filterNoteInput("a", "a\nb"))
        assertEquals("x".repeat(MAX_NOTE_LENGTH), filterNoteInput("", "x".repeat(MAX_NOTE_LENGTH)))
    }
}
