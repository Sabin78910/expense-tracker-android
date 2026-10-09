package com.sabin.expensetracker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HeroCard(monthSpent: Double, status: BudgetStatus, streak: Streak, modifier: Modifier = Modifier) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(monthSpent) { anim.animateTo(1f, tween(800)) }
    val shown = countUpValue(monthSpent, anim.value)
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary)))
            .padding(20.dp)
    ) {
        Column {
            Text("This month", style = MaterialTheme.typography.labelLarge, color = scheme.onPrimary)
            Text(formatNpr(shown), style = MaterialTheme.typography.displayMedium, color = scheme.onPrimary)
            if (status.band != BudgetBand.NONE) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { status.progress },
                    color = scheme.onPrimary,
                    trackColor = scheme.onPrimary.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                )
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
        Text("No expenses yet", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAdd) { Text("Add your first expense") }
    }
}
