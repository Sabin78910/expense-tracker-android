package com.sabin.expensetracker

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import kotlinx.coroutines.launch
import java.time.ZoneId

val CATEGORIES = listOf("Food", "Transport", "Bills", "Shopping", "Other")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        val prefs = getSharedPreferences("expenses", Context.MODE_PRIVATE)
        val firstRun = FirstRun(object : FlagStore {
            override fun get() = prefs.getBoolean("onboarding_done", false)
            override fun set(value: Boolean) = prefs.edit().putBoolean("onboarding_done", value).apply()
        })
        val fromWidgetAdd = intent.getBooleanExtra(EXTRA_OPEN_FORM, false)
        setContent {
            ExpenseTheme {
                var onboarding by remember { mutableStateOf(firstRun.shouldShow()) }
                var openForm by remember { mutableStateOf(false) }
                if (onboarding) {
                    OnboardingScreen(firstRun) { open -> openForm = open; onboarding = false }
                } else {
                    ExpenseScreen(startWithForm = openForm || fromWidgetAdd)
                }
            }
        }
    }
}

@Composable
fun ExpenseTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> fallbackColorScheme(dark)
    }
    MaterialTheme(colorScheme = scheme, shapes = expenseShapes(), typography = expenseTypography(), content = content)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseScreen(startWithForm: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("expenses", Context.MODE_PRIVATE) }
    val book = remember { ExpenseBook.deserialize(prefs.getString("data", "") ?: "") }
    val recurring = remember {
        RecurringList.deserialize(prefs.getString("recurring", "") ?: "").also {
            if (it.applyDue(book, java.time.LocalDate.now()) > 0) {
                prefs.edit().putString("data", book.serialize()).putString("recurring", it.serialize()).apply()
            }
        }
    }
    val scope = rememberCoroutineScope()
    fun save() {
        prefs.edit().putString("data", book.serialize()).putString("recurring", recurring.serialize()).apply()
        scope.launch { ExpenseWidget.refresh(context) }
    }
    var budget by remember { mutableDoubleStateOf(Double.fromBits(prefs.getLong("budget", 0L))) }
    var budgetText by remember { mutableStateOf(if (budget > 0) budget.toString() else "") }
    var version by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CATEGORIES.first()) }
    var monthly by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(startWithForm) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            backupMessage = runCatching {
                context.contentResolver.openOutputStream(uri, "wt")!!.use {
                    it.write(Backup.export(book.expenses).toByteArray(Charsets.UTF_8))
                }
                "Backup saved"
            }.getOrElse { "Could not save backup" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            backupMessage = try {
                val text = context.contentResolver.openInputStream(uri)!!.use {
                    it.readBytes().toString(Charsets.UTF_8)
                }
                val added = book.merge(Backup.parse(text))
                save(); version++
                "Restored $added new expenses"
            } catch (e: BackupException) {
                "Invalid backup file: " + e.message
            } catch (e: java.io.IOException) {
                "Could not read backup"
            }
        }
    }
    val expenses = remember(version, filter) { book.filterByCategory(filter) }

    fun addExpense() {
        runCatching {
            val value = amount.toDoubleOrNull() ?: 0.0
            val e = book.add(title, value, category)
            if (monthly) {
                val day = Instant.ofEpochMilli(e.timestamp).atZone(ZoneId.systemDefault()).dayOfMonth
                recurring.add(title, value, category, day)
                // Today's occurrence is the expense just added.
                recurring.applyDue(ExpenseBook(), java.time.LocalDate.now())
            }
        }
            .onSuccess { title = ""; amount = ""; monthly = false; error = null; showForm = false; save(); version++ }
            .onFailure { error = it.message }
    }
    if (showForm) {
        ModalBottomSheet(onDismissRequest = { showForm = false }) {
            Column(Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
                val titleFocus = remember { FocusRequester() }
                val amountFocus = remember { FocusRequester() }
                // First-run: open straight onto the amount keypad.
                LaunchedEffect(Unit) { (if (startWithForm && book.expenses.isEmpty()) amountFocus else titleFocus).requestFocus() }
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth().focusRequester(titleFocus))
                OutlinedTextField(
                    amount, { amount = filterDecimalInput(amount, it) }, label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().focusRequester(amountFocus)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(monthly, { monthly = it })
                    Text("Repeat monthly")
                }
                Button(onClick = { addExpense() }, modifier = Modifier.fillMaxWidth()) { Text("Add expense") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text("Expense Tracker") }, scrollBehavior = scrollBehavior) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showForm = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add expense") }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
        ) {
            item {
                val now = remember(version) { java.time.LocalDate.now() }
                val monthSpent = book.totalForMonth(now.year, now.monthValue)
                val streak = loggingStreak(book.expenses, now)
                val status = budgetStatus(monthSpent, budget, now.dayOfMonth, now.lengthOfMonth())
                HeroCard(monthSpent, status, streak, Modifier.padding(bottom = 12.dp))
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("All time", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatNpr(book.total()), style = MaterialTheme.typography.titleMedium)
                }
                status.message?.let { Text(it) }
                OutlinedTextField(
                    budgetText,
                    {
                        budgetText = filterDecimalInput(budgetText, it)
                        budget = budgetText.toDoubleOrNull()?.takeIf { v -> v > 0 } ?: 0.0
                        prefs.edit().putLong("budget", budget.toRawBits()).apply()
                        scope.launch { ExpenseWidget.refresh(context) }
                    },
                    label = { Text("Monthly budget (NPR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                CategoryDonut(categoryShares(monthCategoryTotals(book.expenses, now.year, now.monthValue)))
                Spacer(Modifier.height(12.dp))
                WeekBars(dailyTotals(book.expenses, now))
                Spacer(Modifier.height(12.dp))
                book.sortedCategoryTotals().forEach { (c, t) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(c)
                        Text(formatNpr(t))
                    }
                }
                val chips = remember(version) { recentChips(book.expenses) }
                if (chips.isNotEmpty()) {
                    Text("Recent", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        chips.forEach { c ->
                            AssistChip(
                                onClick = { book.add(c.category, c.amount, c.category); save(); version++ },
                                label = { Text(c.category + " " + formatNpr(c.amount)) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                ReminderSettings()
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exportLauncher.launch("expenses-backup.json") }) { Text("Back up") }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }) { Text("Restore") }
                }
                backupMessage?.let { Text(it) }
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
            if (book.expenses.isEmpty()) {
                item { EmptyState(onAdd = { showForm = true }) }
            }
            items(expenses, key = { it.id }) { e ->
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp).animateItem(
                    fadeInSpec = spring(), placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    fadeOutSpec = spring()
                )) {
                    ListItem(
                        leadingContent = { CategoryBadge(e.category) },
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
}

@Composable
fun ReminderSettings() {
    val context = LocalContext.current
    var on by remember { mutableStateOf(ReminderScheduler.isEnabled(context)) }
    var time by remember { mutableStateOf(ReminderScheduler.time(context)) }
    var explain by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        on = granted
        ReminderScheduler.setEnabled(context, granted)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Remind me to log expenses")
        Switch(checked = on, onCheckedChange = { want ->
            when {
                !want -> { on = false; ReminderScheduler.setEnabled(context, false) }
                Build.VERSION.SDK_INT < 33 -> { on = true; ReminderScheduler.setEnabled(context, true) }
                else -> explain = true
            }
        })
    }
    if (on) {
        TextButton(onClick = {
            android.app.TimePickerDialog(context, { _, h, m ->
                time = java.time.LocalTime.of(h, m)
                ReminderScheduler.setTime(context, time)
            }, time.hour, time.minute, true).show()
        }) { Text("Reminder time: %02d:%02d".format(time.hour, time.minute)) }
    }
    if (explain) {
        AlertDialog(
            onDismissRequest = { explain = false },
            title = { Text("Allow notifications?") },
            text = { Text("We need notification permission to send one gentle reminder a day, only on days you haven't logged an expense. You can turn it off any time.") },
            confirmButton = {
                TextButton(onClick = {
                    explain = false
                    permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { explain = false }) { Text("Not now") } }
        )
    }
}
