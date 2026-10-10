package com.sabin.expensetracker

class BackupException(message: String) : Exception(message)

data class BackupData(val expenses: List<Expense>, val limits: CategoryLimits, val categories: List<String> = emptyList())

/** JSON backup of expenses (no Android dependencies, so it is unit-testable). */
object Backup {
    private const val VERSION = 1

    fun export(expenses: List<Expense>): String = exportFull(expenses, CategoryLimits.EMPTY)

    /** Like [export] but also stores category limits (omitted when there are none). */
    fun exportFull(
        expenses: List<Expense>,
        limits: CategoryLimits,
        categories: CategoryList = CategoryList.EMPTY
    ): String = buildString {
        append("{\"version\":").append(VERSION)
        if (categories.custom.isNotEmpty()) {
            append(",\"customCategories\":[").append(categories.custom.joinToString(",") { quote(it) }).append(']')
        }
        if (!limits.isEmpty()) {
            append(",\"categoryLimits\":{")
            limits.entries().entries.forEachIndexed { i, (k, v) ->
                if (i > 0) append(',')
                append(quote(k)).append(':').append(v)
            }
            append('}')
        }
        append(",\"expenses\":[")
        expenses.forEachIndexed { i, e ->
            if (i > 0) append(',')
            append("{\"id\":").append(e.id)
            append(",\"title\":").append(quote(e.title))
            append(",\"amount\":").append(e.amount)
            append(",\"category\":").append(quote(e.category))
            append(",\"timestamp\":").append(e.timestamp)
            if (e.note.isNotEmpty()) append(",\"note\":").append(quote(e.note))
            append('}')
        }
        append("]}")
    }

    /** Parses a backup; throws [BackupException] if the file is not a valid backup. */
    fun parse(json: String): List<Expense> = parseFull(json).expenses

    /** Parses expenses and category limits; backups without limits yield empty limits. */
    fun parseFull(json: String): BackupData {
        try {
            val root = Parser(json).parseDocument() as? Map<*, *> ?: bad("Not a backup file")
            if (root["version"] != VERSION.toDouble()) bad("Unsupported backup version")
            val list = root["expenses"] as? List<*> ?: bad("Missing expenses")
            val limits = (root["categoryLimits"] as? Map<*, *>)?.entries?.fold(CategoryLimits.EMPTY) { acc, (k, v) ->
                val d = v as? Double
                if (k is String && d != null) acc.with(k, d) else acc
            } ?: CategoryLimits.EMPTY
            val categories = (root["customCategories"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val expenses = list.map { item ->
                val m = item as? Map<*, *> ?: bad("Invalid expense")
                val amount = m["amount"] as? Double ?: bad("Invalid amount")
                val title = m["title"] as? String ?: bad("Invalid title")
                if (title.isBlank() || !amount.isFinite() || amount <= 0) bad("Invalid expense")
                Expense(
                    wholeNumber(m["id"]),
                    title,
                    amount,
                    m["category"] as? String ?: bad("Invalid category"),
                    wholeNumber(m["timestamp"]),
                    (m["note"] as? String ?: "").trim().take(MAX_NOTE_LENGTH)
                )
            }
            return BackupData(expenses, limits, categories)
        } catch (e: BackupException) {
            throw e
        } catch (e: RuntimeException) {
            throw BackupException("Not a valid backup file")
        }
    }

    private fun bad(msg: String): Nothing = throw BackupException(msg)

    private fun wholeNumber(v: Any?): Long {
        val d = v as? Double ?: bad("Invalid number")
        if (d != Math.floor(d) || Math.abs(d) > 9.0e15) bad("Invalid number")
        return d.toLong()
    }

    private fun quote(s: String) = buildString {
        append('"')
        s.forEach {
            when {
                it == '"' -> append("\\\"")
                it == '\\' -> append("\\\\")
                it < ' ' -> append("\\u%04x".format(it.code))
                else -> append(it)
            }
        }
        append('"')
    }

    /** Minimal strict JSON parser: objects, arrays, strings, numbers (as Double), true/false/null. */
    private class Parser(private val s: String) {
        private var i = 0

        fun parseDocument(): Any? {
            val v = value()
            ws()
            if (i != s.length) bad("Trailing data")
            return v
        }

        private fun ws() { while (i < s.length && s[i] in " \t\r\n") i++ }

        private fun peek(): Char = if (i < s.length) s[i] else bad("Unexpected end")

        private fun expect(c: Char) { if (peek() != c) bad("Expected $c"); i++ }

        private fun value(): Any? {
            ws()
            return when (peek()) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> lit("true", true)
                'f' -> lit("false", false)
                'n' -> lit("null", null)
                else -> num()
            }
        }

        private fun lit(word: String, v: Any?): Any? {
            if (!s.startsWith(word, i)) bad("Unexpected token")
            i += word.length
            return v
        }

        private fun obj(): Map<String, Any?> {
            expect('{')
            val m = LinkedHashMap<String, Any?>()
            ws()
            if (peek() == '}') { i++; return m }
            while (true) {
                ws()
                val k = str()
                ws(); expect(':')
                m[k] = value()
                ws()
                if (peek() == ',') i++ else { expect('}'); return m }
            }
        }

        private fun arr(): List<Any?> {
            expect('[')
            val l = ArrayList<Any?>()
            ws()
            if (peek() == ']') { i++; return l }
            while (true) {
                l.add(value())
                ws()
                if (peek() == ',') i++ else { expect(']'); return l }
            }
        }

        private fun str(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                val c = peek(); i++
                when {
                    c == '"' -> return sb.toString()
                    c == '\\' -> {
                        val e = peek(); i++
                        when (e) {
                            '"', '\\', '/' -> sb.append(e)
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n'); 'r' -> sb.append('\r'); 't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) bad("Bad escape")
                                sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4
                            }
                            else -> bad("Bad escape")
                        }
                    }
                    c < ' ' -> bad("Control character in string")
                    else -> sb.append(c)
                }
            }
        }

        private fun num(): Double {
            val start = i
            while (i < s.length && s[i] in "+-0123456789.eE") i++
            return s.substring(start, i).toDoubleOrNull() ?: bad("Invalid number")
        }
    }
}
