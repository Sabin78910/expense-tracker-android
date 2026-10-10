package com.sabin.expensetracker

import java.util.Locale

/** Formats [amount] like "NPR 1,850.00" using [locale] digits and grouping (device locale by default). */
fun formatNpr(amount: Double, locale: Locale = Locale.getDefault()): String = String.format(locale, "NPR %,.2f", amount)
