package com.sabin.expensetracker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.app.NotificationCompat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private const val CHANNEL_ID = "daily_reminder"

object ReminderScheduler {
    private fun prefs(c: Context) = c.getSharedPreferences("expenses", Context.MODE_PRIVATE)

    fun isEnabled(c: Context) = prefs(c).getBoolean("reminder_on", false)
    fun time(c: Context): LocalTime =
        LocalTime.ofSecondOfDay(prefs(c).getLong("reminder_secs", DEFAULT_REMINDER_TIME.toSecondOfDay().toLong()))

    fun setEnabled(c: Context, on: Boolean) {
        prefs(c).edit().putBoolean("reminder_on", on).apply()
        if (on) schedule(c) else cancel(c)
    }

    fun setTime(c: Context, t: LocalTime) {
        prefs(c).edit().putLong("reminder_secs", t.toSecondOfDay().toLong()).apply()
        if (isEnabled(c)) schedule(c)
    }

    private fun pending(c: Context) = PendingIntent.getBroadcast(
        c, 0, Intent(c, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Inexact alarm: needs no exact-alarm permission. */
    fun schedule(c: Context) {
        val book = ExpenseBook.deserialize(prefs(c).getString("data", "") ?: "")
        val next = nextReminder(LocalDateTime.now(), time(c), book.expenses)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        c.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC, millis, pending(c))
    }

    fun cancel(c: Context) {
        c.getSystemService(AlarmManager::class.java).cancel(pending(c))
    }

    fun notify(c: Context) {
        val granted = c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val book = ExpenseBook.deserialize(prefs(c).getString("data", "") ?: "")
        if (!granted || !shouldRemind(book.expenses, java.time.LocalDate.now())) return
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, c.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        nm.notify(
            1,
            NotificationCompat.Builder(c, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(c.getString(R.string.app_name))
                .setContentText(c.getString(R.string.reminder_text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        )
    }
}

/** Fires the daily alarm, then books the next one. Also reschedules after reboot. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ReminderScheduler.isEnabled(context)) return
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) ReminderScheduler.notify(context)
        ReminderScheduler.schedule(context)
    }
}
