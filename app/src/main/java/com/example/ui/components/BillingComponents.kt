package com.example.ui.components

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmAdd
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.Bill
import com.example.data.model.BillStatus
import com.example.data.model.BillingDashboardData
import com.example.data.model.Customer
import com.example.data.model.LandUnit
import com.example.data.model.Payment
import com.example.data.model.PaymentMode
import com.example.data.model.calculateBigha
import com.example.data.model.formatInr
import com.example.data.model.roundMoney
import com.example.data.model.todayIsoDate
import com.example.notifications.BillDueReminder
import com.example.notifications.DueReminderScheduler
import com.example.notifications.DueReminderStore
import com.example.ui.theme.AmberBadgeBg
import com.example.ui.theme.AmberBadgeFg
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PaidBadgeBg
import com.example.ui.theme.PaidBadgeFg
import com.example.ui.theme.PartialBadgeBg
import com.example.ui.theme.PartialBadgeFg
import com.example.util.BillDesignModel
import com.example.util.BillDocumentEngine
import com.example.util.BillPrintSize
import com.example.util.InvoicePrinter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AshaPanelCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.5.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
fun StatusBadge(
    status: BillStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        BillStatus.PAID -> PaidBadgeBg to PaidBadgeFg
        BillStatus.PARTIAL -> PartialBadgeBg to PartialBadgeFg
        BillStatus.DUE -> AmberBadgeBg to AmberBadgeFg
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun rememberCallCustomerAction(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { rawPhone: String ->
            if (rawPhone.isBlank()) {
                Toast.makeText(context, "Customer phone number is not saved", Toast.LENGTH_SHORT).show()
            } else {
                InvoicePrinter.dialCustomer(context, rawPhone)
            }
        }
    }
}

@Composable
fun BillRowItem(
    bill: Bill,
    data: BillingDashboardData,
    onViewClick: () -> Unit,
    showQuickActions: Boolean = false,
    defaultDesign: BillDesignModel = BillDesignModel.PREMIUM_GREEN,
    defaultSize: BillPrintSize = BillPrintSize.THERMAL_80,
    qrMode: String = "Dynamic amount QR",
    customQrPath: String = "",
    bengaliLabels: Boolean = false,
    onEditBill: ((Bill) -> Unit)? = null,
    onDeleteBill: ((Bill) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val payments = data.paymentsForBill(bill.id)
    val paid = data.paidForBill(bill.id)
    val cashPaid = data.cashCollectedForBill(bill.id)
    val settledWaived = data.settledForBill(bill.id)
    val due = data.dueForBill(bill)
    val status = data.statusForBill(bill)
    val reminderStore = remember(context) { DueReminderStore(context) }
    val reminders by reminderStore.reminders.collectAsState()
    val scheduledReminder = remember(reminders, bill.id) {
        reminders.firstOrNull { it.billId == bill.id && it.enabled }
    }
    var rowMenuExpanded by remember { mutableStateOf(false) }
    var showPrintSizePicker by remember { mutableStateOf(false) }

    val pdfSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    BillDocumentEngine.writeBillPdfToStream(
                        outputStream = out,
                        settings = data.settings,
                        bill = bill,
                        payments = payments,
                        paid = paid,
                        due = due,
                        design = defaultDesign,
                        size = defaultSize,
                        qrMode = qrMode,
                        customQrPath = customQrPath,
                        bengaliLabels = bengaliLabels
                    )
                }
            }.onSuccess {
                Toast.makeText(context, "PDF saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onViewClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag("bill_row_${bill.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = bill.billNo.ifBlank { "—" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(bill.customerName)
                        if (bill.village.isNotBlank()) append(" · ${bill.village}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${bill.date}${if (bill.season.isNotBlank()) " · ${bill.season}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (scheduledReminder != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Due ${scheduledReminder.dueDateIso}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(0.9f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatInr(bill.allTotal),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        due > 0.001 -> "Due ${formatInr(due)}"
                        settledWaived > 0.001 -> "Paid ${formatInr(cashPaid)} · Set ${formatInr(settledWaived)}"
                        else -> "Cleared"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        due > 0.001 -> CrimsonDue
                        settledWaived > 0.001 -> AmberBadgeFg
                        else -> PaidBadgeFg
                    },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusBadge(status = status)
                Text(
                    text = "Pass",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (onEditBill != null || onDeleteBill != null) {
                Box {
                    IconButton(
                        onClick = { rowMenuExpanded = true },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("bill_row_menu_${bill.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Bill options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = rowMenuExpanded,
                        onDismissRequest = { rowMenuExpanded = false }
                    ) {
                        if (onEditBill != null) {
                            DropdownMenuItem(
                                text = { Text("Edit / Fix Bill") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    rowMenuExpanded = false
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
                                    rowMenuExpanded = false
                                    onDeleteBill(bill)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showQuickActions) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BillMiniIconAction(
                    icon = Icons.Rounded.Print,
                    label = "Print",
                    onClick = { showPrintSizePicker = true },
                    modifier = Modifier.weight(0.9f)
                )
                BillMiniIconActionCustom(
                    label = "WhatsApp Bill",
                    onClick = {
                        BillDocumentEngine.sendBillImageReminderToWhatsApp(
                            context = context,
                            settings = data.settings,
                            bill = bill,
                            payments = payments,
                            paid = paid,
                            due = due,
                            design = defaultDesign,
                            size = defaultSize,
                            qrMode = qrMode,
                            customQrPath = customQrPath,
                            bengaliLabels = bengaliLabels
                        )
                    },
                    iconContent = { IosWhatsAppBillIcon(size = 18.dp) },
                    modifier = Modifier.weight(1.35f)
                )
                BillMiniIconAction(
                    icon = Icons.Rounded.PictureAsPdf,
                    label = "PDF",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            BillDocumentEngine.saveBillPdfToDownloads(
                                context = context,
                                settings = data.settings,
                                bill = bill,
                                payments = payments,
                                paid = paid,
                                due = due,
                                design = defaultDesign,
                                size = defaultSize,
                                qrMode = qrMode,
                                customQrPath = customQrPath,
                                bengaliLabels = bengaliLabels
                            ).onSuccess {
                                Toast.makeText(context, "Saved PDF to Downloads/ASHA_Bills", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                pdfSaverLauncher.launch("ASHA_${bill.billNo}.pdf")
                            }
                        } else {
                            pdfSaverLauncher.launch("ASHA_${bill.billNo}.pdf")
                        }
                    },
                    modifier = Modifier.weight(0.8f)
                )
                BillMiniIconAction(
                    icon = Icons.Rounded.NotificationsActive,
                    label = "Remind",
                    onClick = {
                        val msg = BillDocumentEngine.buildBillReminderMessage(data.settings, bill, due)
                        InvoicePrinter.openWhatsAppOrShare(context, bill.phone, msg)
                    },
                    modifier = Modifier.weight(0.95f)
                )
            }
        }
    }

    if (showPrintSizePicker) {
        PrintSizePickerDialog(
            initialSize = defaultSize,
            onSizeSelected = { chosenSize ->
                showPrintSizePicker = false
                InvoicePrinter.printInvoice(
                    context = context,
                    settings = data.settings,
                    bill = bill,
                    payments = payments,
                    paid = paid,
                    due = due,
                    design = defaultDesign,
                    printSize = chosenSize,
                    qrMode = qrMode,
                    customQrPath = customQrPath,
                    bengaliLabels = bengaliLabels
                )
            },
            onDismiss = { showPrintSizePicker = false }
        )
    }
}

@Composable
private fun BillMiniIconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BillMiniIconActionCustom(
        label = label,
        onClick = onClick,
        iconContent = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
        },
        modifier = modifier
    )
}

@Composable
private fun BillMiniIconActionCustom(
    label: String,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            iconContent()
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 9.5.sp,
                lineHeight = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AshaDatePickerDialog(
    initialDateIso: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = try {
        LocalDate.parse(initialDateIso, DateTimeFormatter.ISO_LOCAL_DATE)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                        onDateSelected(date)
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun InvoiceDetailDialog(
    bill: Bill,
    data: BillingDashboardData,
    initialDesignLabel: String = BillDesignModel.PREMIUM_GREEN.label,
    initialPrintSizeLabel: String = BillPrintSize.THERMAL_80.label,
    qrMode: String = "Dynamic amount QR",
    customQrPath: String = "",
    initialBengaliMode: Boolean = false,
    onToggleBengaliMode: ((Boolean) -> Unit)? = null,
    onEditBill: ((Bill) -> Unit)? = null,
    onDeleteBill: ((Bill) -> Unit)? = null,
    onEditPayment: ((Payment) -> Unit)? = null,
    onDeletePayment: ((Payment) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val payments = remember(bill.id, data.payments) { data.paymentsForBill(bill.id) }
    val paid = remember(bill.id, data.payments) { data.paidForBill(bill.id) }
    val due = remember(bill, data.payments) { data.dueForBill(bill) }
    val status = remember(bill, data.payments) { data.statusForBill(bill) }

    val reminderStore = remember(context) { DueReminderStore(context) }
    val reminders by reminderStore.reminders.collectAsState()
    val existingReminder = remember(reminders, bill.id) {
        reminders.firstOrNull { it.billId == bill.id && it.enabled }
    }

    var showDueDatePicker by remember { mutableStateOf(false) }
    var reminderDate by remember(existingReminder, bill.id) {
        mutableStateOf(existingReminder?.dueDateIso ?: todayIsoDate())
    }
    var reminderHour by remember(existingReminder, bill.id) {
        mutableIntStateOf(existingReminder?.hour ?: 9)
    }
    var pendingAlarmScheduleAfterPerm by remember { mutableStateOf(false) }
    var passDisplayMode by remember { mutableIntStateOf(0) } // 0 = iOS Wallet Pass, 1 = Print Sheet
    var bengaliMode by remember(initialBengaliMode) { mutableStateOf(initialBengaliMode) }
    var showPrintSizeDialog by remember { mutableStateOf(false) }

    fun scheduleDueNow(targetDate: String, targetHour: Int) {
        val rem = BillDueReminder(
            billId = bill.id,
            billNo = bill.billNo,
            customerName = bill.customerName,
            customerPhone = bill.phone,
            dueAmount = due,
            dueDateIso = targetDate,
            hour = targetHour,
            minute = 0
        )
        DueReminderScheduler.scheduleReminder(context, rem)
        if (DueReminderScheduler.isDueTodayOrEarlier(targetDate)) {
            DueReminderScheduler.showPersistentNotification(context, rem)
        }
        Toast.makeText(context, "Reminder set for $targetDate", Toast.LENGTH_SHORT).show()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingAlarmScheduleAfterPerm) {
            scheduleDueNow(reminderDate, reminderHour)
        } else if (!granted) {
            Toast.makeText(context, "Allow notifications for due alerts", Toast.LENGTH_SHORT).show()
        }
        pendingAlarmScheduleAfterPerm = false
    }

    var selectedDesign by remember(initialDesignLabel) {
        mutableStateOf(BillDesignModel.fromLabel(initialDesignLabel))
    }
    var selectedSize by remember(initialPrintSizeLabel) {
        mutableStateOf(BillPrintSize.fromLabel(initialPrintSizeLabel))
    }

    val qrBitmap by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        data.settings,
        due,
        qrMode,
        customQrPath
    ) {
        value = withContext(Dispatchers.Default) {
            BillDocumentEngine.resolveQrBitmap(data.settings, due, qrMode, customQrPath)
        }
    }

    val previewImageState by produceState<Pair<ImageBitmap, Float>?>(
        initialValue = null,
        bill,
        data.settings,
        payments,
        paid,
        due,
        selectedDesign,
        selectedSize,
        qrMode,
        customQrPath,
        bengaliMode
    ) {
        value = withContext(Dispatchers.Default) {
            val previewRenderSize = if (selectedSize.widthPx > BillPrintSize.THERMAL_80.widthPx) {
                BillPrintSize.THERMAL_80
            } else {
                selectedSize
            }
            val bmp = BillDocumentEngine.renderBillBitmap(
                settings = data.settings,
                bill = bill,
                payments = payments,
                paid = paid,
                due = due,
                design = selectedDesign,
                size = previewRenderSize,
                qrMode = qrMode,
                customQrPath = customQrPath,
                bengaliLabels = bengaliMode
            )
            bmp.asImageBitmap() to (bmp.width.toFloat() / bmp.height.coerceAtLeast(1).toFloat())
        }
    }

    val exportScope = rememberCoroutineScope()

    val pdfSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val appContext = context.applicationContext
            exportScope.launch {
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        appContext.contentResolver.openOutputStream(uri)?.use { out ->
                            BillDocumentEngine.writeBillPdfToStream(
                                outputStream = out,
                                settings = data.settings,
                                bill = bill,
                                payments = payments,
                                paid = paid,
                                due = due,
                                design = selectedDesign,
                                size = selectedSize,
                                qrMode = qrMode,
                                customQrPath = customQrPath,
                                bengaliLabels = bengaliMode
                            )
                        }
                    }
                }
                res.onSuccess {
                    Toast.makeText(appContext, "PDF saved", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(appContext, "Could not save PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun renderFullExportBitmap(): android.graphics.Bitmap = BillDocumentEngine.renderBillBitmap(
        settings = data.settings,
        bill = bill,
        payments = payments,
        paid = paid,
        due = due,
        design = selectedDesign,
        size = selectedSize,
        qrMode = qrMode,
        customQrPath = customQrPath,
        bengaliLabels = bengaliMode
    )

    val pngSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            val appContext = context.applicationContext
            exportScope.launch {
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        appContext.contentResolver.openOutputStream(uri)?.use { out ->
                            BillDocumentEngine.writeBillPngToStream(out, renderFullExportBitmap())
                        }
                    }
                }
                res.onSuccess {
                    Toast.makeText(appContext, "Image saved", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(appContext, "Could not save image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // iOS Wallet Pass Fullscreen Overlay Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.68f))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 14.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 6.dp,
                shadowElevation = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .heightIn(max = 760.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Overlay Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(selectedDesign.primaryColorInt).copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "PASS",
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(selectedDesign.primaryColorInt),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                            Text(
                                text = bill.billNo,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (bill.id != "preview" && onEditBill != null) {
                                IconButton(
                                    onClick = { onEditBill(bill) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .testTag("overlay_edit_bill_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit Bill",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            if (bill.id != "preview" && onDeleteBill != null) {
                                IconButton(
                                    onClick = { onDeleteBill(bill) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CrimsonContainer)
                                        .testTag("overlay_delete_bill_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete Bill",
                                        tint = CrimsonDue,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    BillDocumentEngine.shareBillImageWithReminder(
                                        context = context,
                                        settings = data.settings,
                                        bill = bill,
                                        payments = payments,
                                        paid = paid,
                                        due = due,
                                        design = selectedDesign,
                                        size = selectedSize,
                                        qrMode = qrMode,
                                        customQrPath = customQrPath,
                                        bengaliLabels = bengaliMode
                                    )
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .testTag("overlay_quick_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share Bill Pass",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("close_invoice_dialog_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.close),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    // View Switcher: iOS Wallet Pass vs Print Sheet + Bengali Toggle Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                selected = passDisplayMode == 0,
                                onClick = { passDisplayMode = 0 },
                                modifier = Modifier.testTag("preview_mode_wallet_pass")
                            ) {
                                Text("Wallet Pass", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                selected = passDisplayMode == 1,
                                onClick = { passDisplayMode = 1 },
                                modifier = Modifier.testTag("preview_mode_print_sheet")
                            ) {
                                Text("Print Sheet", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }

                        FilterChip(
                            selected = bengaliMode,
                            onClick = {
                                val next = !bengaliMode
                                bengaliMode = next
                                onToggleBengaliMode?.invoke(next)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Translate,
                                    contentDescription = "Toggle Bengali Labels",
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (bengaliMode) "বাংলা ✓" else "বাংলা",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier.testTag("preview_bengali_toggle")
                        )
                    }

                    // Pass Theme / Bill Model Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BillDesignModel.entries.forEach { model ->
                            FilterChip(
                                selected = selectedDesign == model,
                                onClick = { selectedDesign = model },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(Color(model.primaryColorInt))
                                    )
                                },
                                label = { Text(model.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("preview_model_${model.name.lowercase()}")
                            )
                        }
                    }

                    if (passDisplayMode == 1) {
                        // Print Size Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BillPrintSize.entries.forEach { size ->
                                FilterChip(
                                    selected = selectedSize == size,
                                    onClick = { selectedSize = size },
                                    label = { Text(size.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("preview_size_${size.name.lowercase()}")
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Mini paper layout diagram
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(selectedDesign.primaryColorInt).copy(alpha = 0.6f)),
                                    modifier = Modifier.size(width = 26.dp, height = 34.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                        when {
                                            selectedSize.isHalfA4 -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(14.dp)
                                                        .background(Color(selectedDesign.primaryColorInt), RoundedCornerShape(2.dp))
                                                )
                                            }
                                            selectedSize.isQuarterA4 -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.48f)
                                                        .height(14.dp)
                                                        .background(Color(selectedDesign.primaryColorInt), RoundedCornerShape(2.dp))
                                                )
                                            }
                                            selectedSize.isFourUpA4 -> {
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    repeat(2) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                            repeat(2) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .height(13.dp)
                                                                        .background(Color(selectedDesign.primaryColorInt), RoundedCornerShape(1.5.dp))
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            else -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color(selectedDesign.primaryColorInt).copy(alpha = 0.85f), RoundedCornerShape(2.dp))
                                                )
                                            }
                                        }
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when {
                                            selectedSize.isHalfA4 -> "A4 · 1/2 Top-Half Print (Bottom Half Blank)"
                                            selectedSize.isQuarterA4 -> "A4 · 1/4th Top-Left Print (3/4 Page Blank)"
                                            selectedSize.isFourUpA4 -> "A4 · 4x Bill Grid (4 Copies Divided on 1 Sheet)"
                                            selectedSize == BillPrintSize.THERMAL_80 -> "80 mm Thermal Roll (Any Brand POS Printer)"
                                            selectedSize == BillPrintSize.THERMAL_58 -> "58 mm Thermal Roll (Compact POS Printer)"
                                            else -> selectedSize.label
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = selectedDesign.tagline,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val rendered = previewImageState
                            if (rendered != null) {
                                Image(
                                    bitmap = rendered.first,
                                    contentDescription = "Bill Print Sheet Preview",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(rendered.second)
                                        .testTag("bill_preview_image")
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(strokeWidth = 2.5.dp)
                                }
                            }
                        }
                    } else {
                        // High-Fidelity iOS Wallet Pass Card
                        IosWalletBillPassCard(
                            bill = bill,
                            settings = data.settings,
                            payments = payments,
                            paid = paid,
                            due = due,
                            status = status,
                            design = selectedDesign,
                            qrBitmap = qrBitmap,
                            backdropColor = MaterialTheme.colorScheme.background,
                            bengaliMode = bengaliMode,
                            onEditPayment = onEditPayment,
                            onDeletePayment = onDeletePayment
                        )
                    }

                    // Prominent Primary Share CTA
                    Button(
                        onClick = {
                            BillDocumentEngine.shareBillImageWithReminder(
                                context = context,
                                settings = data.settings,
                                bill = bill,
                                payments = payments,
                                paid = paid,
                                due = due,
                                design = selectedDesign,
                                size = selectedSize,
                                qrMode = qrMode,
                                customQrPath = customQrPath,
                                bengaliLabels = bengaliMode
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(selectedDesign.primaryColorInt),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("primary_share_bill_pass_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Bill Pass & Reminder", fontWeight = FontWeight.Bold)
                    }

                    // 4 Secondary Action Icons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PreviewActionIconButtonCustom(
                            label = "WhatsApp Bill",
                            cardColor = MaterialTheme.colorScheme.surface,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            tag = "bill_image_reminder_button",
                            onClick = {
                                BillDocumentEngine.sendBillImageReminderToWhatsApp(
                                    context = context,
                                    settings = data.settings,
                                    bill = bill,
                                    payments = payments,
                                    paid = paid,
                                    due = due,
                                    design = selectedDesign,
                                    size = selectedSize,
                                    qrMode = qrMode,
                                    customQrPath = customQrPath,
                                    bengaliLabels = bengaliMode
                                )
                            },
                            iconContent = { IosWhatsAppBillIcon(size = 32.dp) },
                            modifier = Modifier.weight(1.25f)
                        )
                        PreviewActionIconButton(
                            icon = Icons.Rounded.Print,
                            label = "Print",
                            iconContainer = MaterialTheme.colorScheme.primary,
                            cardColor = MaterialTheme.colorScheme.surface,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            tag = "print_invoice_button",
                            onClick = { showPrintSizeDialog = true },
                            modifier = Modifier.weight(0.92f)
                        )
                        PreviewActionIconButton(
                            icon = Icons.Rounded.PictureAsPdf,
                            label = "PDF",
                            iconContainer = Color(0xFFEF4444),
                            cardColor = MaterialTheme.colorScheme.surface,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            tag = "save_pdf_button",
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    BillDocumentEngine.saveBillPdfToDownloads(
                                        context = context,
                                        settings = data.settings,
                                        bill = bill,
                                        payments = payments,
                                        paid = paid,
                                        due = due,
                                        design = selectedDesign,
                                        size = selectedSize,
                                        qrMode = qrMode,
                                        customQrPath = customQrPath,
                                        bengaliLabels = bengaliMode
                                    ).onSuccess {
                                        Toast.makeText(context, "Saved PDF to Downloads/ASHA_Bills", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        pdfSaverLauncher.launch("ASHA_${bill.billNo}.pdf")
                                    }
                                } else {
                                    pdfSaverLauncher.launch("ASHA_${bill.billNo}.pdf")
                                }
                            },
                            modifier = Modifier.weight(0.88f)
                        )
                        PreviewActionIconButton(
                            icon = Icons.Rounded.NotificationsActive,
                            label = "Remind",
                            iconContainer = Color(0xFFF59E0B),
                            cardColor = MaterialTheme.colorScheme.surface,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            tag = "send_bill_reminder_button",
                            onClick = {
                                val msg = BillDocumentEngine.buildBillReminderMessage(data.settings, bill, due)
                                InvoicePrinter.openWhatsAppOrShare(context, bill.phone, msg)
                            },
                            modifier = Modifier.weight(0.95f)
                        )
                    }

                    // Save PNG + Share Text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    BillDocumentEngine.saveBillPngToDownloads(
                                        context = context,
                                        bitmap = renderFullExportBitmap(),
                                        bill = bill
                                    ).onSuccess {
                                        Toast.makeText(context, "Saved PNG to Downloads/ASHA_Bills", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        pngSaverLauncher.launch("ASHA_${bill.billNo}.png")
                                    }
                                } else {
                                    pngSaverLauncher.launch("ASHA_${bill.billNo}.png")
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("save_bill_image_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save PNG", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Surface(
                            onClick = {
                                val summary = InvoicePrinter.buildShareSummary(
                                    settings = data.settings,
                                    bill = bill,
                                    paid = paid,
                                    due = due
                                )
                                InvoicePrinter.shareText(context, bill.billNo, summary)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("share_invoice_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Text", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    // Due Date Persistent Notification Scheduler Card (for saved bills)
                    if (bill.id != "preview" && due > 0.001) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IosSquircleIcon(
                                        icon = Icons.Rounded.AlarmAdd,
                                        containerColor = EmeraldPrimary,
                                        size = 34.dp,
                                        iconSize = 18.dp
                                    )
                                    Column {
                                        Text(
                                            text = if (existingReminder != null) {
                                                "Due Alert · ${existingReminder.dueDateIso}"
                                            } else {
                                                "Due Date Alert"
                                            },
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = if (existingReminder != null) "Persistent notification active" else "Schedule due notification",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = { showDueDatePicker = true },
                                        modifier = Modifier.testTag("dialog_set_due_date_button")
                                    ) {
                                        Text(if (existingReminder != null) "Change" else "Set Date")
                                    }
                                    if (existingReminder != null) {
                                        IconButton(
                                            onClick = {
                                                DueReminderScheduler.cancelReminder(context, bill.id)
                                                Toast.makeText(context, "Reminder cancelled", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AlarmOff,
                                                contentDescription = "Cancel reminder",
                                                tint = CrimsonDue
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

    if (showDueDatePicker) {
        AshaDatePickerDialog(
            initialDateIso = reminderDate,
            onDateSelected = { chosen ->
                reminderDate = chosen
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !DueReminderScheduler.hasNotificationPermission(context)
                ) {
                    pendingAlarmScheduleAfterPerm = true
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    scheduleDueNow(chosen, reminderHour)
                }
            },
            onDismiss = { showDueDatePicker = false }
        )
    }

    if (showPrintSizeDialog) {
        PrintSizePickerDialog(
            initialSize = selectedSize,
            onSizeSelected = { chosenSize ->
                selectedSize = chosenSize
                showPrintSizeDialog = false
                InvoicePrinter.printInvoice(
                    context = context,
                    settings = data.settings,
                    bill = bill,
                    payments = payments,
                    paid = paid,
                    due = due,
                    design = selectedDesign,
                    printSize = chosenSize,
                    qrMode = qrMode,
                    customQrPath = customQrPath,
                    bengaliLabels = bengaliMode
                )
            },
            onDismiss = { showPrintSizeDialog = false }
        )
    }
}

@Composable
private fun IosWalletBillPassCard(
    bill: Bill,
    settings: com.example.data.model.BusinessSettings,
    payments: List<Payment>,
    paid: Double,
    due: Double,
    status: BillStatus,
    design: BillDesignModel,
    qrBitmap: android.graphics.Bitmap?,
    backdropColor: Color,
    bengaliMode: Boolean = false,
    onEditPayment: ((Payment) -> Unit)? = null,
    onDeletePayment: ((Payment) -> Unit)? = null
) {
    val headerTop = Color(design.primaryColorInt)
    val headerBottom = Color(design.secondaryColorInt)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ios_wallet_bill_pass_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. iOS Wallet Pass Gradient Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(headerTop, headerBottom)))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            IosAppLogoMark(size = 38.dp)
                            Column {
                                Text(
                                    text = settings.company.ifBlank { "আশা মিনি শ্যালো" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = listOfNotNull(
                                        settings.owner.takeIf { it.isNotBlank() },
                                        settings.contact.takeIf { it.isNotBlank() }
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.80f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = status.label.uppercase(Locale.US),
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Pass Hero KPIs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = if (bengaliMode) "মোট মূল্য" else "TOTAL AMOUNT",
                                fontSize = 9.5.sp,
                                letterSpacing = 0.8.sp,
                                color = Color.White.copy(alpha = 0.78f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatInr(bill.allTotal),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        val passSettled = remember(payments) {
                            com.example.data.model.roundMoney(
                                payments
                                    .filter { PaymentMode.fromLabel(it.mode) == PaymentMode.SETTLEMENT }
                                    .sumOf { it.amount }
                            )
                        }
                        val passCashPaid = if (passSettled > 0.001) {
                            com.example.data.model.roundMoney(kotlin.math.max(0.0, paid - passSettled))
                        } else {
                            paid
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (bengaliMode) "জমা" else "PAID",
                                    fontSize = 9.5.sp,
                                    letterSpacing = 0.6.sp,
                                    color = Color.White.copy(alpha = 0.78f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatInr(passCashPaid),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF86EFAC),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (passSettled > 0.001) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (bengaliMode) "ছাড়" else "SETTLED",
                                        fontSize = 9.5.sp,
                                        letterSpacing = 0.6.sp,
                                        color = Color.White.copy(alpha = 0.78f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = formatInr(passSettled),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFFFDE68A),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (bengaliMode) "মোট বাকি" else "DUE",
                                    fontSize = 9.5.sp,
                                    letterSpacing = 0.6.sp,
                                    color = Color.White.copy(alpha = 0.78f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatInr(due),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (due > 0.001) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Perforated Wallet Pass Tear-Off Notch Line
            val dividerInk = MaterialTheme.colorScheme.outlineVariant
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
            ) {
                val notchRadius = 11.dp.toPx()
                val cy = size.height / 2f
                drawLine(
                    color = dividerInk,
                    start = Offset(notchRadius + 6.dp.toPx(), cy),
                    end = Offset(size.width - notchRadius - 6.dp.toPx(), cy),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f), 0f)
                )
                drawCircle(color = backdropColor, radius = notchRadius, center = Offset(0f, cy))
                drawCircle(color = backdropColor, radius = notchRadius, center = Offset(size.width, cy))
            }

            // 3. Structured Pass Fields (2-column Apple Wallet grid)
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PassFieldCell(
                        label = if (bengaliMode) "গ্রাহক" else "CUSTOMER",
                        value = bill.customerName,
                        sub = listOfNotNull(
                            bill.phone.takeIf { it.isNotBlank() },
                            bill.village.takeIf { it.isNotBlank() }
                        ).joinToString(" · "),
                        modifier = Modifier.weight(1.1f)
                    )
                    PassFieldCell(
                        label = if (bengaliMode) "বিল নং ও সিজন" else "BILL NO & SEASON",
                        value = bill.billNo,
                        sub = listOfNotNull(
                            bill.date.takeIf { it.isNotBlank() },
                            bill.season.takeIf { it.isNotBlank() }?.let {
                                if (bengaliMode) "সিজন: $it" else it
                            }
                        ).joinToString(" · "),
                        alignEnd = true,
                        modifier = Modifier.weight(0.9f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                val hasLand = bill.landAmount > 0.001 || bill.rate > 0.001
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PassFieldCell(
                        label = when {
                            bengaliMode && hasLand -> "জমির পরিমাণ ও রেট"
                            bengaliMode -> "বিলের ধরন"
                            hasLand -> "LAND & RATE"
                            else -> "BILL TYPE"
                        },
                        value = if (hasLand) {
                            "${formatCleanQty(bill.landAmount)} ${bill.unit.uppercase(Locale.US)}"
                        } else {
                            if (bengaliMode) "জের বাকি বিল" else "Previous Due"
                        },
                        sub = if (hasLand) {
                            val rateLabel = if (bengaliMode) "রেট" else "@"
                            "${String.format(Locale.US, "%.2f", bill.bigha)} Bigha $rateLabel ${formatInr(bill.rate)}"
                        } else {
                            if (bengaliMode) "আগের বাকি হিসাব" else "Opening / Carried Balance"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PassFieldCell(
                        label = if (bengaliMode) "বর্তমান বিল ও জের বাকি" else "CURRENT & PREV DUE",
                        value = if (hasLand) {
                            "${if (bengaliMode) "বর্তমান বিল " else ""}${formatInr(bill.current)}"
                        } else {
                            formatInr(bill.previousDue)
                        },
                        sub = if (bill.previousDue > 0.001 && hasLand) {
                            "${if (bengaliMode) "+ জের বাকি " else "+ Prev Due "}${formatInr(bill.previousDue)}"
                        } else {
                            if (bengaliMode) "জের বাকি ₹0" else "No previous balance"
                        },
                        alignEnd = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (payments.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (bengaliMode) "জমা ইতিহাস" else "RECENT PAYMENTS",
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        payments.take(4).forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isSetEntry = PaymentMode.fromLabel(p.mode) == PaymentMode.SETTLEMENT
                                Text(
                                    text = "${p.date} · ${if (isSetEntry) "Settled / Discount" else p.mode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (isSetEntry) formatInr(p.amount) else "+${formatInr(p.amount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSetEntry) AmberBadgeFg else PaidBadgeFg,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (onEditPayment != null) {
                                        IconButton(
                                            onClick = { onEditPayment(p) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = "Edit payment",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    if (onDeletePayment != null) {
                                        IconButton(
                                            onClick = { onDeletePayment(p) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Undo payment",
                                                tint = CrimsonDue,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (bill.note.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Note: ${bill.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }

                // Handwritten Owner Signature + Date Verification Strip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "AUTHORIZED SIGN & DATE",
                                fontSize = 9.sp,
                                letterSpacing = 0.8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = settings.owner.ifBlank { "SK EKRAMUL HAQUE" },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Date: ${bill.date.ifBlank { todayIsoDate() }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Canvas(
                                modifier = Modifier
                                    .width(106.dp)
                                    .height(26.dp)
                            ) {
                                val w = size.width
                                val h = size.height
                                val sigPath = Path().apply {
                                    moveTo(w * 0.04f, h * 0.72f)
                                    cubicTo(w * 0.14f, h * 0.14f, w * 0.24f, h * 0.88f, w * 0.34f, h * 0.42f)
                                    cubicTo(w * 0.41f, h * 0.16f, w * 0.47f, h * 0.78f, w * 0.56f, h * 0.48f)
                                    cubicTo(w * 0.64f, h * 0.22f, w * 0.73f, h * 0.76f, w * 0.86f, h * 0.34f)
                                    quadraticTo(w * 0.93f, h * 0.26f, w * 0.98f, h * 0.42f)
                                }
                                drawPath(
                                    path = sigPath,
                                    color = Color(0xFF1E3A8A),
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                                drawLine(
                                    color = Color(0xFF1E3A8A).copy(alpha = 0.7f),
                                    start = Offset(w * 0.08f, h * 0.88f),
                                    end = Offset(w * 0.94f, h * 0.78f),
                                    strokeWidth = 1.2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                            Text(
                                text = settings.owner.ifBlank { "Ekramul" }.split(" ").firstOrNull() ?: "Owner",
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                    }
                }
                // Compact 1D Code 128C Barcode for Customer ID (fast camera lookup)
                val customerBarcodeId = com.example.data.model.compactCustomerBarcodeId(bill.customerId)
                    .takeIf { it.isNotBlank() }
                val customerBarcodeImg by produceState<ImageBitmap?>(
                    initialValue = null,
                    customerBarcodeId
                ) {
                    value = customerBarcodeId?.let { id ->
                        withContext(Dispatchers.Default) {
                            BillDocumentEngine.generateCode128BarcodeBitmap(id, widthPx = 480, heightPx = 96)
                                ?.asImageBitmap()
                        }
                    }
                }
                if (customerBarcodeImg != null && customerBarcodeId != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Image(
                                bitmap = customerBarcodeImg!!,
                                contentDescription = "Customer ID Barcode",
                                contentScale = ContentScale.Fit,
                                filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            )
                            Text(
                                text = "CUSTOMER ID · CID-$customerBarcodeId",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Crisp White UPI Payment QR Footer (standard square QR)
            Surface(
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "UPI Payment QR",
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCode2,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "SCAN TO PAY (UPI)",
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            color = headerTop,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = settings.payee.ifBlank { settings.company },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (settings.upi.isNotBlank()) {
                            Text(
                                text = settings.upi,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (due > 0.001) {
                            Text(
                                text = "Due: ${formatInr(due)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = CrimsonDue,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassFieldCell(
    label: String,
    value: String,
    sub: String,
    alignEnd: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (sub.isNotBlank()) {
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QuickReceivePaymentDialog(
    customer: Customer,
    data: BillingDashboardData,
    onDismiss: () -> Unit,
    onRecordBillPayment: (
        bill: Bill,
        amount: Double,
        currentDue: Double,
        mode: PaymentMode,
        receivedIn: String,
        date: String,
        note: String,
        onSaved: () -> Unit
    ) -> Unit,
    onSettleOverall: (amount: Double, onSettled: () -> Unit) -> Unit
) {
    val totalDue = remember(customer, data) { data.dueForCustomer(customer.id) }
    val unpaidBills = remember(customer, data) {
        data.billsForCustomer(customer.id)
            .map { it to data.dueForBill(it) }
            .filter { it.second > 0.001 }
    }

    // Default to single unpaid bill if only 1 unpaid bill and no openingDue, else overall balance or latest unpaid bill
    var selectedBillId by remember(customer.id, unpaidBills) {
        mutableStateOf(
            if (unpaidBills.size == 1 && customer.openingDue <= 0.001) {
                unpaidBills.first().first.id
            } else {
                "__OVERALL__"
            }
        )
    }

    val selectedBillPair = unpaidBills.firstOrNull { it.first.id == selectedBillId }
    val maxPayable = selectedBillPair?.second ?: totalDue

    var amountText by remember(selectedBillId, maxPayable) {
        mutableStateOf(
            if (maxPayable > 0.0) {
                if (maxPayable == maxPayable.toLong().toDouble()) maxPayable.toLong().toString()
                else String.format(Locale.US, "%.2f", maxPayable)
            } else ""
        )
    }
    var selectedMode by remember { mutableStateOf(PaymentMode.CASH) }
    var paymentDate by remember { mutableStateOf(todayIsoDate()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        ProvideSafeTextToolbar {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            icon = Icons.Rounded.CurrencyRupee,
                            containerColor = EmeraldPrimary,
                            size = 36.dp,
                            iconSize = 18.dp
                        )
                        Column {
                            Text(
                                text = "Receive Payment",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "${customer.name} · Due ${formatInr(totalDue)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (totalDue > 0.001) CrimsonDue else PaidBadgeFg,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                    }
                }

                if (unpaidBills.isNotEmpty()) {
                    Text(
                        text = "Apply Against",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedBillId == "__OVERALL__",
                            onClick = { selectedBillId = "__OVERALL__" },
                            label = { Text("All Due (${formatInr(totalDue)})") }
                        )
                        unpaidBills.forEach { (bill, billDue) ->
                            FilterChip(
                                selected = selectedBillId == bill.id,
                                onClick = { selectedBillId = bill.id },
                                label = { Text("${bill.billNo} (${formatInr(billDue)})") }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMsg = null
                    },
                    label = { Text("Amount Received (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_receive_amount_input")
                )

                // Quick Amount Chips
                if (maxPayable > 0.0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                amountText = formatCleanQty(maxPayable)
                                errorMsg = null
                            },
                            label = { Text("Full ${formatInr(maxPayable)}") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                amountText = formatCleanQty(maxPayable / 2.0)
                                errorMsg = null
                            },
                            label = { Text("Half ${formatInr(maxPayable / 2.0)}") }
                        )
                    }
                }

                if (selectedBillPair != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMode.selectableModes.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(mode.label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Manual Payment Date Picker
                Surface(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_receive_date_picker_button")
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

                if (!errorMsg.isNullOrBlank()) {
                    Text(
                        text = errorMsg.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonDue
                    )
                }

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull()
                        if (amt == null || amt <= 0.0 || amt > maxPayable + 0.001) {
                            errorMsg = "Enter an amount between ₹1 and ${formatInr(maxPayable)}"
                            return@Button
                        }
                        if (selectedBillPair != null) {
                            onRecordBillPayment(
                                selectedBillPair.first,
                                amt,
                                selectedBillPair.second,
                                selectedMode,
                                selectedMode.label,
                                paymentDate,
                                note.trim()
                            ) {
                                onDismiss()
                            }
                        } else {
                            onSettleOverall(amt) {
                                onDismiss()
                            }
                        }
                    },
                    enabled = maxPayable > 0.001,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_quick_receive_button")
                ) {
                    Text("Update Payment", fontWeight = FontWeight.Bold)
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
}

private fun formatCleanQty(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}

@Composable
private fun PreviewActionIconButton(
    icon: ImageVector,
    label: String,
    iconContainer: Color,
    cardColor: Color,
    textColor: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PreviewActionIconButtonCustom(
        label = label,
        cardColor = cardColor,
        textColor = textColor,
        tag = tag,
        onClick = onClick,
        iconContent = {
            IosSquircleIcon(
                icon = icon,
                contentDescription = label,
                containerColor = iconContainer,
                size = 32.dp,
                iconSize = 17.dp
            )
        },
        modifier = modifier
    )
}

@Composable
private fun PreviewActionIconButtonCustom(
    label: String,
    cardColor: Color,
    textColor: Color,
    tag: String,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .height(72.dp)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            iconContent()
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 9.5.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EditBillDialog(
    bill: Bill,
    onDismiss: () -> Unit,
    onConfirm: (
        season: String,
        date: String,
        landAmount: Double,
        unit: LandUnit,
        ratePerBigha: Double,
        previousDue: Double,
        note: String
    ) -> Unit
) {
    var season by remember(bill.id) { mutableStateOf(bill.season) }
    var date by remember(bill.id) { mutableStateOf(bill.date.ifBlank { todayIsoDate() }) }
    var unit by remember(bill.id) { mutableStateOf(LandUnit.fromKey(bill.unit)) }
    var landText by remember(bill.id) { mutableStateOf(formatCleanQty(bill.landAmount)) }
    var rateText by remember(bill.id) { mutableStateOf(formatCleanQty(bill.rate)) }
    var prevDueText by remember(bill.id) { mutableStateOf(formatCleanQty(bill.previousDue)) }
    var note by remember(bill.id) { mutableStateOf(bill.note) }

    val landQty = landText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val rateVal = rateText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val prevVal = prevDueText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val bigha = calculateBigha(landQty, unit)
    val currVal = roundMoney(bigha * rateVal)
    val totalVal = roundMoney(currVal + prevVal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fix / Edit ${bill.billNo}") },
        text = {
            ProvideSafeTextToolbar {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                Text(
                    text = "${bill.customerName} · ${bill.phone}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    LandUnit.entries.forEachIndexed { idx, option ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(idx, LandUnit.entries.size),
                            selected = unit == option,
                            onClick = { unit = option }
                        ) {
                            Text(option.label, fontSize = 11.5.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = landText,
                        onValueChange = { landText = it },
                        label = { Text("Land (${unit.label})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_bill_land_input")
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Rate / Bigha") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_bill_rate_input")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prevDueText,
                        onValueChange = { prevDueText = it },
                        label = { Text("Prev Due (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_bill_prev_due_input")
                    )
                    OutlinedTextField(
                        value = season,
                        onValueChange = { season = it },
                        label = { Text("Season") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("খরা", "বর্ষা", "আমন", "বোরো", "রবি").forEach { option ->
                        FilterChip(
                            selected = season == option,
                            onClick = { season = option },
                            label = { Text(option, fontSize = 12.sp) }
                        )
                    }
                }
    var showDatePicker by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = "Pick bill date")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showDatePicker) {
                    AshaDatePickerDialog(
                        initialDateIso = date,
                        onDateSelected = { date = it },
                        onDismiss = { showDatePicker = false }
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", bigha)} Bigha · Curr ${formatInr(currVal)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formatInr(totalVal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(season, date, landQty, unit, rateVal, prevVal, note) },
                enabled = totalVal > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("save_edit_bill_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun EditPaymentDialog(
    payment: Payment,
    billNo: String,
    onDismiss: () -> Unit,
    onConfirm: (
        amount: Double,
        mode: PaymentMode,
        receivedIn: String,
        date: String,
        note: String
    ) -> Unit
) {
    var amountText by remember(payment.id) { mutableStateOf(formatCleanQty(payment.amount)) }
    var selectedMode by remember(payment.id) { mutableStateOf(PaymentMode.fromLabel(payment.mode)) }
    var receivedIn by remember(payment.id) { mutableStateOf(payment.receivedIn) }
    var date by remember(payment.id) { mutableStateOf(payment.date.ifBlank { todayIsoDate() }) }
    var note by remember(payment.id) { mutableStateOf(payment.note) }

    val amountVal = amountText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fix Payment ($billNo)") },
        text = {
            ProvideSafeTextToolbar {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_payment_amount_input")
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = {
                                selectedMode = mode
                                if (receivedIn.isBlank() || receivedIn in PaymentMode.entries.map { it.label }) {
                                    receivedIn = mode.label
                                }
                            },
                            label = { Text(mode.label, fontSize = 11.5.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = receivedIn,
                    onValueChange = { receivedIn = it },
                    label = { Text("Received In") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                var showEditPayDatePicker by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    trailingIcon = {
                        IconButton(onClick = { showEditPayDatePicker = true }) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = "Pick payment date")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showEditPayDatePicker) {
                    AshaDatePickerDialog(
                        initialDateIso = date,
                        onDateSelected = { date = it },
                        onDismiss = { showEditPayDatePicker = false }
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amountVal, selectedMode, receivedIn, date, note) },
                enabled = amountVal > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("save_edit_payment_button")
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Print Size Selection Dialog shown on EVERY Print action so the owner can choose
 * A4 · 1/4 Page vs A4 · 1/2 Page (or Thermal / Full A4) right before launching Android Print.
 */
@Composable
fun PrintSizePickerDialog(
    initialSize: BillPrintSize = BillPrintSize.A4_QUARTER,
    onSizeSelected: (BillPrintSize) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalAshaHaptics.current
    val printerPrefs = remember(context) {
        com.example.security.AppPreferencesStore(context.applicationContext).state.value
    }
    val hasDirectThermal = remember(printerPrefs) {
        com.example.util.ThermalPrinterManager.isConfigured(printerPrefs)
    }
    var chosenSize by remember(initialSize) {
        mutableStateOf(
            if (initialSize == BillPrintSize.A4_QUARTER || initialSize == BillPrintSize.A4_HALF) {
                initialSize
            } else {
                BillPrintSize.A4_QUARTER
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            icon = Icons.Rounded.Print,
                            containerColor = EmeraldPrimary,
                            size = 38.dp,
                            iconSize = 20.dp
                        )
                        Column {
                            Text(
                                text = "Select Print Size",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "High-resolution crisp bill output",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                    }
                }

                if (hasDirectThermal) {
                    val directSize = if (printerPrefs.printerPaperWidth == com.example.security.ThermalPaperWidth.MM_58) {
                        BillPrintSize.THERMAL_58
                    } else {
                        BillPrintSize.THERMAL_80
                    }
                    Surface(
                        onClick = {
                            haptics.confirm()
                            onSizeSelected(directSize)
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0EA5E9).copy(alpha = 0.12f),
                        border = BorderStroke(1.5.dp, Color(0xFF0EA5E9)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("direct_thermal_print_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Direct Thermal Print",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0284C7)
                                )
                                Text(
                                    text = com.example.util.ThermalPrinterManager.configuredSummary(printerPrefs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0284C7)
                            ) {
                                Text(
                                    text = "PRINT NOW",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Primary A4-1/4 vs A4-1/2 Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrintSizeChoiceCard(
                        size = BillPrintSize.A4_QUARTER,
                        title = "A4 · 1/4 Page",
                        subtitle = "Top-left slip\n3/4 sheet blank",
                        selected = chosenSize == BillPrintSize.A4_QUARTER,
                        onClick = { chosenSize = BillPrintSize.A4_QUARTER },
                        tag = "print_size_option_a4_quarter",
                        modifier = Modifier.weight(1f)
                    )
                    PrintSizeChoiceCard(
                        size = BillPrintSize.A4_HALF,
                        title = "A4 · 1/2 Page",
                        subtitle = "Top half bill\nExtra readability",
                        selected = chosenSize == BillPrintSize.A4_HALF,
                        onClick = { chosenSize = BillPrintSize.A4_HALF },
                        tag = "print_size_option_a4_half",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Other supported sizes
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "MORE FORMATS",
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            BillPrintSize.THERMAL_80,
                            BillPrintSize.THERMAL_58,
                            BillPrintSize.A4_FOUR_UP,
                            BillPrintSize.A4_INVOICE,
                            BillPrintSize.COMPACT_A6
                        ).forEach { size ->
                            FilterChip(
                                selected = chosenSize == size,
                                onClick = { chosenSize = size },
                                label = { Text(size.label, fontSize = 11.5.sp) },
                                modifier = Modifier.testTag("print_size_option_${size.name.lowercase()}")
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = { onSizeSelected(chosenSize) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("confirm_print_size_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Print,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print ${chosenSize.label}", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrintSizeChoiceCard(
    size: BillPrintSize,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Miniature A4 Paper Sheet Diagram
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.White,
                border = BorderStroke(1.dp, if (selected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(width = 40.dp, height = 54.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                    if (size.isHalfA4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(23.dp)
                                .background(EmeraldPrimary, RoundedCornerShape(3.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(23.dp)
                                .background(EmeraldPrimary, RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
