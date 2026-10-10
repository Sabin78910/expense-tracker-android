package com.sabin.expensetracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val CHART_COLORS = listOf(
    Color(0xFF4285F4), Color(0xFFF4B400), Color(0xFF0F9D58), Color(0xFFDB4437), Color(0xFF9C27B0),
    Color(0xFF00ACC1), Color(0xFFFF7043)
)

fun chartColor(index: Int) = CHART_COLORS[index % CHART_COLORS.size]
private fun colorFor(index: Int) = chartColor(index)

@Composable
fun CategoryDonut(shares: List<CategoryShare>) {
    if (shares.isEmpty()) return
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        contentDescription = donutDescription(shares)
    }) {
        Text(stringResource(R.string.chart_by_category), style = MaterialTheme.typography.titleMedium)
        Canvas(Modifier.size(160.dp).align(Alignment.CenterHorizontally).padding(8.dp)) {
            val stroke = 28.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            var start = -90f
            shares.forEachIndexed { i, s ->
                val sweep = (s.amount / shares.sumOf { it.amount } * 360).toFloat()
                drawArc(colorFor(i), start, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                start += sweep
            }
        }
        shares.forEachIndexed { i, s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(12.dp)) { drawRect(colorFor(i)) }
                Spacer(Modifier.width(8.dp))
                Text(s.category, Modifier.weight(1f))
                Text("${s.percent}%")
            }
        }
    }
}

@Composable
fun WeekBars(days: List<DayTotal>) {
    if (days.none { it.total > 0 }) return
    val barColor = MaterialTheme.colorScheme.primary
    val max = days.maxOfOrNull { it.total } ?: 0.0
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        contentDescription = barsDescription(days)
    }) {
        Text(stringResource(R.string.chart_last_7_days), style = MaterialTheme.typography.titleMedium)
        Canvas(Modifier.fillMaxWidth().height(100.dp).padding(vertical = 4.dp)) {
            val slot = size.width / days.size
            days.forEachIndexed { i, d ->
                val h = if (max > 0) (d.total / max * size.height).toFloat() else 0f
                val w = slot * 0.6f
                drawRect(barColor, Offset(i * slot + (slot - w) / 2, size.height - h), Size(w, h))
            }
        }
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            days.forEach {
                Text(
                    weekdayLabel(it.date),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
