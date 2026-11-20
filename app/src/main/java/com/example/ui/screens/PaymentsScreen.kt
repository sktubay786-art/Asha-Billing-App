package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Bill
import com.example.data.model.BillingDashboardData
import com.example.data.model.Payment
import com.example.data.model.PaymentMode
import com.example.data.model.formatInr
import com.example.data.model.roundMoney
import com.example.data.model.todayIsoDate
import com.example.ui.components.AnimatedBmwPrecisionStripe
import com.example.ui.components.AshaDatePickerDialog
import com.example.ui.components.AshaPanelCard
import com.example.ui.components.IosSquircleIcon
import com.example.ui.components.LocalAshaHaptics
import com.example.ui.components.PremiumPayIcon
import com.example.ui.components.PrintSizePickerDialog
import com.example.ui.components.StaggeredPremiumReveal
import com.example.ui.theme.AmberBadgeBg
import com.example.ui.theme.AmberBadgeFg
import com.example.ui.theme.BmwTelemetryStyle
import com.example.ui.theme.CrimsonDue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PaidBadgeBg
import com.example.ui.theme.PaidBadgeFg
import com.example.util.BillDesignModel
import com.example.util.BillPrintSize
import com.example.util.InvoicePrinter
import java.util.Locale

private data class CompletedPaymentReceipt(
    val bill: Bill,
    val title: String,
    val summary: String,
    val isDiscountSettlement: Boolean
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentsScreen(
    data: BillingDashboardData,
    preselectedBillId: String?,
    onConsumePreselectedBill: () -> Unit,
    initialShowHistory: Boolean = false,
    onConsumeShowHistoryFlag: () -> Unit = {},
    onViewInvoice: (String) -> Unit = {},
    onSavePayment: (
        bill: Bill,
        amount: Double,
        currentDue: Double,
        mode: PaymentMode,
        receivedIn: String,
        date: String,
        note: String,
        onSaved: () -> Unit
    ) -> Unit,
    onSettleBillWithDiscount: (
        bill: Bill,
        cashReceived: Double,
        currentDue: Double,
        mode: PaymentMode,
        receivedIn: String,
        date: String,
        note: String,
        onSettled: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onEditPayment: ((Payment) -> Unit)? = null,
    onDeletePayment: ((Payment) -> Unit)? = null,
    defaultBillDesign: BillDesignModel = BillDesignModel.PREMIUM_GREEN,
    defaultPrintSize: BillPrintSize = BillPrintSize.A4_QUARTER,
    qrMode: String = "Dynamic amount QR",
    customQrPath: String = "",
    bengaliLabels: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalAshaHaptics.current
    val dueBills = remember(data.bills, data.payments) {
        data.bills.filter { data.dueForBill(it) > 0.001 }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Pay / Settle, 1 = Payment History
    var selectedBillId by remember { mutableStateOf("") }
    var billExpanded by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(PaymentMode.CASH) }
    var modeExpanded by remember { mutableStateOf(false) }
    var receivedIn by remember { mutableStateOf("Cash") }
    var paymentDate by remember { mutableStateOf(todayIsoDate()) }
    var showPaymentDatePicker by remember { mutableStateOf(false) }
    var pendingPrintBill by remember { mutableStateOf<Bill?>(null) }

    // Full & Final Discount Settlement ("Settle for Less", e.g., ₹500 due -> pay ₹400 & clear ₹100)
    var discountSettleMode by remember { mutableStateOf(false) }
    var settlementNote by remember { mutableStateOf("") }
    var lastCompletedReceipt by remember { mutableStateOf<CompletedPaymentReceipt?>(null) }

    var historySearch by remember { mutableStateOf("") }
    var modeFilter by remember { mutableStateOf<PaymentMode?>(null) }

    LaunchedEffect(initialShowHistory) {
        if (initialShowHistory) {
            selectedTab = 1
            onConsumeShowHistoryFlag()
        }
    }

    LaunchedEffect(preselectedBillId, dueBills) {
        if (!preselectedBillId.isNullOrBlank()) {
            selectedTab = 0
            selectedBillId = preselectedBillId
            discountSettleMode = false
            settlementNote = ""
            val found = dueBills.firstOrNull { it.id == preselectedBillId }
            if (found != null) {
                amountText = formatAmountPlain(data.dueForBill(found))
            }
            onConsumePreselectedBill()
        }
    }

    val selectedBill = remember(dueBills, selectedBillId) {
        dueBills.firstOrNull { it.id == selectedBillId }
    }
    val selectedBillDue = selectedBill?.let { data.dueForBill(it) } ?: 0.0
    val selectedBillPaid = selectedBill?.let { data.paidForBill(it.id) } ?: 0.0

    val billMap = remember(data.bills) { data.bills.associateBy { it.id } }
    val customerMap = remember(data.customers) { data.customers.associateBy { it.id } }

    val filteredPayments = remember(data.payments, billMap, customerMap, historySearch, modeFilter) {
        val q = historySearch.trim().lowercase()
        data.payments
            .sortedByDescending { it.date }
            .filter { p ->
                val bill = billMap[p.billId]
                val cust = customerMap[p.customerId]
                val matchesQuery = q.isEmpty() || listOf(
                    cust?.name.orEmpty(),
                    bill?.customerName.orEmpty(),
                    bill?.billNo.orEmpty(),
                    p.mode,
                    p.receivedIn,
                    p.note
                ).joinToString(" ").lowercase().contains(q)
                val matchesMode = modeFilter == null || p.mode.equals(modeFilter?.label, ignoreCase = true)
                matchesQuery && matchesMode
            }
    }

    val filteredCollectedTotal = remember(filteredPayments) {
        roundMoney(
            filteredPayments
                .filter { PaymentMode.fromLabel(it.mode) != PaymentMode.SETTLEMENT }
                .sumOf { it.amount }
        )
    }
    val filteredSettledTotal = remember(filteredPayments) {
        roundMoney(
            filteredPayments
                .filter { PaymentMode.fromLabel(it.mode) == PaymentMode.SETTLEMENT }
                .sumOf { it.amount }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    selected = selectedTab == 0,
                    onClick = {
                        haptics.tick()
                        selectedTab = 0
                    },
                    modifier = Modifier.testTag("payments_tab_receive")
                ) {
                    Text("Receive / Settle", fontWeight = FontWeight.SemiBold)
                }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    selected = selectedTab == 1,
                    onClick = {
                        haptics.tick()
                        selectedTab = 1
                    },
                    modifier = Modifier.testTag("payments_tab_history")
                ) {
                    Text("Payment History (${data.payments.size})", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (selectedTab == 0) {
            // Instant confirmation card with one-tap Print Settled Bill icon right after recording a payment/settlement
            item {
                AnimatedVisibility(
                    visible = lastCompletedReceipt != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    lastCompletedReceipt?.let { receipt ->
                        val liveBill = billMap[receipt.bill.id] ?: receipt.bill
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = PaidBadgeBg,
                            border = BorderStroke(1.2.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settled_receipt_banner")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
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
                                            icon = if (receipt.isDiscountSettlement) Icons.Rounded.Handshake else Icons.Rounded.CheckCircle,
                                            containerColor = EmeraldPrimary,
                                            size = 38.dp,
                                            iconSize = 20.dp
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = receipt.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PaidBadgeFg
                                            )
                                            Text(
                                                text = "${liveBill.billNo} · ${liveBill.customerName}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = PaidBadgeFg.copy(alpha = 0.85f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = receipt.summary,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = PaidBadgeFg,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { lastCompletedReceipt = null },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("dismiss_settled_banner_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Dismiss confirmation",
                                            tint = PaidBadgeFg
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            haptics.confirm()
                                            pendingPrintBill = liveBill
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1.25f)
                                            .height(48.dp)
                                            .testTag("print_settled_bill_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Print,
                                            contentDescription = "Print settled bill",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (receipt.isDiscountSettlement) "Print Settled Bill" else "Print Receipt",
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            haptics.tap()
                                            onViewInvoice(liveBill.id)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .weight(0.85f)
                                            .height(48.dp)
                                            .testTag("view_settled_bill_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ReceiptLong,
                                            contentDescription = null,
                                            tint = PaidBadgeFg,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "View Bill",
                                            color = PaidBadgeFg,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                StaggeredPremiumReveal(index = 0) {
                    AshaPanelCard {
                        AnimatedBmwPrecisionStripe(
                            height = 2.5.dp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            PremiumPayIcon(size = 42.dp, active = true)
                            Column {
                                Text(
                                    text = "PAYMENT SETTLEMENT",
                                    style = BmwTelemetryStyle,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Pay / Receive Due / Settle",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Customer & Due Bill",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = billExpanded,
                            onExpandedChange = { billExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedBill?.let {
                                    "${it.billNo} · ${it.customerName} (${formatInr(selectedBillDue)})"
                                } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("Select customer bill with due") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = billExpanded) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("payment_bill_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = billExpanded,
                                onDismissRequest = { billExpanded = false }
                            ) {
                                if (dueBills.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No unpaid invoices") },
                                        onClick = { billExpanded = false }
                                    )
                                } else {
                                    dueBills.forEach { bill ->
                                        val d = data.dueForBill(bill)
                                        DropdownMenuItem(
                                            text = {
                                                Text("${bill.billNo} · ${bill.customerName} · Due ${formatInr(d)}")
                                            },
                                            onClick = {
                                                selectedBillId = bill.id
                                                amountText = formatAmountPlain(d)
                                                discountSettleMode = false
                                                settlementNote = ""
                                                billExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Selected Bill Due Card + Direct Print Icon
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (selectedBill == null) {
                                Text(
                                    text = "Select a customer bill above to collect payment or settle for a lower amount",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = "${selectedBill.billNo} · ${selectedBill.customerName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Total ${formatInr(selectedBill.allTotal)} · Paid ${formatInr(selectedBillPaid)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Current Due: ${formatInr(selectedBillDue)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = CrimsonDue,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Surface(
                                        onClick = {
                                            haptics.tap()
                                            pendingPrintBill = selectedBill
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = EmeraldPrimary.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("selected_bill_print_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Print,
                                                contentDescription = "Print selected bill",
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Full & Final Negotiated Settlement ("Settle for Less") Toggle Card
                        AnimatedVisibility(
                            visible = selectedBill != null && selectedBillDue > 0.001,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    onClick = {
                                        haptics.tick()
                                        discountSettleMode = !discountSettleMode
                                        if (discountSettleMode && selectedMode == PaymentMode.SETTLEMENT) {
                                            selectedMode = PaymentMode.CASH
                                            receivedIn = "Cash"
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (discountSettleMode) {
                                        AmberBadgeBg.copy(alpha = 0.85f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    },
                                    border = BorderStroke(
                                        width = if (discountSettleMode) 1.5.dp else 1.dp,
                                        color = if (discountSettleMode) {
                                            AmberBadgeFg.copy(alpha = 0.6f)
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            IosSquircleIcon(
                                                icon = Icons.Rounded.Handshake,
                                                containerColor = if (discountSettleMode) AmberBadgeFg else MaterialTheme.colorScheme.primary,
                                                size = 32.dp,
                                                iconSize = 17.dp
                                            )
                                            Text(
                                                text = "Final Settlement",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (discountSettleMode) AmberBadgeFg else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Switch(
                                            checked = discountSettleMode,
                                            onCheckedChange = { checked ->
                                                haptics.tick()
                                                discountSettleMode = checked
                                                if (checked && selectedMode == PaymentMode.SETTLEMENT) {
                                                    selectedMode = PaymentMode.CASH
                                                    receivedIn = "Cash"
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = AmberBadgeFg
                                            ),
                                            modifier = Modifier.testTag("payment_discount_settle_switch")
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                        val clampedCashForSettle = roundMoney(parsedAmount.coerceIn(0.0, selectedBillDue))
                        val waivedDiscountAmount = roundMoney((selectedBillDue - clampedCashForSettle).coerceAtLeast(0.0))

                        // Amount & Mode Row
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it },
                                label = {
                                    Text(if (discountSettleMode) "Cash / Final Paid (₹)" else "Amount")
                                },
                                placeholder = {
                                    Text(
                                        if (discountSettleMode && selectedBillDue > 0.0) {
                                            "e.g. ${formatAmountPlain(roundMoney(selectedBillDue * 0.8))}"
                                        } else {
                                            "Amount"
                                        }
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("payment_amount_input")
                            )

                            ExposedDropdownMenuBox(
                                expanded = modeExpanded,
                                onExpandedChange = { modeExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedMode.label,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Mode") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                        .testTag("payment_mode_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = modeExpanded,
                                    onDismissRequest = { modeExpanded = false }
                                ) {
                                    PaymentMode.selectableModes.forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(mode.label) },
                                            onClick = {
                                                selectedMode = mode
                                                receivedIn = if (mode == PaymentMode.CASH) "Cash" else mode.label
                                                modeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Live Breakdown Preview when "Settle for Less" is ON
                        AnimatedVisibility(
                            visible = discountSettleMode && selectedBill != null && selectedBillDue > 0.001,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, AmberBadgeFg.copy(alpha = 0.38f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("discount_settlement_breakdown_card")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "SETTLEMENT BREAKDOWN",
                                            style = BmwTelemetryStyle,
                                            color = AmberBadgeFg
                                        )
                                        SettlementBreakdownRow(
                                            label = "Bill Due",
                                            value = formatInr(selectedBillDue),
                                            valueColor = MaterialTheme.colorScheme.onSurface
                                        )
                                        SettlementBreakdownRow(
                                            label = "Collecting Now (${selectedMode.label} → Collected)",
                                            value = formatInr(clampedCashForSettle),
                                            valueColor = PaidBadgeFg
                                        )
                                        SettlementBreakdownRow(
                                            label = "Discount / Waived (→ Settled only)",
                                            value = formatInr(waivedDiscountAmount),
                                            valueColor = AmberBadgeFg
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        SettlementBreakdownRow(
                                            label = "Remaining Due After Settlement",
                                            value = "₹0 (Cleared)",
                                            valueColor = EmeraldPrimary,
                                            bold = true
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = settlementNote,
                                    onValueChange = { settlementNote = it },
                                    label = { Text("Settlement Note (optional)") },
                                    placeholder = { Text("e.g. Mutual final discount") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settlement_note_input")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = receivedIn,
                            onValueChange = { receivedIn = it },
                            label = { Text("Received In / Bank") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_received_in_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Manual Payment Date Picker (always accessible)
                        Surface(
                            onClick = {
                                haptics.tap()
                                showPaymentDatePicker = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_date_picker_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Payment Date (Tap to change)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = paymentDate,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = "Pick payment date",
                                    tint = EmeraldPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Pill Strip: Full Due | Cash | Online
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptics.tick()
                                    if (selectedBill != null) {
                                        amountText = formatAmountPlain(selectedBillDue)
                                    }
                                },
                                enabled = selectedBill != null,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("quick_full_due_button")
                            ) {
                                Text("Full Due", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    haptics.tick()
                                    selectedMode = PaymentMode.CASH
                                    receivedIn = "Cash"
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = if (selectedMode == PaymentMode.CASH) {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("quick_cash_button")
                            ) {
                                Text("Cash", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    haptics.tick()
                                    selectedMode = PaymentMode.UPI
                                    receivedIn = "UPI / Online"
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = if (selectedMode == PaymentMode.UPI) {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("quick_online_button")
                            ) {
                                Text("Online", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (discountSettleMode && selectedBill != null) {
                            // Full & Final Negotiated Settlement Primary CTA + Print Icon
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        val bill = selectedBill
                                        val dueSnapshot = selectedBillDue
                                        val cashSnapshot = clampedCashForSettle
                                        val waivedSnapshot = waivedDiscountAmount
                                        haptics.confirm()
                                        onSettleBillWithDiscount(
                                            bill,
                                            cashSnapshot,
                                            dueSnapshot,
                                            selectedMode,
                                            receivedIn,
                                            paymentDate,
                                            settlementNote
                                        ) {
                                            lastCompletedReceipt = CompletedPaymentReceipt(
                                                bill = bill,
                                                title = "Bill Settled & Cleared",
                                                summary = if (waivedSnapshot > 0.001) {
                                                    "Collected ${formatInr(cashSnapshot)} · Waived ${formatInr(waivedSnapshot)} · Due ₹0"
                                                } else {
                                                    "Collected ${formatInr(cashSnapshot)} · Due ₹0"
                                                },
                                                isDiscountSettlement = true
                                            )
                                            selectedBillId = ""
                                            amountText = ""
                                            settlementNote = ""
                                            discountSettleMode = false
                                        }
                                    },
                                    enabled = selectedBillDue > 0.001 && parsedAmount >= 0.0 && parsedAmount <= selectedBillDue + 0.01,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AmberBadgeFg,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("confirm_discount_settle_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Handshake,
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (waivedDiscountAmount > 0.001) {
                                            "Settle for ${formatInr(clampedCashForSettle)} (Clear ₹0)"
                                        } else {
                                            "Settle & Clear Full Due"
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        haptics.tap()
                                        pendingPrintBill = selectedBill
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .size(50.dp)
                                        .testTag("discount_settle_print_icon_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Print,
                                        contentDescription = "Print bill",
                                        tint = EmeraldPrimary
                                    )
                                }
                            }
                        } else {
                            // Standard Action Row: Receive Payment | Waive / Settle | Print Icon
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val bill = selectedBill ?: return@Button
                                        val paidSnapshot = roundMoney(parsedAmount)
                                        val remAfter = roundMoney((selectedBillDue - paidSnapshot).coerceAtLeast(0.0))
                                        haptics.confirm()
                                        onSavePayment(
                                            bill,
                                            parsedAmount,
                                            selectedBillDue,
                                            selectedMode,
                                            receivedIn,
                                            paymentDate,
                                            ""
                                        ) {
                                            lastCompletedReceipt = CompletedPaymentReceipt(
                                                bill = bill,
                                                title = if (remAfter <= 0.009) "Bill Paid in Full" else "Payment Recorded",
                                                summary = "Collected ${formatInr(paidSnapshot)} (${selectedMode.label}) · Remaining Due ${formatInr(remAfter)}",
                                                isDiscountSettlement = false
                                            )
                                            selectedBillId = ""
                                            amountText = ""
                                        }
                                    },
                                    enabled = selectedBill != null && parsedAmount > 0.0,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier
                                        .weight(1.15f)
                                        .height(48.dp)
                                        .testTag("save_payment_button")
                                ) {
                                    Text(
                                        text = "Receive Payment",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    onClick = {
                                        val bill = selectedBill ?: return@Surface
                                        val settleAmt = if (parsedAmount > 0.0) parsedAmount else selectedBillDue
                                        val remAfter = roundMoney((selectedBillDue - settleAmt).coerceAtLeast(0.0))
                                        haptics.confirm()
                                        onSavePayment(
                                            bill,
                                            settleAmt,
                                            selectedBillDue,
                                            PaymentMode.SETTLEMENT,
                                            "Adjusted",
                                            paymentDate,
                                            "Balance settlement"
                                        ) {
                                            lastCompletedReceipt = CompletedPaymentReceipt(
                                                bill = bill,
                                                title = "Bill Settled",
                                                summary = "Settled / Waived ${formatInr(settleAmt)} · Remaining Due ${formatInr(remAfter)}",
                                                isDiscountSettlement = true
                                            )
                                            selectedBillId = ""
                                            amountText = ""
                                        }
                                    },
                                    enabled = selectedBill != null && selectedBillDue > 0.001,
                                    shape = RoundedCornerShape(14.dp),
                                    color = AmberBadgeBg,
                                    border = BorderStroke(1.dp, AmberBadgeFg.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .weight(0.95f)
                                        .height(48.dp)
                                        .testTag("settle_amount_button")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Settle / Waive",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = AmberBadgeFg,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        haptics.tap()
                                        val bill = selectedBill ?: dueBills.firstOrNull()
                                        if (bill != null) {
                                            pendingPrintBill = bill
                                        }
                                    },
                                    enabled = selectedBill != null || dueBills.isNotEmpty(),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier
                                        .weight(0.85f)
                                        .height(48.dp)
                                        .testTag("due_receipt_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Print,
                                        contentDescription = "Print due receipt",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Print",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = historySearch,
                        onValueChange = { historySearch = it },
                        placeholder = { Text("Search customer, invoice #, mode or note…") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_history_search_input")
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = modeFilter == null,
                            onClick = {
                                haptics.tick()
                                modeFilter = null
                            },
                            label = { Text("All") }
                        )
                        PaymentMode.entries.forEach { mode ->
                            FilterChip(
                                selected = modeFilter == mode,
                                onClick = {
                                    haptics.tick()
                                    modeFilter = mode
                                },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredPayments.size} matching entries",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = when (modeFilter) {
                                    PaymentMode.SETTLEMENT -> "Settled: ${formatInr(filteredSettledTotal)}"
                                    null -> "Collected: ${formatInr(filteredCollectedTotal)}"
                                    else -> formatInr(filteredCollectedTotal)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        if (modeFilter == null && filteredSettledTotal > 0.001) {
                            Text(
                                text = "Settled / Waived (tracked separately): ${formatInr(filteredSettledTotal)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberBadgeFg,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                AshaPanelCard {
                    if (filteredPayments.isEmpty()) {
                        Text(
                            text = "No payments recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column {
                            filteredPayments.forEachIndexed { idx, payment ->
                                val bill = billMap[payment.billId]
                                val customerName = customerMap[payment.customerId]?.name
                                    ?: bill?.customerName
                                    ?: "Customer"
                                val billNo = bill?.billNo ?: "Invoice"
                                val isSettlement = payment.mode.equals(PaymentMode.SETTLEMENT.label, ignoreCase = true)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = bill != null) {
                                            if (bill != null) onViewInvoice(bill.id)
                                        }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = customerName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$billNo · ${payment.date}${if (payment.receivedIn.isNotBlank()) " · ${payment.receivedIn}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (payment.note.isNotBlank()) {
                                            Text(
                                                text = payment.note,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = formatInr(payment.amount),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (isSettlement) AmberBadgeFg else PaidBadgeFg,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSettlement) AmberBadgeBg else PaidBadgeBg
                                            ) {
                                                Text(
                                                    text = payment.mode,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSettlement) AmberBadgeFg else PaidBadgeFg,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        if (bill != null) {
                                            IconButton(
                                                onClick = {
                                                    haptics.tap()
                                                    pendingPrintBill = bill
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("payments_print_${payment.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Print,
                                                    contentDescription = "Print settled bill",
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        if (onEditPayment != null) {
                                            IconButton(
                                                onClick = {
                                                    haptics.tap()
                                                    onEditPayment(payment)
                                                },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("payments_edit_${payment.id}")
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
                                                onClick = {
                                                    haptics.heavy()
                                                    onDeletePayment(payment)
                                                },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("payments_delete_${payment.id}")
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
                                if (idx < filteredPayments.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPaymentDatePicker) {
        AshaDatePickerDialog(
            initialDateIso = paymentDate,
            onDateSelected = { paymentDate = it },
            onDismiss = { showPaymentDatePicker = false }
        )
    }

    pendingPrintBill?.let { billToPrint ->
        val freshBill = billMap[billToPrint.id] ?: billToPrint
        PrintSizePickerDialog(
            initialSize = defaultPrintSize,
            onSizeSelected = { chosenSize ->
                pendingPrintBill = null
                InvoicePrinter.printInvoice(
                    context = context,
                    settings = data.settings,
                    bill = freshBill,
                    payments = data.paymentsForBill(freshBill.id),
                    paid = data.paidForBill(freshBill.id),
                    due = data.dueForBill(freshBill),
                    design = defaultBillDesign,
                    printSize = chosenSize,
                    qrMode = qrMode,
                    customQrPath = customQrPath,
                    bengaliLabels = bengaliLabels
                )
            },
            onDismiss = { pendingPrintBill = null }
        )
    }
}

@Composable
private fun SettlementBreakdownRow(
    label: String,
    value: String,
    valueColor: Color,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
    }
}

private fun formatAmountPlain(v: Double): String {
    return if (v == v.toLong().toDouble()) v.toLong().toString()
    else String.format(Locale.US, "%.2f", v)
}
