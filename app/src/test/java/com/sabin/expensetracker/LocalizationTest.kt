package com.sabin.expensetracker

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationTest {
    private fun strings(path: String): Map<String, String> {
        val xml = File(path).readText()
        return Regex("""<string\s+name="([^"]+)"([^>]*)>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml)
            .filter { !it.groupValues[2].contains("translatable=\"false\"") }
            .associate { it.groupValues[1] to it.groupValues[3].trim() }
    }

    private val default = strings("src/main/res/values/strings.xml")
    private val nepali = strings("src/main/res/values-ne/strings.xml")

    @Test fun everyDefaultStringHasNepaliTranslation() {
        val missing = default.keys - nepali.keys
        assertTrue("Missing Nepali strings: $missing", missing.isEmpty())
        assertTrue("Stale Nepali strings: ${nepali.keys - default.keys}", (nepali.keys - default.keys).isEmpty())
    }

    @Test fun nepaliStringsAreActuallyTranslated() {
        val blank = nepali.filterValues { it.isBlank() }.keys
        assertTrue("Blank: $blank", blank.isEmpty())
        val untranslated = default.filter { (k, v) -> v.any { it.isLetter() } && nepali[k] == v && k != "app_name" }.keys
        assertTrue("Same as English: $untranslated", untranslated.isEmpty())
    }

    @Test fun placeholdersMatch() {
        val ph = Regex("""%\d\$[sd]""")
        default.forEach { (k, v) ->
            assertEquals("Placeholders for $k", ph.findAll(v).map { it.value }.sorted().toList(),
                ph.findAll(nepali[k] ?: "").map { it.value }.sorted().toList())
        }
    }

    @Test fun formatsMoneyWithGivenLocale() {
        assertEquals("NPR 1,850.00", formatNpr(1850.0, java.util.Locale.US))
    }
}
