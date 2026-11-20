package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.BillingDashboardData
import com.example.data.model.todayIsoDate
import com.example.ui.components.IosSquircleIcon
import com.example.ui.components.LocalAshaHaptics
import com.example.ui.theme.EmeraldPrimary
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

enum class CsvReportType(
    val title: String,
    val subtitle: String,
    val filePrefix: String
) {
    BILLS_LEDGER(
        title = "Bills & Ledger CSV",
        subtitle = "All invoices, land area, rate, paid & due balances",
        filePrefix = "ASHA_Bills_Ledger"
    ),
    CUSTOMERS_DUE(
        title = "Customers & Dues CSV",
        subtitle = "Customer directory, land unit, owner & outstanding dues",
        filePrefix = "ASHA_Customers_Dues"
    ),
    PAYMENTS(
        title = "Payments Register CSV",
        subtitle = "Every cash, UPI, bank & settlement receipt",
        filePrefix = "ASHA_Payments_Register"
    ),
    EXPENSES(
        title = "Expenses Register CSV",
        subtitle = "Fuel, diesel, maintenance & operational costs",
        filePrefix = "ASHA_Expenses"
    ),
    WBSEDCL(
        title = "WBSEDCL Electricity CSV",
        subtitle = "Electricity bill payments & consumer IDs",
        filePrefix = "ASHA_WBSEDCL_Bills"
    )
}

object CsvReportExporter {

    private const val UTF8_BOM = "\uFEFF"

    fun buildCsvContent(type: CsvReportType, data: BillingDashboardData): String {
        val sb = StringBuilder(UTF8_BOM)
        when (type) {
            CsvReportType.BILLS_LEDGER -> {
                sb.appendLine(
                    csvRow(
                        "Bill No", "Date", "Customer Name", "Phone", "Village",
                        "Season", "Land Area", "Unit", "Bigha", "Rate/Bigha",
                        "Current Bill", "Previous Due", "Total Amount", "Paid", "Due", "Status", "Note"
                    )
                )
                data.bills.sortedByDescending { it.date }.forEach { bill ->
                    val paid = data.paidForBill(bill.id)
                    val due = data.dueForBill(bill)
                    val status = data.statusForBill(bill).label
                    sb.appendLine(
                        csvRow(
                            bill.billNo,
                            bill.date,
                            bill.customerName,
                            bill.phone,
                            bill.village,
                            bill.season,
                            fmtNum(bill.landAmount),
                            bill.unit,
                            fmtNum(bill.bigha),
                            fmtNum(bill.rate),
                            fmtNum(bill.current),
                            fmtNum(bill.previousDue),
                            fmtNum(bill.allTotal),
                            fmtNum(paid),
                            fmtNum(due),
                            status,
                            bill.note
                        )
                    )
                }
            }

            CsvReportType.CUSTOMERS_DUE -> {
                sb.appendLine(
                    csvRow(
                        "Customer ID", "Name", "Phone", "Village", "Address",
                        "Land Area", "Land Unit", "Owner Type", "Owner Name",
                        "Opening Due", "Total Billed", "Total Paid", "Outstanding Due"
                    )
                )
                data.customers.sortedBy { it.name.lowercase() }.forEach { cust ->
                    val billed = data.bills.filter { it.customerId == cust.id }.sumOf { it.current }
                    val paid = data.payments.filter { it.customerId == cust.id }.sumOf { it.amount }
                    val due = data.dueForCustomer(cust.id)
                    sb.appendLine(
                        csvRow(
                            cust.id,
                            cust.name,
                            cust.phone,
                            cust.village,
                            cust.address,
                            cust.landArea,
                            cust.normalizedLandUnit().label,
                            cust.normalizedOwnerType(),
                            cust.landOwnerName,
                            fmtNum(cust.openingDue),
                            fmtNum(billed),
                            fmtNum(paid),
                            fmtNum(due)
                        )
                    )
                }
            }

            CsvReportType.PAYMENTS -> {
                val billMap = data.bills.associateBy { it.id }
                val custMap = data.customers.associateBy { it.id }
                sb.appendLine(
                    csvRow("Date", "Bill No", "Customer Name", "Phone", "Amount", "Mode", "Received In", "Note")
                )
                data.payments.sortedByDescending { it.date }.forEach { pay ->
                    val bill = billMap[pay.billId]
                    val cust = custMap[pay.customerId]
                    sb.appendLine(
                        csvRow(
                            pay.date,
                            bill?.billNo ?: pay.billId,
                            cust?.name ?: bill?.customerName.orEmpty(),
                            cust?.phone ?: bill?.phone.orEmpty(),
                            fmtNum(pay.amount),
                            pay.mode,
                            pay.receivedIn,
                            pay.note
                        )
                    )
                }
            }

            CsvReportType.EXPENSES -> {
                sb.appendLine(
                    csvRow("Date", "Category", "Title", "Amount", "Paid Mode", "Note")
                )
                data.expenses.sortedByDescending { it.date }.forEach { exp ->
                    sb.appendLine(
                        csvRow(
                            exp.date,
                            exp.category,
                            exp.title,
                            fmtNum(exp.amount),
                            exp.paidMode,
                            exp.note
                        )
                    )
                }
            }

            CsvReportType.WBSEDCL -> {
                sb.appendLine(
                    csvRow("Date", "Consumer Label", "Consumer ID", "Bill Month", "Amount Paid", "Note")
                )
                data.wbsedclPayments.sortedByDescending { it.date }.forEach { wp ->
                    sb.appendLine(
                        csvRow(
                            wp.date,
                            wp.consumerLabel,
                            wp.consumerNo,
                            wp.billMonth,
                            fmtNum(wp.amount),
                            wp.note
                        )
                    )
                }
            }
        }
        return sb.toString()
    }

    fun shareCsv(context: Context, type: CsvReportType, data: BillingDashboardData): Boolean {
        return runCatching {
            val csvText = buildCsvContent(type, data)
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val fileName = "${type.filePrefix}_${todayIsoDate()}.csv"
            val outFile = File(dir, fileName)
            outFile.writeText(csvText, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                this.type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ${type.title}"))
            true
        }.getOrElse {
            Toast.makeText(context, "Could not export CSV report", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun saveCsvToDownloads(context: Context, type: CsvReportType, data: BillingDashboardData): String? {
        return runCatching {
            val csvBytes = buildCsvContent(type, data).toByteArray(Charsets.UTF_8)
            val fileName = "${type.filePrefix}_${todayIsoDate()}.csv"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ASHA_Reports")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Unable to create file in Downloads")
                context.contentResolver.openOutputStream(uri)?.use { it.write(csvBytes) }
                    ?: throw IllegalStateException("Unable to open output stream")
            } else {
                val dir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
                    "ASHA_Reports"
                ).apply { mkdirs() }
                FileOutputStream(File(dir, fileName)).use { it.write(csvBytes) }
            }
            Toast.makeText(context, "Saved $fileName to Downloads/ASHA_Reports", Toast.LENGTH_LONG).show()
            fileName
        }.getOrElse {
            Toast.makeText(context, "Failed to save CSV: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun csvRow(vararg cols: String): String = cols.joinToString(",") { escapeCsv(it) }

    private fun escapeCsv(raw: String): String {
        val cleaned = raw.replace("\r\n", " ").replace('\n', ' ').trim()
        return if (cleaned.contains(',') || cleaned.contains('"')) {
            "\"${cleaned.replace("\"", "\"\"")}\""
        } else {
            cleaned
        }
    }

    private fun fmtNum(v: Double): String = String.format(Locale.US, "%.2f", v)
}

@Composable
fun CsvReportExportDialog(
    data: BillingDashboardData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalAshaHaptics.current
    var selectedType by remember { mutableStateOf(CsvReportType.BILLS_LEDGER) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IosSquircleIcon(
                            icon = Icons.Rounded.TableView,
                            containerColor = Color(0xFF0B6B50),
                            size = 38.dp,
                            iconSize = 20.dp
                        )
                        Column {
                            Text(
                                text = "Export CSV Report",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Excel & Google Sheets compatible (UTF-8)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                    }
                }

                CsvReportType.entries.forEach { reportType ->
                    val selected = selectedType == reportType
                    val rowCount = when (reportType) {
                        CsvReportType.BILLS_LEDGER -> data.bills.size
                        CsvReportType.CUSTOMERS_DUE -> data.customers.size
                        CsvReportType.PAYMENTS -> data.payments.size
                        CsvReportType.EXPENSES -> data.expenses.size
                        CsvReportType.WBSEDCL -> data.wbsedclPayments.size
                    }
                    Surface(
                        onClick = {
                            haptics.tick()
                            selectedType = reportType
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("csv_report_type_${reportType.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reportType.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = reportType.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = "$rowCount rows",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptics.confirm()
                            CsvReportExporter.saveCsvToDownloads(context, selectedType, data)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("csv_save_downloads_button")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save CSV", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            haptics.confirm()
                            CsvReportExporter.shareCsv(context, selectedType, data)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("csv_share_button")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share CSV", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
