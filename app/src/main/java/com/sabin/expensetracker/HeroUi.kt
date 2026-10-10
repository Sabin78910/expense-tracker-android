package com.sabin.expensetracker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun HeroCard(
    monthSpent: Double,
    status: BudgetStatus,
    streak: Streak,
    monthLabel: String,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    comparison: MonthComparison? = null,
    modifier: Modifier = Modifier,
    budget: Double = 0.0
) {
    val reduced = animationsOff(LocalContext.current)
    val anim = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(monthSpent) {
        if (reduced) anim.snapTo(1f) else { anim.snapTo(0f); anim.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 120f)) }
    }
    val shown = countUpValue(monthSpent, anim.value)
    val leftTarget = budgetLeftFraction(monthSpent, budget)
    val ring = remember { Animatable(leftTarget) }
    LaunchedEffect(leftTarget) {
        if (reduced) ring.snapTo(leftTarget) else ring.animateTo(leftTarget, spring(dampingRatio = 0.6f, stiffness = 200f))
    }
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary)))
            .padding(20.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onPrevious?.invoke() }, enabled = onPrevious != null, modifier = Modifier.size(48.dp)) {
                    Icon(AppIcons.ChevronLeft, stringResource(R.string.previous_month))
                }
                Text(
                    monthLabel, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                    color = scheme.onPrimary, textAlign = TextAlign.Center
                )
                IconButton(onClick = { onNext?.invoke() }, enabled = onNext != null, modifier = Modifier.size(48.dp)) {
                    Icon(AppIcons.ChevronRight, stringResource(R.string.next_month))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatNpr(shown), Modifier.weight(1f),
                    style = MaterialTheme.typography.displayMedium, color = scheme.onPrimary
                )
                if (status.band != BudgetBand.NONE) {
                    val band = ringBand(monthSpent, budget)
                    val ringColor = when (band) {
                        BudgetBand.AMBER -> Color(0xFFFFC107)
                        BudgetBand.RED -> Color(0xFFFF5252)
                        else -> scheme.onPrimary
                    }
                    val track = scheme.onPrimary.copy(alpha = 0.3f)
                    Canvas(
                        Modifier.size(72.dp).semantics { contentDescription = ringDescription(band, leftTarget) }
                    ) {
                        val stroke = 10.dp.toPx()
                        val arc = Size(size.width - stroke, size.height - stroke)
                        val topLeft = Offset(stroke / 2, stroke / 2)
                        drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
                        drawArc(ringColor, -90f, 360f * ring.value, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                }
            }
            if (comparison != null) {
                val pct = comparison.percentChange
                val line = when {
                    pct == null -> stringResource(R.string.compare_none_last)
                    pct < 0 -> stringResource(R.string.compare_less, -pct)
                    pct > 0 -> stringResource(R.string.compare_more, pct)
                    else -> stringResource(R.string.compare_same)
                }
                val arrow = deltaArrow(pct)
                Text(
                    if (arrow.isEmpty()) line else "$arrow $line",
                    style = MaterialTheme.typography.bodyMedium, color = scheme.onPrimary
                )
                comparison.topCategory?.let {
                    val res = if (it.delta < 0) R.string.compare_category_less else R.string.compare_category_more
                    Text(
                        stringResource(res, it.category, formatNpr(abs(it.delta))),
                        style = MaterialTheme.typography.bodyMedium, color = scheme.onPrimary
                    )
                }
            }
            if (status.band != BudgetBand.NONE) {
                Spacer(Modifier.height(8.dp))
                Text(
                    budgetPercentLabel(status.progress, true) + " · " + status.remainingLabel,
                    style = MaterialTheme.typography.bodyMedium, color = scheme.onPrimary
                )
            }
            if (streak.current > 0) {
                Spacer(Modifier.height(8.dp))
                AssistChip(onClick = {}, label = { Text(streakLabel(streak)) })
            }
        }
    }
}

/** Category chips with tinted icons, shown above the donut. */
@Composable
fun CategoryChips(shares: List<CategoryShare>, modifier: Modifier = Modifier) {
    if (shares.isEmpty()) return
    androidx.compose.foundation.layout.FlowRow(
        modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        shares.forEachIndexed { i, s ->
            AssistChip(
                onClick = {},
                modifier = Modifier.heightIn(min = 48.dp),
                label = { Text("${categoryLabel(s.category)} ${s.percent}%") },
                leadingIcon = {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape).background(chartColor(i).copy(alpha = 0.25f))
                            .clearAndSetSemantics {},
                        contentAlignment = Alignment.Center
                    ) { Text(categoryGlyph(s.category)) }
                }
            )
        }
    }
}

@Composable
fun CategoryBadge(category: String) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center
    ) { Text(categoryGlyph(category)) }
}

@Composable
fun EmptyState(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(120.dp).clearAndSetSemantics {}) {
            drawCircle(c.secondaryContainer)
            drawRoundRect(c.primary, Offset(size.width * 0.25f, size.height * 0.3f),
                Size(size.width * 0.5f, size.height * 0.4f), CornerRadius(16f, 16f))
            drawCircle(c.onPrimary, radius = size.width * 0.08f, center = Offset(size.width * 0.5f, size.height * 0.5f))
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.no_expenses), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAdd) { Text(stringResource(R.string.add_first_expense)) }
    }
}
