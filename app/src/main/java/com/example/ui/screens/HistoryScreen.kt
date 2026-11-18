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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyRupee
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
import com.example.data.model.BillStatus
import com.example.data.model.BillingDashboardData
import com.example.data.model.PaymentMode
import com.example.data.model.formatInr
import com.example.ui.components.AshaPanelCard
import com.example.ui.components.BillRowItem
import com.example.ui.components.IosSquircleIcon
import com.example.ui.theme.AmberBadgeBg
import com.example.ui.theme.AmberBadgeFg
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDue
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PaidBadgeBg
import com.example.ui.theme.PaidBadgeFg
import com.example.util.BillDesignModel
import com.example.util.BillPrintSize
import com.example.util.InvoicePrinter

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Bills, 1 = Payments, 2 = Village Due
    var selectedStatus by remember { mutableStateOf<BillStatus?>(null) }
    var selectedSeason by remember { mutableStateOf("All Seasons") }

    val billMap = remember(data.bills) { data.bills.associateBy { it.id } }
    val customerMap = remember(data.customers) { data.customers.associateBy { it.id } }

    val seasonOptions = remember(data.bills) {
        listOf("All Seasons") + data.bills
            .map { it.season.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val totalBilled = remember(data.bills) { data.bills.sumOf { it.current } }
    val totalCollected = remember(data.payments) { data.payments.sumOf { it.amount } }
    val totalOutstanding = remember(data.customers, data.bills, data.payments) {
        data.customers.sumOf { data.dueForCustomer(it.id) }
    }
    val collectionEfficiency = if (totalBilled > 0.0) {
        (totalCollected / totalBilled).toFloat().coerceIn(0f, 1f)
    } else 0f

    val filteredBills = remember(data.bills, data.payments, searchQuery, selectedStatus, selectedSeason) {
        val q = searchQuery.trim().lowercase()
        data.bills.filter { bill ->
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
        }.sortedByDescending { it.date }
    }

    val filteredPayments = remember(data.payments, billMap, customerMap, searchQuery) {
        val q = searchQuery.trim().lowercase()
        data.payments
            .sortedByDescending { it.date }
            .filter { p ->
                val bill = billMap[p.billId]
                val cust = customerMap[p.customerId]
                q.isEmpty() || listOf(
                    cust?.name.orEmpty(),
                    cust?.village.orEmpty(),
                    bill?.customerName.orEmpty(),
                    bill?.billNo.orEmpty(),
                    p.mode,
                    p.receivedIn
                ).joinToString(" ").lowercase().contains(q)
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
        // Premium History Header + Summary KPI Strip + Collection Progress + Relocated P&L / Expenses Pills
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
                            icon = Icons.Rounded.History,
                            containerColor = Color(0xFF5856D6),
                            size = 38.dp,
                            iconSize = 20.dp
                        )
                        Column {
                            Text(
                                text = "History",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${data.bills.size} bills · ${data.payments.size} payments",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = onOpenMonthlyAnalytics,
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            modifier = Modifier.testTag("history_pnl_chart_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
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
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            onClick = onOpenExpenses,
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.testTag("history_expenses_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = "Expenses",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Cost",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Financial KPI Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistorySummaryPill(
                        label = "Billed",
                        value = formatInr(totalBilled),
                        bg = MaterialTheme.colorScheme.surfaceVariant,
                        fg = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    HistorySummaryPill(
                        label = "Collected",
                        value = formatInr(totalCollected),
                        bg = PaidBadgeBg,
                        fg = PaidBadgeFg,
                        modifier = Modifier.weight(1f)
                    )
                    HistorySummaryPill(
                        label = "Due",
                        value = formatInr(totalOutstanding),
                        bg = CrimsonContainer,
                        fg = CrimsonDue,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Collection Rate Meter
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Overall Recovery Rate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(collectionEfficiency * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { collectionEfficiency },
                            color = EmeraldPrimary,
                            trackColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(6.dp))
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
                    shape = RoundedCornerShape(14.dp),
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
                        Text("Bills (${data.bills.size})", fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("history_tab_payments")
                    ) {
                        Text("Pay (${data.payments.size})", fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.testTag("history_tab_villages")
                    ) {
                        Text("Village (${villageSummaries.size})", fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }

                if (selectedTab == 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                    if (seasonOptions.size > 1) {
                        Spacer(modifier = Modifier.height(4.dp))
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
        }

        // Content List
        item {
            AshaPanelCard {
                when (selectedTab) {
                    0 -> {
                        if (filteredBills.isEmpty()) {
                            EmptyHistoryState(
                                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                                title = "No bills found",
                                subtitle = if (searchQuery.isNotBlank() || selectedStatus != null) {
                                    "Try clearing your search or filters"
                                } else {
                                    ""
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
                                        customQrPath = customQrPath
                                    )
                                    if (idx < filteredBills.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        if (filteredPayments.isEmpty()) {
                            EmptyHistoryState(
                                icon = Icons.Rounded.AccountBalanceWallet,
                                title = "No payments yet",
                                subtitle = ""
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                filteredPayments.forEach { payment ->
                                    val bill = billMap[payment.billId]
                                    val cust = customerMap[payment.customerId]
                                    val custName = cust?.name ?: bill?.customerName ?: "Customer"
                                    val isSettlement = payment.mode.equals(PaymentMode.SETTLEMENT.label, ignoreCase = true)

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
                                                    containerColor = if (isSettlement) Color(0xFFFF9500) else Color(0xFF34C759),
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
                                            Column(horizontalAlignment = Alignment.End) {
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
                                subtitle = ""
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                villageSummaries.forEach { summary ->
                                    val ratio = if (summary.totalBilled > 0.0) {
                                        (summary.totalCollected / summary.totalBilled).toFloat().coerceIn(0f, 1f)
                                    } else 0f

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
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    IosSquircleIcon(
                                                        icon = Icons.Rounded.LocationOn,
                                                        containerColor = Color(0xFFFF3B30),
                                                        size = 34.dp,
                                                        iconSize = 18.dp
                                                    )
                                                    Column {
                                                        Text(
                                                            text = summary.villageName,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "${summary.customers.size} customers · ${summary.dueCustomerCount} pending due",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

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
                                            }

                                            LinearProgressIndicator(
                                                progress = { ratio },
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

                                            val dueCustomers = summary.customers.filter { data.dueForCustomer(it.id) > 0.001 }
                                            if (dueCustomers.isNotEmpty()) {
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
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.Call,
                                                                    contentDescription = "Call ${cust.name}",
                                                                    tint = EmeraldPrimary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = {
                                                                    val company = data.settings.company.ifBlank { "আশা মিনি শ্যালো" }
                                                                    val msg = "Hello ${cust.name}, reminder from $company: your pending irrigation balance is ${formatInr(custDue)}. Please settle at your convenience."
                                                                    InvoicePrinter.openWhatsAppOrShare(context, cust.phone, msg)
                                                                },
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.NotificationsActive,
                                                                    contentDescription = "Remind ${cust.name}",
                                                                    tint = AmberBadgeFg,
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
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySummaryPill(
    label: String,
    value: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = 0.85f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = fg,
                fontWeight = FontWeight.ExtraBold,
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
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
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
            color = MaterialTheme.colorScheme.onSurface
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
