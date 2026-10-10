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

    @Test fun darkSurfaceIsDeepForest() {
        assertEquals(Color(0xFF07130E), fallbackColorScheme(dark = true).background)
        assertEquals(BRAND_TEAL, fallbackColorScheme(dark = true).tertiary)
    }

    @Test fun moneyUsesTabularNumbers() {
        assertEquals("tnum", moneyTextStyle().fontFeatureSettings)
        assertEquals(FontWeight.ExtraBold, moneyTextStyle().fontWeight)
    }

    @Test fun typeScaleHasExpressiveDisplayAndTabularLabels() {
        val t = expenseTypography()
        assertEquals(FontWeight.Bold, t.displayLarge.fontWeight)
        assertEquals("tnum", t.displayLarge.fontFeatureSettings)
        assertEquals(FontWeight.SemiBold, t.titleLarge.fontWeight)
    }

    @Test fun reducedMotionWhenAnimatorScaleIsZero() {
        assertEquals(true, isReducedMotion(0f))
        assertEquals(false, isReducedMotion(1f))
    }

    @Test fun tonalElevationStepsUp() {
        assertEquals(0f, tonalElevationDp(0))
        assertEquals(true, tonalElevationDp(2) > tonalElevationDp(1))
    }
}
