package com.sabin.expensetracker

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconTest {
    private val main = File("src/main")

    @Test
    fun manifestUsesMipmapIcons() {
        val manifest = File(main, "AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_launcher\""))
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))
    }

    @Test
    fun adaptiveIconsReferenceBackgroundAndForeground() {
        for (name in listOf("ic_launcher", "ic_launcher_round")) {
            val xml = File(main, "res/mipmap-anydpi-v26/$name.xml").readText()
            assertTrue(xml.contains("<adaptive-icon"))
            assertTrue(xml.contains("@drawable/ic_launcher_background"))
            assertTrue(xml.contains("@drawable/ic_launcher_foreground"))
        }
    }

    @Test
    fun layersExistAndOldPlaceholderIsGone() {
        assertTrue(File(main, "res/drawable/ic_launcher_background.xml").exists())
        assertTrue(File(main, "res/drawable/ic_launcher_foreground.xml").exists())
        assertFalse(File(main, "res/drawable/ic_launcher.xml").exists())
    }

    @Test
    fun backgroundUsesStoreGreens() {
        val bg = File(main, "res/drawable/ic_launcher_background.xml").readText().uppercase()
        assertTrue(bg.contains("#10A37F"))
        assertTrue(bg.contains("#0A6E5A"))
    }
}
