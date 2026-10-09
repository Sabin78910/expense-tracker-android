package com.sabin.expensetracker

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

val CATEGORIES = listOf("Food", "Transport", "Bills", "Shopping", "Other")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent { ExpenseTheme { ExpenseScreen() } }
    }
}

@Composable
fun ExpenseTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("expenses", Context.MODE_PRIVATE) }
    val book = remember { ExpenseBook.deserialize(prefs.getString("data", "") ?: "") }
    fun save() = prefs.edit().putString("data", book.serialize()).apply()
    var budget by remember { mutableDoubleStateOf(Double.fromBits(prefs.getLong("budget", 0L))) }
    var budgetText by remember { mutableStateOf(if (budget > 0) budget.toString() else "") }
    var version by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CATEGORIES.first()) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf<String?>(null) }
    val expenses = remember(version, filter) { book.filterByCategory(filter) }

    Scaffold(topBar = { TopAppBar(title = { Text("Expense Tracker") }) }) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text("Total: " + formatNpr(book.total()), style = MaterialTheme.typography.headlineSmall)
                val now = remember(version) { java.time.LocalDate.now() }
                val monthSpent = book.totalForMonth(now.year, now.monthValue)
                Text("This month: " + formatNpr(monthSpent))
                val status = budgetStatus(monthSpent, budget, now.dayOfMonth, now.lengthOfMonth())
                if (status.band != BudgetBand.NONE) {
                    val barColor = when (status.band) {
                        BudgetBand.GREEN -> ComposeColor(0xFF2E7D32)
                        BudgetBand.AMBER -> ComposeColor(0xFFFFA000)
                        else -> ComposeColor(0xFFC62828)
                    }
                    LinearProgressIndicator(
                        progress = { status.progress },
                        color = barColor,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .semantics { contentDescription = "Monthly budget: " + status.remainingLabel }
                    )
                    Text(status.remainingLabel)
                    status.message?.let { Text(it) }
                }
                OutlinedTextField(
                    budgetText,
                    {
                        budgetText = it
                        budget = it.toDoubleOrNull()?.takeIf { v -> v > 0 } ?: 0.0
                        prefs.edit().putLong("budget", budget.toRawBits()).apply()
                    },
                    label = { Text("Monthly budget (NPR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                book.sortedCategoryTotals().forEach { (c, t) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(c)
                        Text(formatNpr(t))
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    amount, { amount = it }, label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(
                    Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CATEGORIES.forEach { c ->
                        FilterChip(selected = c == category, onClick = { category = c }, label = { Text(c) })
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    runCatching { book.add(title, amount.toDoubleOrNull() ?: 0.0, category) }
                        .onSuccess { title = ""; amount = ""; error = null; save(); version++ }
                        .onFailure { error = it.message }
                }, modifier = Modifier.fillMaxWidth()) { Text("Add expense") }
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
                    CATEGORIES.forEach { c ->
                        FilterChip(selected = c == filter, onClick = { filter = c }, label = { Text(c) })
                    }
                }
            }
            items(expenses, key = { it.id }) { e ->
                ListItem(
                    headlineContent = { Text(e.title) },
                    supportingContent = { Text(e.category) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatNpr(e.amount))
                            TextButton(
                                onClick = { book.remove(e.id); save(); version++ },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .semantics { contentDescription = deleteLabel(e.title) }
                            ) { Text("Delete") }
                        }
                    }
                )
            }
        }
    }
}
