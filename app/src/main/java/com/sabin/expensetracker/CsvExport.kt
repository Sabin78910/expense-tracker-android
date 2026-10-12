package com.sabin.expensetracker

import java.time.Instant
import java.time.ZoneId

/** Spreadsheet-friendly CSV export (RFC 4180; no Android dependencies). */
object CsvExport {
    /** A wallet column is added when [wallets] is given. */
    fun export(expenses: List<Expense>, zone: ZoneId, wallets: List<Wallet>? = null): String = buildString {
        append("date,title,category,amount,note").append(if (wallets != null) ",wallet" else "").append("\r\n")
        expenses.forEach { e ->
            val date = if (e.timestamp == 0L) "" else Instant.ofEpochMilli(e.timestamp).atZone(zone).toLocalDate().toString()
            append(date).append(',')
                .append(field(e.title)).append(',')
                .append(field(e.category)).append(',')
                .append(e.amount.toString()).append(',')
                .append(field(e.note))
            if (wallets != null) append(',').append(field(wallets.firstOrNull { it.id == e.walletId }?.name ?: ""))
            append("\r\n")
        }
    }

    private fun field(raw: String): String {
        val s = if (raw.isNotEmpty() && raw[0] in "=+-@") "'$raw" else raw
        return if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
    }
}
