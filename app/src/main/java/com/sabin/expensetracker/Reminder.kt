package com.sabin.expensetracker

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(21, 0)

/** A reminder is only useful on days with no logged expense. */
fun shouldRemind(
    expenses: List<Expense>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): Boolean = expenses.none {
    it.timestamp > 0 && Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() == today
}

/** Next time to fire: today at [time] if still ahead and nothing logged today, else tomorrow. At most one per day. */
fun nextReminder(
    now: LocalDateTime,
    time: LocalTime,
    expenses: List<Expense>,
    zone: ZoneId = ZoneId.systemDefault()
): LocalDateTime {
    val today = now.toLocalDate().atTime(time)
    val useToday = now.isBefore(today) && shouldRemind(expenses, now.toLocalDate(), zone)
    return if (useToday) today else today.plusDays(1)
}
