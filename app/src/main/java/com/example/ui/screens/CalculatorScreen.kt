package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AshaPanelCard
import com.example.ui.components.PremiumCalculatorIcon
import com.example.ui.theme.CalcScreenBg
import com.example.ui.theme.CalcScreenSub
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.CalculatorUiState

@Composable
fun CalculatorScreen(
    calcState: CalculatorUiState,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("AC", "⌫", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("(", "0", ".", ")")
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AshaPanelCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumCalculatorIcon(size = 42.dp, active = true)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.asha_calculator),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Standard & Land Bigha Rate Math",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(CalcScreenBg, Color(0xFF0B221B))
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = calcState.historyLine.ifEmpty { "Ready" },
                                style = MaterialTheme.typography.bodySmall,
                                color = CalcScreenSub,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = calcState.display,
                                style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End,
                                modifier = Modifier.testTag("calculator_display")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Land & Irrigation Rate Quick Helpers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalcQuickHelperChip(
                            label = "÷40 Dec→Bigha",
                            tag = "calc_helper_dec",
                            onClick = {
                                listOf("÷", "4", "0", "=").forEach(onKeyPress)
                            }
                        )
                        CalcQuickHelperChip(
                            label = "÷20 Katha→Bigha",
                            tag = "calc_helper_katha",
                            onClick = {
                                listOf("÷", "2", "0", "=").forEach(onKeyPress)
                            }
                        )
                        CalcQuickHelperChip(
                            label = "×₹2200",
                            tag = "calc_helper_rate_2200",
                            onClick = {
                                listOf("×", "2", "2", "0", "0", "=").forEach(onKeyPress)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { rowKeys ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowKeys.forEach { key ->
                                    val isOperator = key in setOf("÷", "×", "−", "+")
                                    val isUtility = key in setOf("AC", "⌫", "%", "(", ")")
                                    val bg = when {
                                        isOperator -> MaterialTheme.colorScheme.primaryContainer
                                        isUtility -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    }
                                    val fg = when {
                                        isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
                                        isUtility -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                    Surface(
                                        onClick = { onKeyPress(key) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = bg,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                            .testTag("calc_key_$key")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = fg,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { onKeyPress("=") },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("calc_key_equals")
                        ) {
                            Text(
                                text = "=",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcQuickHelperChip(
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier
            .height(36.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
