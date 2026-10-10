package com.sabin.expensetracker

/** Spoken label for the delete action of one expense row. */
fun deleteLabel(title: String): String = "Delete $title"

/** Spoken summary of one expense row. */
fun expenseSummary(title: String, category: String, amount: Double, note: String = ""): String =
    "$title, $category, ${formatNpr(amount)}" + if (note.isEmpty()) "" else ", $note"
