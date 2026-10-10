package com.sabin.expensetracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Limit field for one category, plus a progress bar and amount left when a limit is set. */
@Composable
fun CategoryBudgetRow(category: String, spent: Double, limit: Double, onLimit: (Double) -> Unit) {
    val name = categoryLabel(category)
    var text by remember(category) { mutableStateOf(if (limit > 0) limit.toString() else "") }
    OutlinedTextField(
        text,
        {
            text = filterDecimalInput(text, it)
            onLimit(text.toDoubleOrNull() ?: 0.0)
        },
        label = { Text(stringResource(R.string.category_limit, name)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    )
    val status = categoryBudgetStatus(spent, limit)
    if (status.band == BudgetBand.NONE) return
    val scheme = MaterialTheme.colorScheme
    val color: Color = when (status.band) {
        BudgetBand.RED -> scheme.error
        BudgetBand.AMBER -> scheme.tertiary
        else -> scheme.primary
    }
    val remaining = categoryRemaining(spent, limit) ?: 0.0
    val description = categoryBudgetDescription(name, spent, limit)
    LinearProgressIndicator(
        progress = { status.progress },
        color = color,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).semantics { contentDescription = description }
    )
    Text(
        if (remaining < 0) stringResource(R.string.category_over, formatNpr(-remaining))
        else stringResource(R.string.category_left, formatNpr(remaining)),
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = Modifier.clearAndSetSemantics {}
    )
}
