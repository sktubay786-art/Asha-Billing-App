package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Bill
import com.example.data.model.BillStatus
import com.example.data.model.BillingDashboardData
import com.example.data.model.Payment
import com.example.data.model.PaymentMode
import com.example.data.model.formatInr
import com.example.ui.components.AnimatedBmwPrecisionStripe
import com.example.ui.components.AshaPanelCard
import com.example.ui.components.BillRowItem
import com.example.ui.components.BmwPrecisionStripe
import com.example.ui.components.IosSquircleIcon
import com.example.ui.components.PremiumHistoryIcon
import com.example.ui.components.StaggeredPremiumReveal
import com.example.ui.theme.AmberBadgeBg
import com.example.ui.theme.AmberBadgeFg
import com.example.ui.theme.BmwTelemetryStyle
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDue
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PaidBadgeBg
import com.example.ui.theme.PaidBadgeFg
import androidx.compose.material.icons.rounded.TableView
import com.example.ui.components.LocalAshaHaptics
import com.example.util.BillDesignModel
import com.example.util.BillPrintSize
import com.example.util.CsvReportExportDialog
import com.example.util.InvoicePrinter

private enum class BillSortOption(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST_DUE("Highest Due")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    data: BillingDashboardData,
    defaultBillDesign: BillDesignModel = BillDesignModel.PREMIUM_GREEN,
    defaultPrintSize: BillPrintSize = BillPrintSize.THERMAL_80,
    qrMode: String = "Dynamic amount QR",
    customQrPath: String = "",
    onOpenMonthlyAnalytics: () -> Unit = {},
    onOpenExpenses: () -> Unit = {},
    onViewInvoice: (String) -> Unit,
    onEditBill: ((Bill) -> Unit)? = null,
    onDeleteBill: ((Bill) -> Unit)? = null,
    onEditPayment: ((Payment) -> Unit)? = null,
    onDeletePayment: ((Payment) -> Unit)? = null,
    bengaliLabels: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalAshaHaptics.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Bills, 1 = Payments, 2 = Village Due
    var selectedStatus by remember { mutableStateOf<BillStatus?>(null) }
    var selectedSeason by remember { mutableStateOf("All Seasons") }
    var selectedSort by remember { mutableStateOf(BillSortOption.NEWEST) }
    var selectedPaymentMode by remember { mutableStateOf("All Modes") }
    var expandedVillage by remember { mutableStateOf<String?>(null) }
    var showCsvExportDialog by remember { mutableStateOf(false) }

    val billMap = remember(data.bills) { data.bills.associateBy { it.id } }
    val customerMap = remember(data.customers) { data.customers.associateBy { it.id } }

    val seasonOptions = remember(data.bills) {
        listOf("All Seasons") + data.bills
            .map { it.season.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val paymentModeOptions = remember(data.payments) {
        val base = listOf("All Modes", "Cash", "UPI", "Bank", PaymentMode.SETTLEMENT.label)
        val extra = data.payments.map { it.mode.trim() }.filter { it.isNotBlank() }.distinct()
        (base + extra).distinctBy { it.lowercase() }
    }

    val totalBilled = remember(data.bills) { data.bills.sumOf { it.current } }
    val totalCollected = remember(data.payments) { data.payments.sumOf { it.amount } }
    val totalOutstanding = remember(data.customers, data.bills, data.payments) {
        data.customers.sumOf { data.dueForCustomer(it.id) }
    }
    val collectionEfficiency = if (totalBilled > 0.0) {
        (totalCollected / totalBilled).toFloat().coerceIn(0f, 1f)
    } else 0f
    val animatedEfficiency by animateFloatAsState(
        targetValue = collectionEfficiency,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "history_recovery_progress"
    )

    val filteredBills = remember(data.bills, data.payments, searchQuery, selectedStatus, selectedSeason, selectedSort) {
        val q = searchQuery.trim().lowercase()
        val matched = data.bills.filter { bill ->
            val matchesQuery = q.isEmpty() || listOf(
                bill.billNo,
                bill.customerName,
                bill.phone,
                bill.village,
                bill.season
            ).joinToString(" ").lowercase().contains(q)
            val matchesStatus = selectedStatus == null || data.statusForBill(bill) == selectedStatus
            val matchesSeason = selectedSeason == "All Seasons" ||
                bill.season.equals(selectedSeason, ignoreCase = true)
            matchesQuery && matchesStatus && matchesSeason
        }
        when (selectedSort) {
            BillSortOption.NEWEST -> matched.sortedByDescending { it.date }
            BillSortOption.OLDEST -> matched.sortedBy { it.date }
            BillSortOption.HIGHEST_DUE -> matched.sortedByDescending { data.dueForBill(it) }
        }
    }

    val filteredPayments = remember(data.payments, billMap, customerMap, searchQuery, selectedPaymentMode) {
        val q = searchQuery.trim().lowercase()
        data.payments
            .sortedByDescending { it.date }
            .filter { p ->
                val bill = billMap[p.billId]
                val cust = customerMap[p.customerId]
                val matchesMode = selectedPaymentMode == "All Modes" ||
                    p.mode.equals(selectedPaymentMode, ignoreCase = true)
                val matchesQuery = q.isEmpty() || listOf(
                    cust?.name.orEmpty(),
                    cust?.village.orEmpty(),
                    bill?.customerName.orEmpty(),
                    bill?.billNo.orEmpty(),
                    p.mode,
                    p.receivedIn,
                    p.note
                ).joinToString(" ").lowercase().contains(q)
                matchesMode && matchesQuery
            }
    }

    val villageSummaries = remember(data.customers, data.bills, data.payments, searchQuery) {
        val q = searchQuery.trim().lowercase()
        data.customers
            .groupBy { it.village.trim().ifBlank { "Unspecified Village" } }
            .map { (village, customers) ->
                val custIds = customers.map { it.id }.toSet()
                val vBilled = data.bills.filter { it.customerId in custIds }.sumOf { it.current }
                val vPaid = data.payments.filter { it.customerId in custIds }.sumOf { it.amount }
                val vDue = customers.sumOf { data.dueForCustomer(it.id) }
                val dueCount = customers.count { data.dueForCustomer(it.id) > 0.001 }
                VillageDueSummary(
                    villageName = village,
                    customers = customers,
                    totalBilled = vBilled,
                    totalCollected = vPaid,
                    totalDue = vDue,
                    dueCustomerCount = dueCount
                )
            }
            .filter { q.isEmpty() || it.villageName.lowercase().contains(q) }
            .sortedByDescending { it.totalDue }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Redesigned BMW-Precision History Telemetry Header & Filter Deck
        item {
            StaggeredPremiumReveal(index = 0) {
            AshaPanelCard {
                AnimatedBmwPrecisionStripe(
                    height = 2.5.dp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        PremiumHistoryIcon(
                            size = 42.dp,
                            active = true
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "LEDGER ARCHIVE",
                                style = BmwTelemetryStyle,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "History",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${data.bills.size} bills · ${data.payments.size} receipts · ${villageSummaries.size} villages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = onOpenMonthlyAnalytics,
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                            modifier = Modifier.testTag("history_pnl_chart_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BarChart,
                                    contentDescription = "P&L Chart",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "P&L",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                haptics.tap()
                                onOpenExpenses()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.testTag("history_expenses_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = "Expenses",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Cost",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                haptics.tap()
                                showCsvExportDialog = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = EmeraldPrimary.copy(alpha = 0.14f),
                            modifier = Modifier.testTag("history_csv_export_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.TableView,
                                    contentDescription = "Export CSV Report",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "CSV",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // High-contrast Financial Telemetry Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistorySummaryPill(
                        label = "BILLED",
                        value = formatInr(totalBilled),
                        bg = MaterialTheme.colorScheme.surfaceVariant,
                        fg = MaterialTheme.colorScheme.onSurface,
                        accentBar = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    HistorySummaryPill(
                        label = "COLLECTED",
                        value = formatInr(totalCollected),
                        bg = PaidBadgeBg,
                        fg = PaidBadgeFg,
                        accentBar = PaidBadgeFg,
                        modifier = Modifier.weight(1f)
                    )
                    HistorySummaryPill(
                        label = "DUE",
                        value = formatInr(totalOutstanding),
                        bg = CrimsonContainer,
                        fg = CrimsonDue,
                        accentBar = CrimsonDue,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Animated Collection Recovery Gauge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OVERALL RECOVERY RATE",
                                style = BmwTelemetryStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(collectionEfficiency * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { animatedEfficiency },
                            color = EmeraldPrimary,
                            trackColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_history_hint)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("history_tab_bills")
                    ) {
                        Text("Bills (${data.bills.size})", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("history_tab_payments")
                    ) {
                        Text("Pay (${data.payments.size})", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.testTag("history_tab_villages")
                    ) {
                        Text("Village (${villageSummaries.size})", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                // Contextual filter strip per tab
                AnimatedVisibility(visible = selectedTab == 0) {
                    Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedStatus == null,
                                onClick = { selectedStatus = null },
                                label = { Text("All") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("filter_status_all")
                            )
                            BillStatus.entries.forEach { status ->
                                FilterChip(
                                    selected = selectedStatus == status,
                                    onClick = { selectedStatus = status },
                                    label = { Text(status.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("filter_status_${status.name.lowercase()}")
                                )
                            }
                        }

                        // Sort pills row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = "Sort bills",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            BillSortOption.entries.forEach { sortOpt ->
                                FilterChip(
                                    selected = selectedSort == sortOpt,
                                    onClick = { selectedSort = sortOpt },
                                    label = { Text(sortOpt.label, fontSize = 11.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.testTag("history_sort_${sortOpt.name.lowercase()}")
                                )
                            }
                        }

                        if (seasonOptions.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                seasonOptions.forEach { seasonLabel ->
                                    val active = selectedSeason == seasonLabel
                                    FilterChip(
                                        selected = active,
                                        onClick = { selectedSeason = seasonLabel },
                                        label = { Text(seasonLabel, fontSize = 11.5.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldMint,
                                            selectedLabelColor = EmeraldPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = selectedTab == 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        paymentModeOptions.forEach { modeLabel ->
                            FilterChip(
                                selected = selectedPaymentMode.equals(modeLabel, ignoreCase = true),
                                onClick = { selectedPaymentMode = modeLabel },
                                label = { Text(modeLabel, fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PaidBadgeBg,
                                    selectedLabelColor = PaidBadgeFg
                                ),
                                modifier = Modifier.testTag("history_pay_mode_${modeLabel.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
            }
            }
        }

        // Animated Content Panel
        item {
            AshaPanelCard {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        (fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 24 }) togetherWith fadeOut(tween(150))
                    },
                    label = "history_tab_content"
                ) { tab ->
                    when (tab) {
                        0 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Filtered summary ribbon
                                val filteredSum = remember(filteredBills) { filteredBills.sumOf { it.current } }
                                val filteredDue = remember(filteredBills, data.payments) { filteredBills.sumOf { data.dueForBill(it) } }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SHOWING ${filteredBills.size} BILLS",
                                        style = BmwTelemetryStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Total ${formatInr(filteredSum)} · Due ${formatInr(filteredDue)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                if (filteredBills.isEmpty()) {
                                    EmptyHistoryState(
                                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                                        title = "No bills found",
                                        subtitle = if (searchQuery.isNotBlank() || selectedStatus != null || selectedSeason != "All Seasons") {
                                            "Try clearing your search or filters"
                                        } else {
                                            "Bills you create will appear here"
                                        }
                                    )
                                } else {
                                    Column {
                                        filteredBills.forEachIndexed { idx, bill ->
                                            BillRowItem(
                                                bill = bill,
                                                data = data,
                                                onViewClick = { onViewInvoice(bill.id) },
                                                showQuickActions = true,
                                                defaultDesign = defaultBillDesign,
                                                defaultSize = defaultPrintSize,
                                                qrMode = qrMode,
                                                customQrPath = customQrPath,
                                                bengaliLabels = bengaliLabels,
                                                onEditBill = onEditBill,
                                                onDeleteBill = onDeleteBill
                                            )
                                            if (idx < filteredBills.lastIndex) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                val filteredPaySum = remember(filteredPayments) { filteredPayments.sumOf { it.amount } }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${filteredPayments.size} RECEIPTS (${selectedPaymentMode.uppercase()})",
                                        style = BmwTelemetryStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatInr(filteredPaySum),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = PaidBadgeFg,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                if (filteredPayments.isEmpty()) {
                                    EmptyHistoryState(
                                        icon = Icons.Rounded.AccountBalanceWallet,
                                        title = "No payments found",
                                        subtitle = if (selectedPaymentMode != "All Modes" || searchQuery.isNotBlank()) {
                                            "Try selecting All Modes or clearing search"
                                        } else {
                                            "Recorded customer payments appear here"
                                        }
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        filteredPayments.forEach { payment ->
                                            val bill = billMap[payment.billId]
                                            val cust = customerMap[payment.customerId]
                                            val custName = cust?.name ?: bill?.customerName ?: "Customer"
                                            val isSettlement = payment.mode.equals(PaymentMode.SETTLEMENT.label, ignoreCase = true)
                                            val modeColor = when {
                                                isSettlement -> Color(0xFFFF9500)
                                                payment.mode.equals("UPI", ignoreCase = true) -> Color(0xFF007AFF)
                                                payment.mode.equals("Bank", ignoreCase = true) -> Color(0xFF5856D6)
                                                else -> Color(0xFF34C759)
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable(enabled = bill != null) {
                                                        if (bill != null) onViewInvoice(bill.id)
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        IosSquircleIcon(
                                                            icon = Icons.Rounded.CurrencyRupee,
                                                            containerColor = modeColor,
                                                            size = 38.dp,
                                                            iconSize = 19.dp
                                                        )
                                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                            Text(
                                                                text = custName,
                                                                style = MaterialTheme.typography.titleSmall,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "${bill?.billNo ?: "Ledger"} · ${payment.date}${if (payment.receivedIn.isNotBlank()) " · ${payment.receivedIn}" else ""}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            if (payment.note.isNotBlank()) {
                                                                Text(
                                                                    text = payment.note,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Text(
                                                                text = formatInr(payment.amount),
                                                                style = MaterialTheme.typography.titleSmall,
                                                                color = PaidBadgeFg,
                                                                fontWeight = FontWeight.ExtraBold
                                                            )
                                                            Surface(
                                                                shape = RoundedCornerShape(10.dp),
                                                                color = if (isSettlement) AmberBadgeBg else PaidBadgeBg
                                                            ) {
                                                                Text(
                                                                    text = payment.mode,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = if (isSettlement) AmberBadgeFg else PaidBadgeFg,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                        if (onEditPayment != null) {
                                                            IconButton(
                                                                onClick = { onEditPayment(payment) },
                                                                modifier = Modifier
                                                                    .size(34.dp)
                                                                    .testTag("history_edit_payment_${payment.id}")
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.Edit,
                                                                    contentDescription = "Edit payment",
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                        if (onDeletePayment != null) {
                                                            IconButton(
                                                                onClick = { onDeletePayment(payment) },
                                                                modifier = Modifier
                                                                    .size(34.dp)
                                                                    .testTag("history_delete_payment_${payment.id}")
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                                    contentDescription = "Undo payment",
                                                                    tint = CrimsonDue,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            if (villageSummaries.isEmpty()) {
                                EmptyHistoryState(
                                    icon = Icons.Rounded.LocationOn,
                                    title = "No village records",
                                    subtitle = "Add customers with village names to track area-wise dues"
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "${villageSummaries.size} VILLAGES · TAP TO EXPAND DUES",
                                        style = BmwTelemetryStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    villageSummaries.forEach { summary ->
                                        val ratio = if (summary.totalBilled > 0.0) {
                                            (summary.totalCollected / summary.totalBilled).toFloat().coerceIn(0f, 1f)
                                        } else 0f
                                        val animatedRatio by animateFloatAsState(
                                            targetValue = ratio,
                                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                                            label = "village_ratio_${summary.villageName}"
                                        )
                                        val isExpanded = expandedVillage == summary.villageName
                                        val dueCustomers = summary.customers.filter { data.dueForCustomer(it.id) > 0.001 }

                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            expandedVillage = if (isExpanded) null else summary.villageName
                                                        },
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                                                    ) {
                                                        IosSquircleIcon(
                                                            icon = Icons.Rounded.LocationOn,
                                                            containerColor = Color(0xFFFF3B30),
                                                            size = 36.dp,
                                                            iconSize = 18.dp
                                                        )
                                                        Column {
                                                            Text(
                                                                text = summary.villageName,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "${summary.customers.size} customers · ${summary.dueCustomerCount} pending due",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = if (summary.totalDue > 0.001) CrimsonContainer else PaidBadgeBg
                                                        ) {
                                                            Text(
                                                                text = if (summary.totalDue > 0.001) formatInr(summary.totalDue) else "Settled",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = if (summary.totalDue > 0.001) CrimsonDue else PaidBadgeFg,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                            )
                                                        }
                                                        Icon(
                                                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                            contentDescription = if (isExpanded) "Collapse village" else "Expand village",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                LinearProgressIndicator(
                                                    progress = { animatedRatio },
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "Billed: ${formatInr(summary.totalBilled)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "Collected: ${formatInr(summary.totalCollected)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = PaidBadgeFg,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }

                                                AnimatedVisibility(visible = isExpanded && dueCustomers.isNotEmpty()) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                                        dueCustomers.forEach { cust ->
                                                            val custDue = data.dueForCustomer(cust.id)
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        text = cust.name,
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                    if (cust.phone.isNotBlank()) {
                                                                        Text(
                                                                            text = cust.phone,
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                        )
                                                                    }
                                                                }
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                                ) {
                                                                    Text(
                                                                        text = formatInr(custDue),
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        color = CrimsonDue,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                    IconButton(
                                                                        onClick = { InvoicePrinter.dialCustomer(context, cust.phone) },
                                                                        modifier = Modifier.size(44.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Rounded.Call,
                                                                            contentDescription = "Call ${cust.name}",
                                                                            tint = EmeraldPrimary,
                                                                            modifier = Modifier.size(18.dp)
                                                                        )
                                                                    }
                                                                    IconButton(
                                                                        onClick = {
                                                                            val company = data.settings.company.ifBlank { "আশা মিনি শ্যালো" }
                                                                            val msg = "Hello ${cust.name}, reminder from $company: your pending irrigation balance is ${formatInr(custDue)}. Please settle at your convenience."
                                                                            InvoicePrinter.openWhatsAppOrShare(context, cust.phone, msg)
                                                                        },
                                                                        modifier = Modifier.size(44.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Rounded.NotificationsActive,
                                                                            contentDescription = "Remind ${cust.name}",
                                                                            tint = AmberBadgeFg,
                                                                            modifier = Modifier.size(18.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCsvExportDialog) {
        CsvReportExportDialog(
            data = data,
            onDismiss = { showCsvExportDialog = false }
        )
    }
}

@Composable
private fun HistorySummaryPill(
    label: String,
    value: String,
    bg: Color,
    fg: Color,
    accentBar: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentBar)
            )
            Text(
                text = label,
                style = BmwTelemetryStyle,
                fontSize = 9.sp,
                color = fg.copy(alpha = 0.85f),
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = fg,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyHistoryState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
