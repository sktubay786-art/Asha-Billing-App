package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.widget.Toast
import com.example.data.model.Bill
import com.example.data.model.BusinessSettings
import com.example.data.model.Customer
import com.example.data.model.Payment
import com.example.data.model.formatInr
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

object InvoicePrinter {
    fun buildShareSummary(
        settings: BusinessSettings,
        bill: Bill,
        paid: Double,
        due: Double
    ): String {
        return buildString {
            appendLine(settings.company.ifBlank { "ASHA Mini Shallow" })
            appendLine("Invoice: ${bill.billNo}")
            appendLine("Date: ${bill.date}")
            appendLine("Customer: ${bill.customerName}")
            if (bill.phone.isNotBlank()) appendLine("Phone: ${bill.phone}")
            if (bill.landAmount > 0.0 || bill.rate > 0.0) {
                appendLine("Land: ${formatQty(bill.landAmount)} ${bill.unit} (${String.format(Locale.US, "%.2f", bill.bigha)} bigha)")
            } else {
                appendLine("Bill Type: Previous Due Recovery")
            }
            appendLine("Total: ${formatInr(bill.allTotal)}")
            appendLine("Paid: ${formatInr(paid)}")
            appendLine("Due: ${formatInr(due)}")
            if (settings.upi.isNotBlank()) {
                appendLine("UPI ID: ${settings.upi}")
            }
        }.trim()
    }

    fun shareText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, title))
        }.onFailure {
            Toast.makeText(context, "No sharing app available", Toast.LENGTH_SHORT).show()
        }
    }

    fun normalizeWhatsAppDigits(phoneRaw: String): String {
        val digits = phoneRaw.replace(Regex("""\D"""), "")
        return when {
            digits.length == 10 -> "91$digits"
            digits.length == 11 && digits.startsWith("0") -> "91${digits.drop(1)}"
            else -> digits
        }
    }

    fun dialCustomer(context: Context, phoneRaw: String) {
        val digits = phoneRaw.replace(Regex("""\D"""), "")
        if (digits.isBlank()) {
            Toast.makeText(context, "No phone number saved for this customer", Toast.LENGTH_SHORT).show()
            return
        }
        val dialTarget = if (digits.length == 10) digits else "+$digits"
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialTarget"))
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "Dialer not available", Toast.LENGTH_SHORT).show()
        }
    }

    fun buildReminderText(customer: Customer, outstanding: Double): String {
        return if (outstanding > 0.001) {
            "Hello ${customer.name}, this is a friendly reminder regarding your outstanding আশা মিনি শ্যালো (ASHA Mini Shallow) bill. Pending balance: ${formatInr(outstanding)}. Thank you!"
        } else {
            "Hello ${customer.name}, thank you for clearing your আশা মিনি শ্যালো (ASHA Mini Shallow) dues. Let us know if you need any assistance!"
        }
    }

    fun openWhatsAppOrShare(context: Context, phoneRaw: String, message: String): Boolean {
        val digits = normalizeWhatsAppDigits(phoneRaw)
        return if (digits.isNotBlank()) {
            val encoded = URLEncoder.encode(message, "UTF-8")
            val uri = Uri.parse("https://wa.me/$digits?text=$encoded")
            val waIntent = Intent(Intent.ACTION_VIEW, uri)
            try {
                context.startActivity(waIntent)
                true
            } catch (_: Exception) {
                shareText(context, "Payment Reminder", message)
                false
            }
        } else {
            shareText(context, "Payment Reminder", message)
            false
        }
    }

    fun printInvoice(
        context: Context,
        settings: BusinessSettings,
        bill: Bill,
        payments: List<Payment>,
        paid: Double,
        due: Double,
        design: BillDesignModel = BillDesignModel.PREMIUM_GREEN,
        printSize: BillPrintSize = BillPrintSize.THERMAL_80,
        qrMode: String = "Dynamic amount QR",
        customQrPath: String = ""
    ) {
        val hostContext = context.findActivity() ?: context
        val printManager = hostContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Print service not available on this device", Toast.LENGTH_SHORT).show()
            return
        }

        val billBitmap = BillDocumentEngine.renderBillBitmap(
            settings = settings,
            bill = bill,
            payments = payments,
            paid = paid,
            due = due,
            design = design,
            size = printSize,
            qrMode = qrMode,
            customQrPath = customQrPath
        )

        val safeName = bill.billNo.ifBlank { "Bill" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val aspectRatio = billBitmap.height.toFloat() / billBitmap.width.coerceAtLeast(1).toFloat()
        val mediaSize = when (printSize) {
            BillPrintSize.A4_INVOICE,
            BillPrintSize.A4_HALF,
            BillPrintSize.A4_QUARTER,
            BillPrintSize.A4_FOUR_UP -> PrintAttributes.MediaSize.ISO_A4
            BillPrintSize.COMPACT_A6 -> PrintAttributes.MediaSize.ISO_A6
            BillPrintSize.THERMAL_80 -> {
                val widthMils = 3150 // ~80mm
                val heightMils = (widthMils * aspectRatio).roundToInt().coerceIn(2200, 14000)
                PrintAttributes.MediaSize("ASHA_80MM", "Thermal 80mm", widthMils, heightMils)
            }
            BillPrintSize.THERMAL_58 -> {
                val widthMils = 2283 // ~58mm
                val heightMils = (widthMils * aspectRatio).roundToInt().coerceIn(2000, 14000)
                PrintAttributes.MediaSize("ASHA_58MM", "Thermal 58mm", widthMils, heightMils)
            }
        }

        val attributes = PrintAttributes.Builder()
            .setMediaSize(mediaSize)
            .setColorMode(
                if (design == BillDesignModel.THERMAL_COMPACT) {
                    PrintAttributes.COLOR_MODE_MONOCHROME
                } else {
                    PrintAttributes.COLOR_MODE_COLOR
                }
            )
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        val adapter = object : PrintDocumentAdapter() {
            private var currentAttributes: PrintAttributes = attributes

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                currentAttributes = newAttributes
                val info = PrintDocumentInfo.Builder("ASHA_$safeName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                val pdfDoc = PrintedPdfDocument(hostContext, currentAttributes)
                try {
                    val page = pdfDoc.startPage(0)
                    val contentRect = page.info.contentRect
                    val canvas = page.canvas
                    canvas.save()
                    canvas.translate(contentRect.left.toFloat(), contentRect.top.toFloat())
                    BillDocumentEngine.drawBillOnPrintPage(
                        canvas = canvas,
                        pageWidth = contentRect.width().coerceAtLeast(1),
                        pageHeight = contentRect.height().coerceAtLeast(1),
                        billBitmap = billBitmap,
                        size = printSize
                    )
                    canvas.restore()
                    pdfDoc.finishPage(page)

                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                        return
                    }

                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdfDoc.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.localizedMessage ?: "Print rendering failed")
                } finally {
                    pdfDoc.close()
                }
            }
        }

        runCatching {
            printManager.print("ASHA_Invoice_$safeName", adapter, attributes)
        }.onFailure {
            Toast.makeText(context, "Could not start print job", Toast.LENGTH_SHORT).show()
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private fun formatQty(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }
}
