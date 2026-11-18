package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Bill
import com.example.data.model.BillingDashboardData
import com.example.data.model.BusinessSettings
import com.example.data.model.todayIsoDate
import com.example.security.AppPreferencesStore
import com.example.security.AppThemeMode
import com.example.security.ThemeColorOption
import com.example.security.checkBiometricAvailability
import com.example.security.launchBiometricPrompt
import com.example.security.loadBitmapFromPath
import com.example.ui.components.AshaPanelCard
import com.example.ui.components.IosSquircleIcon
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDue
import com.example.util.BillDesignModel
import com.example.util.BillDocumentEngine
import com.example.util.BillPrintSize

private val QR_MODES = listOf("Dynamic amount QR", "Static QR image", "UPI link only")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    data: BillingDashboardData,
    prefsStore: AppPreferencesStore,
    userEmail: String?,
    canEditAppInfo: Boolean = true,
    cloudStatus: String,
    isSyncing: Boolean,
    onSaveSettings: (BusinessSettings) -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings = data.settings
    val localPrefs by prefsStore.state.collectAsStateWithLifecycle()
    val biometricStatus = remember(context) { checkBiometricAvailability(context) }

    var company by remember(settings) { mutableStateOf(settings.company) }
    var owner by remember(settings) { mutableStateOf(settings.owner) }
    var contact by remember(settings) { mutableStateOf(settings.contact) }
    var address by remember(settings) { mutableStateOf(settings.address) }
    var upi by remember(settings) { mutableStateOf(settings.upi) }
    var payee by remember(settings) { mutableStateOf(settings.payee) }

    var qrModeExpanded by remember { mutableStateOf(false) }
    var showCloudDoctor by remember { mutableStateOf(false) }
    var biometricTestStatus by remember { mutableStateOf<String?>(null) }

    val qrPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            prefsStore.saveCustomQrFromUri(uri)
        }
    }

    val customQrBitmap = remember(localPrefs.qrImagePath) {
        loadBitmapFromPath(localPrefs.qrImagePath)
    }

    val selectedDesign = BillDesignModel.fromLabel(localPrefs.defaultBillTemplate)
    val selectedSize = BillPrintSize.fromLabel(localPrefs.defaultPrintSize)

    val sampleBill = remember(settings) {
        Bill(
            id = "sample",
            billNo = "ASHA-2026-0001",
            date = todayIsoDate(),
            customerId = "sample",
            customerName = "Sample Farmer",
            phone = "9876543210",
            village = "Raghunathpur",
            address = "Ghatal",
            season = "খরা",
            landAmount = 2.0,
            unit = "bigha",
            bigha = 2.0,
            rate = 2200.0,
            current = 4400.0,
            previousDue = 600.0,
            allTotal = 5000.0,
            note = "Sample preview for ${selectedDesign.label}"
        )
    }

    val sampleBillBitmap = remember(
        sampleBill,
        settings,
        selectedDesign,
        selectedSize,
        localPrefs.qrMode,
        localPrefs.qrImagePath
    ) {
        BillDocumentEngine.renderBillBitmap(
            settings = settings,
            bill = sampleBill,
            payments = emptyList(),
            paid = 2000.0,
            due = 3000.0,
            design = selectedDesign,
            size = selectedSize,
            qrMode = localPrefs.qrMode,
            customQrPath = localPrefs.qrImagePath
        )
    }

    LaunchedEffect(settings) {
        company = settings.company
        owner = settings.owner
        contact = settings.contact
        address = settings.address
        upi = settings.upi
        payee = settings.payee
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Appearance: Light / Dark / System Mode + Accent Swatches
        item {
            AshaPanelCard {
                SettingsSectionHeader(
                    icon = Icons.Rounded.Palette,
                    badgeColor = Color(0xFF007AFF),
                    title = "Appearance",
                    subtitle = null
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Display Mode",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AppThemeMode.entries.forEachIndexed { idx, mode ->
                        val modeIcon = when (mode) {
                            AppThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                            AppThemeMode.LIGHT -> Icons.Rounded.LightMode
                            AppThemeMode.DARK -> Icons.Rounded.DarkMode
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = idx,
                                count = AppThemeMode.entries.size
                            ),
                            selected = localPrefs.themeMode == mode,
                            onClick = { prefsStore.update { it.copy(themeMode = mode) } },
                            icon = {
                                Icon(
                                    imageVector = modeIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("theme_mode_${mode.key}")
                        ) {
                            Text(mode.label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "App Theme Colour",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeColorOption.entries.forEach { option ->
                        val selected = localPrefs.themeColorKey == option.key
                        Surface(
                            onClick = { prefsStore.update { it.copy(themeColorKey = option.key) } },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) Color(option.primaryHex) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_color_${option.key}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(option.primaryHex))
                                        .border(
                                            width = if (selected) 2.dp else 0.dp,
                                            color = if (selected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = option.label,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = option.label.substringAfter(" "),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Business Profile & UPI / QR Settings Card
        item {
            AshaPanelCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSectionHeader(
                        icon = Icons.Rounded.Storefront,
                        badgeColor = Color(0xFF0B6B50),
                        title = "Business Info & UPI",
                        subtitle = if (canEditAppInfo) null else "Owner-only edit access",
                        subtitleColor = if (canEditAppInfo) MaterialTheme.colorScheme.onSurfaceVariant else CrimsonDue,
                        modifier = Modifier.weight(1f)
                    )
                    if (!canEditAppInfo) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CrimsonContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Locked",
                                    tint = CrimsonDue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Locked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrimsonDue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = company,
                    onValueChange = { if (canEditAppInfo) company = it },
                    enabled = canEditAppInfo,
                    label = { Text("Company Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_company_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = owner,
                        onValueChange = { if (canEditAppInfo) owner = it },
                        enabled = canEditAppInfo,
                        label = { Text("Owner") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_owner_input")
                    )
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { if (canEditAppInfo) contact = it },
                        enabled = canEditAppInfo,
                        label = { Text("Contact") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_contact_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { if (canEditAppInfo) address = it },
                    enabled = canEditAppInfo,
                    label = { Text("Address") },
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_address_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = upi,
                        onValueChange = { if (canEditAppInfo) upi = it },
                        enabled = canEditAppInfo,
                        label = { Text("UPI ID") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_upi_input")
                    )
                    OutlinedTextField(
                        value = payee,
                        onValueChange = { if (canEditAppInfo) payee = it },
                        enabled = canEditAppInfo,
                        label = { Text(stringResource(R.string.payee_name)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_payee_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = qrModeExpanded,
                    onExpandedChange = { if (canEditAppInfo) qrModeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = localPrefs.qrMode,
                        onValueChange = {},
                        readOnly = true,
                        enabled = canEditAppInfo,
                        label = { Text("QR Mode") },
                        leadingIcon = { Icon(Icons.Rounded.QrCode2, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = qrModeExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("settings_qr_mode")
                    )
                    ExposedDropdownMenu(
                        expanded = qrModeExpanded,
                        onDismissRequest = { qrModeExpanded = false }
                    ) {
                        QR_MODES.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode) },
                                onClick = {
                                    prefsStore.update { it.copy(qrMode = mode) }
                                    qrModeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Custom Static QR Image",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            qrPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = canEditAppInfo,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("upload_qr_button")
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (customQrBitmap != null) "Change QR" else "Upload QR")
                    }

                    OutlinedButton(
                        onClick = { prefsStore.removeCustomQr() },
                        enabled = canEditAppInfo && customQrBitmap != null,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("remove_qr_button")
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove")
                    }
                }

                if (customQrBitmap != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                bitmap = customQrBitmap,
                                contentDescription = "Custom payment QR",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Text("Custom QR Ready", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (canEditAppInfo) {
                            onSaveSettings(
                                settings.copy(
                                    company = company,
                                    owner = owner,
                                    contact = contact,
                                    upi = upi,
                                    address = address,
                                    payee = payee
                                )
                            )
                        }
                    },
                    enabled = canEditAppInfo,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_settings_button")
                ) {
                    Icon(
                        imageVector = if (canEditAppInfo) Icons.Rounded.Save else Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (canEditAppInfo) "Save Business Settings" else "Locked · Authorized Owners Only",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Default Bill Models & Paper Sizes Gallery with Live Preview
        item {
            AshaPanelCard {
                SettingsSectionHeader(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    badgeColor = Color(0xFFFF9500),
                    title = "Bill Models & Paper Size",
                    subtitle = null
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Bill Design Model",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BillDesignModel.entries.forEach { model ->
                        val active = selectedDesign == model
                        FilterChip(
                            selected = active,
                            onClick = { prefsStore.update { it.copy(defaultBillTemplate = model.label) } },
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
                            modifier = Modifier.testTag("settings_bill_model_${model.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Default Paper / Roll Size",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BillPrintSize.entries.forEach { size ->
                        val active = selectedSize == size
                        FilterChip(
                            selected = active,
                            onClick = { prefsStore.update { it.copy(defaultPrintSize = size.label) } },
                            label = { Text(size.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("settings_bill_size_${size.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Template Live Preview (${selectedDesign.label} · ${selectedSize.label})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val ratio = sampleBillBitmap.width.toFloat() / sampleBillBitmap.height.toFloat()
                    Image(
                        bitmap = sampleBillBitmap.asImageBitmap(),
                        contentDescription = "Sample Bill Model Preview",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(ratio)
                            .testTag("settings_sample_bill_preview")
                    )
                }
            }
        }

        // 4. Biometric Security Layer Card
        item {
            AshaPanelCard {
                SettingsSectionHeader(
                    icon = Icons.Rounded.Fingerprint,
                    badgeColor = Color(0xFF5856D6),
                    title = "Biometric Security",
                    subtitle = biometricStatus.message,
                    subtitleColor = if (biometricStatus.isAvailable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SecurityToggleRow(
                    title = "Fingerprint / Face Unlock",
                    subtitle = null,
                    checked = localPrefs.biometricEnabled,
                    enabled = biometricStatus.isAvailable,
                    tag = "biometric_master_switch",
                    onCheckedChange = { wantEnable ->
                        if (wantEnable) {
                            launchBiometricPrompt(
                                context = context,
                                title = "Enable Biometric Lock",
                                subtitle = "Confirm fingerprint or face to enable security lock",
                                onSuccess = {
                                    prefsStore.update { it.copy(biometricEnabled = true) }
                                    biometricTestStatus = "Biometric lock enabled"
                                },
                                onError = { msg -> biometricTestStatus = msg },
                                onCancelled = { biometricTestStatus = "Setup cancelled" }
                            )
                        } else {
                            prefsStore.update { it.copy(biometricEnabled = false) }
                            biometricTestStatus = "Biometric lock disabled"
                        }
                    }
                )

                SecurityToggleRow(
                    title = "Protect Billing Modules",
                    subtitle = null,
                    checked = localPrefs.protectBilling,
                    enabled = localPrefs.biometricEnabled && biometricStatus.isAvailable,
                    tag = "biometric_billing_switch",
                    onCheckedChange = { checked ->
                        prefsStore.update { it.copy(protectBilling = checked) }
                    }
                )

                SecurityToggleRow(
                    title = "Protect Calculator",
                    subtitle = null,
                    checked = localPrefs.protectCalculator,
                    enabled = localPrefs.biometricEnabled && biometricStatus.isAvailable,
                    tag = "biometric_calculator_switch",
                    onCheckedChange = { checked ->
                        prefsStore.update { it.copy(protectCalculator = checked) }
                    }
                )

                SecurityToggleRow(
                    title = "Unlock on Launch",
                    subtitle = null,
                    checked = localPrefs.biometricOnLogin,
                    enabled = localPrefs.biometricEnabled && biometricStatus.isAvailable,
                    tag = "biometric_login_switch",
                    onCheckedChange = { checked ->
                        prefsStore.update { it.copy(biometricOnLogin = checked) }
                    }
                )

                if (!biometricTestStatus.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = biometricTestStatus.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        launchBiometricPrompt(
                            context = context,
                            title = "Test Biometric Unlock",
                            subtitle = "Verify fingerprint or face sensor",
                            onSuccess = { biometricTestStatus = "Biometric check succeeded!" },
                            onError = { msg -> biometricTestStatus = msg },
                            onCancelled = { biometricTestStatus = "Verification cancelled" }
                        )
                    },
                    enabled = biometricStatus.isAvailable,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("test_biometric_button")
                ) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Biometric Unlock")
                }
            }
        }

        // 5. Cloud Sync, Diagnostics & Logout Card
        item {
            AshaPanelCard {
                SettingsSectionHeader(
                    icon = Icons.Rounded.HealthAndSafety,
                    badgeColor = Color(0xFF30B0C7),
                    title = "Cloud Sync & Account",
                    subtitle = "${userEmail ?: "Owner Account"} · $cloudStatus"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSyncNow,
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pull_cloud_button")
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isSyncing) "Syncing…" else "Pull Cloud", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = {
                            if (canEditAppInfo) {
                                onSaveSettings(
                                    settings.copy(
                                        company = company,
                                        owner = owner,
                                        contact = contact,
                                        upi = upi,
                                        address = address,
                                        payee = payee
                                    )
                                )
                            }
                        },
                        enabled = canEditAppInfo,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("push_cloud_button")
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Push Cloud", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCloudDoctor = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cloud_doctor_button")
                    ) {
                        Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Diagnostics", fontWeight = FontWeight.SemiBold)
                    }
                    Surface(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(14.dp),
                        color = CrimsonContainer,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = null,
                                tint = CrimsonDue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Logout",
                                style = MaterialTheme.typography.labelLarge,
                                color = CrimsonDue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCloudDoctor) {
        AlertDialog(
            onDismissRequest = { showCloudDoctor = false },
            title = { Text("Cloud Doctor Diagnostics") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account: ${userEmail ?: "Google Account"}")
                    Text("Sync Status: $cloudStatus")
                    Text("Theme Mode: ${localPrefs.themeMode.label}")
                    Text("Default Model: ${localPrefs.defaultBillTemplate}")
                    Text("Customers: ${data.customers.size}")
                    Text("Invoices: ${data.bills.size}")
                    Text("Payments: ${data.payments.size}")
                    Text("QR Mode: ${localPrefs.qrMode}")
                    Text("Biometric Status: ${biometricStatus.message}")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showCloudDoctor = false
                    onSyncNow()
                }) {
                    Text("Force Cloud Sync")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloudDoctor = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    badgeColor: Color = Color(0xFF0B6B50),
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IosSquircleIcon(
            icon = icon,
            containerColor = badgeColor,
            size = 38.dp,
            iconSize = 20.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor
                )
            }
        }
    }
}

@Composable
private fun SecurityToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = Modifier.testTag(tag)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
