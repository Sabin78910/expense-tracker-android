package com.sabin.expensetracker

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import kotlinx.coroutines.launch
import java.time.ZoneId

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
        if (privacyMode(this).enabled) applyPrivacy(this, true)
        val fromWidgetAdd = intent.getBooleanExtra(EXTRA_OPEN_FORM, false)
        setContent {
            ExpenseTheme {
                var onboarding by remember { mutableStateOf(firstRun.shouldShow()) }
                var openForm by remember { mutableStateOf(false) }
                if (onboarding) {
                    OnboardingScreen(firstRun) { open -> openForm = open; onboarding = false }
                } else {
                    ExpenseScreen(startWithForm = shouldOpenForm(fromWidgetAdd, openForm))
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
    val resources = LocalResources.current
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
    var limits by remember { mutableStateOf(CategoryLimits.deserialize(prefs.getString("category_limits", "") ?: "")) }
    var categories by remember { mutableStateOf(CategoryList.deserialize(prefs.getString("custom_categories", "") ?: "")) }
    var newCategory by remember { mutableStateOf("") }
    var categoryToRemove by remember { mutableStateOf<String?>(null) }
    fun saveCategories() { prefs.edit().putString("custom_categories", categories.serialize()).apply() }
    var version by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CategoryList.DEFAULTS.first()) }
    var monthly by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(java.time.LocalDate.now()) }
    var timeOfDay by remember { mutableStateOf<java.time.LocalTime?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(startWithForm) }
    val view = androidx.compose.ui.platform.LocalView.current
    var confirmToken by remember { mutableIntStateOf(0) }
    var confirmMessage by remember { mutableStateOf<String?>(null) }
    var milestone by remember { mutableStateOf<Int?>(null) }
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
    milestone?.let { StreakMilestoneSheet(it) { milestone = null } }
    if (showShelf) BadgeShelf(unlocked) { showShelf = false }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            backupMessage = runCatching {
                context.contentResolver.openOutputStream(uri, "wt")!!.use {
                    it.write(Backup.exportFull(book.expenses, limits, categories).toByteArray(Charsets.UTF_8))
                }
                prefs.edit().putBoolean("backed_up", true).apply()
                backedUp = true
                resources.getString(R.string.backup_saved)
            }.getOrElse { resources.getString(R.string.backup_save_failed) }
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
                resources.getString(R.string.csv_saved)
            }.getOrElse { resources.getString(R.string.csv_save_failed) }
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
                val data = Backup.parseFull(text)
                categories = categories.withAll(data.categories + data.expenses.map { it.category })
                saveCategories()
                val added = book.merge(data.expenses)
                if (!data.limits.isEmpty()) {
                    limits = data.limits
                    prefs.edit().putString("category_limits", limits.serialize()).apply()
                }
                save(); version++
                resources.getString(R.string.restored_count, added)
            } catch (e: BackupException) {
                resources.getString(R.string.backup_invalid, e.message ?: "")
            } catch (e: java.io.IOException) {
                resources.getString(R.string.backup_read_failed)
            }
        }
    }
    val today = remember(version) { java.time.LocalDate.now() }
    val currentMonth = java.time.YearMonth.from(today)
    val earliestMonth = remember(version) { MonthSelection.earliest(book.expenses, currentMonth) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    val month = MonthSelection.clamp(selectedMonth, earliestMonth, currentMonth)
    val monthLabel = MonthSelection.label(month)
    val expenses = remember(version, filter, query, month) { MonthSelection.filter(book.search(query, filter), month) }

    fun closeForm() { title = ""; amount = ""; note = ""; monthly = false; error = null; showForm = false; editingId = null
        date = java.time.LocalDate.now(); timeOfDay = null }
    fun startEdit(e: Expense) {
        title = e.title; amount = e.amount.toString(); note = e.note; category = e.category
        val zone = ZoneId.systemDefault()
        date = if (e.timestamp > 0) Instant.ofEpochMilli(e.timestamp).atZone(zone).toLocalDate() else java.time.LocalDate.now()
        timeOfDay = if (e.timestamp > 0) ExpenseDate.timeOfDay(e.timestamp, zone) else null
        monthly = false; error = null; editingId = e.id; showForm = true
    }
    fun addExpense() {
        val editing = editingId
        val stamp = runCatching {
            ExpenseDate.timestampFor(date, timeOfDay ?: java.time.LocalTime.now())
        }.getOrElse { error = it.message; return }
        if (editing != null) {
            runCatching { book.update(editing, title, amount.toDoubleOrNull() ?: 0.0, category, stamp, note) }
                .onSuccess { closeForm(); save(); version++ }
                .onFailure { error = it.message }
            return
        }
        val streakBefore = loggingStreak(book.expenses, java.time.LocalDate.now()).current
        runCatching {
            val value = amount.toDoubleOrNull() ?: 0.0
            val e = book.add(title, value, category, stamp, note)
            if (monthly) {
                val day = Instant.ofEpochMilli(e.timestamp).atZone(ZoneId.systemDefault()).dayOfMonth
                recurring.add(title, value, category, day)
                // Today's occurrence is the expense just added.
                recurring.applyDue(ExpenseBook(), java.time.LocalDate.now())
            }
        }
            .onSuccess {
                title = ""; amount = ""; note = ""; monthly = false; error = null; showForm = false; save(); version++
                val streakAfter = loggingStreak(book.expenses, java.time.LocalDate.now()).current
                confirmHaptic(view)
                confirmMessage = if (streakGrew(streakBefore, streakAfter)) resources.getString(R.string.streak_grew, streakAfter)
                else resources.getString(R.string.saved_confirmation)
                confirmToken++
                milestone = streakMilestoneReached(streakBefore, streakAfter)
            }
            .onFailure { error = it.message }
    }
    categoryToRemove?.let { c ->
        AlertDialog(
            onDismissRequest = { categoryToRemove = null },
            title = { Text(stringResource(R.string.remove_category, c)) },
            text = { Text(stringResource(R.string.remove_category_message, c)) },
            confirmButton = {
                TextButton(onClick = {
                    book.reassignCategory(c, CategoryList.OTHER)
                    categories = categories.remove(c); saveCategories()
                    limits = limits.with(c, 0.0)
                    prefs.edit().putString("category_limits", limits.serialize()).apply()
                    if (category == c) category = CategoryList.OTHER
                    if (filter == c) filter = null
                    categoryToRemove = null
                    save(); version++
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { categoryToRemove = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (showForm) {
        ModalBottomSheet(onDismissRequest = { closeForm() }) {
            Column(Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
                val titleFocus = remember { FocusRequester() }
                val amountFocus = remember { FocusRequester() }
                // Fast add: new expenses open onto the amount with the number pad.
                LaunchedEffect(Unit) { (if (editingId == null) amountFocus else titleFocus).requestFocus() }
                val padMode = editingId == null
                if (!padMode) {
                    OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.title)) }, modifier = Modifier.fillMaxWidth().focusRequester(titleFocus))
                }
                OutlinedTextField(
                    amount, { amount = filterDecimalInput(amount, it) }, label = { Text(stringResource(R.string.amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    readOnly = padMode,
                    modifier = Modifier.fillMaxWidth().focusRequester(amountFocus)
                )
                if (padMode) {
                    NumberPad({ k -> amount = keypadPress(amount, k) }, Modifier.padding(vertical = 8.dp))
                    OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.title)) }, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(
                    note, { note = filterNoteInput(note, it) }, label = { Text(stringResource(R.string.note)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                FlowRow(
                    Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.all.forEach { c ->
                        FilterChip(selected = c == category, onClick = { category = c }, label = { Text(categoryLabel(c)) })
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        newCategory, { newCategory = it.take(MAX_CATEGORY_LENGTH).replace("\n", "") },
                        label = { Text(stringResource(R.string.new_category)) },
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                    val addLabel = stringResource(R.string.add_category)
                    TextButton(
                        onClick = {
                            categories.add(newCategory)?.let { updated ->
                                categories = updated; saveCategories()
                                category = newCategory.trim(); newCategory = ""
                            }
                        },
                        enabled = categories.add(newCategory) != null,
                        modifier = Modifier.semantics { contentDescription = addLabel }
                    ) { Text(addLabel) }
                }
                if (categories.custom.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.custom.forEach { c ->
                            val removeLabel = stringResource(R.string.remove_category, c)
                            TextButton(
                                onClick = { categoryToRemove = c },
                                modifier = Modifier.semantics { contentDescription = removeLabel }
                            ) { Text("✕ $c") }
                        }
                    }
                }
                val dateText = date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
                val dateLabel = stringResource(R.string.expense_date_label, dateText)
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = dateLabel }
                ) { Text(dateLabel) }
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
    if (showForm && showDatePicker) {
        val today = java.time.LocalDate.now()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = ExpenseDate.toPickerMillis(date),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = ExpenseDate.isSelectable(utcTimeMillis, today)
                override fun isSelectableYear(year: Int) = year <= today.year
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = ExpenseDate.fromPickerMillis(it) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(pickerState) }
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
    val emptyMonthText = stringResource(R.string.no_expenses_in_month, monthLabel)
    val noResultsText = stringResource(R.string.no_results)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                confirmMessage?.let { m ->
                    SaveConfirmation(confirmToken, m)
                    LaunchedEffect(confirmToken) { kotlinx.coroutines.delay(1800); confirmMessage = null }
                }
                SnackbarHost(snackbarHost)
            }
        },
        topBar = { LargeTopAppBar(title = { Text(stringResource(R.string.app_name)) }, scrollBehavior = scrollBehavior) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showForm = true },
                icon = { Icon(AppIcons.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_expense)) }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
        ) {
            item {
                val now = today
                val monthSpent = book.totalForMonth(month.year, month.monthValue)
                val streak = loggingStreak(book.expenses, now)
                val isCurrent = month == currentMonth
                val status = budgetStatus(monthSpent, budget, if (isCurrent) now.dayOfMonth else month.lengthOfMonth(), month.lengthOfMonth())
                HeroCard(
                    monthSpent, status, streak, monthLabel,
                    MonthSelection.previous(month, earliestMonth)?.let { m -> { selectedMonth = m } },
                    MonthSelection.next(month, currentMonth)?.let { m -> { selectedMonth = m } },
                    compareWithPreviousMonth(book.expenses, month, now),
                    Modifier.padding(bottom = 12.dp),
                    budget
                )
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
                val shares = categoryShares(monthCategoryTotals(book.expenses, month.year, month.monthValue))
                CategoryChips(shares)
                CategoryDonut(shares)
                Spacer(Modifier.height(12.dp))
                WeekBars(dailyTotals(book.expenses, now))
                Spacer(Modifier.height(12.dp))
                book.sortedCategoryTotals().forEach { (c, t) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(categoryLabel(c))
                        Text(formatNpr(t))
                    }
                }
                val monthTotals = monthCategoryTotals(book.expenses, month.year, month.monthValue).toMap()
                categories.all.forEach { c ->
                    CategoryBudgetRow(
                        c, monthTotals[c] ?: 0.0, limits.limitFor(c),
                        onLimit = { v ->
                            limits = limits.with(c, v)
                            prefs.edit().putString("category_limits", limits.serialize()).apply()
                        }
                    )
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
                PrivacySettings()
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
                    categories.all.forEach { c ->
                        FilterChip(selected = c == filter, onClick = { filter = c }, label = { Text(categoryLabel(c)) })
                    }
                }
                val searchLabel = stringResource(R.string.search_expenses)
                val clearLabel = stringResource(R.string.clear_search)
                OutlinedTextField(
                    query, { query = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        .semantics { contentDescription = searchLabel },
                    label = { Text(searchLabel) },
                    singleLine = true,
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(AppIcons.Clear, contentDescription = clearLabel)
                            }
                        }
                    }
                )
            }
            if (book.expenses.isEmpty()) {
                item { EmptyState(onAdd = { showForm = true }) }
            } else if (expenses.isEmpty()) {
                item { Text(if (query.isEmpty() && filter == null) emptyMonthText else noResultsText, Modifier.padding(16.dp)) }
            }
            items(expenses, key = { it.id }) { e ->
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp).animateItem(
                    fadeInSpec = spring(), placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    fadeOutSpec = spring()
                )) {
                    val editDescription = stringResource(R.string.edit_row_label, e.title)
                    val categoryText = categoryLabel(e.category)
                    ListItem(
                        modifier = Modifier.clickable(onClickLabel = editDescription) { startEdit(e) }
                            .semantics(mergeDescendants = true) {
                                if (e.note.isNotEmpty()) contentDescription = expenseSummary(e.title, categoryText, e.amount, e.note)
                            },
                        leadingContent = { CategoryBadge(e.category) },
                        headlineContent = { Text(e.title) },
                        supportingContent = {
                            Column {
                                Text(categoryText)
                                if (e.note.isNotEmpty()) Text(e.note, style = MaterialTheme.typography.bodySmall)
                            }
                        },
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

fun privacyMode(context: Context): PrivacyMode {
    val prefs = context.getSharedPreferences("expenses", Context.MODE_PRIVATE)
    return PrivacyMode(object : FlagStore {
        override fun get() = prefs.getBoolean("privacy_mode", false)
        override fun set(value: Boolean) = prefs.edit().putBoolean("privacy_mode", value).apply()
    })
}

fun applyPrivacy(activity: ComponentActivity, on: Boolean) {
    if (on) activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
}

@Composable
fun PrivacySettings() {
    val context = LocalContext.current
    val mode = remember { privacyMode(context) }
    var on by remember { mutableStateOf(mode.enabled) }
    val label = stringResource(R.string.hide_content)
    val state = stringResource(if (on) R.string.state_on else R.string.state_off)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label)
            Text(stringResource(R.string.hide_content_hint), style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = on,
            onCheckedChange = { want ->
                on = want
                mode.setEnabled(want)
                (context as? ComponentActivity)?.let { applyPrivacy(it, want) }
            },
            modifier = Modifier.semantics { contentDescription = "$label, $state" }
        )
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
