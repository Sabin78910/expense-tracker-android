package com.sabin.expensetracker

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvExportTest {
    private val utc = ZoneOffset.UTC
    private val header = "date,title,category,amount,note"

    @Test fun emptyListGivesHeaderOnly() {
        assertEquals("$header\r\n", CsvExport.export(emptyList(), utc))
    }

    @Test fun rowsUseIsoDateAndDotDecimal() {
        val e = Expense(1, "Lunch", 12.5, "Food", 1_700_000_000_000L)
        assertEquals("$header\r\n2023-11-14,Lunch,Food,12.5,\r\n", CsvExport.export(listOf(e), utc))
    }

    @Test fun zeroTimestampGivesEmptyDate() {
        val e = Expense(1, "Old", 3.0, "Other", 0L)
        assertEquals("$header\r\n,Old,Other,3.0,\r\n", CsvExport.export(listOf(e), utc))
    }

    @Test fun zoneAffectsDate() {
        val e = Expense(1, "Late", 1.0, "Other", 1_700_000_000_000L + 6 * 3_600_000L)
        assertEquals("2023-11-15", CsvExport.export(listOf(e), ZoneOffset.ofHours(5)).lines()[1].substringBefore(','))
    }

    @Test fun specialCharactersAreQuoted() {
        val e = Expense(1, "a, \"b\"\nc", 1.0, "Food", 0L)
        assertEquals("$header\r\n,\"a, \"\"b\"\"\nc\",Food,1.0,\r\n", CsvExport.export(listOf(e), utc))
    }

    @Test fun formulaInjectionIsGuarded() {
        listOf("=1+1", "+1", "-1", "@SUM(A1)").forEach {
            val row = CsvExport.export(listOf(Expense(1, it, 1.0, "Food", 0L)), utc).lines()[1]
            assertEquals(",'$it,Food,1.0,", row)
        }
    }

    @Test fun formulaGuardAppliesToCategoryToo() {
        val row = CsvExport.export(listOf(Expense(1, "x", 1.0, "=cmd", 0L)), utc).lines()[1]
        assertEquals(",x,'=cmd,1.0,", row)
    }

    @Test fun noteColumnIsEscaped() {
        val e = Expense(1, "x", 1.0, "Food", 0L, "a, \"b\"")
        assertEquals("$header\r\n,x,Food,1.0,\"a, \"\"b\"\"\"\r\n", CsvExport.export(listOf(e), utc))
    }

    @Test fun noteFormulaIsGuarded() {
        val e = Expense(1, "x", 1.0, "Food", 0L, "=1+1")
        assertEquals(",x,Food,1.0,'=1+1", CsvExport.export(listOf(e), utc).lines()[1])
    }
}
