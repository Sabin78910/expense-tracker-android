package com.sabin.expensetracker

import java.time.Instant
import java.time.ZoneId

/** Spreadsheet-friendly CSV export (RFC 4180; no Android dependencies). */
object CsvExport {
    fun export(expenses: List<Expense>, zone: ZoneId): String = buildString {
        append("date,title,category,amount,note\r\n")
        expenses.forEach { e ->
            val date = if (e.timestamp == 0L) "" else Instant.ofEpochMilli(e.timestamp).atZone(zone).toLocalDate().toString()
            append(date).append(',')
                .append(field(e.title)).append(',')
                .append(field(e.category)).append(',')
                .append(e.amount.toString()).append(',')
                .append(field(e.note)).append("\r\n")
        }
    }

    private fun field(raw: String): String {
        val s = if (raw.isNotEmpty() && raw[0] in "=+-@") "'$raw" else raw
        return if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
    }
}
