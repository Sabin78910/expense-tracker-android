package com.sabin.expensetracker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {
    @Test fun darkFallbackUsesEmeraldSeed() {
        assertEquals(Color(0xFF10B981), fallbackColorScheme(dark = true).primary)
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

    @Test fun textRolesMeetContrastInLightAndDark() {
        for (dark in listOf(false, true)) {
            val c = fallbackColorScheme(dark)
            assertTrue("onSurface $dark", contrastRatio(c.onSurface, c.surface) >= 4.5)
            assertTrue("onSurfaceVariant $dark", contrastRatio(c.onSurfaceVariant, c.surface) >= 4.5)
            assertTrue("onPrimary $dark", contrastRatio(c.onPrimary, c.primary) >= 4.5)
            assertTrue("primary text $dark", contrastRatio(c.primary, c.surface) >= 4.5)
            assertTrue("onPrimaryContainer $dark", contrastRatio(c.onPrimaryContainer, c.primaryContainer) >= 4.5)
        }
    }

    @Test fun darkPrimaryIsEmeraldFamilyNotBlue() {
        val p = fallbackColorScheme(dark = true).primary
        assertTrue(p.green > p.blue && p.green > p.red)
    }

    @Test fun contrastRatioMatchesWcagExtremes() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)
        assertEquals(1.0, contrastRatio(Color.White, Color.White), 0.01)
    }

    @Test fun fieldShapeIsAtLeast24dp() {
        assertTrue(FIELD_RADIUS_DP >= 24f)
    }

    @Test fun amountFieldStyleIsTabular() {
        assertEquals("tnum", amountFieldTextStyle().fontFeatureSettings)
    }
}
