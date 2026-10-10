package com.sabin.expensetracker

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchShortcutTest {
    private val main = File("src/main")

    @Test fun normalLaunchDoesNotOpenForm() = assertFalse(shouldOpenForm(extraOpenForm = false, afterOnboarding = false))

    @Test fun shortcutLaunchOpensForm() = assertTrue(shouldOpenForm(extraOpenForm = true, afterOnboarding = false))

    @Test fun onboardingChoiceOpensForm() = assertTrue(shouldOpenForm(extraOpenForm = false, afterOnboarding = true))

    @Test fun manifestReferencesShortcuts() {
        val manifest = File(main, "AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:name=\"android.app.shortcuts\""))
        assertTrue(manifest.contains("android:resource=\"@xml/shortcuts\""))
    }

    @Test fun shortcutTargetsMainActivityWithOpenFormExtra() {
        val xml = File(main, "res/xml/shortcuts.xml").readText()
        assertEquals(1, Regex("<shortcut\\b").findAll(xml).count())
        assertTrue(xml.contains("android:targetClass=\"com.sabin.expensetracker.MainActivity\""))
        assertTrue(xml.contains("android:name=\"$EXTRA_OPEN_FORM\""))
        assertTrue(xml.contains("android:value=\"true\""))
        assertTrue(xml.contains("@string/shortcut_add_short"))
        assertTrue(xml.contains("@string/shortcut_add_long"))
    }

    @Test fun shortcutStringsExistInBothLocales() {
        for (path in listOf("res/values/strings.xml", "res/values-ne/strings.xml")) {
            val xml = File(main, path).readText()
            assertTrue(path, xml.contains("name=\"shortcut_add_short\""))
            assertTrue(path, xml.contains("name=\"shortcut_add_long\""))
        }
    }
}
