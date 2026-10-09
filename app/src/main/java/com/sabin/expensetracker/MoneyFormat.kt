package com.sabin.expensetracker

import java.util.Locale

/** Formats [amount] like "NPR 1,850.00" (locale-independent). */
fun formatNpr(amount: Double): String = String.format(Locale.US, "NPR %,.2f", amount)
