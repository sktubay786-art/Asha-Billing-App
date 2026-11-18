package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Bill
import com.example.data.model.BillingDashboardData
import com.example.data.model.PaymentMode
import com.example.data.model.formatInr
import com.example.data.model.todayIsoDate
import com.example.ui.components.AshaPanelCard
import com.example.ui.theme.AmberBadgeBg
import com.example.ui.theme.AmberBadgeFg
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PaidBadgeBg
import com.example.ui.theme.PaidBadgeFg
import com.example.util.InvoicePrinter
import java.util.Locale

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("payments_tab_receive")
                ) {
                    Text("Receive / Settle", fontWeight = FontWeight.SemiBold)
                }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("payments_tab_history")
                ) {
                    Text("Payment History (${data.payments.size})", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (selectedTab == 0) {
            item {
                AshaPanelCard {
                    Text(
                        text = "Pay / Receive Due / Settle",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Bill",
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
                            placeholder = { Text("Select bill") },
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
                                            Text("${bill.billNo} · ${bill.customerName} · ${formatInr(d)}")
                                        },
                                        onClick = {
                                            selectedBillId = bill.id
                                            amountText = formatAmountPlain(d)
                                            billExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (selectedBill == null) {
                            Text(
                                text = "Select bill",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "${selectedBill.billNo} · ${selectedBill.customerName}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Total ${formatInr(selectedBill.allTotal)} · Paid ${formatInr(selectedBillPaid)} · Due ${formatInr(selectedBillDue)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Amount & Mode Row
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Amount") },
                            placeholder = { Text("Amount") },
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Pill Strip: Full Due | Cash | Online
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (selectedBill != null) {
                                    amountText = formatAmountPlain(selectedBillDue)
                                }
                            },
                            enabled = selectedBill != null,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("quick_full_due_button")
                        ) {
                            Text("Full Due", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = {
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
                                .height(46.dp)
                                .testTag("quick_cash_button")
                        ) {
                            Text("Cash", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = {
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
                                .height(46.dp)
                                .testTag("quick_online_button")
                        ) {
                            Text("Online", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                    // Action Row: Receive Payment | Settle Amount | Due Receipt
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val bill = selectedBill ?: return@Button
                                onSavePayment(
                                    bill,
                                    parsedAmount,
                                    selectedBillDue,
                                    selectedMode,
                                    receivedIn,
                                    todayIsoDate(),
                                    ""
                                ) {
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
                                .weight(1.1f)
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
                                onSavePayment(
                                    bill,
                                    settleAmt,
                                    selectedBillDue,
                                    PaymentMode.SETTLEMENT,
                                    "Adjusted",
                                    todayIsoDate(),
                                    "Balance settlement"
                                ) {
                                    selectedBillId = ""
                                    amountText = ""
                                }
                            },
                            enabled = selectedBill != null && selectedBillDue > 0.001,
                            shape = RoundedCornerShape(14.dp),
                            color = AmberBadgeBg,
                            border = BorderStroke(1.dp, AmberBadgeFg.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .weight(1f)
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
                                    text = "Settle Amount",
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
                                val bill = selectedBill ?: dueBills.firstOrNull()
                                if (bill != null) {
                                    InvoicePrinter.printInvoice(
                                        context = context,
                                        settings = data.settings,
                                        bill = bill,
                                        payments = data.paymentsForBill(bill.id),
                                        paid = data.paidForBill(bill.id),
                                        due = data.dueForBill(bill)
                                    )
                                }
                            },
                            enabled = selectedBill != null || dueBills.isNotEmpty(),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier
                                .weight(0.95f)
                                .height(48.dp)
                                .testTag("due_receipt_button")
                        ) {
                            Text(
                                text = "Due Receipt",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
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
                            onClick = { modeFilter = null },
                            label = { Text("All") }
                        )
                        PaymentMode.entries.forEach { mode ->
                            FilterChip(
                                selected = modeFilter == mode,
                                onClick = { modeFilter = mode },
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
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredPayments.size} matching entries",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formatInr(filteredPayments.sumOf { it.amount }),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.ExtraBold
                        )
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
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = formatInr(payment.amount),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = PaidBadgeFg,
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
}

private fun formatAmountPlain(v: Double): String {
    return if (v == v.toLong().toDouble()) v.toLong().toString()
    else String.format(Locale.US, "%.2f", v)
}
