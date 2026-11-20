package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.MonthlyPaymentTrendPoint
import com.example.data.model.formatInr
import com.example.ui.theme.AmberBadgeBg
import com.example.ui.theme.AmberBadgeFg
import com.example.ui.theme.BmwMetricNumberStyle
import com.example.ui.theme.BmwTelemetryStyle
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PaidBadgeBg
import com.example.ui.theme.PaidBadgeFg
import com.example.util.InvoicePrinter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

private val TrendRegularBarTop = Color(0xFF10B981)
private val TrendRegularBarBottom = Color(0xFF047857)
private val TrendSettledBarTop = Color(0xFFF59E0B)
private val TrendSettledBarBottom = Color(0xFFD97706)
private val SettledGoldAccent = Color(0xFFD97706)

/**
 * Dedicated Home KPI Banner showing total Settled Amount + Settlement Count.
 */
@Composable
fun SettledSummaryBannerCard(
    settledAmount: Double,
    settledCount: Int,
    totalCollected: Double,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settledSharePct = remember(settledAmount, totalCollected) {
        val totalCleared = totalCollected + settledAmount
        if (totalCleared > 0.01) ((settledAmount / totalCleared) * 100.0).toInt().coerceIn(0, 100)
        else 0
    }

    Surface(
        onClick = onOpenHistory,
        modifier = modifier
            .fillMaxWidth()
            .testTag("overview_settled_kpi_card"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SettledGoldAccent.copy(alpha = 0.32f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            SettledGoldAccent.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                IosSquircleIcon(
                    icon = Icons.Rounded.Handshake,
                    containerColor = SettledGoldAccent,
                    size = 40.dp,
                    iconSize = 21.dp
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "SETTLED AMOUNT",
                        style = BmwTelemetryStyle,
                        color = SettledGoldAccent
                    )
                    AnimatedCurrencyText(
                        amount = settledAmount,
                        style = BmwMetricNumberStyle,
                        color = SettledGoldAccent
                    )
                    Text(
                        text = if (settledCount > 0) {
                            "$settledCount settlement${if (settledCount == 1) "" else "s"} · $settledSharePct% of cleared (excl. Collected)"
                        } else {
                            "Tracked separately from Collected"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AmberBadgeBg
            ) {
                Text(
                    text = if (settledCount > 0) "$settledCount Settled" else "View →",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberBadgeFg,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Interactive Recharts-inspired 6-Month Customer Payment History Bar Chart rendered natively
 * on Jetpack Compose Canvas for zero-lag 60fps performance.
 *
 * Features:
 * - Dashed horizontal gridlines with clean INR Y-axis ticks
 * - Stacked rounded-top bars (Regular Paid in Emerald + Settled in Amber)
 * - Tap any bar column or month chip to inspect exact monthly breakdown in the tooltip card
 * - 1-tap Share 6-Month Report button for WhatsApp / owner sharing
 */
@Composable
fun MonthlyPaymentTrendsBarChartCard(
    trends: List<MonthlyPaymentTrendPoint>,
    companyName: String,
    scopeLabel: String,
    onOpenFullAnalytics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val defaultIndex = remember(trends) {
        val lastActive = trends.indexOfLast { it.totalAmount > 0.001 }
        if (lastActive >= 0) lastActive else (trends.lastIndex).coerceAtLeast(0)
    }
    var selectedIdx by remember(trends) { mutableIntStateOf(defaultIndex) }
    val safeSelectedIdx = selectedIdx.coerceIn(0, (trends.lastIndex).coerceAtLeast(0))
    val selectedPoint = trends.getOrNull(safeSelectedIdx)

    val sixMonthTotal = remember(trends) { trends.sumOf { it.totalAmount } }
    val sixMonthSettled = remember(trends) { trends.sumOf { it.settledPaid } }
    val peakMonth = remember(trends) { trends.maxByOrNull { it.totalAmount } }
    val chartMaxVal = remember(trends) {
        val rawMax = trends.maxOfOrNull { it.totalAmount } ?: 0.0
        niceAxisMax(rawMax)
    }

    // Crisp one-shot entrance animation (zero idle GPU loop)
    val barProgress = remember { Animatable(0f) }
    LaunchedEffect(trends) {
        barProgress.snapTo(0.15f)
        barProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )
    }

    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val highlightColColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
    val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)

    AshaPanelCard(
        modifier = modifier.testTag("overview_monthly_payment_trends_chart")
    ) {
        // Header Row
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
                        text = "6-MONTH PAYMENT TRENDS",
                        style = BmwTelemetryStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Monthly History",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = {
                        val report = buildSixMonthShareReport(
                            companyName = companyName,
                            scopeLabel = scopeLabel,
                            trends = trends,
                            totalAmount = sixMonthTotal,
                            totalSettled = sixMonthSettled
                        )
                        InvoicePrinter.shareText(context, "6-Month Payment Trends", report)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.testTag("share_6month_trends_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share 6-month payment report",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Share",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                TextButton(
                    onClick = onOpenFullAnalytics,
                    modifier = Modifier.testTag("trends_full_analytics_button")
                ) {
                    Text("Full →", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Summary pills strip (6M Total + Peak Month)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PaidBadgeBg,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Text(
                        text = "6M Collected",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaidBadgeFg.copy(alpha = 0.85f)
                    )
                    Text(
                        text = formatInr(sixMonthTotal),
                        style = MaterialTheme.typography.titleSmall,
                        color = PaidBadgeFg,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AmberBadgeBg,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Text(
                        text = "Peak Month",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberBadgeFg.copy(alpha = 0.85f)
                    )
                    Text(
                        text = if (peakMonth != null && peakMonth.totalAmount > 0.001) {
                            "${peakMonth.shortLabel} · ${formatCompactInr(peakMonth.totalAmount)}"
                        } else {
                            "—"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = AmberBadgeFg,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Recharts-style Floating Tooltip Strip for Selected Month
        if (selectedPoint != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trends_selected_tooltip")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedPoint.fullLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${selectedPoint.receiptCount} receipt${if (selectedPoint.receiptCount == 1) "" else "s"} · Tap bar to inspect",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Regular",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatInr(selectedPoint.regularPaid),
                                style = MaterialTheme.typography.labelLarge,
                                color = PaidBadgeFg,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (selectedPoint.settledPaid > 0.001) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Settled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatInr(selectedPoint.settledPaid),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SettledGoldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Recharts-style Y-Axis + Interactive Stacked Bar Chart Canvas
        val yTicks = remember(chartMaxVal) {
            listOf(chartMaxVal, chartMaxVal * 0.66, chartMaxVal * 0.33, 0.0)
        }
        val animFrac = barProgress.value

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Y-Axis Ticks Column
            Column(
                modifier = Modifier
                    .width(46.dp)
                    .height(176.dp)
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yTicks.forEach { tick ->
                    Text(
                        text = formatCompactInr(tick),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Interactive Bar Canvas
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(176.dp)
                    .pointerInput(trends) {
                        detectTapGestures { offset ->
                            if (trends.isNotEmpty() && size.width > 0) {
                                val slotW = size.width.toFloat() / trends.size.toFloat()
                                val tapped = (offset.x / slotW).toInt().coerceIn(0, trends.lastIndex)
                                selectedIdx = tapped
                            }
                        }
                    }
            ) {
                val chartW = size.width
                val chartH = size.height
                val topPad = 8.dp.toPx()
                val bottomPad = 4.dp.toPx()
                val usableH = (chartH - topPad - bottomPad).coerceAtLeast(1f)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                // 4 horizontal dashed gridlines (CartesianGrid)
                for (i in 0..3) {
                    val gy = topPad + (usableH * i / 3f)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, gy),
                        end = Offset(chartW, gy),
                        strokeWidth = if (i == 3) 1.5.dp.toPx() else 1.dp.toPx(),
                        pathEffect = if (i == 3) null else dashEffect
                    )
                }

                if (trends.isNotEmpty()) {
                    val count = trends.size
                    val slotW = chartW / count
                    val barW = (slotW * 0.50f).coerceIn(14.dp.toPx(), 36.dp.toPx())
                    val cornerRad = CornerRadius(6.dp.toPx(), 6.dp.toPx())

                    trends.forEachIndexed { idx, pt ->
                        val slotLeft = idx * slotW
                        val centerX = slotLeft + slotW / 2f
                        val barLeft = centerX - barW / 2f

                        // Recharts active cursor highlight behind selected column
                        if (idx == safeSelectedIdx) {
                            drawRoundRect(
                                color = highlightColColor,
                                topLeft = Offset(slotLeft + slotW * 0.1f, topPad),
                                size = Size(slotW * 0.8f, usableH),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }

                        val totalFrac = if (chartMaxVal > 0.0) {
                            (pt.totalAmount / chartMaxVal).toFloat().coerceIn(0f, 1f) * animFrac
                        } else 0f

                        if (totalFrac <= 0.004f) {
                            // Subtle zero-state stub pill so month slot feels anchored
                            val stubH = 4.dp.toPx()
                            drawRoundRect(
                                color = emptyBarColor,
                                topLeft = Offset(barLeft, topPad + usableH - stubH),
                                size = Size(barW, stubH),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        } else {
                            val totalH = max(8.dp.toPx(), usableH * totalFrac)
                            val regularShare = if (pt.totalAmount > 0.001) {
                                (pt.regularPaid / pt.totalAmount).toFloat().coerceIn(0f, 1f)
                            } else 1f
                            val regH = totalH * regularShare
                            val setH = (totalH - regH).coerceAtLeast(0f)

                            val baseBottom = topPad + usableH
                            // 1. Regular Paid bottom bar (Emerald gradient)
                            if (regH > 0.5f) {
                                val regTop = baseBottom - regH
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(TrendRegularBarTop, TrendRegularBarBottom),
                                        startY = regTop,
                                        endY = baseBottom
                                    ),
                                    topLeft = Offset(barLeft, regTop),
                                    size = Size(barW, regH),
                                    cornerRadius = cornerRad
                                )
                            }

                            // 2. Settled stacked cap (Amber-gold gradient)
                            if (setH > 1.5f) {
                                val setTop = baseBottom - totalH
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(TrendSettledBarTop, TrendSettledBarBottom),
                                        startY = setTop,
                                        endY = setTop + setH
                                    ),
                                    topLeft = Offset(barLeft, setTop),
                                    size = Size(barW, setH),
                                    cornerRadius = cornerRad
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Interactive X-Axis Month Labels Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 46.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            trends.forEachIndexed { idx, pt ->
                val isSelected = idx == safeSelectedIdx
                Surface(
                    onClick = { selectedIdx = idx },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("trend_month_chip_${pt.shortLabel}")
                ) {
                    Text(
                        text = pt.shortLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendDot(color = TrendRegularBarTop, label = "Regular Paid")
                ChartLegendDot(color = TrendSettledBarTop, label = "Settled")
            }
            Text(
                text = scopeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Extra Feature: Top Due Customers Quick Follow-Up Deck directly on Home
 * with 1-tap Call (ACTION_DIAL), WhatsApp Reminder, and Open Chat.
 */
@Composable
fun TopDueFollowUpCard(
    topDueCustomers: List<Pair<Customer, Double>>,
    onOpenCustomerChat: (Customer) -> Unit,
    onOpenAllUsers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if (topDueCustomers.isEmpty()) return

    AshaPanelCard(
        modifier = modifier.testTag("overview_top_due_followup_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PRIORITY FOLLOW-UP",
                    style = BmwTelemetryStyle,
                    color = CrimsonDue
                )
                Text(
                    text = "Top Pending Dues",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            TextButton(onClick = onOpenAllUsers) {
                Text("All Users →", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        topDueCustomers.forEachIndexed { idx, (customer, dueAmount) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenCustomerChat(customer) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val initials = remember(customer.name) {
                        customer.name.trim().split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .joinToString("") { it.first().uppercase() }
                            .ifBlank { "C" }
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CrimsonContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelLarge,
                            color = CrimsonDue,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatInr(dueAmount),
                                style = MaterialTheme.typography.labelMedium,
                                color = CrimsonDue,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (customer.village.isNotBlank()) {
                                Text(
                                    text = "· ${customer.village}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (customer.phone.isNotBlank()) {
                        IconButton(
                            onClick = { InvoicePrinter.dialCustomer(context, customer.phone) },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PaidBadgeBg)
                                .testTag("top_due_call_${customer.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Call,
                                contentDescription = "Call ${customer.name}",
                                tint = PaidBadgeFg,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val msg = InvoicePrinter.buildReminderText(customer, dueAmount)
                            InvoicePrinter.openWhatsAppOrShare(context, customer.phone, msg)
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AmberBadgeBg)
                            .testTag("top_due_remind_${customer.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "Remind ${customer.name}",
                            tint = AmberBadgeFg,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            if (idx < topDueCustomers.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

private fun niceAxisMax(rawMax: Double): Double {
    if (rawMax <= 0.0) return 1000.0
    val step = when {
        rawMax <= 1000.0 -> 250.0
        rawMax <= 5000.0 -> 1000.0
        rawMax <= 20000.0 -> 5000.0
        rawMax <= 100000.0 -> 20000.0
        else -> 50000.0
    }
    return ceil(rawMax / step) * step
}

private fun formatCompactInr(amount: Double): String {
    return when {
        amount <= 0.001 -> "₹0"
        amount >= 100000.0 -> String.format(Locale.US, "₹%.1fL", amount / 100000.0)
        amount >= 1000.0 -> {
            val k = amount / 1000.0
            if (k == k.toLong().toDouble()) "₹${k.toLong()}k"
            else String.format(Locale.US, "₹%.1fk", k)
        }
        else -> "₹${amount.toInt()}"
    }
}

private fun buildSixMonthShareReport(
    companyName: String,
    scopeLabel: String,
    trends: List<MonthlyPaymentTrendPoint>,
    totalAmount: Double,
    totalSettled: Double
): String {
    return buildString {
        appendLine("${companyName.ifBlank { "ASHA Mini Shallow" }} — 6-Month Payment Summary")
        appendLine("Filter: $scopeLabel")
        appendLine("─────────────────────")
        trends.forEach { pt ->
            val settledTag = if (pt.settledPaid > 0.001) " (Settled: ${formatInr(pt.settledPaid)})" else ""
            appendLine("${pt.fullLabel}: ${formatInr(pt.totalAmount)}$settledTag · ${pt.receiptCount} receipts")
        }
        appendLine("─────────────────────")
        appendLine("6-Month Total: ${formatInr(totalAmount)}")
        appendLine("Total Settled: ${formatInr(totalSettled)}")
    }.trim()
}
