package com.sabin.expensetracker

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.appwidget.updateAll
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.time.LocalDate

const val EXTRA_OPEN_FORM = "open_form"

/** Home-screen widget: this month's total, budget progress and an "+ Add" shortcut. */
class ExpenseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadWidgetState(context)
        provideContent {
            GlanceTheme {
                WidgetContent(state, context)
            }
        }
    }

    companion object {
        fun loadWidgetState(context: Context): WidgetState {
            val prefs = context.getSharedPreferences("expenses", Context.MODE_PRIVATE)
            val book = ExpenseBook.deserialize(prefs.getString("data", "") ?: "")
            val budget = Double.fromBits(prefs.getLong("budget", 0L))
            return widgetState(book.expenses, budget, LocalDate.now())
        }

        /** Re-renders every placed widget; call after expenses or budget change. */
        suspend fun refresh(context: Context) {
            val ids = GlanceAppWidgetManager(context).getGlanceIds(ExpenseWidget::class.java)
            if (ids.isNotEmpty()) ExpenseWidget().updateAll(context)
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetContent(state: WidgetState, context: Context) {
    val open = actionStartActivity(Intent(context, MainActivity::class.java))
    val add = actionStartActivity(
        Intent(context, MainActivity::class.java).putExtra(EXTRA_OPEN_FORM, true)
    )
    Column(
        GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground).padding(12.dp)
            .clickable(open)
    ) {
        Text("This month", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
        Text(
            state.totalLabel,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        )
        if (state.hasBudget) {
            Spacer(GlanceModifier.height(6.dp))
            LinearProgressIndicator(
                progress = state.progress,
                modifier = GlanceModifier.fillMaxWidth(),
                color = if (state.band == BudgetBand.RED) GlanceTheme.colors.error else GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.secondaryContainer
            )
            Text(state.remainingLabel, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "+ Add",
                modifier = GlanceModifier.background(GlanceTheme.colors.primary).padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable(add),
                style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            )
        }
    }
}

class ExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseWidget()
}
