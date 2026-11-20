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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Bill
import com.example.data.model.BillingDashboardData
import com.example.data.model.ChatMessage
import com.example.data.model.Customer
import com.example.data.model.Payment
import com.example.data.model.PaymentMode
import com.example.data.model.customerInitials
import com.example.data.model.formatInr
import com.example.data.model.todayIsoDate
import com.example.ui.components.CustomerQrPassDialog
import com.example.ui.components.IosSquircleIcon
import com.example.ui.components.IosWhatsAppBillIcon
import com.example.ui.components.PrintSizePickerDialog
import com.example.ui.components.QuickReceivePaymentDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberBadgeBg
import com.example.ui.theme.AmberBadgeFg
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
    val itemKey: String

    data class BillEntry(val bill: Bill) : ChatLedgerItem {
        override val sortDate: String get() = bill.date
        override val sortOrder: Int get() = 0
        override val itemKey: String get() = "bill_${bill.id}"
    }

    data class PaymentEntry(val payment: Payment, val billNo: String) : ChatLedgerItem {
        override val sortDate: String get() = payment.date
        override val sortOrder: Int get() = 1
        override val itemKey: String get() = "pay_${payment.id}"
    }

    data class NoteEntry(val message: ChatMessage) : ChatLedgerItem {
        override val sortDate: String get() = message.date
        override val sortOrder: Int get() = 2
        override val itemKey: String get() = "msg_${message.id}"
    }
}

@Composable
fun UserChatLedgerScreen(
    customer: Customer,
    data: BillingDashboardData,
    defaultBillDesign: BillDesignModel = BillDesignModel.PREMIUM_GREEN,
    defaultPrintSize: BillPrintSize = BillPrintSize.A4_QUARTER,
    qrMode: String = "Dynamic amount QR",
    customQrPath: String = "",
    bengaliLabels: Boolean = false,
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
    onEditBill: ((Bill) -> Unit)? = null,
    onDeleteBill: ((Bill) -> Unit)? = null,
    onEditPayment: ((Payment) -> Unit)? = null,
    onDeletePayment: ((Payment) -> Unit)? = null,
    onSendChatMessage: (text: String, onSent: () -> Unit) -> Unit = { _, _ -> },
    onUpdateChatMessage: (message: ChatMessage, newText: String, onDone: () -> Unit) -> Unit = { _, _, _ -> },
    onDeleteChatMessage: (messageId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val chatWallpaper = if (isDark) Color(0xFF0B141A) else Color(0xFFEFEAE2)
    val incomingBubbleBg = if (isDark) Color(0xFF1F2C34) else Color.White
    val outgoingBubbleBg = if (isDark) Color(0xFF005C4B) else Color(0xFFD9FDD3)
    val ownerNoteBubbleBg = if (isDark) Color(0xFF163A32) else Color(0xFFE6F4EA)
    val headerBg = if (isDark) Color(0xFF1F2C34) else EmeraldDeep

    var menuExpanded by remember { mutableStateOf(false) }
    var showQuickReceiveDialog by remember { mutableStateOf(false) }
    var showCustomerBarcodeDialog by remember { mutableStateOf(false) }
    var pendingPrintBill by remember { mutableStateOf<Bill?>(null) }
    var composerText by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessageText by remember { mutableStateOf("") }
    var pendingDeleteMessage by remember { mutableStateOf<ChatMessage?>(null) }

    val customerDue = data.dueForCustomer(customer.id)
    val customerBills = remember(data.bills, customer.id) { data.billsForCustomer(customer.id) }
    val customerPayments = remember(data.payments, customer.id) {
        data.payments.filter { it.customerId == customer.id }
    }
    val customerMessages = remember(data.chatMessages, customer.id) {
        data.chatMessagesForCustomer(customer.id)
    }
    val openingSettled = remember(customerPayments) {
        customerPayments.filter { it.billId == "opening_due" }.sumOf { it.amount }
    }
    val totalBilled = remember(customerBills, customer.openingDue, openingSettled) {
        customerBills.sumOf { it.allTotal } + customer.openingDue + openingSettled
    }
    val totalPaid = remember(customerPayments) {
        customerPayments
            .filter { PaymentMode.fromLabel(it.mode) != PaymentMode.SETTLEMENT }
            .sumOf { it.amount }
    }
    val totalSettled = remember(customerPayments) {
        customerPayments
            .filter { PaymentMode.fromLabel(it.mode) == PaymentMode.SETTLEMENT }
            .sumOf { it.amount }
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
            customQrPath = customQrPath,
            bengaliLabels = bengaliLabels
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
            appendLine("Total Paid (Cash/UPI): ${formatInr(totalPaid)}")
            if (totalSettled > 0.001) {
                appendLine("Total Settled / Waived: ${formatInr(totalSettled)}")
            }
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

    val timelineItems = remember(data.bills, data.payments, customerMessages, customer.id) {
        val bills = data.bills
            .filter { it.customerId == customer.id }
            .map { ChatLedgerItem.BillEntry(it) }
        val payments = data.payments
            .filter { it.customerId == customer.id }
            .map { p ->
                val billNo = when {
                    p.billId == "opening_due" -> "Opening Due"
                    else -> billMap[p.billId]?.billNo ?: "Ledger"
                }
                ChatLedgerItem.PaymentEntry(p, billNo)
            }
        val notes = customerMessages.map { ChatLedgerItem.NoteEntry(it) }
        (bills + payments + notes).sortedWith(
            compareBy<ChatLedgerItem> { it.sortDate }.thenBy { it.sortOrder }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(chatWallpaper)
            .imePadding()
    ) {
        // WhatsApp-style Header (Back, Avatar, Name/Village, Direct Dial Call, Barcode ID, More Menu)
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
                            contentDescription = "Dial customer phone",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { showCustomerBarcodeDialog = true },
                        modifier = Modifier.testTag("chat_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = "Customer Barcode ID",
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
                                text = { Text("Customer Barcode ID") },
                                leadingIcon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    showCustomerBarcodeDialog = true
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
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ChatStatPill("Billed", formatInr(totalBilled), Color.White)
                                ChatStatPill("Paid", formatInr(totalPaid), Color(0xFF86EFAC))
                                if (totalSettled > 0.001) {
                                    ChatStatPill("Settled", formatInr(totalSettled), Color(0xFFFDE68A))
                                }
                                ChatStatPill(
                                    "Due",
                                    if (customerDue > 0.001) formatInr(customerDue) else "Cleared",
                                    if (customerDue > 0.001) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

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
                                    maxLines = 1,
                                    softWrap = false,
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

        // Chat Ledger Stream (Bills, Payments, and Owner Manual Chat Notes — all with Edit/Undo)
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
                                text = "No bills, payments, or notes yet.\nCreate a bill or type a note below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            } else {
                items(timelineItems, key = { it.itemKey }) { item ->
                    when (item) {
                        is ChatLedgerItem.BillEntry -> {
                            BillChatBubble(
                                bill = item.bill,
                                data = data,
                                bubbleColor = incomingBubbleBg,
                                onClick = { onViewInvoice(item.bill.id) },
                                onPrintClick = { pendingPrintBill = item.bill },
                                onEditBill = onEditBill,
                                onDeleteBill = onDeleteBill
                            )
                        }
                        is ChatLedgerItem.PaymentEntry -> {
                            PaymentChatBubble(
                                payment = item.payment,
                                billNo = item.billNo,
                                bubbleColor = outgoingBubbleBg,
                                isDark = isDark,
                                onEditPayment = onEditPayment,
                                onDeletePayment = onDeletePayment
                            )
                        }
                        is ChatLedgerItem.NoteEntry -> {
                            OwnerNoteChatBubble(
                                message = item.message,
                                bubbleColor = ownerNoteBubbleBg,
                                isDark = isDark,
                                onEdit = {
                                    editingMessage = item.message
                                    editingMessageText = item.message.text
                                },
                                onDelete = { pendingDeleteMessage = item.message }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Dock: Manual Owner Chat Composer + 4-Button Per-User Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Manual Owner Chat Note Composer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = composerText,
                        onValueChange = { composerText = it },
                        placeholder = { Text("Add a private note or chat message…", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_manual_message_input")
                    )
                    Surface(
                        onClick = {
                            val clean = composerText.trim()
                            if (clean.isNotEmpty()) {
                                onSendChatMessage(clean) {
                                    composerText = ""
                                }
                            }
                        },
                        enabled = composerText.isNotBlank(),
                        shape = CircleShape,
                        color = if (composerText.isNotBlank()) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_send_message_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send note",
                                tint = if (composerText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Ergonomic 4-Button Action Bar (Bill, Receive, WhatsApp Bill w/ iOS icon, Remind)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChatBottomActionTile(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        label = "Bill",
                        badgeColor = Color(0xFF0B6B50),
                        tag = "chat_new_bill_button",
                        onClick = onNewBill,
                        modifier = Modifier.weight(0.92f)
                    )
                    ChatBottomActionTile(
                        icon = Icons.Rounded.CurrencyRupee,
                        label = "Receive",
                        badgeColor = Color(0xFF16A34A),
                        tag = "chat_receive_pay_button",
                        onClick = { openQuickReceive() },
                        modifier = Modifier.weight(0.95f)
                    )
                    ChatBottomActionTileCustom(
                        label = "WhatsApp Bill",
                        tag = "chat_wa_bill_button",
                        onClick = { sendWhatsAppBillImage() },
                        iconContent = { IosWhatsAppBillIcon(size = 28.dp) },
                        modifier = Modifier.weight(1.22f)
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
                        modifier = Modifier.weight(0.92f)
                    )
                }
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

    if (showCustomerBarcodeDialog) {
        CustomerQrPassDialog(
            customer = customer,
            onDismiss = { showCustomerBarcodeDialog = false }
        )
    }

    pendingPrintBill?.let { billToPrint ->
        PrintSizePickerDialog(
            initialSize = defaultPrintSize,
            onSizeSelected = { chosenSize ->
                pendingPrintBill = null
                InvoicePrinter.printInvoice(
                    context = context,
                    settings = data.settings,
                    bill = billToPrint,
                    payments = data.paymentsForBill(billToPrint.id),
                    paid = data.paidForBill(billToPrint.id),
                    due = data.dueForBill(billToPrint),
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

    editingMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Edit Chat Note") },
            text = {
                OutlinedTextField(
                    value = editingMessageText,
                    onValueChange = { editingMessageText = it },
                    label = { Text("Note") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_chat_note_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = editingMessageText.trim()
                        if (updated.isNotEmpty()) {
                            onUpdateChatMessage(msg, updated) {
                                editingMessage = null
                            }
                        }
                    },
                    enabled = editingMessageText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    pendingDeleteMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { pendingDeleteMessage = null },
            title = { Text("Delete Chat Note?") },
            text = { Text("Remove \"${msg.text.take(80)}\" from ${customer.name}'s chat history?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteChatMessage(msg.id)
                        pendingDeleteMessage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDue)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteMessage = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
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
    ChatBottomActionTileCustom(
        label = label,
        tag = tag,
        onClick = onClick,
        iconContent = {
            IosSquircleIcon(
                icon = icon,
                contentDescription = label,
                containerColor = badgeColor,
                size = 28.dp,
                iconSize = 15.dp
            )
        },
        modifier = modifier
    )
}

@Composable
private fun ChatBottomActionTileCustom(
    label: String,
    tag: String,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .height(62.dp)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            iconContent()
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 9.5.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
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
    onClick: () -> Unit,
    onPrintClick: () -> Unit,
    onEditBill: ((Bill) -> Unit)? = null,
    onDeleteBill: ((Bill) -> Unit)? = null
) {
    val due = data.dueForBill(bill)
    val cashPaid = data.cashCollectedForBill(bill.id)
    val settled = data.settledForBill(bill.id)
    val status = data.statusForBill(bill)
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 315.dp)
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
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        StatusBadge(status = status)
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("chat_bill_menu_${bill.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Bill actions",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Print Bill") },
                                    leadingIcon = { Icon(Icons.Rounded.Print, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onPrintClick()
                                    }
                                )
                                if (onEditBill != null) {
                                    DropdownMenuItem(
                                        text = { Text("Edit / Fix Bill") },
                                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onEditBill(bill)
                                        }
                                    )
                                }
                                if (onDeleteBill != null) {
                                    DropdownMenuItem(
                                        text = { Text("Undo / Delete Bill", color = CrimsonDue) },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = CrimsonDue)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onDeleteBill(bill)
                                        }
                                    )
                                }
                            }
                        }
                    }
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
                    val statusLine = when {
                        due > 0.001 && settled > 0.001 -> "Due ${formatInr(due)} · Set ${formatInr(settled)}"
                        due > 0.001 -> "Due ${formatInr(due)}"
                        settled > 0.001 -> "Paid ${formatInr(cashPaid)} · Set ${formatInr(settled)}"
                        else -> "Paid in full"
                    }
                    val statusColor = when {
                        due > 0.001 -> CrimsonDue
                        settled > 0.001 -> AmberBadgeFg
                        else -> PaidBadgeFg
                    }
                    Text(
                        text = statusLine,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${bill.date} · Pass →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
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
    isDark: Boolean,
    onEditPayment: ((Payment) -> Unit)? = null,
    onDeletePayment: ((Payment) -> Unit)? = null
) {
    val isSettlement = PaymentMode.fromLabel(payment.mode) == PaymentMode.SETTLEMENT
    val resolvedBubbleColor = if (isSettlement) {
        if (isDark) Color(0xFF3E2C0A) else Color(0xFFFEF3C7)
    } else {
        bubbleColor
    }
    val textColor = when {
        isSettlement && isDark -> Color(0xFFFDE68A)
        isSettlement -> Color(0xFF78350F)
        isDark -> Color.White
        else -> Color(0xFF111B21)
    }
    val subColor = when {
        isSettlement && isDark -> Color(0xFFFCD34D).copy(alpha = 0.85f)
        isSettlement -> Color(0xFF92400E)
        isDark -> Color(0xFFB3D5CC)
        else -> Color(0xFF4B6358)
    }
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
            color = resolvedBubbleColor,
            border = if (isSettlement) BorderStroke(0.75.dp, AmberBadgeFg.copy(alpha = 0.45f)) else null,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSettlement) {
                            Icon(
                                imageVector = Icons.Rounded.Handshake,
                                contentDescription = "Settlement",
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (isSettlement) "Settled ${formatInr(payment.amount)}" else "+ ${formatInr(payment.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSettlement) AmberBadgeBg.copy(alpha = if (isDark) 0.25f else 0.9f) else Color.Black.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = if (isSettlement) "Discount / Waiver" else payment.mode,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSettlement && !isDark) AmberBadgeFg else textColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (onEditPayment != null || onDeletePayment != null) {
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("chat_payment_menu_${payment.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Payment options",
                                    tint = subColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                if (onEditPayment != null) {
                                    DropdownMenuItem(
                                        text = { Text("Edit / Fix Payment") },
                                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onEditPayment(payment)
                                        }
                                    )
                                }
                                if (onDeletePayment != null) {
                                    DropdownMenuItem(
                                        text = { Text("Undo / Delete Payment", color = CrimsonDue) },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = CrimsonDue)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onDeletePayment(payment)
                                        }
                                    )
                                }
                            }
                        }
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
                        tint = if (isSettlement) AmberBadgeFg else if (isDark) Color(0xFF53BDEB) else Color(0xFF007B68),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerNoteChatBubble(
    message: ChatMessage,
    bubbleColor: Color,
    isDark: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val textColor = if (isDark) Color.White else Color(0xFF111B21)
    val subColor = if (isDark) Color(0xFFA7C7BE) else Color(0xFF4B6358)
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 290.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
            color = bubbleColor,
            border = BorderStroke(0.75.dp, EmeraldPrimary.copy(alpha = 0.25f)),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(26.dp)
                                .testTag("chat_note_menu_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Note options",
                                tint = subColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Note") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Note", color = CrimsonDue) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = CrimsonDue)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Owner Note · ${message.date}",
                        style = MaterialTheme.typography.labelSmall,
                        color = subColor
                    )
                    Icon(
                        imageVector = Icons.Rounded.DoneAll,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(13.dp)
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
