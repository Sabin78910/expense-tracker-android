package com.sabin.expensetracker

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Brand seed (emerald) used when dynamic colour is unavailable (Android < 12). */
val BRAND_SEED = Color(0xFF10B981)

/** Teal end of the emerald-to-teal brand gradient. */
val BRAND_TEAL = Color(0xFF14B8A6)

/** True when the user disabled animations (ANIMATOR_DURATION_SCALE = 0). */
fun isReducedMotion(animatorScale: Float): Boolean = animatorScale == 0f

/** Tonal elevation in dp for surface level 0..5. */
fun tonalElevationDp(level: Int): Float = level.coerceIn(0, 5) * 2f

const val EXPENSE_CARD_RADIUS_DP = 24f
const val expenseShapesLargeDp = 28f

fun fallbackColorScheme(dark: Boolean): ColorScheme =
    if (dark) darkColorScheme(
        primary = BRAND_SEED,
        onPrimary = Color(0xFF003824),
        primaryContainer = Color(0xFF005138),
        onPrimaryContainer = Color(0xFF6FFBBE),
        secondary = Color(0xFFB3CCBE),
        tertiary = BRAND_TEAL,
        background = Color(0xFF07130E),
        surface = Color(0xFF07130E),
        surfaceContainer = Color(0xFF0F211A),
        onSurface = Color(0xFFDFE4DE)
    ) else lightColorScheme(
        primary = BRAND_SEED,
        onPrimary = Color(0xFF00382A),
        primaryContainer = Color(0xFFB8F5D8),
        onPrimaryContainer = Color(0xFF002114),
        secondary = Color(0xFF4D6357),
        tertiary = BRAND_TEAL,
        background = Color(0xFFF5FBF6),
        surface = Color(0xFFF5FBF6),
        onSurface = Color(0xFF171D19)
    )

fun expenseShapes(): Shapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(EXPENSE_CARD_RADIUS_DP.dp),
    large = RoundedCornerShape(expenseShapesLargeDp.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** Bold display style for money amounts with tabular (fixed-width) numerals. */
fun moneyTextStyle(): TextStyle = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontFeatureSettings = "tnum"
)

fun expenseTypography(): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontFeatureSettings = "tnum")
    )
}
