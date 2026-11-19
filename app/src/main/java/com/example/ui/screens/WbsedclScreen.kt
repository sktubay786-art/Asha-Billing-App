package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BillingDashboardData
import com.example.data.model.WbsedclBillPayment
import com.example.data.model.WbsedclConsumer
import com.example.data.model.formatInr
import com.example.data.model.todayIsoDate
import com.example.ui.components.AshaDatePickerDialog
import com.example.ui.components.AshaPanelCard
import com.example.ui.components.BmwPrecisionStripe
import com.example.ui.components.IosSquircleIcon
import com.example.ui.theme.BmwTelemetryStyle
import com.example.ui.theme.CrimsonDue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PaidBadgeBg
import com.example.ui.theme.PaidBadgeFg
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val WBSEDCL_QUICK_PAY_URL = "https://www.wbsedcl.in/"

@Composable
fun WbsedclScreen(
    data: BillingDashboardData,
    onSaveConsumer: (
        existingId: String,
        label: String,
        consumerNo: String,
        installationNote: String,
        onComplete: (Boolean) -> Unit
    ) -> Unit,
    onDeleteConsumer: (String) -> Unit,
    onSavePayment: (
        existingId: String,
        consumerId: String,
        consumerNo: String,
        consumerLabel: String,
        billMonth: String,
        amountPaid: Double,
        date: String,
        note: String,
        onComplete: (Boolean) -> Unit
    ) -> Unit,
    onDeletePayment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showConsumerDialog by remember { mutableStateOf(false) }
    var editingConsumer by remember { mutableStateOf<WbsedclConsumer?>(null) }
    var pendingDeleteConsumer by remember { mutableStateOf<WbsedclConsumer?>(null) }

    var editingPaymentId by remember { mutableStateOf("") }
    var selectedConsumerId by remember { mutableStateOf("") }
    var consumerNoInput by remember { mutableStateOf("") }
    var consumerLabelInput by remember { mutableStateOf("") }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale.US) }
    val quickMonths = remember {
        val now = LocalDate.now()
        listOf(
            now.format(monthFormatter),
            now.minusMonths(1).format(monthFormatter),
            now.minusMonths(2).format(monthFormatter)
        )
    }
    var billMonthInput by remember { mutableStateOf(quickMonths.first()) }
    var amountInput by remember { mutableStateOf("") }
    var paymentDate by remember { mutableStateOf(todayIsoDate()) }
    var noteInput by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var pendingDeletePayment by remember { mutableStateOf<WbsedclBillPayment?>(null) }

    fun copyConsumerAndOpenPortal(consumerNo: String?) {
        if (!consumerNo.isNullOrBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("WBSEDCL Consumer ID", consumerNo))
            Toast.makeText(context, "Consumer ID copied!", Toast.LENGTH_SHORT).show()
        }
        val uri = Uri.parse(WBSEDCL_QUICK_PAY_URL)
        val launchedCustomTab = runCatching {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(context, uri)
        }.isSuccess
        if (!launchedCustomTab) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }.onFailure {
                Toast.makeText(context, "Unable to open WBSEDCL website", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun resetPaymentForm() {
        editingPaymentId = ""
        selectedConsumerId = ""
        consumerNoInput = ""
        consumerLabelInput = ""
        billMonthInput = quickMonths.first()
        amountInput = ""
        paymentDate = todayIsoDate()
        noteInput = ""
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Telemetry & Total Spent Summary Card
        item {
            AshaPanelCard(modifier = Modifier.testTag("wbsedcl_summary_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IosSquircleIcon(
                            icon = Icons.Rounded.ElectricBolt,
                            containerColor = Color(0xFFF59E0B),
                            size = 44.dp,
                            iconSize = 24.dp
                        )
                        Column {
                            Text(
                                text = "UTILITY TELEMETRY · WBSEDCL",
                                style = BmwTelemetryStyle,
                                color = Color(0xFFD97706)
                            )
                            Text(
                                text = "Electricity Bills",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { copyConsumerAndOpenPortal(null) },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("wbsedcl_open_portal_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Open WBSEDCL Portal",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Portal", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                BmwPrecisionStripe()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "TOTAL ELECTRICITY SPENT",
                                fontSize = 9.5.sp,
                                letterSpacing = 0.7.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatInr(data.totalWbsedclSpent),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "SAVED METERS",
                                fontSize = 9.5.sp,
                                letterSpacing = 0.7.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${data.wbsedclConsumers.size} IDs",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 2. Saved WBSEDCL Consumer IDs Section
        item {
            AshaPanelCard(modifier = Modifier.testTag("wbsedcl_consumers_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saved Consumer IDs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Tap Pay Bill to copy ID & open WBSEDCL Quick Pay",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            editingConsumer = null
                            showConsumerDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("wbsedcl_add_consumer_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add ID", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (data.wbsedclConsumers.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No Consumer IDs saved yet.\nTap \"Add ID\" to save your Shallow 1 / Main Pump WBSEDCL Consumer ID.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        data.wbsedclConsumers.forEach { consumer ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wbsedcl_consumer_card_${consumer.id}")
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = consumer.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "ID: ${consumer.consumerNo}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (consumer.installationNote.isNotBlank()) {
                                                Text(
                                                    text = consumer.installationNote,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                    clipboard?.setPrimaryClip(ClipData.newPlainText("WBSEDCL Consumer ID", consumer.consumerNo))
                                                    Toast.makeText(context, "Consumer ID copied!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("wbsedcl_copy_${consumer.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ContentCopy,
                                                    contentDescription = "Copy Consumer ID",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    editingConsumer = consumer
                                                    showConsumerDialog = true
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("wbsedcl_edit_${consumer.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Edit,
                                                    contentDescription = "Edit Consumer ID",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { pendingDeleteConsumer = consumer },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("wbsedcl_delete_${consumer.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = "Delete Consumer ID",
                                                    tint = CrimsonDue,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { copyConsumerAndOpenPortal(consumer.consumerNo) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .height(44.dp)
                                                .testTag("wbsedcl_pay_bill_${consumer.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ElectricBolt,
                                                contentDescription = null,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Pay Bill", fontWeight = FontWeight.ExtraBold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                selectedConsumerId = consumer.id
                                                consumerNoInput = consumer.consumerNo
                                                consumerLabelInput = consumer.label
                                                Toast.makeText(context, "Selected ${consumer.label} below to log payment", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("wbsedcl_log_for_${consumer.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Receipt,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Record Paid", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Record Paid Bill Form Card
        item {
            AshaPanelCard(modifier = Modifier.testTag("wbsedcl_record_form_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (editingPaymentId.isBlank()) "Record Paid Bill" else "Edit Paid Bill",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Track your paid WBSEDCL electricity expenses in Firestore",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (editingPaymentId.isNotBlank()) {
                        TextButton(onClick = { resetPaymentForm() }) {
                            Text("Cancel Edit")
                        }
                    }
                }

                if (data.wbsedclConsumers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        data.wbsedclConsumers.forEach { consumer ->
                            val selected = consumerNoInput == consumer.consumerNo
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedConsumerId = consumer.id
                                    consumerNoInput = consumer.consumerNo
                                    consumerLabelInput = consumer.label
                                },
                                label = { Text("${consumer.label} (${consumer.consumerNo})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.16f),
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = consumerNoInput,
                        onValueChange = { consumerNoInput = it },
                        label = { Text("Consumer ID") },
                        placeholder = { Text("Enter 9-digit WBSEDCL Consumer ID") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wbsedcl_payment_consumer_input")
                    )

                    // Quick Bill Month Chips + Editable Field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickMonths.forEach { m ->
                            FilterChip(
                                selected = billMonthInput.equals(m, ignoreCase = true),
                                onClick = { billMonthInput = m },
                                label = { Text(m, fontSize = 11.5.sp) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = billMonthInput,
                            onValueChange = { billMonthInput = it },
                            label = { Text("Bill Month") },
                            placeholder = { Text("e.g. Nov 2026") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wbsedcl_payment_month_input")
                        )
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("Amount Paid (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wbsedcl_payment_amount_input")
                        )
                    }

                    // Manual Date Picker Field
                    Surface(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wbsedcl_payment_date_button")
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
                                    text = "Payment Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = paymentDate,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = "Select payment date",
                                tint = EmeraldPrimary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Note / Transaction Ref (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wbsedcl_payment_note_input")
                    )

                    val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
                    Button(
                        onClick = {
                            val matchedLabel = consumerLabelInput.ifBlank {
                                data.wbsedclConsumers.firstOrNull { it.consumerNo == consumerNoInput.trim() }?.label.orEmpty()
                            }
                            onSavePayment(
                                editingPaymentId,
                                selectedConsumerId,
                                consumerNoInput.trim(),
                                matchedLabel,
                                billMonthInput.trim(),
                                parsedAmount,
                                paymentDate,
                                noteInput.trim()
                            ) { ok ->
                                if (ok) resetPaymentForm()
                            }
                        },
                        enabled = consumerNoInput.isNotBlank() && billMonthInput.isNotBlank() && parsedAmount > 0.0,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("wbsedcl_save_payment_button")
                    ) {
                        Text(
                            text = if (editingPaymentId.isBlank()) "Save Paid Bill" else "Update Paid Bill",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // 4. Past WBSEDCL Payments History Card
        item {
            AshaPanelCard(modifier = Modifier.testTag("wbsedcl_history_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Payment History (${data.wbsedclPayments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PaidBadgeBg
                    ) {
                        Text(
                            text = "Total ${formatInr(data.totalWbsedclSpent)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = PaidBadgeFg,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (data.wbsedclPayments.isEmpty()) {
                    Text(
                        text = "No electricity bill payments logged yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column {
                        data.wbsedclPayments.forEachIndexed { idx, payment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = buildString {
                                            if (payment.consumerLabel.isNotBlank()) {
                                                append(payment.consumerLabel)
                                                append(" · ")
                                            }
                                            append(payment.billMonth)
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ID ${payment.consumerNo} · ${payment.date}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = formatInr(payment.amount),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFFD97706),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    IconButton(
                                        onClick = {
                                            editingPaymentId = payment.id
                                            selectedConsumerId = data.wbsedclConsumers
                                                .firstOrNull { it.consumerNo == payment.consumerNo }?.id.orEmpty()
                                            consumerNoInput = payment.consumerNo
                                            consumerLabelInput = payment.consumerLabel
                                            billMonthInput = payment.billMonth
                                            amountInput = if (payment.amount == payment.amount.toLong().toDouble()) {
                                                payment.amount.toLong().toString()
                                            } else {
                                                String.format(Locale.US, "%.2f", payment.amount)
                                            }
                                            paymentDate = payment.date
                                            noteInput = payment.note
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Edit payment",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { pendingDeletePayment = payment },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete payment",
                                            tint = CrimsonDue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            if (idx < data.wbsedclPayments.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        AshaDatePickerDialog(
            initialDateIso = paymentDate,
            onDateSelected = { paymentDate = it },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showConsumerDialog) {
        WbsedclConsumerFormDialog(
            initial = editingConsumer,
            onDismiss = { showConsumerDialog = false },
            onConfirm = { label, consumerNo, note ->
                onSaveConsumer(
                    editingConsumer?.id.orEmpty(),
                    label,
                    consumerNo,
                    note
                ) { ok ->
                    if (ok) showConsumerDialog = false
                }
            }
        )
    }

    pendingDeleteConsumer?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteConsumer = null },
            title = { Text("Remove Consumer ID?") },
            text = { Text("Delete \"${target.label}\" (${target.consumerNo}) from saved WBSEDCL meters?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteConsumer(target.id)
                        pendingDeleteConsumer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDue)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteConsumer = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    pendingDeletePayment?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeletePayment = null },
            title = { Text("Delete Electricity Payment?") },
            text = { Text("Remove ${formatInr(target.amount)} (${target.billMonth}) for ID ${target.consumerNo}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePayment(target.id)
                        pendingDeletePayment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDue)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePayment = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun WbsedclConsumerFormDialog(
    initial: WbsedclConsumer?,
    onDismiss: () -> Unit,
    onConfirm: (label: String, consumerNo: String, note: String) -> Unit
) {
    var label by remember(initial) { mutableStateOf(initial?.label.orEmpty()) }
    var consumerNo by remember(initial) { mutableStateOf(initial?.consumerNo.orEmpty()) }
    var note by remember(initial) { mutableStateOf(initial?.installationNote.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add WBSEDCL Consumer ID" else "Edit Consumer ID") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (e.g. Shallow 1, Main Pump)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wbsedcl_dialog_label_input")
                )
                OutlinedTextField(
                    value = consumerNo,
                    onValueChange = { consumerNo = it },
                    label = { Text("WBSEDCL Consumer ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wbsedcl_dialog_consumer_no_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Location / Meter Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(label.trim(), consumerNo.trim(), note.trim()) },
                enabled = label.isNotBlank() && consumerNo.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("wbsedcl_dialog_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
