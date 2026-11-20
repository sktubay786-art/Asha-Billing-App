package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.LandUnit
import com.example.data.model.calculateBigha
import com.example.data.model.formatInr
import com.example.data.model.roundMoney
import com.example.ui.components.AnimatedBmwPrecisionStripe
import com.example.ui.components.AshaPanelCard
import com.example.ui.components.LocalAshaHaptics
import com.example.ui.components.PremiumCalculatorIcon
import com.example.ui.theme.AmberBadgeBg
import com.example.ui.theme.AmberBadgeFg
import com.example.ui.theme.BmwTelemetryStyle
import com.example.ui.theme.CalcScreenBg
import com.example.ui.theme.CalcScreenSub
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDue
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.CalculatorUiState
import com.example.util.SafeCalculator
import java.util.Locale
import kotlin.math.max

@Composable
fun CalculatorScreen(
    calcState: CalculatorUiState,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalAshaHaptics.current
    var selectedMode by rememberSaveable { mutableIntStateOf(0) } // 0 = Standard, 1 = Land & Bill
    var showTape by rememberSaveable { mutableStateOf(false) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        haptics.confirm()
        Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AshaPanelCard {
                    AnimatedBmwPrecisionStripe(
                        height = 2.5.dp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Studio Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumCalculatorIcon(size = 42.dp, active = true)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PRECISION STUDIO",
                                style = BmwTelemetryStyle,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.asha_calculator),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Switcher: Standard | Land & Bill
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            selected = selectedMode == 0,
                            onClick = {
                                haptics.tick()
                                selectedMode = 0
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Calculate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("calc_mode_standard")
                        ) {
                            Text("Standard", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            selected = selectedMode == 1,
                            onClick = {
                                haptics.tick()
                                selectedMode = 1
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Landscape,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("calc_mode_land_bill")
                        ) {
                            Text("Land & Bill", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedContent(
                        targetState = selectedMode,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "calculator_mode_transition"
                    ) { mode ->
                        if (mode == 0) {
                            StandardCalculatorDeck(
                                calcState = calcState,
                                showTape = showTape,
                                onToggleTape = {
                                    haptics.tick()
                                    showTape = !showTape
                                },
                                onCopyResult = { value -> copyToClipboard("Calculation Result", value) },
                                onKeyPress = onKeyPress
                            )
                        } else {
                            LandAndBillCalculatorDeck(
                                onCopySummary = { summary -> copyToClipboard("Bill Calculation", summary) },
                                onSendToStandard = { amountStr ->
                                    haptics.confirm()
                                    onKeyPress("USE:$amountStr")
                                    selectedMode = 0
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StandardCalculatorDeck(
    calcState: CalculatorUiState,
    showTape: Boolean,
    onToggleTape: () -> Unit,
    onCopyResult: (String) -> Unit,
    onKeyPress: (String) -> Unit
) {
    val haptics = LocalAshaHaptics.current
    val keyRows = listOf(
        listOf("AC", "⌫", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("00", "0", ".", "=")
    )

    val displayFontSize = when {
        calcState.display.length > 16 -> 24.sp
        calcState.display.length > 12 -> 28.sp
        calcState.display.length > 9 -> 34.sp
        else -> 40.sp
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Obsidian & Emerald OLED Display Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            CalcScreenBg,
                            Color(0xFF0A241C),
                            Color(0xFF071A15)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Display top telemetry bar: Memory badge, History Tape toggle, Copy result
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (calcState.memoryValue != 0.0) {
                            Surface(
                                onClick = {
                                    haptics.tap()
                                    onKeyPress("MR")
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.22f),
                                border = BorderStroke(0.75.dp, Color(0xFFFCD34D).copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = "M: ${SafeCalculator.formatResult(calcState.memoryValue)}",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFFFDE68A),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = onToggleTape,
                            shape = RoundedCornerShape(8.dp),
                            color = if (showTape) Color(0xFF10B981).copy(alpha = 0.28f) else Color.White.copy(alpha = 0.10f),
                            modifier = Modifier.testTag("calc_toggle_tape")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = "Calculation history",
                                    tint = if (showTape) Color(0xFF6EE7B7) else CalcScreenSub,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (calcState.tape.isEmpty()) "Tape" else "Tape (${calcState.tape.size})",
                                    fontSize = 10.5.sp,
                                    color = if (showTape) Color(0xFF6EE7B7) else CalcScreenSub,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            val toCopy = calcState.livePreview.ifBlank { calcState.display }
                            onCopyResult(toCopy)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.10f),
                        modifier = Modifier.testTag("calc_copy_result")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy result",
                                tint = Color(0xFFA7F3D0),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Copy",
                                fontSize = 10.5.sp,
                                color = Color(0xFFA7F3D0),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Previous equation history line
                Text(
                    text = calcState.historyLine.ifEmpty { "Ready" },
                    style = MaterialTheme.typography.bodySmall,
                    color = CalcScreenSub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                // Main Auto-Scaling Expression / Result Display
                Text(
                    text = calcState.display,
                    fontSize = displayFontSize,
                    lineHeight = (displayFontSize.value + 4f).sp,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calculator_display")
                )

                // Real-time live evaluation preview line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (calcState.livePreview.isNotBlank()) {
                        Text(
                            text = "= ${calcState.livePreview}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("calculator_live_preview")
                        )
                    }
                }
            }
        }

        // Collapsible Calculation History Tape
        AnimatedVisibility(
            visible = showTape,
            enter = expandVertically(tween(200)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        Text(
                            text = "RECENT CALCULATIONS (TAP TO USE)",
                            style = BmwTelemetryStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (calcState.tape.isNotEmpty()) {
                            Surface(
                                onClick = {
                                    haptics.tap()
                                    onKeyPress("CLEAR_TAPE")
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = CrimsonContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Clear tape",
                                        tint = CrimsonDue,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Clear",
                                        fontSize = 10.5.sp,
                                        color = CrimsonDue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (calcState.tape.isEmpty()) {
                        Text(
                            text = "Press = to save calculations here for quick reuse.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        calcState.tape.take(5).forEachIndexed { idx, (expr, res) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        haptics.tap()
                                        onKeyPress("USE:$res")
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$expr =",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = res,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            if (idx < minOf(4, calcState.tape.lastIndex)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }

        // Memory + Land Quick-Math Strip (Horizontally scrollable, zero text wrap)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalcActionPill("MC", "calc_key_mc", isMemory = true) { haptics.tick(); onKeyPress("MC") }
            CalcActionPill("MR", "calc_key_mr", isMemory = true) { haptics.tick(); onKeyPress("MR") }
            CalcActionPill("M+", "calc_key_mplus", isMemory = true) { haptics.tick(); onKeyPress("M+") }
            CalcActionPill("M−", "calc_key_mminus", isMemory = true) { haptics.tick(); onKeyPress("M-") }
            CalcActionPill("±", "calc_key_sign", isMemory = true) { haptics.tick(); onKeyPress("±") }
            CalcActionPill("(", "calc_key_(", isMemory = true) { haptics.tick(); onKeyPress("(") }
            CalcActionPill(")", "calc_key_)", isMemory = true) { haptics.tick(); onKeyPress(")") }
            CalcActionPill("÷40 Dec", "calc_helper_dec", isMemory = false) {
                haptics.tap()
                listOf("÷", "4", "0", "=").forEach(onKeyPress)
            }
            CalcActionPill("÷20 Katha", "calc_helper_katha", isMemory = false) {
                haptics.tap()
                listOf("÷", "2", "0", "=").forEach(onKeyPress)
            }
            CalcActionPill("×₹2200", "calc_helper_rate_2200", isMemory = false) {
                haptics.tap()
                listOf("×", "2", "2", "0", "0", "=").forEach(onKeyPress)
            }
        }

        // Flagship 4x5 Tactile Keypad
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            keyRows.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    rowKeys.forEach { key ->
                        val isEquals = key == "="
                        val isClear = key == "AC"
                        val isOperator = key in setOf("÷", "×", "−", "+")
                        val isUtility = key in setOf("⌫", "%")

                        val bg = when {
                            isEquals -> EmeraldPrimary
                            isClear -> CrimsonContainer
                            isOperator -> MaterialTheme.colorScheme.primaryContainer
                            isUtility -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)
                        }
                        val fg = when {
                            isEquals -> Color.White
                            isClear -> CrimsonDue
                            isOperator -> MaterialTheme.colorScheme.primary
                            isUtility -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        val border = when {
                            isEquals -> null
                            isClear -> BorderStroke(0.75.dp, CrimsonDue.copy(alpha = 0.25f))
                            isOperator -> BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            else -> BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }

                        Surface(
                            onClick = {
                                if (isEquals) haptics.confirm() else haptics.tap()
                                onKeyPress(key)
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = bg,
                            border = border,
                            shadowElevation = if (isEquals) 4.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .testTag(if (isEquals) "calc_key_equals" else "calc_key_$key")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (key == "⌫") {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                        contentDescription = "Backspace",
                                        tint = fg,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = if (isOperator || isEquals) 24.sp else 20.sp,
                                        color = fg,
                                        fontWeight = FontWeight.ExtraBold
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

@Composable
private fun LandAndBillCalculatorDeck(
    onCopySummary: (String) -> Unit,
    onSendToStandard: (String) -> Unit
) {
    val haptics = LocalAshaHaptics.current
    var landInput by rememberSaveable { mutableStateOf("") }
    var selectedUnit by rememberSaveable { mutableStateOf(LandUnit.DECIMAL.key) }
    var rateInput by rememberSaveable { mutableStateOf("2200") }
    var prevDueInput by rememberSaveable { mutableStateOf("") }
    var cashPaidInput by rememberSaveable { mutableStateOf("") }

    val unitEnum = LandUnit.fromKey(selectedUnit)
    val landQty = landInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val rate = rateInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val prevDue = prevDueInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val bigha = calculateBigha(landQty, unitEnum)
    val currentBill = roundMoney(bigha * rate)
    val grandTotal = roundMoney(currentBill + prevDue)
    val cashOffer = cashPaidInput.toDoubleOrNull()?.coerceAtLeast(0.0)
    val discountWaived = if (cashOffer != null && grandTotal > 0.001 && cashOffer < grandTotal) {
        roundMoney(grandTotal - cashOffer)
    } else 0.0
    val remainingAfterCash = if (cashOffer != null) roundMoney(max(0.0, grandTotal - cashOffer)) else grandTotal

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Live Result OLED Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(listOf(EmeraldDeep, Color(0xFF0A2E23), CalcScreenBg))
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IRRIGATION BILL ESTIMATE",
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = Color(0xFFA7F3D0),
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.3f", bigha)} Bigha",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = formatInr(grandTotal),
                    fontSize = 34.sp,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CURRENT CHARGE", fontSize = 9.5.sp, color = Color(0xFFB7E4D3), fontWeight = FontWeight.Bold)
                        Text(formatInr(currentBill), fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PREVIOUS DUE", fontSize = 9.5.sp, color = Color(0xFFB7E4D3), fontWeight = FontWeight.Bold)
                        Text(formatInr(prevDue), fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (cashOffer != null && grandTotal > 0.001) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Handshake,
                                contentDescription = null,
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (discountWaived > 0.001) {
                                    "If Final Settlement: Discount ${formatInr(discountWaived)}"
                                } else {
                                    "Balance Remaining: ${formatInr(remainingAfterCash)}"
                                },
                                fontSize = 11.5.sp,
                                color = Color(0xFFFDE68A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Land Quantity + Unit Selector
        OutlinedTextField(
            value = landInput,
            onValueChange = { landInput = it },
            label = { Text("Land Quantity (${unitEnum.label})") },
            placeholder = { Text("e.g. 25") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("land_calc_qty_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                LandUnit.DECIMAL to "Decimal (÷40)",
                LandUnit.KATHA to "Katha (÷20)",
                LandUnit.BIGHA to "Bigha (×1)"
            ).forEach { (unit, label) ->
                FilterChip(
                    selected = unitEnum == unit,
                    onClick = {
                        haptics.tick()
                        selectedUnit = unit.key
                    },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Rate per Bigha + Quick Rate Pills
        OutlinedTextField(
            value = rateInput,
            onValueChange = { rateInput = it },
            label = { Text("Rate per Bigha (₹)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("land_calc_rate_input")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("1800", "2000", "2200", "2500", "3000").forEach { preset ->
                CalcActionPill(
                    label = "₹$preset",
                    tag = "land_calc_preset_$preset",
                    isMemory = rateInput != preset,
                    onClick = {
                        haptics.tick()
                        rateInput = preset
                    }
                )
            }
        }

        // Previous Due + Settlement Cash Simulator side-by-side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = prevDueInput,
                onValueChange = { prevDueInput = it },
                label = { Text("Prev Due (₹)") },
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = cashPaidInput,
                onValueChange = { cashPaidInput = it },
                label = { Text("Cash Paid (₹)") },
                placeholder = { Text("Optional") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            )
        }

        // Action Buttons: Copy Summary & Send Total to Standard Calculator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val summary = buildString {
                        append("Land: $landQty ${unitEnum.label} (${String.format(Locale.US, "%.3f", bigha)} Bigha) × ${formatInr(rate)} = ${formatInr(currentBill)}")
                        if (prevDue > 0.001) append(" + Prev Due ${formatInr(prevDue)}")
                        append(" | Total: ${formatInr(grandTotal)}")
                        if (cashOffer != null && discountWaived > 0.001) {
                            append(" | Settled: Paid ${formatInr(cashOffer)}, Discount ${formatInr(discountWaived)}")
                        }
                    }
                    onCopySummary(summary)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("land_calc_copy_summary")
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }

            Button(
                onClick = { onSendToStandard(SafeCalculator.formatResult(grandTotal)) },
                enabled = grandTotal > 0.001,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("land_calc_send_to_std")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Calculate,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Use in Calc", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun CalcActionPill(
    label: String,
    tag: String,
    isMemory: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(11.dp),
        color = if (isMemory) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        },
        border = BorderStroke(
            0.75.dp,
            if (isMemory) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
        ),
        modifier = Modifier
            .height(34.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isMemory) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
