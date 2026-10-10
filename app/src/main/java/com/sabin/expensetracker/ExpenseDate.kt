package com.sabin.expensetracker

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Pure date helpers for choosing an expense's date (no Android dependencies). */
object ExpenseDate {
    /** Epoch millis of [date] at [timeOfDay] in [zone]; future dates (after [today]) are rejected. */
    fun timestampFor(
        date: LocalDate,
        timeOfDay: LocalTime,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        require(!date.isAfter(today)) { "Date cannot be in the future" }
        return date.atTime(timeOfDay).atZone(zone).toInstant().toEpochMilli()
    }

    /** Time of day of [timestamp] in [zone]. */
    fun timeOfDay(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): LocalTime =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalTime()

    /** Material date pickers speak UTC-midnight millis. */
    fun toPickerMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fromPickerMillis(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    fun isSelectable(pickerMillis: Long, today: LocalDate = LocalDate.now()): Boolean =
        !fromPickerMillis(pickerMillis).isAfter(today)
}
