package com.sabin.expensetracker

import kotlin.math.round

/** Largest amount the app accepts, matching the nine-digit number pad. */
const val MAX_AMOUNT = 999_999_999.0

private const val OPERATORS = "+-−×*÷/"
private val EXPRESSION = Regex("""[\d.+\-−×*÷/\s]*""")

/** True when [text] holds only digits, a decimal point, whitespace and calculator operators. */
fun isValidAmountInput(text: String): Boolean = EXPRESSION.matches(text)

/** Returns [new] if valid amount input (a number or simple arithmetic), otherwise keeps [old]. */
fun filterAmountInput(old: String, new: String): String = if (isValidAmountInput(new)) new else old

/** True when [text] contains an operator after its first character, so a live result is worth showing. */
fun isExpression(text: String): Boolean = text.trim().drop(1).any { it in OPERATORS }

/**
 * Evaluates [text] such as `120+45+30` or `2×3.5` (no parentheses; `×`/`*` and `÷`/`/` bind tighter than
 * `+` and `-`). Returns the result rounded to 2 decimals, or null if the text is malformed, divides by zero,
 * or the result is not positive or exceeds [MAX_AMOUNT].
 */
fun evaluateAmount(text: String): Double? {
    val tokens = tokenize(text) ?: return null
    if (tokens.size % 2 == 0) return null
    var sum = 0.0
    var sign = 1.0
    var term = tokens[0] as Double
    var i = 1
    while (i < tokens.size) {
        val op = tokens[i] as Char
        val operand = tokens[i + 1] as Double
        when (op) {
            '×' -> term *= operand
            '÷' -> { if (operand == 0.0) return null; term /= operand }
            else -> { sum += sign * term; sign = if (op == '+') 1.0 else -1.0; term = operand }
        }
        i += 2
    }
    val result = round((sum + sign * term) * 100) / 100
    return result.takeIf { it.isFinite() && it > 0 && it <= MAX_AMOUNT }
}

/** Alternating numbers and normalised operators, or null if malformed. */
private fun tokenize(text: String): List<Any>? {
    val out = mutableListOf<Any>()
    val number = StringBuilder()
    fun flush(): Boolean {
        val n = number.toString().toDoubleOrNull() ?: return false
        number.clear()
        out.add(n)
        return true
    }
    for (c in text) {
        when {
            c.isWhitespace() -> continue
            c.isDigit() && c in '0'..'9' || c == '.' -> number.append(c)
            c in OPERATORS -> {
                if (!flush()) return null
                out.add(when (c) { '*' -> '×'; '/' -> '÷'; '−' -> '-'; else -> c })
            }
            else -> return null
        }
    }
    return if (flush()) out else null
}
