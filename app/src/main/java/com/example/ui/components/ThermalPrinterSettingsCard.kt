package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsBluetooth
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.AppLocalPreferences
import com.example.security.AppPreferencesStore
import com.example.security.ThermalConnectionType
import com.example.security.ThermalPaperWidth
import com.example.util.PairedBtPrinter
import com.example.util.ThermalPrinterManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ThermalPrinterSettingsCard(
    localPrefs: AppLocalPreferences,
    prefsStore: AppPreferencesStore,
    companyName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalAshaHaptics.current
    val scope = rememberCoroutineScope()

    var hasBtPerm by remember { mutableStateOf(ThermalPrinterManager.hasBluetoothPermission(context)) }
    var pairedDevices by remember {
        mutableStateOf(
            if (hasBtPerm) ThermalPrinterManager.listPairedBluetoothDevices(context) else emptyList()
        )
    }
    var wifiHostText by remember(localPrefs.printerWifiHost) { mutableStateOf(localPrefs.printerWifiHost) }
    var wifiPortText by remember(localPrefs.printerWifiPort) { mutableStateOf(localPrefs.printerWifiPort.toString()) }
    var customMmText by remember(localPrefs.printerCustomWidthMm) { mutableStateOf(localPrefs.printerCustomWidthMm.toString()) }
    var isTesting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.all { it } || ThermalPrinterManager.hasBluetoothPermission(context)
        hasBtPerm = granted
        if (granted) {
            pairedDevices = ThermalPrinterManager.listPairedBluetoothDevices(context)
            statusMessage = if (pairedDevices.isEmpty()) {
                "No paired Bluetooth devices found — pair your printer in Android Bluetooth Settings first."
            } else {
                "Found ${pairedDevices.size} paired Bluetooth device(s)"
            }
            statusIsError = false
        } else {
            statusMessage = "Bluetooth permission is required to connect thermal printers."
            statusIsError = true
        }
    }

    fun requestBtAndRefresh() {
        haptics.tap()
        if (ThermalPrinterManager.hasBluetoothPermission(context)) {
            hasBtPerm = true
            pairedDevices = ThermalPrinterManager.listPairedBluetoothDevices(context)
            statusMessage = if (pairedDevices.isEmpty()) {
                "No paired Bluetooth devices found. Tap 'Pair New Device' to pair your printer."
            } else {
                "Refreshed: ${pairedDevices.size} paired Bluetooth device(s)"
            }
            statusIsError = false
        } else {
            val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
            } else {
                arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
            }
            btPermissionLauncher.launch(perms)
        }
    }

    AshaPanelCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IosSquircleIcon(
                icon = when (localPrefs.printerConnectionType) {
                    ThermalConnectionType.BLUETOOTH -> Icons.Rounded.BluetoothConnected
                    ThermalConnectionType.WIFI_TCP -> Icons.Rounded.Wifi
                    ThermalConnectionType.SYSTEM_PRINT -> Icons.Rounded.Print
                },
                containerColor = Color(0xFF0EA5E9),
                size = 38.dp,
                iconSize = 20.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Universal Thermal Printer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = ThermalPrinterManager.configuredSummary(localPrefs),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ThermalPrinterManager.isConfigured(localPrefs)) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Connection Interface (Any ESC/POS Printer)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThermalConnectionType.entries.forEachIndexed { idx, connType ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = idx,
                        count = ThermalConnectionType.entries.size
                    ),
                    selected = localPrefs.printerConnectionType == connType,
                    onClick = {
                        haptics.tick()
                        prefsStore.update { it.copy(printerConnectionType = connType) }
                        if (connType == ThermalConnectionType.BLUETOOTH) {
                            requestBtAndRefresh()
                        }
                    },
                    icon = {},
                    modifier = Modifier.testTag("printer_conn_${connType.key}")
                ) {
                    Text(
                        text = connType.label,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Bluetooth Device Picker
        AnimatedVisibility(visible = localPrefs.printerConnectionType == ThermalConnectionType.BLUETOOTH) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { requestBtAndRefresh() },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("bt_printer_scan_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh Paired", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("bt_printer_pair_settings_button")
                    ) {
                        Icon(Icons.Rounded.SettingsBluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pair New Device", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (pairedDevices.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (!hasBtPerm) {
                                "Tap 'Refresh Paired' to grant Bluetooth permission and select your printer."
                            } else {
                                "No paired Bluetooth devices found. Pair your 58mm/80mm/110mm BT printer in Android Bluetooth Settings, then tap Refresh."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    pairedDevices.take(8).forEach { device: PairedBtPrinter ->
                        val isSelected = localPrefs.printerBluetoothAddress.equals(device.address, ignoreCase = true)
                        Surface(
                            onClick = {
                                haptics.confirm()
                                prefsStore.update {
                                    it.copy(
                                        printerBluetoothName = device.name,
                                        printerBluetoothAddress = device.address
                                    )
                                }
                                statusMessage = "Selected ${device.name} (${device.address})"
                                statusIsError = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bt_device_${device.address}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Bluetooth,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = device.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = buildString {
                                            append(device.address)
                                            if (device.isLikelyPrinter) append(" · Thermal Printer")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Wi-Fi / LAN TCP/IP Settings
        AnimatedVisibility(visible = localPrefs.printerConnectionType == ThermalConnectionType.WIFI_TCP) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = wifiHostText,
                        onValueChange = { raw ->
                            wifiHostText = raw
                            prefsStore.update { it.copy(printerWifiHost = raw.trim()) }
                        },
                        label = { Text("Printer IP Address") },
                        placeholder = { Text("192.168.1.100") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.7f)
                            .testTag("wifi_printer_host_input")
                    )
                    OutlinedTextField(
                        value = wifiPortText,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() }.take(5)
                            wifiPortText = clean
                            val parsed = clean.toIntOrNull()
                            if (parsed != null && parsed in 1..65535) {
                                prefsStore.update { it.copy(printerWifiPort = parsed) }
                            }
                        },
                        label = { Text("Port") },
                        placeholder = { Text("9100") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("wifi_printer_port_input")
                    )
                }
                Text(
                    text = "Supports universal ESC/POS LAN & Wi-Fi printers on Raw TCP (default port 9100).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Paper Size, Darkness & Auto-Cut (for BT and Wi-Fi)
        AnimatedVisibility(visible = localPrefs.printerConnectionType != ThermalConnectionType.SYSTEM_PRINT) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "Thermal Roll / Paper Width (Any Size Supported)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThermalPaperWidth.entries.forEach { widthOpt ->
                        val active = localPrefs.printerPaperWidth == widthOpt
                        FilterChip(
                            selected = active,
                            onClick = {
                                haptics.tick()
                                prefsStore.update { it.copy(printerPaperWidth = widthOpt) }
                            },
                            label = {
                                Text(
                                    text = widthOpt.displayLabel(localPrefs.printerCustomWidthMm),
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("printer_width_${widthOpt.key}")
                        )
                    }
                }

                AnimatedVisibility(visible = localPrefs.printerPaperWidth == ThermalPaperWidth.CUSTOM) {
                    OutlinedTextField(
                        value = customMmText,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() }.take(3)
                            customMmText = clean
                            clean.toIntOrNull()?.let { mm ->
                                if (mm in 38..120) {
                                    prefsStore.update { it.copy(printerCustomWidthMm = mm) }
                                }
                            }
                        },
                        label = { Text("Custom Paper Width (38–120 mm)") },
                        suffix = { Text("mm") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("printer_custom_mm_input")
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Print Contrast / Darkness",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when {
                                localPrefs.printerDensity < 120 -> "Light"
                                localPrefs.printerDensity > 165 -> "Dark (Bold)"
                                else -> "Balanced"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = localPrefs.printerDensity.toFloat(),
                        onValueChange = { v ->
                            prefsStore.update { it.copy(printerDensity = v.roundToInt().coerceIn(85, 200)) }
                        },
                        valueRange = 85f..200f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Cut Paper After Print",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sends universal ESC/POS partial cut command",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = localPrefs.printerAutoCut,
                        onCheckedChange = { checked ->
                            haptics.tick()
                            prefsStore.update { it.copy(printerAutoCut = checked) }
                        },
                        modifier = Modifier.testTag("printer_auto_cut_switch")
                    )
                }

                if (!statusMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (statusIsError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusMessage.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (statusIsError) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        haptics.tap()
                        isTesting = true
                        statusMessage = "Sending ESC/POS test slip…"
                        statusIsError = false
                        scope.launch {
                            val result = ThermalPrinterManager.printTestSlip(
                                context = context,
                                prefs = localPrefs,
                                companyName = companyName
                            )
                            isTesting = false
                            result.onSuccess { msg ->
                                haptics.confirm()
                                statusMessage = msg
                                statusIsError = false
                            }.onFailure { err ->
                                haptics.error()
                                statusMessage = err.localizedMessage ?: "Printer connection failed"
                                statusIsError = true
                            }
                        }
                    },
                    enabled = !isTesting && ThermalPrinterManager.isConfigured(localPrefs),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("thermal_printer_test_button")
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Printing Test Slip…", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Thermal Printer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
