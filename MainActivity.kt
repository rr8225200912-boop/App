package com.pocketbite.app

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.withTransaction
import com.pocketbite.app.data.BackupManager
import com.pocketbite.app.data.BudgetEntity
import com.pocketbite.app.data.BudgetSummary
import com.pocketbite.app.data.ExpenseEntity
import com.pocketbite.app.data.FoodItemEntity
import com.pocketbite.app.data.MealPlanEntity
import com.pocketbite.app.data.PocketBiteDatabase
import com.pocketbite.app.data.calculateSummary
import com.pocketbite.app.data.expenseCategories
import com.pocketbite.app.data.mealCategories
import com.pocketbite.app.data.money
import com.pocketbite.app.data.typicalPrice
import com.pocketbite.app.notifications.ReminderScheduler
import com.pocketbite.app.notifications.ReminderWorker
import com.pocketbite.app.notifications.createNotificationChannel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Ink = Color(0xFF101114)
private val Panel = Color(0xFF1B1E23)
private val PanelSoft = Color(0xFF242830)
private val Lime = Color(0xFFC8F16A)
private val Muted = Color(0xFF9AA1AA)
private val Orange = Color(0xFFFFB45B)
private val Red = Color(0xFFFF7777)
private val Blue = Color(0xFF82B7FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)
        setContent { PocketBiteApp(PocketBiteDatabase.get(this)) }
    }
}

@Composable
private fun PocketBiteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lime,
            onPrimary = Ink,
            secondary = Orange,
            background = Ink,
            surface = Panel,
            onBackground = Color(0xFFF5F6F1),
            onSurface = Color(0xFFF5F6F1),
            surfaceVariant = PanelSoft,
            onSurfaceVariant = Muted,
            error = Red
        ),
        content = content
    )
}

@Composable
private fun PocketBiteApp(database: PocketBiteDatabase) {
    val context = LocalContext.current
    val dao = database.dao()
    val scope = rememberCoroutineScope()
    val budgets by dao.observeBudgets().collectAsStateWithLifecycle(initialValue = emptyList())
    val expenses by dao.observeExpenses().collectAsStateWithLifecycle(initialValue = emptyList())
    val foods by dao.observeFoodItems().collectAsStateWithLifecycle(initialValue = emptyList())
    val plans by dao.observeMealPlans().collectAsStateWithLifecycle(initialValue = emptyList())

    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val selectedMonth = remember(month) { YearMonth.parse(month) }
    val budget = budgets.firstOrNull { it.monthKey == month }
    val summary = remember(budget, expenses, month) { calculateSummary(budget, expenses, selectedMonth) }
    var tab by rememberSaveable { mutableStateOf("Home") }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showFoodDialog by remember { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<FoodItemEntity?>(null) }
    var recordPlan by remember { mutableStateOf<MealPlanEntity?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val appPrefs = remember { context.getSharedPreferences("pocketbite_app", android.content.Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        if (!appPrefs.getBoolean("sample_foods_seeded", false)) {
            if (dao.getAllFoodItems().isEmpty()) {
                listOf(
                    FoodItemEntity(name = "Paratha", category = "Breakfast", minPrice = 10.0, maxPrice = 10.0),
                    FoodItemEntity(name = "Dal", category = "Breakfast", minPrice = 30.0, maxPrice = 30.0, unit = "bowl"),
                    FoodItemEntity(name = "Tea", category = "Evening Snacks", minPrice = 10.0, maxPrice = 10.0, unit = "cup"),
                    FoodItemEntity(name = "Cake", category = "Evening Snacks", minPrice = 20.0, maxPrice = 20.0),
                    FoodItemEntity(name = "Singara", category = "Evening Snacks", minPrice = 10.0, maxPrice = 10.0),
                    FoodItemEntity(name = "Rice", category = "Extra Food", minPrice = 20.0, maxPrice = 35.0, unit = "plate")
                ).forEach { dao.upsertFoodItem(it) }
            }
            appPrefs.edit().putBoolean("sample_foods_seeded", true).apply()
        }
    }

    LaunchedEffect(month) {
        if (dao.getBudget(month) == null) {
            dao.upsertBudget(BudgetEntity(monthKey = month, amount = 0.0))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        message = if (granted) "Notifications enabled" else "Notification permission was not granted"
    }
    val jsonExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching { BackupManager.exportJson(context, database, uri) }
                .onSuccess { message = "Backup exported successfully" }
                .onFailure { message = "Backup export failed: ${it.message}" }
        }
    }
    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            runCatching { BackupManager.exportCsv(database, context, uri) }
                .onSuccess { message = "CSV exported successfully" }
                .onFailure { message = "CSV export failed: ${it.message}" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestoreUri = uri
    }

    PocketBiteTheme {
        Scaffold(
            containerColor = Ink,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar(containerColor = Panel, contentColor = Color.White) {
                    val navItems = listOf(
                        Triple("Home", Icons.Default.Home, "Home"),
                        Triple("Planner", Icons.Default.Restaurant, "Planner"),
                        Triple("Expenses", Icons.Default.AttachMoney, "Expenses"),
                        Triple("Prices", Icons.Default.Inventory2, "Prices"),
                        Triple("More", Icons.Default.Analytics, "More")
                    )
                    navItems.forEach { (label, icon, key) ->
                        NavigationBarItem(
                            selected = tab == key,
                            onClick = { tab = key },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                Modifier.fillMaxSize().background(Ink).padding(padding)
            ) {
                AppHeader(tab)
                when (tab) {
                    "Home" -> DashboardScreen(
                        summary, budget, expenses, selectedMonth,
                        onPrevious = { month = selectedMonth.minusMonths(1).toString() },
                        onNext = { month = selectedMonth.plusMonths(1).toString() },
                        onAddExpense = { editingExpense = null; showExpenseDialog = true },
                        onEditBudget = { showBudgetDialog = true }
                    )
                    "Planner" -> PlannerScreen(
                        foods, plans,
                        dailyLimit = summary.dailyLimit.coerceAtLeast(0.0),
                        breakfastLimit = summary.dailyLimit.coerceAtLeast(0.0) * ((budget?.breakfastPercent ?: 60) / 100.0),
                        eveningLimit = summary.dailyLimit.coerceAtLeast(0.0) * ((budget?.eveningPercent ?: 40) / 100.0),
                        onSavePlan = { plan -> scope.launch {
                            dao.insertMealPlan(plan); message = "Meal plan saved"
                        } },
                        onRecordActual = { recordPlan = it }
                    )
                    "Expenses" -> ExpensesScreen(
                        expenses.filter { it.date.startsWith(month) },
                        onAdd = { editingExpense = null; showExpenseDialog = true },
                        onEdit = { editingExpense = it; showExpenseDialog = true },
                        onDelete = { expense -> scope.launch { dao.deleteExpense(expense.id); message = "Expense deleted" } }
                    )
                    "Prices" -> PricesScreen(
                        foods,
                        onAdd = { editingFood = null; showFoodDialog = true },
                        onEdit = { editingFood = it; showFoodDialog = true },
                        onDelete = { food -> scope.launch { dao.deleteFoodItem(food.id); message = "Food item deleted" } },
                        onToggle = { food -> scope.launch { dao.upsertFoodItem(food.copy(available = !food.available)) } }
                    )
                    else -> MoreScreen(
                        context = context,
                        onExportJson = { jsonExportLauncher.launch("PocketBite-backup-${LocalDate.now()}.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json", "text/json", "application/octet-stream")) },
                        onExportCsv = { csvExportLauncher.launch("PocketBite-expenses-${LocalDate.now()}.csv") },
                        onPermission = {
                            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else message = "Notification permission is already enabled"
                        },
                        budgets = budgets,
                        expenses = expenses,
                        month = selectedMonth
                    )
                }
            }
        }
    }

    if (showExpenseDialog) {
        ExpenseDialog(
            initial = editingExpense,
            foodItems = foods,
            onDismiss = { showExpenseDialog = false },
            onSave = { amount, category, date, time, description, notes, foodItemId ->
                scope.launch {
                    if (editingExpense == null) {
                        dao.insertExpense(ExpenseEntity(
                            amount = amount, category = category, date = date, time = time,
                            description = description, notes = notes, foodItemId = foodItemId
                        ))
                        message = "Expense recorded"
                    } else {
                        dao.updateExpense(editingExpense!!.copy(
                            amount = amount, category = category, date = date, time = time,
                            description = description, notes = notes, foodItemId = foodItemId
                        ))
                        message = "Expense updated"
                    }
                    showExpenseDialog = false
                }
            }
        )
    }

    if (showBudgetDialog) {
        BudgetDialog(
            initial = budget,
            onDismiss = { showBudgetDialog = false },
            onSave = { amount, breakfast, evening ->
                scope.launch {
                    dao.upsertBudget(BudgetEntity(
                        id = budget?.id ?: 0,
                        monthKey = month,
                        amount = amount,
                        breakfastPercent = breakfast,
                        eveningPercent = evening
                    ))
                    message = "Monthly budget saved"
                    showBudgetDialog = false
                }
            }
        )
    }

    if (showFoodDialog) {
        FoodDialog(
            initial = editingFood,
            onDismiss = { showFoodDialog = false },
            onSave = { name, category, min, max, unit ->
                scope.launch {
                    dao.upsertFoodItem(FoodItemEntity(
                        id = editingFood?.id ?: 0, name = name, category = category,
                        minPrice = min, maxPrice = max, unit = unit,
                        available = editingFood?.available ?: true
                    ))
                    message = "Food item saved"
                    showFoodDialog = false
                }
            }
        )
    }

    if (pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            containerColor = Panel,
            title = { Text("Replace current app data?") },
            text = { Text("Restoring this backup replaces your current budgets, expenses, food prices and meal plans. Export a backup first if you need to keep the current data.", color = Muted) },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingRestoreUri
                    pendingRestoreUri = null
                    if (uri != null) scope.launch {
                        runCatching { BackupManager.importJson(context, database, uri) }
                            .onSuccess { message = "Backup restored successfully" }
                            .onFailure { message = "Restore failed: ${it.message}" }
                    }
                }) { Text("Restore", color = Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pendingRestoreUri = null }) { Text("Cancel") } }
        )
    }

    if (recordPlan != null) {
        ActualCostDialog(
            plan = recordPlan!!,
            onDismiss = { recordPlan = null },
            onSave = { actual ->
                val plan = recordPlan!!
                scope.launch {
                    database.withTransaction {
                        dao.updateMealPlanActual(plan.id, actual)
                        dao.insertExpense(ExpenseEntity(
                            amount = actual,
                            category = plan.category,
                            date = plan.date,
                            time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                            description = "Actual cost for planned ${plan.category.lowercase(Locale.getDefault())}"
                        ))
                    }
                    message = "Actual spending recorded"
                    recordPlan = null
                }
            }
        )
    }

    // Snackbars are shown as small inline status messages in the More screen header area.
    if (message != null) {
        androidx.compose.runtime.LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.delay(2_500)
            message = null
        }
    }
}

@Composable
private fun AppHeader(tab: String) {
    Row(
        Modifier.fillMaxWidth().background(Ink).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("PocketBite", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Lime)
            Text(
                when (tab) {
                    "Home" -> "Make every taka count"
                    "Planner" -> "Plan meals, not pressure"
                    "Expenses" -> "Every taka, accounted for"
                    "Prices" -> "Your local food prices"
                    else -> "Insights & preferences"
                },
                color = Muted, fontSize = 11.sp
            )
        }
        Box(Modifier.size(38.dp).clip(CircleShape).background(Panel), contentAlignment = Alignment.Center) {
            Text("৳", color = Lime, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DashboardScreen(
    summary: BudgetSummary,
    budget: BudgetEntity?,
    expenses: List<ExpenseEntity>,
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAddExpense: () -> Unit,
    onEditBudget: () -> Unit
) {
    val monthExpenses = expenses.filter { it.date.startsWith(month.toString()) }
    val progress = if (summary.budget > 0) (summary.spent / summary.budget).toFloat().coerceIn(0f, 1f) else 0f
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevious) { Icon(Icons.Default.ArrowBackIosNew, null, tint = Muted, modifier = Modifier.size(17.dp)) }
                    Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onNext) { Icon(Icons.Default.ArrowForwardIos, null, tint = Muted, modifier = Modifier.size(17.dp)) }
                }
                Text("BDT • ৳", color = Muted, fontSize = 11.sp)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("AVAILABLE BALANCE", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(7.dp))
                    Text(money(summary.balance), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = if (summary.balance >= 0) Lime else Red)
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Monthly budget", color = Muted, fontSize = 11.sp); Text(money(summary.budget), fontWeight = FontWeight.SemiBold) }
                        Column(horizontalAlignment = Alignment.End) { Text("Total spent", color = Muted, fontSize = 11.sp); Text(money(summary.spent), fontWeight = FontWeight.SemiBold) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(PanelSoft)) {
                        Box(Modifier.fillMaxWidth(progress).height(8.dp).clip(CircleShape).background(Lime))
                    }
                    Spacer(Modifier.height(7.dp))
                    Text("${(progress * 100).toInt()}% of your budget used", color = Muted, fontSize = 11.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("TODAY'S SPENDING", money(summary.todaySpent), Icons.Default.SouthEast, Orange, Modifier.weight(1f))
                MetricCard("DAYS LEFT", summary.remainingDays.toString(), Icons.Default.CalendarMonth, Blue, Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("SMART DAILY LIMIT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Icon(Icons.Default.TrendingUp, null, tint = Lime)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(money(summary.dailyLimit.coerceAtLeast(0.0)), fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Lime)
                    Text("${money(summary.balance)} remaining across ${summary.remainingDays} days", fontSize = 12.sp, color = Muted)
                    Spacer(Modifier.height(14.dp))
                    val breakfast = summary.dailyLimit.coerceAtLeast(0.0) * ((budget?.breakfastPercent ?: 60) / 100.0)
                    val evening = summary.dailyLimit.coerceAtLeast(0.0) * ((budget?.eveningPercent ?: 40) / 100.0)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AllocationTile("Breakfast", money(breakfast), "${budget?.breakfastPercent ?: 60}%", Lime, Modifier.weight(1f))
                        AllocationTile("Evening", money(evening), "${budget?.eveningPercent ?: 40}%", Orange, Modifier.weight(1f))
                    }
                    if (summary.elapsedDays > 0) {
                        Spacer(Modifier.height(10.dp))
                        Text("Average spent so far: ${money(summary.averageDailySpent)} / day", color = Muted, fontSize = 11.sp)
                        val projected = summary.averageDailySpent * selectedMonth.lengthOfMonth()
                        if (summary.budget > 0 && projected > summary.budget) {
                            Text("At this pace, estimated monthly spending is ${money(projected)}. Review optional purchases before they affect your food budget.", color = Orange, fontSize = 11.sp)
                        }
                    }
                    val optionalSpend = monthExpenses.filter { it.category in listOf("Personal Shopping", "Hobbies", "Other Expenses") }.sumOf { it.amount }
                    Text("Personal & other spending: ${money(optionalSpend)}", color = Muted, fontSize = 10.sp)
                    if (summary.balance < 0) {
                        Spacer(Modifier.height(12.dp))
                        Text("Your recorded spending is above this month's budget. Review optional purchases or adjust the budget; keep essential meals covered.", color = Red, fontSize = 12.sp)
                    } else if (summary.dailyLimit < 0.0) {
                        Text("The current allowance is not enough for the remaining period. Review optional expenses or adjust your allowance.", color = Orange, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAddExpense, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add expense", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onEditBudget, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Edit, null); Spacer(Modifier.width(6.dp)); Text("Edit budget")
                }
            }
        }
        item {
            Text("Recent spending", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (monthExpenses.isEmpty()) {
                EmptyState("No expenses yet", "Your spending will appear here after you add a transaction.")
            } else {
                monthExpenses.take(4).forEach { ExpenseRow(it, onClick = {}, showEdit = false) }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
            Spacer(Modifier.height(11.dp))
            Text(value, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(title, fontSize = 9.sp, color = Muted, letterSpacing = 0.8.sp)
        }
    }
}

@Composable
private fun AllocationTile(title: String, amount: String, percent: String, tint: Color, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(13.dp)).background(PanelSoft).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(tint))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Muted, fontSize = 10.sp)
            Text(amount, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text(percent, color = tint, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun ExpensesScreen(
    expenses: List<ExpenseEntity>,
    onAdd: () -> Unit,
    onEdit: (ExpenseEntity) -> Unit,
    onDelete: (ExpenseEntity) -> Unit
) {
    var filter by rememberSaveable { mutableStateOf("All") }
    var dateFilter by rememberSaveable { mutableStateOf("") }
    val filters = listOf("All") + expenseCategories
    val categoryFiltered = if (filter == "All") expenses else expenses.filter { it.category == filter }
    val shown = categoryFiltered.filter { dateFilter.isBlank() || it.date == dateFilter.trim() }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 4.dp, 18.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(17.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("Transactions", color = Muted, fontSize = 11.sp); Text(expenses.size.toString(), fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                    Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) { Icon(Icons.Default.Add, null); Text("Add") }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Showing ${expenses.firstOrNull()?.date?.take(7) ?: "selected month"}", color = Muted, fontSize = 10.sp)
                OutlinedTextField(
                    value = dateFilter,
                    onValueChange = { dateFilter = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Filter exact date (YYYY-MM-DD)") },
                    singleLine = true
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filters.forEach { f -> AssistChip(onClick = { filter = f }, label = { Text(f) }) }
                }
            }
        }
        if (shown.isEmpty()) item { EmptyState("Nothing to show", "Add an expense or choose another category filter.") }
        items(shown, key = { it.id }) { expense ->
            ExpenseRow(expense, onClick = { onEdit(expense) }, onDelete = { onDelete(expense) })
        }
    }
}

@Composable
private fun ExpenseRow(expense: ExpenseEntity, onClick: () -> Unit, onDelete: (() -> Unit)? = null, showEdit: Boolean = true) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth().clickable(enabled = showEdit, onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(PanelSoft), contentAlignment = Alignment.Center) {
                Icon(categoryIcon(expense.category), null, tint = Lime, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(expense.description.ifBlank { expense.category }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("${expense.category} • ${expense.date} • ${expense.time}", color = Muted, fontSize = 10.sp)
            }
            Text("-${money(expense.amount)}", color = Orange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (onDelete != null) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Muted, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

private fun categoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector = when (category) {
    "Breakfast", "Evening Snacks", "Extra Food" -> Icons.Default.Fastfood
    "Hobbies" -> Icons.Default.TrendingUp
    else -> Icons.Default.Wallet
}

@Composable
private fun PlannerScreen(
    foods: List<FoodItemEntity>,
    plans: List<MealPlanEntity>,
    dailyLimit: Double,
    breakfastLimit: Double,
    eveningLimit: Double,
    onSavePlan: (MealPlanEntity) -> Unit,
    onRecordActual: (MealPlanEntity) -> Unit
) {
    var category by rememberSaveable { mutableStateOf("Breakfast") }
    val quantities = remember { mutableStateMapOf<Long, Int>() }
    val activeFoods = foods.filter { it.available }
    val selected = activeFoods.filter { (quantities[it.id] ?: 0) > 0 }
    val total = selected.sumOf { it.typicalPrice * (quantities[it.id] ?: 0) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 4.dp, 18.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Build a flexible meal", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        item {
            Text("MEAL TYPE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                mealCategories.forEach { item ->
                    AssistChip(onClick = { category = item }, label = { Text(if (category == item) "✓ $item" else item) })
                }
            }
            val suggested = when (category) {
                "Breakfast" -> breakfastLimit
                "Evening Snacks" -> eveningLimit
                else -> dailyLimit
            }
            Text("Suggested ${if (category == "Extra Food") "remaining daily" else category.lowercase(Locale.getDefault())} budget: ${money(suggested)}", color = Lime, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("This is a flexible guide based on your remaining balance and saved local prices. You choose what works for you.", color = Muted, fontSize = 10.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(17.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                    Text("Available food", fontWeight = FontWeight.Bold)
                    Text("Tap + to add items. Prices use the midpoint of your saved range.", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(10.dp))
                    if (activeFoods.isEmpty()) EmptyState("Your food list is empty", "Add local food prices from the Prices tab first.")
                    activeFoods.forEach { food ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(food.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("${money(food.minPrice)}${if (food.maxPrice != food.minPrice) "–${money(food.maxPrice)}" else ""} / ${food.unit}", color = Muted, fontSize = 10.sp)
                            }
                            IconButton(onClick = { quantities[food.id] = ((quantities[food.id] ?: 0) - 1).coerceAtLeast(0) }) { Text("−", fontSize = 20.sp, color = Muted) }
                            Text((quantities[food.id] ?: 0).toString(), fontWeight = FontWeight.Bold)
                            IconButton(onClick = { quantities[food.id] = ((quantities[food.id] ?: 0) + 1).coerceAtMost(50) }) { Text("+", fontSize = 20.sp, color = Lime) }
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(17.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estimated meal cost", color = Muted, fontSize = 12.sp)
                        Text(money(total), color = Lime, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(11.dp))
                    Button(
                        onClick = {
                            val json = JSONArray().apply {
                                selected.forEach { food -> put(JSONObject()
                                    .put("id", food.id).put("name", food.name)
                                    .put("quantity", quantities[food.id] ?: 0)
                                    .put("unitPrice", food.typicalPrice)) }
                            }
                            onSavePlan(MealPlanEntity(
                                date = LocalDate.now().toString(), category = category,
                                itemsJson = json.toString(), plannedTotal = total
                            ))
                            quantities.clear()
                        },
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)
                    ) { Text("Save meal plan", fontWeight = FontWeight.Bold) }
                }
            }
        }
        item { Text("Saved meal plans", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        if (plans.isEmpty()) item { EmptyState("No meal plans yet", "Your saved plans will appear here.") }
        items(plans, key = { it.id }) { plan ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(15.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(plan.category, fontWeight = FontWeight.Bold)
                            Text(plan.date, color = Muted, fontSize = 10.sp)
                        }
                        Text(money(plan.plannedTotal), color = Lime, fontWeight = FontWeight.Bold)
                    }
                    if (plan.actualTotal == null) {
                        Spacer(Modifier.height(9.dp))
                        OutlinedButton(onClick = { onRecordActual(plan) }, modifier = Modifier.fillMaxWidth()) { Text("Record actual amount spent") }
                    } else {
                        Text("Actual recorded: ${money(plan.actualTotal)}", color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PricesScreen(
    foods: List<FoodItemEntity>,
    onAdd: () -> Unit,
    onEdit: (FoodItemEntity) -> Unit,
    onDelete: (FoodItemEntity) -> Unit,
    onToggle: (FoodItemEntity) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 4.dp, 18.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Food price manager", fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("Editable local prices", color = Muted, fontSize = 11.sp) }
                Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) { Icon(Icons.Default.Add, null); Text("Add") }
            }
        }
        if (foods.isEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                EmptyState("Start your food list", "Add the prices you actually pay in your area.")
                Text("Sample idea: Paratha ৳10 • Tea ৳10 • Singara ৳10 • Cake ৳20 • Dal ৳30", color = Muted, fontSize = 11.sp)
            }
        }
        items(foods, key = { it.id }) { food ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(15.dp)) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { onEdit(food) }) {
                        Text(food.name, fontWeight = FontWeight.SemiBold)
                        Text("${food.category} • ${money(food.minPrice)}${if (food.maxPrice != food.minPrice) "–${money(food.maxPrice)}" else ""} / ${food.unit}", color = Muted, fontSize = 10.sp)
                    }
                    TextButton(onClick = { onToggle(food) }) { Text(if (food.available) "Available" else "Hidden", color = if (food.available) Lime else Muted, fontSize = 10.sp) }
                    IconButton(onClick = { onEdit(food) }) { Icon(Icons.Default.Edit, null, tint = Muted, modifier = Modifier.size(17.dp)) }
                    IconButton(onClick = { onDelete(food) }) { Icon(Icons.Default.Delete, null, tint = Muted, modifier = Modifier.size(17.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MoreScreen(
    context: android.content.Context,
    onExportJson: () -> Unit,
    onImport: () -> Unit,
    onExportCsv: () -> Unit,
    onPermission: () -> Unit,
    budgets: List<BudgetEntity>,
    expenses: List<ExpenseEntity>,
    month: YearMonth
) {
    val prefs = remember { context.getSharedPreferences(ReminderWorker.PREFS, android.content.Context.MODE_PRIVATE) }
    val reminderTypes = listOf(
        Triple("breakfast", "Breakfast reminder", "Morning meal check-in"),
        Triple("evening", "Evening reminder", "Evening snack check-in"),
        Triple("expense", "Expense reminder", "Record spending before the day ends"),
        Triple("review", "Monthly review", "Review your monthly budget")
    )
    val toggles = remember { mutableStateMapOf<String, Boolean>().apply { reminderTypes.forEach { put(it.first, prefs.getBoolean("${it.first}_enabled", false)) } } }
    val times = remember { mutableStateMapOf<String, String>().apply { reminderTypes.forEach { type ->
        val h = prefs.getInt("${type.first}_hour", when (type.first) { "breakfast" -> 8; "evening" -> 17; "expense" -> 21; else -> 9 })
        val m = prefs.getInt("${type.first}_minute", 0)
        put(type.first, String.format(Locale.US, "%02d:%02d", h, m))
    } } }
    val selectedExpenses = expenses.filter { it.date.startsWith(month.toString()) }
    val categories = selectedExpenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
    val total = categories.values.sum()
    val dailyTotals = selectedExpenses.groupBy { it.date }.mapValues { entry -> entry.value.sumOf { it.amount } }
    val highestDay = dailyTotals.maxByOrNull { it.value }
    val averagePerDay = total / month.lengthOfMonth().coerceAtLeast(1)
    val firstWeek = month.atDay(1).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val weekStarts = (0..4).map { firstWeek.plusWeeks(it.toLong()) }.filter { !it.isAfter(month.atEndOfMonth()) }
    val weeklyValues = weekStarts.map { start ->
        val end = start.plusDays(7)
        val value = selectedExpenses.filter { expense ->
            runCatching { LocalDate.parse(expense.date) }.getOrNull()?.let { !it.isBefore(start) && it.isBefore(end) } ?: false
        }.sumOf { it.amount }
        start to value
    }
    val lastDay = if (month == YearMonth.now()) LocalDate.now() else month.atEndOfMonth()
    val last7 = (6 downTo 0).map { lastDay.minusDays(it.toLong()) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 4.dp, 18.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item { Text("Monthly insights", fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), color = Muted, fontSize = 11.sp) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("CATEGORY BREAKDOWN", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(12.dp))
                    if (categories.isEmpty()) EmptyState("No analytics yet", "Record a few expenses to see your spending breakdown.")
                    else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DonutChart(categories.values.toList(), Modifier.size(126.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                categories.entries.sortedByDescending { it.value }.take(5).forEachIndexed { index, entry ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(7.dp).clip(CircleShape).background(chartColors[index % chartColors.size]))
                                        Spacer(Modifier.width(6.dp))
                                        Text(entry.key, fontSize = 10.sp, color = Muted, modifier = Modifier.weight(1f))
                                        Text(money(entry.value), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Total this month: ${money(total)}", fontWeight = FontWeight.Bold, color = Lime)
                        Spacer(Modifier.height(6.dp))
                        Text("Average per calendar day: ${money(averagePerDay)}", color = Muted, fontSize = 11.sp)
                        Text(highestDay?.let { "Highest spending day: ${it.key} • ${money(it.value)}" } ?: "Highest spending day: —", color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("WEEKLY SPENDING", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(10.dp))
                    BarChart(weeklyValues, Modifier.fillMaxWidth().height(120.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        weeklyValues.forEach { (start, _) -> Text("${start.dayOfMonth}", color = Muted, fontSize = 9.sp) }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("LAST 7 DAYS", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(10.dp))
                    val dayValues = last7.map { day -> day to expenses.filter { it.date == day.toString() }.sumOf { it.amount } }
                    LineChart(dayValues, Modifier.fillMaxWidth().height(140.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        dayValues.forEach { (day, _) -> Text(day.dayOfMonth.toString(), color = Muted, fontSize = 9.sp) }
                    }
                }
            }
        }
        item { Text("Reminders", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    reminderTypes.forEachIndexed { index, item ->
                        val (type, title, subtitle) = item
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(subtitle, color = Muted, fontSize = 10.sp)
                            }
                            TextButton(onClick = {
                                val initial = times[type] ?: "08:00"
                                val split = initial.split(":")
                                TimePickerDialog(context, { _, h, m ->
                                    val formatted = String.format(Locale.US, "%02d:%02d", h, m)
                                    times[type] = formatted
                                    if (toggles[type] == true) {
                                        prefs.edit().putInt("${type}_hour", h).putInt("${type}_minute", m).putBoolean("${type}_enabled", true).apply()
                                        ReminderScheduler.schedule(context, type, h, m)
                                    }
                                }, split[0].toInt(), split[1].toInt(), true).show()
                            }) { Text(times[type] ?: "08:00", color = Lime) }
                            Switch(checked = toggles[type] == true, onCheckedChange = { enabled ->
                                toggles[type] = enabled
                                prefs.edit().putBoolean("${type}_enabled", enabled).apply()
                                if (enabled) {
                                    onPermission()
                                    val split = (times[type] ?: "08:00").split(":")
                                    ReminderScheduler.schedule(context, type, split[0].toInt(), split[1].toInt())
                                } else ReminderScheduler.cancel(context, type)
                            })
                        }
                        if (index != reminderTypes.lastIndex) HorizontalDivider(color = PanelSoft)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onPermission, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.NotificationsActive, null); Spacer(Modifier.width(7.dp)); Text("Notification permission")
                    }
                    Text("Android may delay background reminders to protect battery life. WorkManager reschedules them after delivery and across device restarts.", color = Muted, fontSize = 10.sp)
                }
            }
        }
        item { Text("Backup & restore", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Your data stays on this device. Export a JSON backup before changing phones or reinstalling.", color = Muted, fontSize = 11.sp)
                    Button(onClick = onExportJson, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) { Text("Export complete backup (JSON)", fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Restore JSON backup") }
                    OutlinedButton(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) { Text("Export transactions (CSV)") }
                }
            }
        }
        item { Text("About PocketBite  •  Version 1.0.0", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp)) }
    }
}

private val chartColors = listOf(Lime, Orange, Blue, Color(0xFFCE9BFF), Color(0xFF69D5C3), Red)

@Composable
private fun DonutChart(values: List<Double>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val total = values.sum().coerceAtLeast(0.01)
        var start = -90f
        values.forEachIndexed { index, value ->
            val sweep = ((value / total) * 360f).toFloat()
            drawArc(
                color = chartColors[index % chartColors.size],
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = size.minDimension * 0.18f, cap = StrokeCap.Butt),
                topLeft = Offset(size.width * 0.1f, size.height * 0.1f),
                size = Size(size.width * 0.8f, size.height * 0.8f)
            )
            start += sweep
        }
    }
}

@Composable
private fun LineChart(values: List<Pair<LocalDate, Double>>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val max = values.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
        val horizontalStep = if (values.size > 1) size.width / (values.size - 1) else size.width
        val points = values.mapIndexed { index, item ->
            val x = if (values.size > 1) horizontalStep * index else size.width / 2f
            val y = size.height - ((item.second / max).toFloat() * size.height * 0.78f) - size.height * 0.08f
            Offset(x, y)
        }
        val path = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }
        drawPath(path, color = Lime, style = Stroke(width = 4f, cap = StrokeCap.Round))
        points.forEach { point -> drawCircle(color = Lime, radius = 5f, center = point) }
    }
}

@Composable
private fun BarChart(values: List<Pair<LocalDate, Double>>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val max = values.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
        val slot = size.width / (values.size.coerceAtLeast(1) * 1.0f)
        values.forEachIndexed { index, item ->
            val height = (item.second / max).toFloat() * (size.height * 0.82f)
            val left = slot * index + slot * 0.23f
            drawRoundRect(
                color = if (index == values.lastIndex) Lime else PanelSoft,
                topLeft = Offset(left, size.height - height),
                size = Size(slot * 0.54f, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(PanelSoft.copy(alpha = 0.55f)).padding(17.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(body, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun ExpenseDialog(
    initial: ExpenseEntity?,
    foodItems: List<FoodItemEntity>,
    onDismiss: () -> Unit,
    onSave: (Double, String, String, String, String, String, Long?) -> Unit
) {
    var amount by remember(initial) { mutableStateOf(initial?.amount?.toString() ?: "") }
    var category by remember(initial) { mutableStateOf(initial?.category ?: "Breakfast") }
    var date by remember(initial) { mutableStateOf(initial?.date ?: LocalDate.now().toString()) }
    var time by remember(initial) { mutableStateOf(initial?.time ?: LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
    var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
    var foodItemId by remember(initial) { mutableStateOf(initial?.foodItemId) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember(initial) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text(if (initial == null) "Add expense" else "Edit expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(amount, { amount = it; error = null }, label = { Text("Amount (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                Text("CATEGORY", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    expenseCategories.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pair.forEach { option ->
                                AssistChip(onClick = { category = option }, label = { Text(if (category == option) "✓ $option" else option, fontSize = 10.sp) })
                            }
                        }
                    }
                }
                OutlinedTextField(date, { date = it }, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true)
                OutlinedTextField(time, { time = it }, label = { Text("Time (HH:MM)") }, singleLine = true)
                Text("FOOD ITEM (OPTIONAL)", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = { foodItemId = null }, label = { Text(if (foodItemId == null) "✓ None" else "None", fontSize = 10.sp) })
                    foodItems.filter { it.available }.forEach { food ->
                        AssistChip(onClick = { foodItemId = food.id }, label = { Text(if (foodItemId == food.id) "✓ ${food.name}" else food.name, fontSize = 10.sp) })
                    }
                }
                OutlinedTextField(description, { description = it }, label = { Text("Description (optional)") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, singleLine = true)
                error?.let { Text(it, color = Red, fontSize = 11.sp) }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                val value = amount.toDoubleOrNull()
                when {
                    value == null || !value.isFinite() || value <= 0 -> error = "Enter an amount greater than zero"
                    runCatching { LocalDate.parse(date) }.isFailure -> error = "Enter a real date in YYYY-MM-DD format"
                    runCatching { LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm")) }.isFailure -> error = "Use time format HH:MM"
                    else -> { saving = true; onSave(value, category, date, time, description.trim(), notes.trim(), foodItemId) }
                }
            }) { Text(if (saving) "Saving…" else "Save", color = Lime, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun BudgetDialog(
    initial: BudgetEntity?,
    onDismiss: () -> Unit,
    onSave: (Double, Int, Int) -> Unit
) {
    var amount by remember(initial) { mutableStateOf(if (initial == null || initial.amount == 0.0) "" else initial.amount.toString()) }
    var breakfast by remember(initial) { mutableIntStateOf(initial?.breakfastPercent ?: 60) }
    var evening by remember(initial) { mutableIntStateOf(initial?.eveningPercent ?: 40) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text("Monthly budget", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(amount, { amount = it; error = null }, label = { Text("Monthly allowance (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                Text("Meal allocation", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                AllocationSlider("Breakfast", breakfast, Lime) { breakfast = it; evening = 100 - it }
                AllocationSlider("Evening", evening, Orange) { evening = it; breakfast = 100 - it }
                Text("Together: ${breakfast + evening}%", color = Muted, fontSize = 11.sp)
                error?.let { Text(it, color = Red, fontSize = 11.sp) }
            }
        },
        confirmButton = { TextButton(onClick = {
            val value = amount.toDoubleOrNull()
            if (value == null || !value.isFinite() || value <= 0) error = "Enter a monthly allowance greater than zero"
            else onSave(value, breakfast, evening)
        }) { Text("Save budget", color = Lime, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AllocationSlider(title: String, value: Int, tint: Color, onChange: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontSize = 12.sp)
            Text("$value%", color = tint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onChange((value - 5).coerceAtLeast(0)) }) { Text("−", color = tint) }
            Box(Modifier.weight(1f).height(7.dp).clip(CircleShape).background(PanelSoft)) {
                Box(Modifier.fillMaxWidth(value / 100f).height(7.dp).clip(CircleShape).background(tint))
            }
            TextButton(onClick = { onChange((value + 5).coerceAtMost(100)) }) { Text("+", color = tint) }
        }
    }
}

@Composable
private fun FoodDialog(
    initial: FoodItemEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, String) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var category by remember(initial) { mutableStateOf(initial?.category ?: "Breakfast") }
    var min by remember(initial) { mutableStateOf(initial?.minPrice?.toString() ?: "") }
    var max by remember(initial) { mutableStateOf(initial?.maxPrice?.toString() ?: "") }
    var unit by remember(initial) { mutableStateOf(initial?.unit ?: "piece") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember(initial) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text(if (initial == null) "Add food item" else "Edit food item", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Food name") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("Custom category") }, singleLine = true)
                OutlinedTextField(min, { min = it }, label = { Text("Minimum price (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(max, { max = it }, label = { Text("Maximum price (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(unit, { unit = it }, label = { Text("Unit (piece, plate, cup…)") }, singleLine = true)
                error?.let { Text(it, color = Red, fontSize = 11.sp) }
            }
        },
        confirmButton = { TextButton(enabled = !saving, onClick = {
            val low = min.toDoubleOrNull(); val high = max.toDoubleOrNull()
            when {
                saving -> Unit
                name.isBlank() -> error = "Enter a food name"
                low == null || high == null || !low.isFinite() || !high.isFinite() || low < 0 || high < low -> error = "Enter a valid price range"
                else -> { saving = true; onSave(name.trim(), category.trim().ifBlank { "Other" }, low, high, unit.ifBlank { "unit" }.trim()) }
            }
        }) { Text(if (saving) "Saving…" else "Save item", color = Lime, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ActualCostDialog(plan: MealPlanEntity, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var amount by remember(plan) { mutableStateOf(plan.plannedTotal.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember(plan) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text("Record actual spending") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Planned estimate: ${money(plan.plannedTotal)}", color = Muted, fontSize = 12.sp)
                OutlinedTextField(amount, { amount = it }, label = { Text("Actual amount (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                error?.let { Text(it, color = Red, fontSize = 11.sp) }
            }
        },
        confirmButton = { TextButton(enabled = !saving, onClick = {
            val value = amount.toDoubleOrNull()
            if (value == null || !value.isFinite() || value <= 0) error = "Enter an amount greater than zero"
            else { saving = true; onSave(value) }
        }) { Text(if (saving) "Recording…" else "Record expense", color = Lime) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
