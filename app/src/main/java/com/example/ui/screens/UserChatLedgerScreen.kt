package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Bill
import com.example.data.model.BillingDashboardData
import com.example.data.model.Customer
import com.example.data.model.Payment
import com.example.data.model.PaymentMode
import com.example.data.model.customerInitials
import com.example.data.model.formatInr
import com.example.data.model.todayIsoDate
import com.example.ui.components.CustomerQrPassDialog
import com.example.ui.components.IosSquircleIcon
import com.example.ui.components.QuickReceivePaymentDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CrimsonDue
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PaidBadgeFg
import com.example.util.BillDesignModel
import com.example.util.BillDocumentEngine
import com.example.util.BillPrintSize
import com.example.util.InvoicePrinter
import java.util.Locale
import kotlin.math.max

private sealed interface ChatLedgerItem {
    val sortDate: String
    val sortOrder: Int

    data class BillEntry(val bill: Bill) : ChatLedgerItem {
        override val sortDate: String get() = bill.date
        override val sortOrder: Int get() = 0
    }

    data class PaymentEntry(val payment: Payment, val billNo: String) : ChatLedgerItem {
        override val sortDate: String get() = payment.date
        override val sortOrder: Int get() = 1
    }
}

@Composable
fun UserChatLedgerScreen(
    customer: Customer,
    data: BillingDashboardData,
    defaultBillDesign: BillDesignModel = BillDesignModel.PREMIUM_GREEN,
    defaultPrintSize: BillPrintSize = BillPrintSize.THERMAL_80,
    qrMode: String = "Dynamic amount QR",
    customQrPath: String = "",
    onBack: () -> Unit,
    onNewBill: () -> Unit,
    onRecordBillPayment: (
        bill: Bill,
        amount: Double,
        currentDue: Double,
        mode: PaymentMode,
        receivedIn: String,
        date: String,
        note: String,
        onSaved: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onSettleOverallAmount: (amount: Double, onSettled: () -> Unit) -> Unit = { _, _ -> },
    onEditCustomer: () -> Unit,
    onSettleCustomer: () -> Unit,
    onDeleteCustomer: () -> Unit,
    onViewInvoice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val chatWallpaper = if (isDark) Color(0xFF0B141A) else Color(0xFFEFEAE2)
    val incomingBubbleBg = if (isDark) Color(0xFF1F2C34) else Color.White
    val outgoingBubbleBg = if (isDark) Color(0xFF005C4B) else Color(0xFFD9FDD3)
    val headerBg = if (isDark) Color(0xFF1F2C34) else EmeraldDeep

    var menuExpanded by remember { mutableStateOf(false) }
    var showQuickReceiveDialog by remember { mutableStateOf(false) }
    var showCustomerQrDialog by remember { mutableStateOf(false) }

    val customerDue = data.dueForCustomer(customer.id)
    val customerBills = remember(data.bills, customer.id) { data.billsForCustomer(customer.id) }
    val customerPayments = remember(data.payments, customer.id) {
        data.payments.filter { it.customerId == customer.id }
    }
    val totalBilled = remember(customerBills, customer.openingDue) {
        customerBills.sumOf { it.current } + customer.openingDue
    }
    val totalPaid = remember(customerPayments) {
        customerPayments.sumOf { it.amount }
    }
    val billMap = remember(data.bills) { data.bills.associateBy { it.id } }

    fun sendWhatsAppBillImage() {
        val targetBill = customerBills.firstOrNull { data.dueForBill(it) > 0.001 }
            ?: customerBills.firstOrNull()
            ?: Bill(
                id = "ledger_${customer.id}",
                billNo = "LEDGER",
                date = todayIsoDate(),
                customerId = customer.id,
                customerName = customer.name,
                phone = customer.phone,
                address = customer.address,
                village = customer.village,
                previousDue = max(0.0, customerDue),
                allTotal = max(0.0, customerDue),
                note = "Customer Ledger Summary"
            )
        val targetPayments = data.paymentsForBill(targetBill.id)
        val targetPaid = data.paidForBill(targetBill.id)
        val targetDue = if (customerBills.isEmpty()) max(0.0, customerDue) else data.dueForBill(targetBill)

        BillDocumentEngine.sendBillImageReminderToWhatsApp(
            context = context,
            settings = data.settings,
            bill = targetBill,
            payments = targetPayments,
            paid = targetPaid,
            due = targetDue,
            design = defaultBillDesign,
            size = defaultPrintSize,
            qrMode = qrMode,
            customQrPath = customQrPath
        )
    }

    fun shareLedgerStatement() {
        val company = data.settings.company.ifBlank { "আশা মিনি শ্যালো" }
        val text = buildString {
            appendLine("$company - Customer Ledger")
            appendLine("Customer: ${customer.name}")
            if (customer.phone.isNotBlank()) appendLine("Phone: ${customer.phone}")
            if (customer.village.isNotBlank()) appendLine("Village: ${customer.village}")
            val landMeta = customer.landSummaryLabel()
            if (landMeta.isNotBlank()) appendLine("Land: $landMeta")
            appendLine("Total Billed: ${formatInr(totalBilled)}")
            appendLine("Total Paid: ${formatInr(totalPaid)}")
            appendLine("Balance Due: ${formatInr(max(0.0, customerDue))}")
            if (data.settings.upi.isNotBlank()) appendLine("UPI: ${data.settings.upi}")
        }
        InvoicePrinter.openWhatsAppOrShare(context, customer.phone, text)
    }

    fun openQuickReceive() {
        if (customerDue <= 0.001) {
            Toast.makeText(context, "${customer.name} has no outstanding balance", Toast.LENGTH_SHORT).show()
        } else {
            showQuickReceiveDialog = true
        }
    }

    val timelineItems = remember(data.bills, data.payments, customer.id) {
        val bills = data.bills
            .filter { it.customerId == customer.id }
            .map { ChatLedgerItem.BillEntry(it) }
        val payments = data.payments
            .filter { it.customerId == customer.id }
            .map { p ->
                val billNo = billMap[p.billId]?.billNo ?: "Ledger"
                ChatLedgerItem.PaymentEntry(p, billNo)
            }
        (bills + payments).sortedWith(
            compareBy<ChatLedgerItem> { it.sortDate }.thenBy { it.sortOrder }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(chatWallpaper)
    ) {
        // WhatsApp-style Header (Back, Avatar, Name/Village, Call, Customer QR, More Menu)
        Surface(
            color = headerBg,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f))
                            .clickable { onEditCustomer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customerInitials(customer.name),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onEditCustomer() }
                    ) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val metaText = buildString {
                            if (customer.phone.isNotBlank()) append(customer.phone)
                            if (customer.village.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append(customer.village)
                            }
                        }.ifBlank { "Tap to edit profile" }
                        Text(
                            text = metaText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD2EBE0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { InvoicePrinter.dialCustomer(context, customer.phone) },
                        modifier = Modifier.testTag("chat_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = "Call customer",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { showCustomerQrDialog = true },
                        modifier = Modifier.testTag("chat_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCode2,
                            contentDescription = "Customer QR Pass",
                            tint = Color.White
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("chat_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share Ledger Statement") },
                                leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    shareLedgerStatement()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Customer QR Pass") },
                                leadingIcon = { Icon(Icons.Rounded.QrCode2, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    showCustomerQrDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit)) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEditCustomer()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settle)) },
                                leadingIcon = { Icon(Icons.Rounded.Handshake, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onSettleCustomer()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete), color = CrimsonDue) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = null,
                                        tint = CrimsonDue
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteCustomer()
                                }
                            )
                        }
                    }
                }

                // Pinned Financial Ledger & Land Summary Strip inside Particular User Chat
                Surface(
                    color = Color.Black.copy(alpha = 0.22f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                ChatStatPill("Billed", formatInr(totalBilled), Color.White)
                                ChatStatPill("Paid", formatInr(totalPaid), Color(0xFF86EFAC))
                                ChatStatPill(
                                    "Due",
                                    if (customerDue > 0.001) formatInr(customerDue) else "Cleared",
                                    if (customerDue > 0.001) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                                )
                            }

                            Surface(
                                onClick = { shareLedgerStatement() },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier.testTag("chat_ledger_share_button")
                            ) {
                                Text(
                                    text = "Ledger ↗",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }

                        val landInfo = customer.landSummaryLabel()
                        if (landInfo.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Landscape,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "জমি: $landInfo",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFFDE68A),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Chat Ledger Stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (customer.openingDue > 0.001) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF182229) else Color(0xFFFFF8DC)
                        ) {
                            Text(
                                text = "Opening Balance Carried Forward: ${formatInr(customer.openingDue)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            if (timelineItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = incomingBubbleBg,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "No bills or payments yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            } else {
                items(timelineItems) { item ->
                    when (item) {
                        is ChatLedgerItem.BillEntry -> {
                            BillChatBubble(
                                bill = item.bill,
                                data = data,
                                bubbleColor = incomingBubbleBg,
                                onClick = { onViewInvoice(item.bill.id) }
                            )
                        }
                        is ChatLedgerItem.PaymentEntry -> {
                            PaymentChatBubble(
                                payment = item.payment,
                                billNo = item.billNo,
                                bubbleColor = outgoingBubbleBg,
                                isDark = isDark
                            )
                        }
                    }
                }
            }
        }

        // WhatsApp-style Ergonomic 4-Button Bottom Action Dock (Bill, Receive, WA Bill Img, Remind)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChatBottomActionTile(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    label = "Bill",
                    badgeColor = Color(0xFF0B6B50),
                    tag = "chat_new_bill_button",
                    onClick = onNewBill,
                    modifier = Modifier.weight(1f)
                )
                ChatBottomActionTile(
                    icon = Icons.Rounded.CurrencyRupee,
                    label = "Receive",
                    badgeColor = Color(0xFF16A34A),
                    tag = "chat_receive_pay_button",
                    onClick = { openQuickReceive() },
                    modifier = Modifier.weight(1f)
                )
                ChatBottomActionTile(
                    icon = Icons.Rounded.Image,
                    label = "WA Bill",
                    badgeColor = Color(0xFF25D366),
                    tag = "chat_wa_bill_button",
                    onClick = { sendWhatsAppBillImage() },
                    modifier = Modifier.weight(1f)
                )
                ChatBottomActionTile(
                    icon = Icons.Rounded.NotificationsActive,
                    label = "Remind",
                    badgeColor = Color(0xFFF59E0B),
                    tag = "chat_remind_button",
                    onClick = {
                        val msg = InvoicePrinter.buildReminderText(customer, customerDue)
                        InvoicePrinter.openWhatsAppOrShare(context, customer.phone, msg)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showQuickReceiveDialog) {
        QuickReceivePaymentDialog(
            customer = customer,
            data = data,
            onDismiss = { showQuickReceiveDialog = false },
            onRecordBillPayment = onRecordBillPayment,
            onSettleOverall = onSettleOverallAmount
        )
    }

    if (showCustomerQrDialog) {
        CustomerQrPassDialog(
            customer = customer,
            onDismiss = { showCustomerQrDialog = false }
        )
    }
}

@Composable
private fun ChatStatPill(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label.uppercase(Locale.US),
            fontSize = 9.sp,
            letterSpacing = 0.6.sp,
            color = Color.White.copy(alpha = 0.68f),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            color = valueColor,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun ChatBottomActionTile(
    icon: ImageVector,
    label: String,
    badgeColor: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .height(60.dp)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IosSquircleIcon(
                icon = icon,
                contentDescription = label,
                containerColor = badgeColor,
                size = 28.dp,
                iconSize = 15.dp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 10.5.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BillChatBubble(
    bill: Bill,
    data: BillingDashboardData,
    bubbleColor: Color,
    onClick: () -> Unit
) {
    val due = data.dueForBill(bill)
    val status = data.statusForBill(bill)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
            color = bubbleColor,
            shadowElevation = 1.dp
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
                    Text(
                        text = bill.billNo,
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    StatusBadge(status = status)
                }

                Text(
                    text = formatInr(bill.allTotal),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = if (bill.landAmount > 0.001 || bill.rate > 0.001) {
                        "${formatQty(bill.landAmount)} ${bill.unit} (${String.format(Locale.US, "%.2f", bill.bigha)} bigha) · ${formatInr(bill.rate)}/bigha"
                    } else {
                        "Previous Due Recovery Invoice"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (bill.season.isNotBlank()) {
                    Text(
                        text = "Season: ${bill.season}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (due > 0.001) "Due ${formatInr(due)}" else "Paid in full",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (due > 0.001) CrimsonDue else PaidBadgeFg,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${bill.date} · Pass →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentChatBubble(
    payment: Payment,
    billNo: String,
    bubbleColor: Color,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else Color(0xFF111B21)
    val subColor = if (isDark) Color(0xFFB3D5CC) else Color(0xFF4B6358)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 290.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
            color = bubbleColor,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "+ ${formatInr(payment.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = payment.mode,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Against $billNo${if (payment.receivedIn.isNotBlank()) " · ${payment.receivedIn}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = subColor
                )

                if (payment.note.isNotBlank()) {
                    Text(
                        text = payment.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = payment.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = subColor
                    )
                    Icon(
                        imageVector = Icons.Rounded.DoneAll,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF53BDEB) else Color(0xFF007B68),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private fun formatQty(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}
