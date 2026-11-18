package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Engineering
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.BillingDashboardData
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMode
import com.example.data.model.formatInr
import com.example.data.model.todayIsoDate
import com.example.ui.components.AshaDatePickerDialog
import com.example.ui.components.AshaPanelCard
import com.example.ui.components.IosSquircleIcon
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDue
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PaidBadgeBg
import com.example.ui.theme.PaidBadgeFg
import java.util.Locale
import kotlin.math.abs

private val EXPENSE_PRESETS = listOf(
    "Diesel" to ExpenseCategory.FUEL,
    "Pump Service" to ExpenseCategory.MAINTENANCE,
    "Electricity Bill" to ExpenseCategory.ELECTRICITY,
    "Operator Pay" to ExpenseCategory.SALARY,
    "Oil / Filter" to ExpenseCategory.PARTS
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpensesScreen(
    data: BillingDashboardData,
    onSaveExpense: (
        existingId: String,
        title: String,
        category: ExpenseCategory,
        amount: Double,
        date: String,
        paidMode: PaymentMode,
        note: String,
        onComplete: (Boolean) -> Unit
    ) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onOpenMonthlyAnalytics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }
    var deletingExpense by remember { mutableStateOf<Expense?>(null) }

    val filteredExpenses = remember(data.expenses, searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        data.expenses.filter { exp ->
            val matchesCat = selectedCategory == null ||
                ExpenseCategory.fromLabel(exp.category) == selectedCategory
            val matchesQuery = q.isEmpty() ||
                listOf(exp.title, exp.category, exp.note, exp.paidMode, exp.date)
                    .joinToString(" ")
                    .lowercase()
                    .contains(q)
            matchesCat && matchesQuery
        }
    }

    val categoryTotals = remember(data.expenses) {
        ExpenseCategory.entries.map { cat ->
            cat to data.expenses
                .filter { ExpenseCategory.fromLabel(it.category) == cat }
                .sumOf { it.amount }
        }.filter { it.second > 0.0 }
            .sortedByDescending { it.second }
    }

    val collected = data.totalCollected
    val totalExp = data.totalExpenses
    val netProfit = data.netCollectedProfit
    val isProfit = netProfit >= 0.0

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profit / Loss Hero Card
            item {
                AshaPanelCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IosSquircleIcon(
                                icon = Icons.Rounded.AccountBalanceWallet,
                                containerColor = Color(0xFFEA580C),
                                size = 38.dp,
                                iconSize = 20.dp
                            )
                            Column {
                                Text(
                                    text = "Expenses & P&L",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${data.expenses.size} entries logged",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                onClick = onOpenMonthlyAnalytics,
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.testTag("expenses_monthly_chart_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BarChart,
                                        contentDescription = "Monthly Chart",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Chart",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isProfit) PaidBadgeBg else CrimsonContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isProfit) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isProfit) PaidBadgeFg else CrimsonDue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (isProfit) "Profit" else "Loss",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isProfit) PaidBadgeFg else CrimsonDue,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExpenseMetricPill(
                            label = "Collected",
                            value = formatInr(collected),
                            valueColor = PaidBadgeFg,
                            modifier = Modifier.weight(1f)
                        )
                        ExpenseMetricPill(
                            label = "Expenses",
                            value = formatInr(totalExp),
                            valueColor = CrimsonDue,
                            modifier = Modifier.weight(1f)
                        )
                        ExpenseMetricPill(
                            label = if (isProfit) "Net Profit" else "Net Loss",
                            value = (if (isProfit) "+" else "-") + formatInr(abs(netProfit)),
                            valueColor = if (isProfit) EmeraldPrimary else CrimsonDue,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (categoryTotals.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categoryTotals.forEach { (cat, amt) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(cat.colorHex).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color(cat.colorHex).copy(alpha = 0.30f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(cat.colorHex))
                                        )
                                        Text(
                                            text = "${cat.label} · ${formatInr(amt)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search & Category Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search expenses…") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_search_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All (${data.expenses.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldMint,
                                selectedLabelColor = EmeraldPrimary
                            ),
                            modifier = Modifier.testTag("expense_filter_all")
                        )
                        ExpenseCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                label = { Text(cat.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(cat.colorHex).copy(alpha = 0.16f),
                                    selectedLabelColor = Color(cat.colorHex)
                                ),
                                modifier = Modifier.testTag("expense_filter_${cat.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            if (filteredExpenses.isEmpty()) {
                item {
                    AshaPanelCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IosSquircleIcon(
                                icon = Icons.Rounded.Receipt,
                                containerColor = Color(0xFF64748B),
                                size = 44.dp,
                                iconSize = 22.dp
                            )
                            Text(
                                text = "No operational expenses logged yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = {
                                    editingExpense = null
                                    showFormDialog = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log First Expense")
                            }
                        }
                    }
                }
            } else {
                items(filteredExpenses, key = { it.id }) { expense ->
                    ExpenseRowCard(
                        expense = expense,
                        onEdit = {
                            editingExpense = expense
                            showFormDialog = true
                        },
                        onDelete = { deletingExpense = expense }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingExpense = null
                showFormDialog = true
            },
            containerColor = EmeraldPrimary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_expense_fab")
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Log Expense"
            )
        }
    }

    if (showFormDialog) {
        ExpenseFormDialog(
            initial = editingExpense,
            onDismiss = { showFormDialog = false },
            onSave = { title, category, amount, date, paidMode, note ->
                onSaveExpense(
                    editingExpense?.id.orEmpty(),
                    title,
                    category,
                    amount,
                    date,
                    paidMode,
                    note
                ) { ok ->
                    if (ok) showFormDialog = false
                }
            }
        )
    }

    deletingExpense?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingExpense = null },
            title = { Text("Delete Expense?") },
            text = { Text("Remove \"${target.title}\" (${formatInr(target.amount)})?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(target.id)
                        deletingExpense = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDue)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingExpense = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ExpenseMetricPill(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = valueColor,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExpenseRowCard(
    expense: Expense,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val category = ExpenseCategory.fromLabel(expense.category)
    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_row_${expense.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IosSquircleIcon(
                icon = expenseCategoryIcon(category),
                containerColor = Color(category.colorHex),
                size = 40.dp,
                iconSize = 20.dp
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${category.label} · ${expense.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = expense.paidMode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (expense.note.isNotBlank()) {
                    Text(
                        text = expense.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-${formatInr(expense.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = CrimsonDue,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("delete_expense_${expense.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete expense",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseFormDialog(
    initial: Expense?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        category: ExpenseCategory,
        amount: Double,
        date: String,
        paidMode: PaymentMode,
        note: String
    ) -> Unit
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var category by remember(initial) {
        mutableStateOf(ExpenseCategory.fromLabel(initial?.category ?: ExpenseCategory.FUEL.label))
    }
    var amountText by remember(initial) {
        mutableStateOf(
            if (initial != null && initial.amount > 0.0) {
                if (initial.amount == initial.amount.toLong().toDouble()) {
                    initial.amount.toLong().toString()
                } else {
                    String.format(Locale.US, "%.2f", initial.amount)
                }
            } else ""
        )
    }
    var date by remember(initial) { mutableStateOf(initial?.date?.ifBlank { todayIsoDate() } ?: todayIsoDate()) }
    var paidMode by remember(initial) {
        mutableStateOf(PaymentMode.fromLabel(initial?.paidMode ?: PaymentMode.CASH.label).let {
            if (it == PaymentMode.SETTLEMENT) PaymentMode.CASH else it
        })
    }
    var note by remember(initial) { mutableStateOf(initial?.note.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initial == null) "Log Expense" else "Edit Expense",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                    }
                }

                // Quick Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EXPENSE_PRESETS.forEach { (presetTitle, presetCat) ->
                        FilterChip(
                            selected = title.equals(presetTitle, ignoreCase = true),
                            onClick = {
                                title = presetTitle
                                category = presetCat
                                validationError = null
                            },
                            label = { Text(presetTitle) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("Expense Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ExpenseCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(cat.colorHex).copy(alpha = 0.16f),
                                selectedLabelColor = Color(cat.colorHex)
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            validationError = null
                        },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("expense_amount_input")
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker = true }
                    ) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Date") },
                            trailingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMode.selectableModes.forEach { mode ->
                        FilterChip(
                            selected = paidMode == mode,
                            onClick = { paidMode = mode },
                            label = { Text(mode.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_note_input")
                )

                if (!validationError.isNullOrBlank()) {
                    Text(
                        text = validationError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonDue
                    )
                }

                Button(
                    onClick = {
                        val parsedAmount = amountText.toDoubleOrNull()
                        if (title.trim().isEmpty()) {
                            validationError = "Enter expense title"
                            return@Button
                        }
                        if (parsedAmount == null || parsedAmount <= 0.0) {
                            validationError = "Enter an amount greater than 0"
                            return@Button
                        }
                        onSave(title.trim(), category, parsedAmount, date, paidMode, note.trim())
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_expense_button")
                ) {
                    Text(if (initial == null) "Save Expense" else "Update Expense", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDatePicker) {
        AshaDatePickerDialog(
            initialDateIso = date,
            onDateSelected = { date = it },
            onDismiss = { showDatePicker = false }
        )
    }
}

private fun expenseCategoryIcon(category: ExpenseCategory): ImageVector = when (category) {
    ExpenseCategory.FUEL -> Icons.Rounded.LocalGasStation
    ExpenseCategory.MAINTENANCE -> Icons.Rounded.Build
    ExpenseCategory.ELECTRICITY -> Icons.Rounded.Bolt
    ExpenseCategory.SALARY -> Icons.Rounded.Payments
    ExpenseCategory.PARTS -> Icons.Rounded.Engineering
    ExpenseCategory.OTHER -> Icons.Rounded.Receipt
}
