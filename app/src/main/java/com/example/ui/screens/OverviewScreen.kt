package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BillingDashboardData
import com.example.data.model.Customer
import com.example.data.model.formatInr
import com.example.ui.components.AnimatedBmwPrecisionStripe
import com.example.ui.components.AnimatedCurrencyText
import com.example.ui.components.AshaPanelCard
import com.example.ui.components.BillRowItem
import com.example.ui.components.BmwPrecisionStripe
import com.example.ui.components.IosSquircleIcon
import com.example.ui.components.PremiumBillIcon
import com.example.ui.components.PremiumCalculatorIcon
import com.example.ui.components.PremiumElectricIcon
import com.example.ui.components.PremiumHistoryIcon
import com.example.ui.components.PremiumPayIcon
import com.example.ui.components.PremiumUserIcon
import com.example.ui.components.PulsingLiveDot
import com.example.ui.components.StaggeredPremiumReveal
import com.example.ui.theme.AmberBadgeBg
import com.example.ui.theme.AmberBadgeFg
import com.example.ui.theme.BmwMetricNumberStyle
import com.example.ui.theme.BmwTelemetryStyle
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDue
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PaidBadgeBg
import com.example.ui.theme.PaidBadgeFg
import com.example.util.BillDesignModel
import com.example.util.BillPrintSize
import com.example.util.InvoicePrinter
import kotlin.math.max

data class VillageDueSummary(
    val villageName: String,
    val customers: List<Customer>,
    val totalBilled: Double,
    val totalCollected: Double,
    val totalDue: Double,
    val dueCustomerCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    data: BillingDashboardData,
    isWideLayout: Boolean,
    isDarkTheme: Boolean = false,
    defaultBillDesign: BillDesignModel = BillDesignModel.PREMIUM_GREEN,
    defaultPrintSize: BillPrintSize = BillPrintSize.THERMAL_80,
    qrMode: String = "Dynamic amount QR",
    customQrPath: String = "",
    onToggleTheme: () -> Unit = {},
    onCreateBill: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenCustomerChat: (Customer) -> Unit,
    onReceivePayment: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenWbsedcl: () -> Unit = {},
    onOpenExpenses: () -> Unit = {},
    onOpenMonthlyAnalytics: () -> Unit = {},
    onViewHistory: () -> Unit,
    onViewInvoice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedVillage by remember { mutableStateOf("All villages") }
    var villageExpanded by remember { mutableStateOf(false) }
    var selectedSeason by remember { mutableStateOf("All seasons") }
    var seasonExpanded by remember { mutableStateOf(false) }
    var expandedVillageCard by remember { mutableStateOf<String?>(null) }

    val villageOptions = remember(data.customers) {
        listOf("All villages") + data.customers
            .map { it.village.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val seasonOptions = remember(data.bills) {
        listOf("All seasons") + data.bills
            .map { it.season.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val filteredCustomers = remember(data.customers, selectedVillage) {
        if (selectedVillage == "All villages") data.customers
        else data.customers.filter { it.village.equals(selectedVillage, ignoreCase = true) }
    }
    val filteredCustomerIds = remember(filteredCustomers) { filteredCustomers.map { it.id }.toSet() }
    val filteredBills = remember(data.bills, filteredCustomerIds, selectedVillage, selectedSeason) {
        data.bills.filter { bill ->
            val matchesVillage = selectedVillage == "All villages" ||
                bill.customerId in filteredCustomerIds ||
                bill.village.equals(selectedVillage, ignoreCase = true)
            val matchesSeason = selectedSeason == "All seasons" ||
                bill.season.equals(selectedSeason, ignoreCase = true)
            matchesVillage && matchesSeason
        }
    }

    val totalBilled = filteredBills.sumOf { it.allTotal }
    val totalCollected = filteredBills.sumOf { data.paidForBill(it.id) }
    val totalDue = if (selectedSeason == "All seasons") {
        filteredCustomers.sumOf { data.dueForCustomer(it.id) }
    } else {
        filteredBills.sumOf { data.dueForBill(it) }
    }
    val collectionRatio = if (totalBilled > 0.0) (totalCollected / totalBilled).toFloat().coerceIn(0f, 1f) else 0f
    val animatedCollectionRatio by animateFloatAsState(
        targetValue = collectionRatio,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "overview_collection_ratio"
    )
    val recentBills = filteredBills.sortedByDescending { it.date }.take(5)

    val villageSummaries = remember(data.customers, data.bills, data.payments) {
        data.customers
            .groupBy { it.village.trim().ifBlank { "Unspecified Village" } }
            .map { (village, custList) ->
                val custIds = custList.map { it.id }.toSet()
                val vBills = data.bills.filter { it.customerId in custIds }
                val billed = vBills.sumOf { it.allTotal }
                val collected = vBills.sumOf { data.paidForBill(it.id) }
                val due = custList.sumOf { data.dueForCustomer(it.id) }
                val dueCusts = custList.count { data.dueForCustomer(it.id) > 0.001 }
                VillageDueSummary(
                    villageName = village,
                    customers = custList.sortedByDescending { data.dueForCustomer(it.id) },
                    totalBilled = billed,
                    totalCollected = collected,
                    totalDue = due,
                    dueCustomerCount = dueCusts
                )
            }
            .sortedByDescending { it.totalDue }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // V103 Top Dashboard Header + Filters + Collection Bar Card
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
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "COMMAND DECK",
                            style = BmwTelemetryStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Overview",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = onToggleTheme,
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.testTag("overview_theme_toggle_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = "Toggle Light or Dark mode",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isDarkTheme) "Light" else "Dark",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = PaidBadgeBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                PulsingLiveDot(color = PaidBadgeFg, size = 7.dp)
                                Text(
                                    text = "Live",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PaidBadgeFg,
                                    fontWeight = FontWeight.Bold
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
                    ExposedDropdownMenuBox(
                        expanded = villageExpanded,
                        onExpandedChange = { villageExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedVillage,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = villageExpanded) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("overview_village_filter")
                        )
                        ExposedDropdownMenu(
                            expanded = villageExpanded,
                            onDismissRequest = { villageExpanded = false }
                        ) {
                            villageOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedVillage = option
                                        villageExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = seasonExpanded,
                        onExpandedChange = { seasonExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedSeason,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = seasonExpanded) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("overview_season_filter")
                        )
                        ExposedDropdownMenu(
                            expanded = seasonExpanded,
                            onDismissRequest = { seasonExpanded = false }
                        ) {
                            seasonOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedSeason = option
                                        seasonExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Collection Progress Bar
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Collection Efficiency",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${(collectionRatio * 100).toInt()}% Collected",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { animatedCollectionRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
            }
        }

        // V103 2x2 KPI Cards
        item {
            StaggeredPremiumReveal(index = 1) {
            if (isWideLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    V103KpiCard("Total Bill", formatInr(totalBilled), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f), currencyAmount = totalBilled)
                    V103KpiCard("Collected", formatInr(totalCollected), PaidBadgeFg, Modifier.weight(1f), currencyAmount = totalCollected)
                    V103KpiCard("Total Due", formatInr(max(0.0, totalDue)), if (totalDue > 0.001) CrimsonDue else PaidBadgeFg, Modifier.weight(1f), currencyAmount = max(0.0, totalDue))
                    V103KpiCard("Customers", filteredCustomers.size.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        V103KpiCard("Total Bill", formatInr(totalBilled), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f), currencyAmount = totalBilled)
                        V103KpiCard("Collected", formatInr(totalCollected), PaidBadgeFg, Modifier.weight(1f), currencyAmount = totalCollected)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        V103KpiCard("Total Due", formatInr(max(0.0, totalDue)), if (totalDue > 0.001) CrimsonDue else PaidBadgeFg, Modifier.weight(1f), currencyAmount = max(0.0, totalDue))
                        V103KpiCard("Customers", filteredCustomers.size.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }
                }
            }
            }
        }

        // Profit & Loss Overview Card (Revenue vs Operational Expenses)
        item {
            val allCollected = data.totalCollected
            val totalExp = data.totalExpenses
            val netProfit = data.netCollectedProfit
            val isProfit = netProfit >= 0.0
            val denom = (allCollected + totalExp).coerceAtLeast(1.0)
            val collectedShare = (allCollected / denom).toFloat().coerceIn(0f, 1f)
            val animatedCollectedShare by animateFloatAsState(
                targetValue = collectedShare,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "overview_pnl_share"
            )

            AshaPanelCard(
                modifier = Modifier.testTag("overview_profit_loss_card")
            ) {
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
                            icon = if (isProfit) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                            containerColor = if (isProfit) PaidBadgeFg else CrimsonDue,
                            size = 38.dp,
                            iconSize = 20.dp
                        )
                        Column {
                            Text(
                                text = "FINANCIAL TELEMETRY",
                                style = BmwTelemetryStyle,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Profit & Loss",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${data.expenses.size} expenses logged",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(
                            onClick = onOpenMonthlyAnalytics,
                            modifier = Modifier.testTag("overview_monthly_chart_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BarChart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chart", fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = onOpenExpenses,
                            modifier = Modifier.testTag("overview_manage_expenses_button")
                        ) {
                            Text("Expenses →", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfitMiniStat(
                        label = "Collected",
                        value = formatInr(allCollected),
                        color = PaidBadgeFg,
                        modifier = Modifier.weight(1f)
                    )
                    ProfitMiniStat(
                        label = "Expenses",
                        value = formatInr(totalExp),
                        color = CrimsonDue,
                        modifier = Modifier.weight(1f)
                    )
                    ProfitMiniStat(
                        label = if (isProfit) "Net Profit" else "Net Loss",
                        value = (if (isProfit) "+" else "-") + formatInr(kotlin.math.abs(netProfit)),
                        color = if (isProfit) EmeraldPrimary else CrimsonDue,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { animatedCollectedShare },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = PaidBadgeFg,
                    trackColor = CrimsonContainer
                )
            }
        }

        // iOS 2-Column Action Grid with bespoke crafted icons
        item {
            StaggeredPremiumReveal(index = 2) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    V103ActionTile(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        badgeColor = Color(0xFF5856D6),
                        title = "New Bill",
                        subtitle = "Create invoice",
                        customLeading = { PremiumBillIcon(size = 40.dp, active = true) },
                        onClick = onCreateBill,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_create_bill_button")
                    )
                    V103ActionTile(
                        icon = Icons.Rounded.People,
                        badgeColor = Color(0xFF007AFF),
                        title = "Customers",
                        subtitle = "Ledger chat",
                        customLeading = { PremiumUserIcon(size = 40.dp, active = true) },
                        onClick = onOpenUsers,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_customers_button")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    V103ActionTile(
                        icon = Icons.Rounded.CurrencyRupee,
                        badgeColor = Color(0xFF16A34A),
                        title = "Pay / Settle",
                        subtitle = "Receive payment",
                        customLeading = { PremiumPayIcon(size = 40.dp, active = true) },
                        onClick = onReceivePayment,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_receive_payment_button")
                    )
                    V103ActionTile(
                        icon = Icons.Rounded.BarChart,
                        badgeColor = Color(0xFF9333EA),
                        title = "History",
                        subtitle = "Bills & dues",
                        customLeading = { PremiumHistoryIcon(size = 40.dp, active = true) },
                        onClick = onViewHistory,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_reports_button")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    V103ActionTile(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        badgeColor = Color(0xFFEA580C),
                        title = "Expenses",
                        subtitle = "Operational costs",
                        onClick = onOpenExpenses,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_expenses_button")
                    )
                    V103ActionTile(
                        icon = Icons.Rounded.BarChart,
                        badgeColor = Color(0xFF0284C7),
                        title = "P&L Chart",
                        subtitle = "Monthly analytics",
                        onClick = onOpenMonthlyAnalytics,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_monthly_chart_button")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    V103ActionTile(
                        icon = Icons.Rounded.ElectricBolt,
                        badgeColor = Color(0xFFD97706),
                        title = "Electricity Bill",
                        subtitle = if (data.wbsedclConsumers.isNotEmpty()) {
                            "WBSEDCL · ${data.wbsedclConsumers.size} IDs"
                        } else {
                            "WBSEDCL Auto-Pay"
                        },
                        customLeading = { PremiumElectricIcon(size = 40.dp, active = true) },
                        onClick = onOpenWbsedcl,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_wbsedcl_button")
                    )
                    V103ActionTile(
                        icon = Icons.Rounded.Calculate,
                        badgeColor = Color(0xFFFF9500),
                        title = "ASHA Calculator",
                        subtitle = "Land & Rate math",
                        customLeading = { PremiumCalculatorIcon(size = 40.dp, active = true) },
                        onClick = onOpenCalculator,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_calculator_button")
                    )
                }
            }
            }
        }

        // Village-Wise Due List & Breakdown Card
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
                            icon = Icons.Rounded.LocationOn,
                            containerColor = Color(0xFFFF3B30),
                            size = 36.dp,
                            iconSize = 19.dp
                        )
                        Column {
                            Text(
                                text = "Village Dues",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${villageSummaries.size} villages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (villageSummaries.isEmpty()) {
                    Text(
                        text = "No village records yet. Add customers with village names.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        villageSummaries.forEach { summary ->
                            val isExpanded = expandedVillageCard == summary.villageName
                            val hasDue = summary.totalDue > 0.001

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("village_due_card_${summary.villageName}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedVillageCard = if (isExpanded) null else summary.villageName
                                            },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = summary.villageName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${summary.customers.size} customers · ${summary.dueCustomerCount} with due · Paid ${formatInr(summary.totalCollected)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (hasDue) CrimsonContainer else PaidBadgeBg
                                            ) {
                                                Text(
                                                    text = if (hasDue) formatInr(summary.totalDue) else "Cleared",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (hasDue) CrimsonDue else PaidBadgeFg,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                contentDescription = "Expand village",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(
                                            modifier = Modifier.padding(top = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            summary.customers.forEach { customer ->
                                                val custDue = data.dueForCustomer(customer.id)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable { onOpenCustomerChat(customer) }
                                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = customer.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (customer.phone.isNotBlank()) {
                                                            Text(
                                                                text = customer.phone,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = if (custDue > 0.001) formatInr(custDue) else "₹0",
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = if (custDue > 0.001) CrimsonDue else PaidBadgeFg,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        if (custDue > 0.001) {
                                                            IconButton(
                                                                onClick = {
                                                                    val msg = InvoicePrinter.buildReminderText(customer, custDue)
                                                                    InvoicePrinter.openWhatsAppOrShare(context, customer.phone, msg)
                                                                },
                                                                modifier = Modifier
                                                                    .size(32.dp)
                                                                    .clip(CircleShape)
                                                                    .background(AmberBadgeBg)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.NotificationsActive,
                                                                    contentDescription = "Send reminder",
                                                                    tint = AmberBadgeFg,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            if (summary.villageName != "Unspecified Village") {
                                                TextButton(
                                                    onClick = { selectedVillage = summary.villageName },
                                                    modifier = Modifier.align(Alignment.End)
                                                ) {
                                                    Text("Filter Dashboard by ${summary.villageName}")
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

        // Recent Bills Feed with quick bill icons
        item {
            AshaPanelCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recent_bills),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = onViewHistory,
                        modifier = Modifier.testTag("view_all_history_button")
                    ) {
                        Text(stringResource(R.string.view_all))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                if (recentBills.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_bills_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    recentBills.forEachIndexed { idx, bill ->
                        BillRowItem(
                            bill = bill,
                            data = data,
                            showQuickActions = true,
                            defaultDesign = defaultBillDesign,
                            defaultSize = defaultPrintSize,
                            qrMode = qrMode,
                            customQrPath = customQrPath,
                            onViewClick = { onViewInvoice(bill.id) }
                        )
                        if (idx < recentBills.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V103KpiCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    currencyAmount: Double? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(valueColor.copy(alpha = 0.85f))
            )
            Text(
                text = label.uppercase(),
                style = BmwTelemetryStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (currencyAmount != null) {
                AnimatedCurrencyText(
                    amount = currencyAmount,
                    style = BmwMetricNumberStyle,
                    color = valueColor
                )
            } else {
                Text(
                    text = value,
                    style = BmwMetricNumberStyle,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProfitMiniStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
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
                color = color,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun V103ActionTile(
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    subtitle: String,
    customLeading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "action_tile_press_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(pressScale)
            .height(106.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = if (isPressed) 1.dp else 3.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = badgeColor.copy(alpha = 0.09f),
                    radius = 44.dp.toPx(),
                    center = Offset(size.width, 0f)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (customLeading != null) {
                    customLeading()
                } else {
                    IosSquircleIcon(
                        icon = icon,
                        containerColor = badgeColor,
                        size = 40.dp,
                        iconSize = 21.dp
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
