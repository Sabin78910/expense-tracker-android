package com.sabin.expensetracker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeTest {
    @Test fun lightFallbackUsesEmeraldSeed() {
        assertEquals(Color(0xFF10B981), fallbackColorScheme(dark = false).primary)
    }

    @Test fun darkFallbackDiffersFromLight() {
        val light = fallbackColorScheme(dark = false)
        val dark = fallbackColorScheme(dark = true)
        assertNotEquals(light.background, dark.background)
        assertNotEquals(light.onSurface, dark.onSurface)
    }

    @Test fun shapesAreExpressive() {
        assertEquals(24f, EXPENSE_CARD_RADIUS_DP)
        assertEquals(28f, expenseShapesLargeDp)
    }

    @Test fun moneyStyleIsBoldDisplay() {
        assertEquals(FontWeight.Bold, expenseTypography().displayMedium.fontWeight)
    }
}
