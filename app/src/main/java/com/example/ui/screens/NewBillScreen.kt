package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Bill
import com.example.data.model.BillingDashboardData
import com.example.data.model.Customer
import com.example.data.model.LandUnit
import com.example.data.model.PaymentMode
import com.example.data.model.calculateBigha
import com.example.data.model.formatInr
import com.example.data.model.todayIsoDate
import com.example.notifications.BillDueReminder
import com.example.notifications.DueReminderScheduler
import com.example.ui.components.AshaDatePickerDialog
import com.example.ui.components.AshaPanelCard
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.IosSquircleIcon
import com.example.ui.theme.CrimsonDue
import com.example.util.BillDesignModel
import com.example.util.BillDocumentEngine
import com.example.util.BillPrintSize
import com.example.util.InvoicePrinter
import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale
import kotlin.math.max

private val SEASON_OPTIONS = listOf("খরা", "আমন", "বোরো", "রবি", "Aman", "Boro", "Khora")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBillScreen(
    data: BillingDashboardData,
    preselectedCustomerId: String?,
    defaultBillDesign: String,
    defaultPrintSize: String,
    qrMode: String = "Dynamic amount QR",
    customQrPath: String = "",
    onConsumePreselectedCustomer: () -> Unit,
    onPreviewBill: (Bill, BillDesignModel, BillPrintSize) -> Unit,
    onSaveExistingCustomerBill: (
        customer: Customer,
        season: String,
        date: String,
        landAmount: Double,
        unit: LandUnit,
        rate: Double,
        initialPaid: Double,
        initialMode: PaymentMode,
        initialReceivedIn: String,
        note: String,
        previousDueOverride: Double?,
        onCreated: (String, String) -> Unit
    ) -> Unit,
    onSaveManualCustomerBill: (
        name: String,
        phone: String,
        village: String,
        address: String,
        manualPreviousDue: Double,
        season: String,
        date: String,
        landAmount: Double,
        unit: LandUnit,
        rate: Double,
        initialPaid: Double,
        initialMode: PaymentMode,
        initialReceivedIn: String,
        note: String,
        onCreated: (String, String) -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedCustomerId by remember { mutableStateOf("") }
    var customerExpanded by remember { mutableStateOf(false) }

    var onlyPreviousDue by remember { mutableStateOf(false) }
    var manualPrevOverrideEnabled by remember { mutableStateOf(false) }
    var manualPreviousDueText by remember { mutableStateOf("0") }

    var manualName by remember { mutableStateOf("") }
    var manualPhone by remember { mutableStateOf("") }
    var manualVillage by remember { mutableStateOf("") }
    var manualAddress by remember { mutableStateOf("") }

    var season by remember { mutableStateOf("খরা") }
    var seasonExpanded by remember { mutableStateOf(false) }
    var billDate by remember { mutableStateOf(todayIsoDate()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Due Date Notification Schedule state
    var dueReminderEnabled by remember { mutableStateOf(false) }
    var dueDateIso by remember {
        mutableStateOf(LocalDate.now().plusDays(7).toString())
    }
    var dueReminderHour by remember { mutableIntStateOf(9) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    var selectedUnit by remember { mutableStateOf(LandUnit.BIGHA) }
    var unitExpanded by remember { mutableStateOf(false) }
    var landAmountText by remember { mutableStateOf("0") }
    var rateText by remember { mutableStateOf("2200") }

    var initialPaidText by remember { mutableStateOf("0") }
    var selectedMode by remember { mutableStateOf(PaymentMode.CASH) }
    var modeExpanded by remember { mutableStateOf(false) }
    var receivedIn by remember { mutableStateOf("Cash") }
    var note by remember { mutableStateOf("") }

    var selectedDesign by remember(defaultBillDesign) {
        mutableStateOf(BillDesignModel.fromLabel(defaultBillDesign))
    }
    var selectedSize by remember(defaultPrintSize) {
        mutableStateOf(BillPrintSize.fromLabel(defaultPrintSize))
    }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        dueReminderEnabled = granted
        if (!granted) {
            Toast.makeText(context, "Enable notifications to schedule due reminders", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(preselectedCustomerId) {
        if (!preselectedCustomerId.isNullOrBlank()) {
            selectedCustomerId = preselectedCustomerId
            val autoDue = data.dueForCustomer(preselectedCustomerId)
            manualPreviousDueText = formatCleanDouble(autoDue)
            manualPrevOverrideEnabled = false
            onConsumePreselectedCustomer()
        }
    }

    val selectedCustomer = remember(data.customers, selectedCustomerId) {
        data.customers.firstOrNull { it.id == selectedCustomerId }
    }
    val isManualCustomer = selectedCustomer == null
    val isPrevDueEditable = isManualCustomer || onlyPreviousDue || manualPrevOverrideEnabled

    val autoCustomerDue = remember(selectedCustomer, data) {
        selectedCustomer?.let { data.dueForCustomer(it.id) } ?: 0.0
    }

    val landQty = if (onlyPreviousDue) 0.0 else (landAmountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0)
    val rate = if (onlyPreviousDue) 0.0 else (rateText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0)
    val initialPaid = initialPaidText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val bigha = if (onlyPreviousDue) 0.0 else calculateBigha(landQty, selectedUnit)
    val currentBill = if (onlyPreviousDue) 0.0 else bigha * rate

    val previousDue = if (isPrevDueEditable) {
        manualPreviousDueText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    } else {
        autoCustomerDue
    }
    val grandTotal = currentBill + previousDue
    val remainingAfterPay = max(0.0, grandTotal - initialPaid)

    val draftBill = remember(
        selectedCustomer,
        manualName,
        manualPhone,
        manualVillage,
        manualAddress,
        billDate,
        season,
        landQty,
        selectedUnit,
        bigha,
        rate,
        currentBill,
        previousDue,
        grandTotal,
        note,
        onlyPreviousDue
    ) {
        val effectiveNote = if (onlyPreviousDue && note.isBlank()) {
            "Previous due"
        } else {
            note
        }
        Bill(
            id = "preview",
            billNo = "PREVIEW",
            date = billDate,
            customerId = selectedCustomer?.id ?: "preview",
            customerName = selectedCustomer?.name ?: manualName.ifBlank { "Preview Customer" },
            phone = selectedCustomer?.phone ?: manualPhone,
            village = selectedCustomer?.village ?: manualVillage,
            address = selectedCustomer?.address ?: manualAddress,
            season = if (onlyPreviousDue) "" else season,
            landAmount = landQty,
            unit = selectedUnit.key,
            bigha = bigha,
            rate = rate,
            current = currentBill,
            previousDue = previousDue,
            allTotal = grandTotal,
            note = effectiveNote
        )
    }

    val livePreviewBitmap = remember(
        draftBill,
        data.settings,
        initialPaid,
        remainingAfterPay,
        selectedDesign,
        selectedSize,
        qrMode,
        customQrPath
    ) {
        BillDocumentEngine.renderBillBitmap(
            settings = data.settings,
            bill = draftBill,
            payments = emptyList(),
            paid = initialPaid,
            due = remainingAfterPay,
            design = selectedDesign,
            size = selectedSize,
            qrMode = qrMode,
            customQrPath = customQrPath
        )
    }

    val pdfSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    BillDocumentEngine.writeBillPdfToStream(
                        outputStream = out,
                        settings = data.settings,
                        bill = draftBill,
                        payments = emptyList(),
                        paid = initialPaid,
                        due = remainingAfterPay,
                        design = selectedDesign,
                        size = selectedSize,
                        qrMode = qrMode,
                        customQrPath = customQrPath
                    )
                }
            }.onSuccess {
                Toast.makeText(context, "PDF saved", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleBillSaved(
        savedBillId: String,
        savedBillNo: String,
        custName: String,
        custPhone: String,
        dueAmt: Double
    ) {
        if (dueReminderEnabled && dueAmt > 0.001 && savedBillId.isNotBlank()) {
            val rem = BillDueReminder(
                billId = savedBillId,
                billNo = savedBillNo.ifBlank { "ASHA Bill" },
                customerName = custName,
                customerPhone = custPhone,
                dueAmount = dueAmt,
                dueDateIso = dueDateIso,
                hour = dueReminderHour,
                minute = 0
            )
            DueReminderScheduler.scheduleReminder(context, rem)
            if (DueReminderScheduler.isDueTodayOrEarlier(dueDateIso)) {
                DueReminderScheduler.showPersistentNotification(context, rem)
            }
        }
        manualName = ""
        manualPhone = ""
        manualVillage = ""
        manualAddress = ""
        manualPreviousDueText = "0"
        manualPrevOverrideEnabled = false
        landAmountText = "0"
        initialPaidText = "0"
        note = ""
        billDate = todayIsoDate()
        dueReminderEnabled = false
    }

    fun selectCustomerAndPrefill(customer: Customer) {
        selectedCustomerId = customer.id
        val cDueVal = data.dueForCustomer(customer.id)
        manualPreviousDueText = formatCleanDouble(cDueVal)
        manualPrevOverrideEnabled = false
        val numericArea = Regex("^\\d+(?:\\.\\d+)?").find(customer.landArea.trim())?.value
        if (!numericArea.isNullOrBlank() && (landAmountText == "0" || landAmountText.isBlank())) {
            landAmountText = numericArea
        }
    }

    fun applyScannedCode(rawCode: String) {
        val raw = rawCode.trim()
        if (raw.isEmpty()) return

        fun matchPhoneDigits(input: String): String {
            val digits = input.filter { it.isDigit() }
            return if (digits.length >= 10) digits.takeLast(10) else ""
        }

        fun applyUnitToken(token: String) {
            val lower = token.trim().lowercase(Locale.US)
            when {
                lower.contains("katha") || lower.contains("কাঠা") -> selectedUnit = LandUnit.KATHA
                lower.contains("dec") || lower.contains("শতক") -> selectedUnit = LandUnit.DECIMAL
                lower.contains("bigha") || lower.contains("বিঘা") -> selectedUnit = LandUnit.BIGHA
            }
        }

        // 1. ASHA_CUST:<customerId>
        if (raw.startsWith("ASHA_CUST:", ignoreCase = true)) {
            val custId = raw.substringAfter(":").trim()
            val cust = data.customers.firstOrNull { it.id == custId }
            if (cust != null) {
                selectCustomerAndPrefill(cust)
                Toast.makeText(context, "Customer loaded: ${cust.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Customer QR not found ($custId)", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // 2. JSON payload
        if (raw.startsWith("{") && raw.endsWith("}")) {
            val parsed = runCatching { JSONObject(raw) }.getOrNull()
            if (parsed != null) {
                val cid = parsed.optString("customerId").trim()
                val phoneDigits = matchPhoneDigits(parsed.optString("phone"))
                val matchedCust = data.customers.firstOrNull {
                    (cid.isNotBlank() && it.id == cid) ||
                        (phoneDigits.isNotBlank() && matchPhoneDigits(it.phone) == phoneDigits)
                }
                if (matchedCust != null) {
                    selectCustomerAndPrefill(matchedCust)
                } else {
                    selectedCustomerId = ""
                    parsed.optString("name").takeIf { it.isNotBlank() }?.let { manualName = it }
                    parsed.optString("phone").takeIf { it.isNotBlank() }?.let { manualPhone = it }
                    parsed.optString("village").takeIf { it.isNotBlank() }?.let { manualVillage = it }
                }
                parsed.optString("area").takeIf { it.isNotBlank() }?.let { landAmountText = it }
                parsed.optString("land").takeIf { it.isNotBlank() }?.let { landAmountText = it }
                parsed.optString("unit").takeIf { it.isNotBlank() }?.let { applyUnitToken(it) }
                parsed.optString("rate").takeIf { it.isNotBlank() }?.let { rateText = it }
                parsed.optString("season").takeIf { it.isNotBlank() }?.let { season = it }
                parsed.optString("note").takeIf { it.isNotBlank() }?.let { note = it }
                Toast.makeText(context, "QR details applied", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 3. Structured ASHA_PROD / delimited Key-Value tags (e.g., ASHA_PROD|RATE:2400|AREA:2.5|UNIT:bigha)
        if (raw.startsWith("ASHA_PROD", ignoreCase = true) ||
            raw.contains("RATE:", ignoreCase = true) ||
            raw.contains("AREA:", ignoreCase = true) ||
            raw.contains("PHONE:", ignoreCase = true)
        ) {
            var updatedAny = false
            raw.split("|", ";", "\n").forEach { part ->
                val sep = if (part.contains("=")) "=" else if (part.contains(":")) ":" else ""
                if (sep.isNotEmpty()) {
                    val k = part.substringBefore(sep).trim().uppercase(Locale.US)
                    val v = part.substringAfter(sep).trim()
                    if (v.isNotBlank()) {
                        when (k) {
                            "CUST", "CUSTOMER", "ID" -> {
                                data.customers.firstOrNull { it.id == v }?.let {
                                    selectCustomerAndPrefill(it)
                                    updatedAny = true
                                }
                            }
                            "PHONE", "PH" -> {
                                val target = matchPhoneDigits(v)
                                val byPhone = data.customers.firstOrNull { matchPhoneDigits(it.phone) == target }
                                if (byPhone != null) selectCustomerAndPrefill(byPhone) else manualPhone = v
                                updatedAny = true
                            }
                            "AREA", "LAND", "QTY" -> {
                                landAmountText = v
                                updatedAny = true
                            }
                            "RATE" -> {
                                rateText = v
                                updatedAny = true
                            }
                            "UNIT" -> {
                                applyUnitToken(v)
                                updatedAny = true
                            }
                            "SEASON" -> {
                                season = v
                                updatedAny = true
                            }
                            "NOTE", "PRODUCT" -> {
                                note = v
                                updatedAny = true
                            }
                        }
                    }
                }
            }
            if (updatedAny) {
                Toast.makeText(context, "Product / Tag applied to bill", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 4. UPI QR (upi://pay?pa=...&pn=...)
        if (raw.startsWith("upi://", ignoreCase = true)) {
            val uri = runCatching { android.net.Uri.parse(raw) }.getOrNull()
            val pa = uri?.getQueryParameter("pa").orEmpty()
            val pn = uri?.getQueryParameter("pn").orEmpty()
            val paPhone = matchPhoneDigits(pa)
            val matchedCust = data.customers.firstOrNull {
                (paPhone.isNotBlank() && matchPhoneDigits(it.phone) == paPhone) ||
                    (pn.isNotBlank() && it.name.equals(pn, ignoreCase = true))
            }
            if (matchedCust != null) {
                selectCustomerAndPrefill(matchedCust)
                Toast.makeText(context, "Customer matched from UPI: ${matchedCust.name}", Toast.LENGTH_SHORT).show()
            } else {
                selectedCustomerId = ""
                if (pn.isNotBlank()) manualName = pn
                if (paPhone.isNotBlank()) manualPhone = paPhone
                Toast.makeText(context, "UPI customer info loaded", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // 5. Match existing Bill No
        val billMatch = data.bills.firstOrNull { it.billNo.equals(raw, ignoreCase = true) }
        if (billMatch != null) {
            val cust = data.customers.firstOrNull { it.id == billMatch.customerId }
            if (cust != null) {
                selectCustomerAndPrefill(cust)
                Toast.makeText(context, "Customer loaded from ${billMatch.billNo}: ${cust.name}", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 6. Match 10-digit phone or name/barcode
        val digits10 = matchPhoneDigits(raw)
        if (digits10.isNotBlank()) {
            val phoneMatch = data.customers.firstOrNull { matchPhoneDigits(it.phone) == digits10 }
            if (phoneMatch != null) {
                selectCustomerAndPrefill(phoneMatch)
                Toast.makeText(context, "Customer matched: ${phoneMatch.name}", Toast.LENGTH_SHORT).show()
            } else {
                selectedCustomerId = ""
                manualPhone = digits10
                Toast.makeText(context, "Phone $digits10 filled for manual customer", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val nameMatch = data.customers.firstOrNull {
            it.id == raw || it.name.equals(raw, ignoreCase = true)
        }
        if (nameMatch != null) {
            selectCustomerAndPrefill(nameMatch)
            Toast.makeText(context, "Customer loaded: ${nameMatch.name}", Toast.LENGTH_SHORT).show()
            return
        }

        note = if (note.isBlank()) "Code: $raw" else "$note · Code: $raw"
        Toast.makeText(context, "Barcode added to note: $raw", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                            containerColor = MaterialTheme.colorScheme.primary,
                            size = 36.dp,
                            iconSize = 19.dp
                        )
                        Text(
                            text = "New Bill",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        onClick = { showBarcodeScanner = true },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("scan_barcode_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = "Scan Customer or Product QR/Barcode",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Scan QR",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        selected = !onlyPreviousDue,
                        onClick = { onlyPreviousDue = false },
                        modifier = Modifier.testTag("bill_mode_full")
                    ) {
                        Text("Land Bill", fontWeight = FontWeight.SemiBold)
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        selected = onlyPreviousDue,
                        onClick = {
                            onlyPreviousDue = true
                            manualPrevOverrideEnabled = true
                            if (!isManualCustomer && (manualPreviousDueText == "0" || manualPreviousDueText.isBlank())) {
                                manualPreviousDueText = formatCleanDouble(autoCustomerDue)
                            }
                        },
                        modifier = Modifier.testTag("bill_mode_previous_due_only")
                    ) {
                        Text("Previous Due Only", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = customerExpanded,
                    onExpandedChange = { customerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.let {
                            if (it.phone.isNotBlank()) "${it.name} · ${it.phone}" else it.name
                        } ?: "+ New customer manually",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Customer") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("bill_customer_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = customerExpanded,
                        onDismissRequest = { customerExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("+ New customer manually", fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                selectedCustomerId = ""
                                manualPreviousDueText = "0"
                                customerExpanded = false
                            }
                        )
                        data.customers.forEach { customer ->
                            val cDue = data.dueForCustomer(customer.id)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = buildString {
                                                append(customer.name)
                                                if (customer.village.isNotBlank()) append(" (${customer.village})")
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (cDue > 0.001) {
                                            Text(
                                                text = formatInr(cDue),
                                                color = CrimsonDue,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectCustomerAndPrefill(customer)
                                    customerExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Clean Previous Due / আগের বাকি Card matching reference photo (NO extra instruction text)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
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
                            Text(
                                text = "Previous Due / আগের বাকি",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!isManualCustomer && !onlyPreviousDue) {
                                Surface(
                                    onClick = {
                                        manualPrevOverrideEnabled = !manualPrevOverrideEnabled
                                        if (manualPrevOverrideEnabled) {
                                            manualPreviousDueText = formatCleanDouble(autoCustomerDue)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.testTag("toggle_manual_previous_due")
                                ) {
                                    Text(
                                        text = if (isPrevDueEditable) "Manual" else "Auto",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = if (isPrevDueEditable) manualPreviousDueText else formatCleanDouble(previousDue),
                            onValueChange = { if (isPrevDueEditable) manualPreviousDueText = it },
                            readOnly = !isPrevDueEditable,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bill_previous_due_input")
                        )
                    }
                }

                AnimatedVisibility(visible = isManualCustomer) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = manualName,
                                    onValueChange = { manualName = it },
                                    placeholder = { Text("Name") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("bill_manual_name_input")
                                )
                                OutlinedTextField(
                                    value = manualPhone,
                                    onValueChange = { manualPhone = it },
                                    placeholder = { Text("Phone") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("bill_manual_phone_input")
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = manualVillage,
                                    onValueChange = { manualVillage = it },
                                    placeholder = { Text("Village") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("bill_manual_village_input")
                                )
                                OutlinedTextField(
                                    value = manualAddress,
                                    onValueChange = { manualAddress = it },
                                    placeholder = { Text("Address") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("bill_manual_address_input")
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = !onlyPreviousDue) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = seasonExpanded,
                                onExpandedChange = { seasonExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = season,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Season") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = seasonExpanded) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                        .testTag("bill_season_input")
                                )
                                ExposedDropdownMenu(
                                    expanded = seasonExpanded,
                                    onDismissRequest = { seasonExpanded = false }
                                ) {
                                    SEASON_OPTIONS.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                season = option
                                                seasonExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = unitExpanded,
                                onExpandedChange = { unitExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = unitBengaliLabel(selectedUnit),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Land Unit") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                        .testTag("bill_unit_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = unitExpanded,
                                    onDismissRequest = { unitExpanded = false }
                                ) {
                                    LandUnit.entries.forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unitBengaliLabel(unit)) },
                                            onClick = {
                                                selectedUnit = unit
                                                unitExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = landAmountText,
                                onValueChange = { landAmountText = it },
                                label = { Text("Land") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bill_land_amount_input")
                            )
                            OutlinedTextField(
                                value = rateText,
                                onValueChange = { rateText = it },
                                label = { Text("Rate / Bigha") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bill_rate_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rates:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            listOf("2000", "2200", "2400", "2500").forEach { preset ->
                                FilterChip(
                                    selected = rateText == preset,
                                    onClick = { rateText = preset },
                                    label = { Text("₹$preset") }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = initialPaidText,
                        onValueChange = { initialPaidText = it },
                        label = { Text("Paid Now") },
                        trailingIcon = {
                            if (grandTotal > 0.0) {
                                Text(
                                    text = "Full",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { initialPaidText = formatCleanDouble(grandTotal) }
                                        .padding(end = 10.dp)
                                        .testTag("bill_full_paid_chip")
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bill_initial_paid_input")
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("bill_mode_dropdown")
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
                                        if (receivedIn.isBlank() || receivedIn == "Cash" || receivedIn == "UPI") {
                                            receivedIn = mode.label
                                        }
                                        modeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = receivedIn,
                        onValueChange = { receivedIn = it },
                        label = { Text("Received In") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bill_received_in_input")
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker = true }
                    ) {
                        OutlinedTextField(
                            value = billDate,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Bill Date") },
                            trailingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bill_date_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bill_note_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Persistent Due Date Notification Schedule Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IosSquircleIcon(
                                    icon = Icons.Rounded.Alarm,
                                    containerColor = Color(0xFFF59E0B),
                                    size = 32.dp,
                                    iconSize = 17.dp
                                )
                                Text(
                                    text = "Due Date Notification",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = dueReminderEnabled,
                                onCheckedChange = { enable ->
                                    if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !DueReminderScheduler.hasNotificationPermission(context)
                                    ) {
                                        notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        dueReminderEnabled = enable
                                    }
                                },
                                modifier = Modifier.testTag("due_reminder_switch")
                            )
                        }

                        AnimatedVisibility(visible = dueReminderEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = { showDueDatePicker = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.testTag("due_reminder_date_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CalendarToday,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = dueDateIso,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                                FilterChip(
                                    selected = dueDateIso == todayIsoDate(),
                                    onClick = { dueDateIso = todayIsoDate() },
                                    label = { Text("Today") }
                                )
                                listOf(9 to "9 AM", 18 to "6 PM").forEach { (hr, lbl) ->
                                    FilterChip(
                                        selected = dueReminderHour == hr,
                                        onClick = { dueReminderHour = hr },
                                        label = { Text(lbl) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Calculated Totals Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (onlyPreviousDue) {
                                    "Previous Due"
                                } else {
                                    "${formatInr(currentBill)} · ${String.format(Locale.US, "%.2f", bigha)} Bigha"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Due: ${formatInr(remainingAfterPay)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = formatInr(grandTotal),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val saveNote = if (onlyPreviousDue && note.isBlank()) {
                                "Previous due"
                            } else {
                                note
                            }
                            if (isManualCustomer) {
                                onSaveManualCustomerBill(
                                    manualName,
                                    manualPhone,
                                    manualVillage,
                                    manualAddress,
                                    previousDue,
                                    if (onlyPreviousDue) "" else season,
                                    billDate,
                                    landQty,
                                    selectedUnit,
                                    rate,
                                    initialPaid,
                                    selectedMode,
                                    receivedIn,
                                    saveNote
                                ) { newId, newBillNo ->
                                    handleBillSaved(newId, newBillNo, manualName, manualPhone, remainingAfterPay)
                                }
                            } else {
                                val cust = selectedCustomer!!
                                onSaveExistingCustomerBill(
                                    cust,
                                    if (onlyPreviousDue) "" else season,
                                    billDate,
                                    landQty,
                                    selectedUnit,
                                    rate,
                                    initialPaid,
                                    selectedMode,
                                    receivedIn,
                                    saveNote,
                                    if (isPrevDueEditable) previousDue else null
                                ) { newId, newBillNo ->
                                    handleBillSaved(newId, newBillNo, cust.name, cust.phone, remainingAfterPay)
                                }
                            }
                        },
                        enabled = grandTotal > 0.0 && (!isManualCustomer || manualName.isNotBlank()),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp)
                            .testTag("save_bill_button")
                    ) {
                        Text(stringResource(R.string.save_bill), fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { onPreviewBill(draftBill, selectedDesign, selectedSize) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("preview_bill_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Wallet Pass", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Bill Design Models & Live Preview Card
        item {
            AshaPanelCard {
                Text(
                    text = "Bill Pass & Sheet Preview",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(model.primaryColorInt))
                                )
                            },
                            label = { Text(model.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("bill_model_chip_${model.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

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
                            modifier = Modifier.testTag("bill_size_chip_${size.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPreviewBill(draftBill, selectedDesign, selectedSize) }
                ) {
                    val aspect = livePreviewBitmap.width.toFloat() / livePreviewBitmap.height.toFloat()
                    Image(
                        bitmap = livePreviewBitmap.asImageBitmap(),
                        contentDescription = "Live Bill Preview",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspect)
                            .testTag("new_bill_live_preview_image")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NewBillActionIconTile(
                        icon = Icons.Rounded.Print,
                        label = "Print",
                        iconColor = MaterialTheme.colorScheme.primary,
                        cardColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        tag = "new_bill_print_icon",
                        onClick = {
                            InvoicePrinter.printInvoice(
                                context = context,
                                settings = data.settings,
                                bill = draftBill,
                                payments = emptyList(),
                                paid = initialPaid,
                                due = remainingAfterPay,
                                design = selectedDesign,
                                printSize = selectedSize,
                                qrMode = qrMode,
                                customQrPath = customQrPath
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NewBillActionIconTile(
                        icon = Icons.Rounded.Image,
                        label = "WA Bill",
                        iconColor = Color(0xFF25D366),
                        cardColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        tag = "new_bill_image_reminder_icon",
                        onClick = {
                            BillDocumentEngine.sendBillImageReminderToWhatsApp(
                                context = context,
                                settings = data.settings,
                                bill = draftBill,
                                payments = emptyList(),
                                paid = initialPaid,
                                due = remainingAfterPay,
                                design = selectedDesign,
                                size = selectedSize,
                                qrMode = qrMode,
                                customQrPath = customQrPath
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NewBillActionIconTile(
                        icon = Icons.Rounded.PictureAsPdf,
                        label = "PDF",
                        iconColor = Color(0xFFEF4444),
                        cardColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        tag = "new_bill_pdf_icon",
                        onClick = {
                            val cleanName = draftBill.customerName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            pdfSaverLauncher.launch("ASHA_${cleanName}_${draftBill.date}.pdf")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NewBillActionIconTile(
                        icon = Icons.Rounded.NotificationsActive,
                        label = "Remind",
                        iconColor = Color(0xFFF59E0B),
                        cardColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        tag = "new_bill_remind_icon",
                        onClick = {
                            val msg = BillDocumentEngine.buildBillReminderMessage(
                                settings = data.settings,
                                bill = draftBill,
                                due = remainingAfterPay
                            )
                            InvoicePrinter.openWhatsAppOrShare(context, draftBill.phone, msg)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        AshaDatePickerDialog(
            initialDateIso = billDate,
            onDateSelected = { billDate = it },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showDueDatePicker) {
        AshaDatePickerDialog(
            initialDateIso = dueDateIso,
            onDateSelected = { dueDateIso = it },
            onDismiss = { showDueDatePicker = false }
        )
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { scanned ->
                showBarcodeScanner = false
                applyScannedCode(scanned)
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}

@Composable
private fun NewBillActionIconTile(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    cardColor: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        modifier = modifier
            .height(66.dp)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IosSquircleIcon(
                icon = icon,
                contentDescription = label,
                containerColor = iconColor,
                size = 30.dp,
                iconSize = 16.dp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun unitBengaliLabel(unit: LandUnit): String = when (unit) {
    LandUnit.BIGHA -> "বিঘা (Bigha)"
    LandUnit.KATHA -> "কাঠা (Katha)"
    LandUnit.DECIMAL -> "শতক (Decimal)"
}

private fun formatCleanDouble(v: Double): String {
    return if (v == v.toLong().toDouble()) v.toLong().toString()
    else String.format(Locale.US, "%.2f", v)
}
