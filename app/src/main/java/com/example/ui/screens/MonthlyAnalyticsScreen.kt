package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BillingDashboardData
import com.example.data.model.formatInr
import com.example.ui.components.AshaPanelCard
import com.example.ui.components.IosSquircleIcon
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDue
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PaidBadgeBg
import com.example.ui.theme.PaidBadgeFg
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class MonthlyPnLPoint(
    val monthKey: String,        // "YYYY-MM"
    val shortLabel: String,      // "Nov 26"
    val fullLabel: String,       // "Nov 2026"
    val collectedRevenue: Double,
    val billedRevenue: Double,
    val operationalCosts: Double,
    val expenseCount: Int,
    val paymentCount: Int,
    val billCount: Int
) {
    fun revenueFor(useBilled: Boolean): Double = if (useBilled) billedRevenue else collectedRevenue
    fun netProfitFor(useBilled: Boolean): Double = revenueFor(useBilled) - operationalCosts
}

@Composable
fun MonthlyAnalyticsScreen(
    data: BillingDashboardData,
    onOpenExpenses: () -> Unit,
    modifier: Modifier = Modifier
) {
    var periodFilterMonths by remember { mutableIntStateOf(6) } // 6, 12, or 0 for All
    var useBilledRevenue by remember { mutableStateOf(false) }

    val allMonthlyPoints = remember(data.bills, data.payments, data.expenses) {
        buildMonthlyPoints(data)
    }

    val visiblePoints = remember(allMonthlyPoints, periodFilterMonths) {
        if (periodFilterMonths <= 0 || allMonthlyPoints.size <= periodFilterMonths) {
            allMonthlyPoints
        } else {
            allMonthlyPoints.takeLast(periodFilterMonths)
        }
    }

    var selectedMonthKey by remember(visiblePoints) {
        mutableStateOf(visiblePoints.lastOrNull()?.monthKey)
    }
    val selectedPoint = remember(visiblePoints, selectedMonthKey) {
        visiblePoints.firstOrNull { it.monthKey == selectedMonthKey } ?: visiblePoints.lastOrNull()
    }

    val totalRev = visiblePoints.sumOf { it.revenueFor(useBilledRevenue) }
    val totalCosts = visiblePoints.sumOf { it.operationalCosts }
    val totalNet = totalRev - totalCosts
    val isOverallProfit = totalNet >= 0.0
    val marginPct = if (totalRev > 0.0) ((totalNet / totalRev) * 100.0) else 0.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("monthly_analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Metric & Period Selectors
        item {
            AshaPanelCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IosSquircleIcon(
                            icon = Icons.Rounded.BarChart,
                            containerColor = EmeraldPrimary,
                            size = 38.dp,
                            iconSize = 20.dp
                        )
                        Column {
                            Text(
                                text = "Monthly Profit & Loss",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Revenue vs Operational Costs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    TextButton(
                        onClick = onOpenExpenses,
                        modifier = Modifier.testTag("analytics_open_expenses_button")
                    ) {
                        Text("Expenses →", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Revenue Source Toggle: Collected vs Billed
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        selected = !useBilledRevenue,
                        onClick = { useBilledRevenue = false },
                        modifier = Modifier.testTag("analytics_mode_collected")
                    ) {
                        Text("Collected Cash", fontWeight = FontWeight.SemiBold)
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        selected = useBilledRevenue,
                        onClick = { useBilledRevenue = true },
                        modifier = Modifier.testTag("analytics_mode_billed")
                    ) {
                        Text("Billed Work", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Period Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(6 to "Last 6M", 12 to "Last 12M", 0 to "All Time").forEach { (months, label) ->
                        FilterChip(
                            selected = periodFilterMonths == months,
                            onClick = { periodFilterMonths = months },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldMint,
                                selectedLabelColor = EmeraldPrimary
                            ),
                            modifier = Modifier.testTag("analytics_period_${months}")
                        )
                    }
                }
            }
        }

        // Top 3 Summary KPIs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsSummaryCard(
                    title = if (useBilledRevenue) "Billed Rev." else "Collected",
                    value = formatInr(totalRev),
                    sub = "${visiblePoints.size} mo",
                    accent = PaidBadgeFg,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsSummaryCard(
                    title = "Op. Costs",
                    value = formatInr(totalCosts),
                    sub = "${visiblePoints.sumOf { it.expenseCount }} entries",
                    accent = CrimsonDue,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsSummaryCard(
                    title = if (isOverallProfit) "Net Profit" else "Net Loss",
                    value = (if (isOverallProfit) "+" else "-") + formatInr(abs(totalNet)),
                    sub = String.format(Locale.US, "%.0f%% margin", marginPct),
                    accent = if (isOverallProfit) EmeraldPrimary else CrimsonDue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Interactive Visual Multi-Series Bar + Net Profit Line Chart
        item {
            AshaPanelCard(modifier = Modifier.testTag("monthly_pnl_chart_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Performance Chart",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap a month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Legend Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegendDot(color = EmeraldPrimary, label = if (useBilledRevenue) "Billed" else "Revenue")
                    ChartLegendDot(color = CrimsonDue, label = "Op. Costs")
                    ChartLegendLine(color = Color(0xFFF59E0B), label = "Net Profit")
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (visiblePoints.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IosSquircleIcon(
                            icon = Icons.Rounded.BarChart,
                            containerColor = Color(0xFF64748B),
                            size = 44.dp,
                            iconSize = 22.dp
                        )
                        Text(
                            text = "No monthly revenue or expense data yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    MonthlyProfitComboChart(
                        points = visiblePoints,
                        useBilledRevenue = useBilledRevenue,
                        selectedMonthKey = selectedPoint?.monthKey,
                        onSelectMonth = { key -> selectedMonthKey = key },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                }

                // Selected Month Inspector Card
                selectedPoint?.let { sel ->
                    Spacer(modifier = Modifier.height(12.dp))
                    val rev = sel.revenueFor(useBilledRevenue)
                    val net = sel.netProfitFor(useBilledRevenue)
                    val profitPositive = net >= 0.0
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analytics_selected_month_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${sel.fullLabel} Breakdown",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (profitPositive) PaidBadgeBg else CrimsonContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (profitPositive) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                            contentDescription = null,
                                            tint = if (profitPositive) PaidBadgeFg else CrimsonDue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = (if (profitPositive) "+" else "-") + formatInr(abs(net)),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (profitPositive) PaidBadgeFg else CrimsonDue,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Revenue (${sel.paymentCount} pay · ${sel.billCount} bills)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatInr(rev),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = PaidBadgeFg,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Op. Costs (${sel.expenseCount} items)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatInr(sel.operationalCosts),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = CrimsonDue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Monthly Ledger Table Cards
        if (visiblePoints.isNotEmpty()) {
            item {
                Text(
                    text = "Monthly Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            items(visiblePoints.reversed(), key = { it.monthKey }) { pt ->
                val rev = pt.revenueFor(useBilledRevenue)
                val net = pt.netProfitFor(useBilledRevenue)
                val profitPositive = net >= 0.0
                val isSelected = selectedPoint?.monthKey == pt.monthKey

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shadowElevation = if (isSelected) 3.dp else 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { selectedMonthKey = pt.monthKey }
                        .testTag("analytics_month_row_${pt.monthKey}")
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
                            Text(
                                text = pt.fullLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (profitPositive) PaidBadgeBg else CrimsonContainer
                            ) {
                                Text(
                                    text = "${if (profitPositive) "Profit +" else "Loss -"}${formatInr(abs(net))}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (profitPositive) PaidBadgeFg else CrimsonDue,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Revenue",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatInr(rev),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PaidBadgeFg,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Op. Costs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatInr(pt.operationalCosts),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CrimsonDue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Margin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val monthMargin = if (rev > 0.0) (net / rev) * 100.0 else 0.0
                                Text(
                                    text = String.format(Locale.US, "%.0f%%", monthMargin),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyProfitComboChart(
    points: List<MonthlyPnLPoint>,
    useBilledRevenue: Boolean,
    selectedMonthKey: String?,
    onSelectMonth: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val selectedBg = EmeraldPrimary.copy(alpha = 0.08f)

    val revColor = EmeraldPrimary
    val costColor = CrimsonDue
    val netLineColor = Color(0xFFF59E0B)

    val maxPositive = remember(points, useBilledRevenue) {
        val raw = points.maxOfOrNull {
            max(it.revenueFor(useBilledRevenue), max(it.operationalCosts, it.netProfitFor(useBilledRevenue)))
        } ?: 0.0
        niceScaleCeiling(raw.coerceAtLeast(1000.0))
    }
    val minNegative = remember(points, useBilledRevenue) {
        val raw = points.minOfOrNull { it.netProfitFor(useBilledRevenue) } ?: 0.0
        if (raw < -1.0) -niceScaleCeiling(abs(raw)) else 0.0
    }
    val totalRange = (maxPositive - minNegative).coerceAtLeast(1.0)

    Canvas(
        modifier = modifier
            .pointerInput(points) {
                detectTapGestures { offset ->
                    if (points.isEmpty()) return@detectTapGestures
                    val leftPad = 48.dp.toPx()
                    val rightPad = 12.dp.toPx()
                    val plotW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                    val relX = (offset.x - leftPad).coerceIn(0f, plotW - 1f)
                    val slotW = plotW / points.size
                    val index = (relX / slotW).toInt().coerceIn(0, points.lastIndex)
                    onSelectMonth(points[index].monthKey)
                }
            }
    ) {
        val leftPad = 48.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 14.dp.toPx()
        val bottomPad = 30.dp.toPx()

        val plotW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
        val plotH = (size.height - topPad - bottomPad).coerceAtLeast(1f)

        fun yForValue(v: Double): Float {
            val fraction = ((v - minNegative) / totalRange).toFloat().coerceIn(0f, 1f)
            return topPad + plotH * (1f - fraction)
        }

        val zeroY = yForValue(0.0)

        // Horizontal Y-axis gridlines & INR compact ticks
        val gridSteps = 4
        for (i in 0..gridSteps) {
            val valAtStep = minNegative + (totalRange * i / gridSteps)
            val y = yForValue(valAtStep)
            val isZero = abs(valAtStep) < (totalRange * 0.02)
            drawLine(
                color = if (isZero) axisLabelColor.copy(alpha = 0.45f) else gridColor,
                start = Offset(leftPad, y),
                end = Offset(size.width - rightPad, y),
                strokeWidth = if (isZero) 1.4.dp.toPx() else 0.8.dp.toPx(),
                pathEffect = if (isZero) null else PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            val label = formatCompactInr(valAtStep)
            val measured = textMeasurer.measure(
                text = label,
                style = TextStyle(fontSize = 10.sp, color = axisLabelColor, fontWeight = FontWeight.Medium)
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = (leftPad - measured.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                    y = (y - measured.size.height / 2f).coerceIn(0f, size.height - measured.size.height)
                )
            )
        }

        val slotW = plotW / points.size
        val barWidth = min(18.dp.toPx(), slotW * 0.30f).coerceAtLeast(5.dp.toPx())
        val barGap = min(4.dp.toPx(), slotW * 0.08f)
        val lineAnchors = mutableListOf<Offset>()

        points.forEachIndexed { idx, pt ->
            val slotLeft = leftPad + idx * slotW
            val centerX = slotLeft + slotW / 2f
            val isSelected = pt.monthKey == selectedMonthKey

            if (isSelected) {
                drawRoundRect(
                    color = selectedBg,
                    topLeft = Offset(slotLeft + 2.dp.toPx(), topPad),
                    size = Size(slotW - 4.dp.toPx(), plotH),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }

            // Revenue Bar
            val revVal = pt.revenueFor(useBilledRevenue)
            val revTopY = yForValue(revVal)
            val revHeight = (zeroY - revTopY).coerceAtLeast(2.dp.toPx())
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(revColor, revColor.copy(alpha = 0.78f))
                ),
                topLeft = Offset(centerX - barGap / 2f - barWidth, revTopY),
                size = Size(barWidth, revHeight),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )

            // Operational Cost Bar
            val costTopY = yForValue(pt.operationalCosts)
            val costHeight = (zeroY - costTopY).coerceAtLeast(2.dp.toPx())
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(costColor, costColor.copy(alpha = 0.78f))
                ),
                topLeft = Offset(centerX + barGap / 2f, costTopY),
                size = Size(barWidth, costHeight),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )

            // Net Profit Anchor
            val netVal = pt.netProfitFor(useBilledRevenue)
            lineAnchors.add(Offset(centerX, yForValue(netVal)))

            // X-axis Month Label (thin labels cleanly if > 8 months)
            val showLabel = points.size <= 8 || idx % 2 == 0 || idx == points.lastIndex || isSelected
            if (showLabel) {
                val monthMeasured = textMeasurer.measure(
                    text = pt.shortLabel,
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = if (isSelected) revColor else axisLabelColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                )
                drawText(
                    textLayoutResult = monthMeasured,
                    topLeft = Offset(
                        x = (centerX - monthMeasured.size.width / 2f)
                            .coerceIn(leftPad, size.width - rightPad - monthMeasured.size.width),
                        y = topPad + plotH + 8.dp.toPx()
                    )
                )
            }
        }

        // Smooth Net Profit Trend Line Overlay
        if (lineAnchors.size >= 2) {
            val linePath = Path().apply {
                moveTo(lineAnchors.first().x, lineAnchors.first().y)
                for (i in 1 until lineAnchors.size) {
                    val prev = lineAnchors[i - 1]
                    val curr = lineAnchors[i]
                    val midX = (prev.x + curr.x) / 2f
                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                }
            }
            drawPath(
                path = linePath,
                color = netLineColor,
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Net Profit Nodes
        lineAnchors.forEachIndexed { idx, anchor ->
            val isSelected = points[idx].monthKey == selectedMonthKey
            val outerR = if (isSelected) 6.5.dp.toPx() else 4.5.dp.toPx()
            drawCircle(color = surfaceColor, radius = outerR + 1.8.dp.toPx(), center = anchor)
            drawCircle(color = netLineColor, radius = outerR, center = anchor)
        }
    }
}

@Composable
private fun AnalyticsSummaryCard(
    title: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ChartLegendLine(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun buildMonthlyPoints(data: BillingDashboardData): List<MonthlyPnLPoint> {
    val keys = linkedSetOf<String>()
    data.payments.forEach { extractMonthKey(it.date)?.let(keys::add) }
    data.bills.forEach { extractMonthKey(it.date)?.let(keys::add) }
    data.expenses.forEach { extractMonthKey(it.date)?.let(keys::add) }

    if (keys.isEmpty()) return emptyList()

    val shortFmt = DateTimeFormatter.ofPattern("MMM yy", Locale.US)
    val fullFmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)

    return keys.sorted().map { key ->
        val monthPayments = data.payments.filter { extractMonthKey(it.date) == key }
        val monthBills = data.bills.filter { extractMonthKey(it.date) == key }
        val monthExpenses = data.expenses.filter { extractMonthKey(it.date) == key }

        val ym = runCatching { YearMonth.parse(key) }.getOrNull()
        MonthlyPnLPoint(
            monthKey = key,
            shortLabel = ym?.format(shortFmt) ?: key,
            fullLabel = ym?.format(fullFmt) ?: key,
            collectedRevenue = monthPayments.sumOf { it.amount },
            billedRevenue = monthBills.sumOf { it.current.coerceAtLeast(0.0) },
            operationalCosts = monthExpenses.sumOf { it.amount },
            expenseCount = monthExpenses.size,
            paymentCount = monthPayments.size,
            billCount = monthBills.size
        )
    }
}

private fun extractMonthKey(dateIso: String): String? {
    val clean = dateIso.trim()
    if (clean.length < 7) return null
    val candidate = clean.substring(0, 7)
    return if (Regex("""^\d{4}-\d{2}$""").matches(candidate)) candidate else null
}

private fun niceScaleCeiling(raw: Double): Double {
    if (raw <= 0.0) return 1000.0
    val mag = Math.pow(10.0, kotlin.math.floor(kotlin.math.log10(raw)))
    val normalized = raw / mag
    val niceFactor = when {
        normalized <= 1.0 -> 1.0
        normalized <= 1.5 -> 1.5
        normalized <= 2.0 -> 2.0
        normalized <= 3.0 -> 3.0
        normalized <= 5.0 -> 5.0
        normalized <= 8.0 -> 8.0
        else -> 10.0
    }
    return ceil(niceFactor * mag)
}

private fun formatCompactInr(amount: Double): String {
    val sign = if (amount < -0.5) "-" else ""
    val v = abs(amount)
    return when {
        v >= 100000.0 -> "${sign}₹${String.format(Locale.US, "%.1fL", v / 100000.0)}"
        v >= 1000.0 -> "${sign}₹${String.format(Locale.US, "%.0fk", v / 1000.0)}"
        else -> "${sign}₹${v.toInt()}"
    }
}
