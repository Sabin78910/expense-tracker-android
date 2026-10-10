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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
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
        prefs.edit().putInt("launch_count", prefs.getInt("launch_count", 0) + 1).apply()
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
    var editingId by remember { mutableStateOf<Long?>(null) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var backedUp by remember { mutableStateOf(prefs.getBoolean("backed_up", false)) }
    var unlocked by remember { mutableStateOf(BadgeStore.parse(prefs.getString("badges", "") ?: "")) }
    var celebrate by remember { mutableStateOf(emptySet<Badge>()) }
    var showShelf by remember { mutableStateOf(false) }
    LaunchedEffect(version, backedUp, budget) {
        val now = earnedBadges(book.expenses, budget, backedUp, java.time.LocalDate.now())
        val fresh = newlyUnlocked(unlocked, now)
        if (fresh.isNotEmpty()) {
            unlocked = unlocked + fresh
            prefs.edit().putString("badges", BadgeStore.serialize(unlocked)).apply()
            celebrate = fresh
            val activity = context as? android.app.Activity
            if (activity != null) {
                val manager = com.google.android.play.core.review.ReviewManagerFactory.create(activity)
                ReviewPrompt(
                    object : ReviewLauncher {
                        override fun launch() {
                            manager.requestReviewFlow().addOnSuccessListener { info ->
                                manager.launchReviewFlow(activity, info)
                            }
                        }
                    },
                    object : ReviewStore {
                        override fun lastAskedMillis() = prefs.getLong("review_last_ms", -1L).takeIf { it >= 0 }
                        override fun setLastAskedMillis(value: Long) = prefs.edit().putLong("review_last_ms", value).apply()
                    }
                ).onPositiveMoment(fresh, System.currentTimeMillis(), prefs.getInt("launch_count", 0), error != null)
            }
        }
    }
    if (celebrate.isNotEmpty()) UnlockCelebration(celebrate) { celebrate = emptySet() }
    if (showShelf) BadgeShelf(unlocked) { showShelf = false }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            backupMessage = runCatching {
                context.contentResolver.openOutputStream(uri, "wt")!!.use {
                    it.write(Backup.export(book.expenses).toByteArray(Charsets.UTF_8))
                }
                prefs.edit().putBoolean("backed_up", true).apply()
                backedUp = true
                context.getString(R.string.backup_saved)
            }.getOrElse { context.getString(R.string.backup_save_failed) }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            backupMessage = runCatching {
                context.contentResolver.openOutputStream(uri, "wt")!!.use {
                    it.write(CsvExport.export(book.expenses, java.time.ZoneId.systemDefault()).toByteArray(Charsets.UTF_8))
                }
                context.getString(R.string.csv_saved)
            }.getOrElse { context.getString(R.string.csv_save_failed) }
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
                context.getString(R.string.restored_count, added)
            } catch (e: BackupException) {
                context.getString(R.string.backup_invalid, e.message ?: "")
            } catch (e: java.io.IOException) {
                context.getString(R.string.backup_read_failed)
            }
        }
    }
    val expenses = remember(version, filter) { book.filterByCategory(filter) }

    fun closeForm() { title = ""; amount = ""; monthly = false; error = null; showForm = false; editingId = null }
    fun startEdit(e: Expense) {
        title = e.title; amount = e.amount.toString(); category = e.category
        monthly = false; error = null; editingId = e.id; showForm = true
    }
    fun addExpense() {
        val editing = editingId
        if (editing != null) {
            runCatching { book.update(editing, title, amount.toDoubleOrNull() ?: 0.0, category) }
                .onSuccess { closeForm(); save(); version++ }
                .onFailure { error = it.message }
            return
        }
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
        ModalBottomSheet(onDismissRequest = { closeForm() }) {
            Column(Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
                val titleFocus = remember { FocusRequester() }
                val amountFocus = remember { FocusRequester() }
                // First-run: open straight onto the amount keypad.
                LaunchedEffect(Unit) { (if (startWithForm && book.expenses.isEmpty()) amountFocus else titleFocus).requestFocus() }
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.title)) }, modifier = Modifier.fillMaxWidth().focusRequester(titleFocus))
                OutlinedTextField(
                    amount, { amount = filterDecimalInput(amount, it) }, label = { Text(stringResource(R.string.amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().focusRequester(amountFocus)
                )
                FlowRow(
                    Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CATEGORIES.forEach { c ->
                        FilterChip(selected = c == category, onClick = { category = c }, label = { Text(categoryLabel(c)) })
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (editingId == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(monthly, { monthly = it })
                        Text(stringResource(R.string.repeat_monthly))
                    }
                }
                Button(onClick = { addExpense() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (editingId == null) R.string.add_expense else R.string.save_changes))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    val snackbarHost = remember { SnackbarHostState() }
    val deletedMsg = stringResource(R.string.expense_deleted)
    val undoLabel = stringResource(R.string.undo)
    fun deleteWithUndo(e: Expense) {
        book.remove(e.id); save(); version++
        scope.launch {
            snackbarHost.currentSnackbarData?.dismiss()
            if (snackbarHost.showSnackbar(deletedMsg, undoLabel, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
                book.restore(e); save(); version++
            }
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = { LargeTopAppBar(title = { Text(stringResource(R.string.app_name)) }, scrollBehavior = scrollBehavior) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showForm = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_expense)) }
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
                    Text(stringResource(R.string.all_time), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    label = { Text(stringResource(R.string.monthly_budget)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                CategoryDonut(categoryShares(monthCategoryTotals(book.expenses, now.year, now.monthValue)))
                Spacer(Modifier.height(12.dp))
                WeekBars(dailyTotals(book.expenses, now))
                Spacer(Modifier.height(12.dp))
                book.sortedCategoryTotals().forEach { (c, t) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(categoryLabel(c))
                        Text(formatNpr(t))
                    }
                }
                val chips = remember(version) { recentChips(book.expenses) }
                if (chips.isNotEmpty()) {
                    Text(stringResource(R.string.recent), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        chips.forEach { c ->
                            AssistChip(
                                onClick = { book.add(c.category, c.amount, c.category); save(); version++ },
                                label = { Text(categoryLabel(c.category) + " " + formatNpr(c.amount)) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                ReminderSettings()
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exportLauncher.launch("expenses-backup.json") }) { Text(stringResource(R.string.back_up)) }
                    OutlinedButton(onClick = { csvLauncher.launch("expenses.csv") }) { Text(stringResource(R.string.export_csv)) }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }) { Text("Restore") }
                }
                backupMessage?.let { Text(it) }
                OutlinedButton(onClick = { showShelf = true }) { Text(stringResource(R.string.badges_count, unlocked.size, Badge.values().size)) }
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text(stringResource(R.string.all)) })
                    CATEGORIES.forEach { c ->
                        FilterChip(selected = c == filter, onClick = { filter = c }, label = { Text(categoryLabel(c)) })
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
                    val editDescription = stringResource(R.string.edit_row_label, e.title)
                    ListItem(
                        modifier = Modifier.clickable(onClickLabel = editDescription) { startEdit(e) },
                        leadingContent = { CategoryBadge(e.category) },
                        headlineContent = { Text(e.title) },
                        supportingContent = { Text(categoryLabel(e.category)) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(formatNpr(e.amount))
                                TextButton(
                                    onClick = { deleteWithUndo(e) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .semantics { contentDescription = deleteLabel(e.title) }
                                ) { Text(stringResource(R.string.delete)) }
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
        Text(stringResource(R.string.remind_me))
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
        }) { Text(stringResource(R.string.reminder_time, "%02d:%02d".format(time.hour, time.minute))) }
    }
    if (explain) {
        AlertDialog(
            onDismissRequest = { explain = false },
            title = { Text(stringResource(R.string.allow_notifications)) },
            text = { Text(stringResource(R.string.notification_rationale)) },
            confirmButton = {
                TextButton(onClick = {
                    explain = false
                    permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(R.string.continue_label)) }
            },
            dismissButton = { TextButton(onClick = { explain = false }) { Text(stringResource(R.string.not_now)) } }
        )
    }
}
