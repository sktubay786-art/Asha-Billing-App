package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.Bill
import com.example.data.model.BusinessSettings
import com.example.data.model.Payment
import com.example.data.model.PaymentMode
import com.example.data.model.compactCustomerBarcodeId
import com.example.data.model.formatInr
import com.example.data.model.roundMoney
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.oned.Code128Writer
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class BillDesignModel(
    val label: String,
    val tagline: String,
    val primaryColorInt: Int,
    val secondaryColorInt: Int,
    val accentBgInt: Int
) {
    PREMIUM_GREEN(
        label = "Emerald Crest Pro",
        tagline = "Sectioned emerald cards, mint summary & Scan & Pay QR",
        primaryColorInt = 0xFF0B7A4B.toInt(),
        secondaryColorInt = 0xFF064E3B.toInt(),
        accentBgInt = 0xFFECFDF5.toInt()
    ),
    ROYAL_GOLD(
        label = "Royal Gold Heritage",
        tagline = "Deep burgundy executive card with antique gold accents",
        primaryColorInt = 0xFF78350F.toInt(),
        secondaryColorInt = 0xFF451A03.toInt(),
        accentBgInt = 0xFFFFFBEB.toInt()
    ),
    MIDNIGHT_PREMIUM(
        label = "Obsidian Midnight Luxe",
        tagline = "Obsidian slate luxury card with dark summary & platinum trim",
        primaryColorInt = 0xFF0F172A.toInt(),
        secondaryColorInt = 0xFF1E293B.toInt(),
        accentBgInt = 0xFFF1F5F9.toInt()
    ),
    SAPPHIRE_GLASS(
        label = "Sapphire Crystal Pro",
        tagline = "Crisp royal cobalt card layout with icy azure highlights",
        primaryColorInt = 0xFF1D4ED8.toInt(),
        secondaryColorInt = 0xFF1E3A8A.toInt(),
        accentBgInt = 0xFFEFF6FF.toInt()
    ),
    BENGALI_HERITAGE(
        label = "Bengali Heritage Gold",
        tagline = "Bengali-first formal headers with heritage gold trim",
        primaryColorInt = 0xFF047857.toInt(),
        secondaryColorInt = 0xFF064E3B.toInt(),
        accentBgInt = 0xFFECFDF5.toInt()
    ),
    QR_FOCUSED(
        label = "UPI QR Express Pass",
        tagline = "Prominent Scan & Pay UPI hero block for instant collection",
        primaryColorInt = 0xFF0D9488.toInt(),
        secondaryColorInt = 0xFF115E59.toInt(),
        accentBgInt = 0xFFF0FDFA.toInt()
    ),
    THERMAL_COMPACT(
        label = "Thermal POS Pro",
        tagline = "High-contrast monochrome POS slip for 58/80mm rolls",
        primaryColorInt = 0xFF111827.toInt(),
        secondaryColorInt = 0xFF111827.toInt(),
        accentBgInt = 0xFFF3F4F6.toInt()
    ),
    IMPERIAL_TEAL(
        label = "Imperial Teal Luxe",
        tagline = "Deep peacock teal & champagne executive trim",
        primaryColorInt = 0xFF0F766E.toInt(),
        secondaryColorInt = 0xFF134E4A.toInt(),
        accentBgInt = 0xFFF0FDFA.toInt()
    ),
    SUNSET_SAFFRON(
        label = "Sunset Saffron Gold",
        tagline = "Warm amber saffron crest with harvest cream summary",
        primaryColorInt = 0xFFD97706.toInt(),
        secondaryColorInt = 0xFF92400E.toInt(),
        accentBgInt = 0xFFFFFBEB.toInt()
    ),
    AMETHYST_LUXE(
        label = "Amethyst Silk Pro",
        tagline = "Royal violet-indigo card with lavender glow summary",
        primaryColorInt = 0xFF6D28D9.toInt(),
        secondaryColorInt = 0xFF4C1D95.toInt(),
        accentBgInt = 0xFFF5F3FF.toInt()
    ),
    SAKURA_IVORY(
        label = "Sakura Ivory Light",
        tagline = "Blush rose & pearl white editorial form with pastel tiles",
        primaryColorInt = 0xFFBE185D.toInt(),
        secondaryColorInt = 0xFFFCE7F3.toInt(),
        accentBgInt = 0xFFFFF1F5.toInt()
    ),
    SKY_PEARL(
        label = "Sky Pearl Modern",
        tagline = "Airy glacier-blue & pearl light atelier form with 2x2 grid",
        primaryColorInt = 0xFF0369A1.toInt(),
        secondaryColorInt = 0xFFE0F2FE.toInt(),
        accentBgInt = 0xFFF0F9FF.toInt()
    ),
    MARIGOLD_CREAM(
        label = "Marigold Cream Form",
        tagline = "Warm sunlit ivory & saffron pastel form for crisp printing",
        primaryColorInt = 0xFFB45309.toInt(),
        secondaryColorInt = 0xFFFEF3C7.toInt(),
        accentBgInt = 0xFFFFFBEB.toInt()
    ),
    PLATINUM_LEDGER(
        label = "Platinum Ledger Pro",
        tagline = "New structured accounting table, top barcode & split UPI ledger",
        primaryColorInt = 0xFF1E3A8A.toInt(),
        secondaryColorInt = 0xFF0F172A.toInt(),
        accentBgInt = 0xFFEFF6FF.toInt()
    ),
    EMERALD_LEDGER(
        label = "Emerald Ledger Elite",
        tagline = "New structured emerald letterhead, itemized table & split QR",
        primaryColorInt = 0xFF065F46.toInt(),
        secondaryColorInt = 0xFF022C22.toInt(),
        accentBgInt = 0xFFECFDF5.toInt()
    );

    companion object {
        fun fromLabel(raw: String): BillDesignModel {
            return when {
                raw.equals("Premium Green Bill", ignoreCase = true) ||
                    raw.equals("Classic Official Bill", ignoreCase = true) ||
                    raw.equals("Standard Clean Bill", ignoreCase = true) -> PREMIUM_GREEN
                raw.equals("Royal Gold Premium", ignoreCase = true) -> ROYAL_GOLD
                raw.equals("Midnight Luxe", ignoreCase = true) ||
                    raw.equals("Midnight Premium", ignoreCase = true) ||
                    raw.equals("Minimal Luxe", ignoreCase = true) -> MIDNIGHT_PREMIUM
                raw.equals("Ocean Blue Modern", ignoreCase = true) ||
                    raw.equals("Ocean Blue Premium", ignoreCase = true) ||
                    raw.equals("Compact Modern Card", ignoreCase = true) ||
                    raw.equals("Boxed Modern Bill", ignoreCase = true) ||
                    raw.equals("Modern Receipt", ignoreCase = true) -> SAPPHIRE_GLASS
                raw.equals("Bengali Official Bill", ignoreCase = true) ||
                    raw.equals("Bengali Heavy Bill", ignoreCase = true) -> BENGALI_HERITAGE
                raw.equals("QR Focused Bill", ignoreCase = true) -> QR_FOCUSED
                raw.equals("Thermal POS Receipt", ignoreCase = true) ||
                    raw.equals("Normal Black Bill", ignoreCase = true) ||
                    raw.equals("Thermal Compact Slip", ignoreCase = true) ||
                    raw.equals("Thermal Compact Bill", ignoreCase = true) -> THERMAL_COMPACT
                else -> entries.firstOrNull { it.label.equals(raw, ignoreCase = true) } ?: PREMIUM_GREEN
            }
        }
    }
}

enum class BillPrintSize(
    val label: String,
    val widthPx: Int,
    val pdfWidthPt: Int,
    val isHalfA4: Boolean = false,
    val isQuarterA4: Boolean = false,
    val isFourUpA4: Boolean = false,
    val isFullA4: Boolean = false
) {
    THERMAL_80("Thermal · 80 mm", 760, 227),
    THERMAL_58("Thermal · 58 mm", 560, 164),
    A4_HALF("A4 · 1/2 Page", 1180, 595, isHalfA4 = true),
    A4_QUARTER("A4 · 1/4 Page", 1080, 595, isQuarterA4 = true),
    A4_FOUR_UP("A4 · 4x Grid", 1020, 595, isFourUpA4 = true),
    A4_INVOICE("A4 · Full Page", 1280, 595, isFullA4 = true),
    COMPACT_A6("Compact A6 · Normal", 720, 298);

    companion object {
        fun fromLabel(raw: String): BillPrintSize {
            return when {
                raw.equals("A4 Invoice", ignoreCase = true) -> A4_INVOICE
                raw.equals("A4 Half Page", ignoreCase = true) -> A4_HALF
                else -> entries.firstOrNull { it.label.equals(raw, ignoreCase = true) } ?: THERMAL_80
            }
        }
    }
}

object BillDocumentEngine {

    private val qrBitmapCache = object : android.util.LruCache<String, Bitmap>(16) {}

    fun generateQrBitmap(content: String, sizePx: Int = 360): Bitmap? {
        if (content.isBlank()) return null
        val cacheKey = "$sizePx|$content"
        synchronized(qrBitmapCache) {
            qrBitmapCache.get(cacheKey)?.let { if (!it.isRecycled) return it }
        }
        return runCatching {
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val pixels = IntArray(sizePx * sizePx)
            var offset = 0
            for (y in 0 until sizePx) {
                for (x in 0 until sizePx) {
                    pixels[offset++] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            bmp.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
            synchronized(qrBitmapCache) {
                qrBitmapCache.put(cacheKey, bmp)
            }
            bmp
        }.getOrNull()
    }

    /**
     * Generates a razor-sharp, pixel-perfect 1D Code 128 barcode with exact integer module widths
     * and ISO/IEC 15417 compliant pure-white quiet zones (12 modules on each side) so camera
     * scanners lock instantly even on compact A4-1/4 bills and 58mm thermal receipts.
     */
    fun generateCode128BarcodeBitmap(
        content: String,
        widthPx: Int = 560,
        heightPx: Int = 110
    ): Bitmap? {
        val clean = content.trim()
        if (clean.isBlank()) return null
        val cacheKey = "C128V2|$widthPx|$heightPx|$clean"
        synchronized(qrBitmapCache) {
            qrBitmapCache.get(cacheKey)?.let { if (!it.isRecycled) return it }
        }
        return runCatching {
            val rawMatrix = Code128Writer().encode(
                clean,
                BarcodeFormat.CODE_128,
                0,
                1,
                mapOf(EncodeHintType.MARGIN to 0)
            )
            val rawModules = rawMatrix.width
            val quietModules = 12
            val totalModules = rawModules + quietModules * 2
            val modulePx = max(2, widthPx / totalModules)
            val outW = totalModules * modulePx
            val outH = heightPx.coerceAtLeast(64)

            val pixels = IntArray(outW * outH) { Color.WHITE }
            for (m in 0 until rawModules) {
                if (rawMatrix.get(m, 0)) {
                    val startX = (quietModules + m) * modulePx
                    val endX = startX + modulePx
                    for (y in 0 until outH) {
                        val rowOffset = y * outW
                        for (x in startX until endX) {
                            pixels[rowOffset + x] = Color.BLACK
                        }
                    }
                }
            }
            val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            bmp.setPixels(pixels, 0, outW, 0, 0, outW, outH)
            synchronized(qrBitmapCache) {
                qrBitmapCache.put(cacheKey, bmp)
            }
            bmp
        }.getOrNull()
    }

    private fun crispBarcodePaint(): Paint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
    }

    /**
     * Splits total credited amount ([totalPaid]) into (actualCashPaid, settledWaived) using the
     * bill's [payments] list so printed receipts show the real money received in "Paid" and any
     * negotiated waiver separately in "Settled / Discount".
     */
    private fun splitPaidAndSettled(
        payments: List<Payment>,
        totalPaid: Double
    ): Pair<Double, Double> {
        val settledWaived = roundMoney(
            payments
                .filter { PaymentMode.fromLabel(it.mode) == PaymentMode.SETTLEMENT }
                .sumOf { it.amount }
        )
        val actualCashPaid = if (settledWaived > 0.001) {
            roundMoney(max(0.0, totalPaid - settledWaived))
        } else {
            roundMoney(max(0.0, totalPaid))
        }
        return actualCashPaid to settledWaived
    }

    fun resolveQrBitmap(
        settings: BusinessSettings,
        due: Double,
        qrMode: String,
        customQrPath: String
    ): Bitmap? {
        if (customQrPath.isNotBlank() && qrMode.contains("Static", ignoreCase = true)) {
            val file = File(customQrPath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.let { return it }
            }
        }
        val upiId = settings.upi.trim()
        if (upiId.isNotBlank() && !qrMode.contains("Static", ignoreCase = true)) {
            val payee = URLEncoder.encode(settings.payee.ifBlank { settings.company }, "UTF-8")
            val amountParam = if (due > 0.01 && qrMode.contains("Dynamic", ignoreCase = true)) {
                "&am=${String.format(Locale.US, "%.2f", due)}"
            } else {
                ""
            }
            val uri = "upi://pay?pa=$upiId&pn=$payee&cu=INR$amountParam"
            return generateQrBitmap(uri, 360)
        }
        if (customQrPath.isNotBlank()) {
            val file = File(customQrPath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.let { return it }
            }
        }
        return null
    }

    fun renderBillBitmap(
        settings: BusinessSettings,
        bill: Bill,
        payments: List<Payment>,
        paid: Double,
        due: Double,
        design: BillDesignModel,
        size: BillPrintSize,
        qrMode: String = "Dynamic amount QR",
        customQrPath: String = "",
        bengaliLabels: Boolean = false
    ): Bitmap {
        val useBengali = bengaliLabels || design == BillDesignModel.BENGALI_HERITAGE
        return when (design) {
            BillDesignModel.PREMIUM_GREEN,
            BillDesignModel.ROYAL_GOLD,
            BillDesignModel.MIDNIGHT_PREMIUM,
            BillDesignModel.SAPPHIRE_GLASS,
            BillDesignModel.BENGALI_HERITAGE,
            BillDesignModel.QR_FOCUSED,
            BillDesignModel.IMPERIAL_TEAL,
            BillDesignModel.SUNSET_SAFFRON,
            BillDesignModel.AMETHYST_LUXE -> renderSectionedCardStyle(
                settings = settings,
                bill = bill,
                payments = payments,
                paid = paid,
                due = due,
                design = design,
                size = size,
                qrMode = qrMode,
                customQrPath = customQrPath,
                isBengali = useBengali
            )
            BillDesignModel.SAKURA_IVORY,
            BillDesignModel.SKY_PEARL,
            BillDesignModel.MARIGOLD_CREAM -> renderLightAtelierStyle(
                settings = settings,
                bill = bill,
                payments = payments,
                paid = paid,
                due = due,
                design = design,
                size = size,
                qrMode = qrMode,
                customQrPath = customQrPath,
                isBengali = useBengali
            )
            BillDesignModel.PLATINUM_LEDGER,
            BillDesignModel.EMERALD_LEDGER -> renderExecutiveLedgerFormStyle(
                settings = settings,
                bill = bill,
                payments = payments,
                paid = paid,
                due = due,
                design = design,
                size = size,
                qrMode = qrMode,
                customQrPath = customQrPath,
                isBengali = useBengali
            )
            BillDesignModel.THERMAL_COMPACT -> renderClassicOrThermalStyle(
                settings = settings,
                bill = bill,
                payments = payments,
                paid = paid,
                due = due,
                design = design,
                size = size,
                qrMode = qrMode,
                customQrPath = customQrPath,
                isBengali = useBengali
            )
        }
    }

    /**
     * Draws a unified, high-contrast pure-white 1D Code 128C Customer ID barcode strip right after
     * the bill header (completely separate from the square Payment UPI QR at the bottom).
     * Barcode bars and compact CID text are unified together with generous white quiet zones
     * and zero bilinear downscaling so mobile scanners lock instantly.
     */
    private fun drawCompactTopCustomerBarcodeBar(
        canvas: Canvas,
        left: Float,
        right: Float,
        topY: Float,
        dp: (Float) -> Float,
        barcodeBitmap: Bitmap,
        customerId: String,
        primaryColor: Int,
        mutedColor: Int,
        borderPaint: Paint,
        fillPaint: Paint? = null
    ): Float {
        val barCardH = dp(78f)
        val rect = RectF(left, topY, right, topY + barCardH)
        val pureWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(rect, dp(12f), dp(12f), pureWhite)
        canvas.drawRoundRect(rect, dp(12f), dp(12f), borderPaint)

        // Centered crisp 1D Code 128C barcode drawn at native integer pixel width (never downscaled!)
        val maxAvailW = (rect.width() - dp(28f)).roundToInt().coerceAtLeast(160)
        val drawW = min(barcodeBitmap.width, maxAvailW)
        val drawH = dp(44f).roundToInt().coerceAtLeast(40)
        val bLeft = (rect.centerX() - drawW / 2f).roundToInt()
        val bTop = (rect.top + dp(9f)).roundToInt()
        canvas.drawBitmap(
            barcodeBitmap,
            null,
            Rect(bLeft, bTop, bLeft + drawW, bTop + drawH),
            crispBarcodePaint()
        )

        // Unified compact Customer Barcode ID label centered cleanly beneath the bars
        val idValPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            textSize = dp(12f)
            letterSpacing = 0.12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val labelText = "CUSTOMER ID  •  CID-$customerId"
        canvas.drawText(
            labelText,
            rect.centerX() - idValPaint.measureText(labelText) / 2f,
            rect.bottom - dp(9f),
            idValPaint
        )

        return topY + barCardH + dp(12f)
    }

    /**
     * Brand-new Executive Ledger Accounting Sheet Structure (Platinum Ledger Pro & Emerald Ledger Elite):
     * Centered royal letterhead -> Compact top Bill Meta + Customer Barcode band -> Customer Profile strip ->
     * Formal 4-column Accounting Ledger Table -> Bottom split Payment Receipt + Square UPI QR -> Signature.
     */
    private fun renderExecutiveLedgerFormStyle(
        settings: BusinessSettings,
        bill: Bill,
        payments: List<Payment>,
        paid: Double,
        due: Double,
        design: BillDesignModel,
        size: BillPrintSize,
        qrMode: String,
        customQrPath: String,
        isBengali: Boolean
    ): Bitmap {
        val w = size.widthPx
        val scale = w / 680f
        fun dp(v: Float) = v * scale
        val outerPad = dp(24f)

        val qrBitmap = resolveQrBitmap(settings, due, qrMode, customQrPath)
        val customerBarcodeId = compactCustomerBarcodeId(bill.customerId).takeIf { it.isNotBlank() }
        val customerBarcodeBitmap = customerBarcodeId?.let {
            generateCode128BarcodeBitmap(
                it,
                widthPx = dp(290f).roundToInt().coerceAtLeast(220),
                heightPx = dp(52f).roundToInt().coerceAtLeast(48)
            )
        }
        val hasLand = bill.landAmount > 0.001 || bill.rate > 0.001
        val hasNote = bill.note.isNotBlank()
        val (actualCashPaid, settledWaived) = splitPaidAndSettled(payments, paid)
        val hasSettlement = settledWaived > 0.001
        val paymentRowsCount = minOf(payments.size, 3)

        val primaryColor = design.primaryColorInt
        val secondaryColor = design.secondaryColorInt
        val accentBg = design.accentBgInt
        val goldAccent = 0xFFD97706.toInt()
        val inkColor = 0xFF0F172A.toInt()
        val mutedColor = 0xFF475569.toInt()
        val ruleColor = 0xFFCBD5E1.toInt()
        val dueColor = if (due > 0.001) 0xFFDC2626.toInt() else 0xFF15803D.toInt()

        var estH = outerPad * 2f
        estH += dp(138f) // Centered Executive Crown Letterhead
        estH += dp(12f) + dp(86f) // Meta + Compact Barcode top strip
        estH += dp(12f) + dp(104f) // Horizontal 2x2 Customer Info band
        estH += dp(14f) + dp(196f) // Formal 4-column Ledger Table + Totals row
        estH += dp(14f) + if (qrBitmap != null) dp(if (hasSettlement) 232f else 210f) else dp(if (hasSettlement) 196f else 170f) // Bottom Split Payment Summary + UPI QR
        if (hasNote) estH += dp(10f) + dp(34f)
        estH += dp(14f) + dp(96f) // Handwritten Signature + footer
        if (size.isFullA4) {
            estH = max(estH, w * 1.38f)
        }

        val h = estH.roundToInt().coerceAtLeast(780)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // Crisp double executive stationery border
        val outerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = primaryColor
            strokeWidth = dp(2.2f)
        }
        val innerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = goldAccent
            alpha = 135
            strokeWidth = dp(0.9f)
        }
        canvas.drawRoundRect(RectF(dp(9f), dp(9f), w - dp(9f), h - dp(9f)), dp(16f), dp(16f), outerStroke)
        canvas.drawRoundRect(RectF(dp(14f), dp(14f), w - dp(14f), h - dp(14f)), dp(12f), dp(12f), innerStroke)

        val capPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = dp(11.5f)
            letterSpacing = 0.08f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = dp(13f)
        }
        val valPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = dp(14.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tableBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = ruleColor
            strokeWidth = dp(1.2f)
        }
        val accentFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentBg }
        val primaryFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor }
        val whiteFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

        var y = outerPad

        // 1. Centered Royal Letterhead Header
        val headerH = dp(136f)
        val headerRect = RectF(outerPad, y, w - outerPad, y + headerH)
        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                headerRect.left,
                headerRect.top,
                headerRect.right,
                headerRect.bottom,
                secondaryColor,
                primaryColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(headerRect, dp(14f), dp(14f), headerBg)

        // Top & bottom thin champagne gold accent rules inside letterhead
        val goldRule = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFBBF24.toInt()
            strokeWidth = dp(2.2f)
        }
        canvas.drawLine(headerRect.left + dp(20f), headerRect.top + dp(10f), headerRect.right - dp(20f), headerRect.top + dp(10f), goldRule)
        canvas.drawLine(headerRect.left + dp(20f), headerRect.bottom - dp(10f), headerRect.right - dp(20f), headerRect.bottom - dp(10f), goldRule)

        // Centered Micro-Header + Centered Company Name + Centered Contact
        val microPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFDE68A.toInt()
            textSize = dp(10.5f)
            letterSpacing = 0.16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val microText = if (isBengali) "অফিশিয়াল বিল লেজার" else "OFFICIAL BILLING LEDGER"
        canvas.drawText(microText, headerRect.centerX() - microPaint.measureText(microText) / 2f, headerRect.top + dp(32f), microPaint)

        val heroTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = dp(28f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val companyName = settings.company.ifBlank { "আশা মিনি শ্যালো" }
        canvas.drawText(companyName, headerRect.centerX() - heroTitlePaint.measureText(companyName) / 2f, headerRect.top + dp(68f), heroTitlePaint)

        val subHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE2E8F0.toInt()
            textSize = dp(13f)
        }
        if (settings.address.isNotBlank()) {
            val addr = settings.address.take(56)
            canvas.drawText(addr, headerRect.centerX() - subHeaderPaint.measureText(addr) / 2f, headerRect.top + dp(93f), subHeaderPaint)
        }
        val contactStrip = listOfNotNull(
            settings.contact.takeIf { it.isNotBlank() }?.let { "Ph: $it" },
            settings.upi.takeIf { it.isNotBlank() }?.let { "UPI: $it" }
        ).joinToString("  •  ")
        if (contactStrip.isNotBlank()) {
            canvas.drawText(contactStrip, headerRect.centerX() - subHeaderPaint.measureText(contactStrip) / 2f, headerRect.top + dp(115f), subHeaderPaint)
        }

        // Top-right status stamp pill
        val stampTxt = if (due <= 0.001) (if (isBengali) "পরিশোধিত" else "PAID") else (if (isBengali) "বকেয়া" else "DUE")
        val stampPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (due <= 0.001) 0xFF064E3B.toInt() else 0xFF7F1D1D.toInt()
            textSize = dp(10.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val stampW = stampPaint.measureText(stampTxt) + dp(18f)
        val stampRect = RectF(headerRect.right - dp(16f) - stampW, headerRect.top + dp(18f), headerRect.right - dp(16f), headerRect.top + dp(39f))
        val stampBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (due <= 0.001) 0xFFA7F3D0.toInt() else 0xFFFECACA.toInt()
        }
        canvas.drawRoundRect(stampRect, dp(10f), dp(10f), stampBg)
        canvas.drawText(stampTxt, stampRect.left + dp(9f), stampRect.top + dp(14.5f), stampPaint)

        y += headerH + dp(12f)

        // 2. Top Meta & Compact Customer Barcode Band (Separated from Bottom QR!)
        val metaH = dp(84f)
        val metaRect = RectF(outerPad, y, w - outerPad, y + metaH)
        canvas.drawRoundRect(metaRect, dp(12f), dp(12f), accentFill)
        canvas.drawRoundRect(metaRect, dp(12f), dp(12f), tableBorderPaint)

        // Left side: Bill No & Date stacked cleanly
        val metaLeftW = if (customerBarcodeBitmap != null) (metaRect.width() * 0.46f) else metaRect.width()
        canvas.drawText(if (isBengali) "বিল নং" else "BILL NO.", metaRect.left + dp(14f), metaRect.top + dp(25f), capPaint)
        canvas.drawText(bill.billNo.ifBlank { "ASHA" }, metaRect.left + dp(14f), metaRect.top + dp(47f), valPaint)

        val dateColX = metaRect.left + metaLeftW * 0.52f
        canvas.drawText(if (isBengali) "তারিখ" else "DATE", dateColX, metaRect.top + dp(25f), capPaint)
        canvas.drawText(bill.date, dateColX, metaRect.top + dp(47f), valPaint)
        if (bill.season.isNotBlank()) {
            val seasonTiny = TextPaint(labelPaint).apply { textSize = dp(11.5f) }
            canvas.drawText("${if (isBengali) "সিজন" else "Season"}: ${bill.season.take(22)}", metaRect.left + dp(14f), metaRect.bottom - dp(10f), seasonTiny)
        }

        // Right side: Pure-white inset box with crisp Code 128C Barcode + compact CID
        if (customerBarcodeBitmap != null && customerBarcodeId != null) {
            val divX = metaRect.left + metaLeftW
            val whiteBox = RectF(divX + dp(6f), metaRect.top + dp(6f), metaRect.right - dp(6f), metaRect.bottom - dp(6f))
            canvas.drawRoundRect(whiteBox, dp(9f), dp(9f), whiteFill)
            canvas.drawRoundRect(whiteBox, dp(9f), dp(9f), tableBorderPaint)

            val maxBoxW = (whiteBox.width() - dp(14f)).roundToInt().coerceAtLeast(140)
            val bW = min(customerBarcodeBitmap.width, maxBoxW)
            val bH = dp(42f).roundToInt().coerceAtLeast(38)
            val bLeft = (whiteBox.centerX() - bW / 2f).roundToInt()
            val bTop = (whiteBox.top + dp(6f)).roundToInt()
            canvas.drawBitmap(customerBarcodeBitmap, null, Rect(bLeft, bTop, bLeft + bW, bTop + bH), crispBarcodePaint())

            val cidPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inkColor
                textSize = dp(11f)
                letterSpacing = 0.08f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val cidTxt = "CID-$customerBarcodeId"
            canvas.drawText(cidTxt, whiteBox.centerX() - cidPaint.measureText(cidTxt) / 2f, whiteBox.bottom - dp(7f), cidPaint)
        }
        y += metaH + dp(12f)

        // 3. Horizontal Customer Profile Strip (2x2 Grid)
        val custH = dp(102f)
        val custRect = RectF(outerPad, y, w - outerPad, y + custH)
        canvas.drawRoundRect(custRect, dp(12f), dp(12f), whiteFill)
        canvas.drawRoundRect(custRect, dp(12f), dp(12f), tableBorderPaint)
        canvas.drawText(if (isBengali) "গ্রাহকের বিবরণ" else "CUSTOMER ACCOUNT", custRect.left + dp(14f), custRect.top + dp(22f), capPaint)
        canvas.drawLine(custRect.left, custRect.top + dp(30f), custRect.right, custRect.top + dp(30f), tableBorderPaint)

        val midX = custRect.centerX()
        val cRow1 = custRect.top + dp(56f)
        val cRow2 = custRect.top + dp(84f)
        canvas.drawText("${if (isBengali) "নাম" else "Name"}: ${bill.customerName.take(22)}", custRect.left + dp(14f), cRow1, valPaint)
        canvas.drawText("${if (isBengali) "ফোন" else "Phone"}: ${bill.phone.ifBlank { "—" }}", midX + dp(8f), cRow1, valPaint)
        canvas.drawText("${if (isBengali) "গ্রাম" else "Village"}: ${bill.village.ifBlank { "—" }}", custRect.left + dp(14f), cRow2, labelPaint)
        canvas.drawText("${if (isBengali) "ঠিকানা" else "Address"}: ${bill.address.take(24).ifBlank { "—" }}", midX + dp(8f), cRow2, labelPaint)
        y += custH + dp(14f)

        // 4. Formal 4-Column Accounting Ledger Table (Brand-New Structure!)
        val tableH = dp(192f)
        val tableRect = RectF(outerPad, y, w - outerPad, y + tableH)
        canvas.drawRoundRect(tableRect, dp(12f), dp(12f), whiteFill)

        // Table Header Bar
        val tHeadH = dp(36f)
        val tHeadRect = RectF(tableRect.left, tableRect.top, tableRect.right, tableRect.top + tHeadH)
        canvas.save()
        val headClip = Path().apply { addRoundRect(tableRect, dp(12f), dp(12f), Path.Direction.CW) }
        canvas.clipPath(headClip)
        canvas.drawRect(tHeadRect, primaryFill)
        canvas.restore()

        val col1X = tableRect.left + dp(12f)
        val col2X = tableRect.left + tableRect.width() * 0.44f
        val col3X = tableRect.left + tableRect.width() * 0.66f
        val col4Right = tableRect.right - dp(12f)

        val thPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = dp(11.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(if (isBengali) "বিবরণ" else "PARTICULARS", col1X, tableRect.top + dp(23f), thPaint)
        canvas.drawText(if (isBengali) "জমির পরিমাণ" else "LAND / QTY", col2X + dp(6f), tableRect.top + dp(23f), thPaint)
        canvas.drawText(if (isBengali) "রেট" else "RATE", col3X + dp(6f), tableRect.top + dp(23f), thPaint)
        val amtHead = if (isBengali) "টাকা" else "AMOUNT"
        canvas.drawText(amtHead, col4Right - thPaint.measureText(amtHead), tableRect.top + dp(23f), thPaint)

        // Vertical column dividers inside table body
        val bodyBottom = tableRect.bottom - dp(44f)
        canvas.drawLine(col2X, tableRect.top + tHeadH, col2X, bodyBottom, tableBorderPaint)
        canvas.drawLine(col3X, tableRect.top + tHeadH, col3X, bodyBottom, tableBorderPaint)
        canvas.drawLine(tableRect.left + tableRect.width() * 0.80f, tableRect.top + tHeadH, tableRect.left + tableRect.width() * 0.80f, bodyBottom, tableBorderPaint)

        // Row 1: Current Charge / Season Irrigation
        val r1Y = tableRect.top + tHeadH + dp(34f)
        val r1LineY = tableRect.top + tHeadH + dp(56f)
        if (hasLand) {
            val desc = if (isBengali) "বর্তমান বিল (${bill.season.ifBlank { "সিজন" }})" else "Current Bill (${bill.season.ifBlank { "Season" }})"
            canvas.drawText(desc.take(26), col1X, r1Y, valPaint)
            val qtyStr = "${String.format(Locale.US, "%.2f", bill.bigha)} Bigha"
            val qtySub = "${formatQty(bill.landAmount)} ${bill.unit}"
            canvas.drawText(qtyStr, col2X + dp(6f), r1Y - dp(6f), valPaint)
            val tinyPaint = TextPaint(labelPaint).apply { textSize = dp(11f) }
            canvas.drawText(qtySub, col2X + dp(6f), r1Y + dp(12f), tinyPaint)
            canvas.drawText(formatInr(bill.rate), col3X + dp(6f), r1Y, valPaint)
            val curStr = formatInr(bill.current)
            canvas.drawText(curStr, col4Right - valPaint.measureText(curStr), r1Y, valPaint)
        } else {
            canvas.drawText(if (isBengali) "জের বাকি হিসাব" else "Previous Due Recovery", col1X, r1Y, valPaint)
            canvas.drawText("—", col2X + dp(6f), r1Y, labelPaint)
            canvas.drawText("—", col3X + dp(6f), r1Y, labelPaint)
            val z = formatInr(0.0)
            canvas.drawText(z, col4Right - valPaint.measureText(z), r1Y, labelPaint)
        }
        canvas.drawLine(tableRect.left, r1LineY, tableRect.right, r1LineY, tableBorderPaint)

        // Row 2: Previous Due Brought Forward
        val r2Y = r1LineY + dp(34f)
        canvas.drawText(if (isBengali) "জের বাকি (পূর্বের বকেয়া)" else "Previous Due Brought Forward", col1X, r2Y, labelPaint)
        canvas.drawText("—", col2X + dp(6f), r2Y, labelPaint)
        canvas.drawText("—", col3X + dp(6f), r2Y, labelPaint)
        val prevStr = formatInr(bill.previousDue)
        canvas.drawText(prevStr, col4Right - valPaint.measureText(prevStr), r2Y, valPaint)

        // Footer Total Bar inside table
        val totRect = RectF(tableRect.left, bodyBottom, tableRect.right, tableRect.bottom)
        canvas.save()
        canvas.clipPath(headClip)
        canvas.drawRect(totRect, accentFill)
        canvas.restore()
        canvas.drawLine(tableRect.left, bodyBottom, tableRect.right, bodyBottom, tableBorderPaint)

        val totTitlePaint = TextPaint(valPaint).apply {
            textSize = dp(16f)
            color = primaryColor
        }
        canvas.drawText(if (isBengali) "মোট মূল্য (TOTAL AMOUNT)" else "TOTAL AMOUNT", col1X, bodyBottom + dp(28f), totTitlePaint)
        val totValStr = formatInr(bill.allTotal)
        canvas.drawText(totValStr, col4Right - totTitlePaint.measureText(totValStr), bodyBottom + dp(28f), totTitlePaint)

        canvas.drawRoundRect(tableRect, dp(12f), dp(12f), tableBorderPaint)
        y += tableH + dp(14f)

        // 5. Bottom Split Block: Left = Payment Receipt & Balance Due, Right = Square UPI QR (Far from top barcode!)
        val splitH = if (qrBitmap != null) dp(if (hasSettlement) 230f else 206f) else dp(if (hasSettlement) 194f else 166f)
        val gap = dp(12f)
        val availW = w - outerPad * 2f
        val leftW = if (qrBitmap != null) (availW - gap) * 0.58f else availW
        val payRect = RectF(outerPad, y, outerPad + leftW, y + splitH)
        canvas.drawRoundRect(payRect, dp(14f), dp(14f), whiteFill)
        canvas.drawRoundRect(payRect, dp(14f), dp(14f), tableBorderPaint)

        canvas.drawText(if (isBengali) "পেমেন্ট সারসংক্ষেপ" else "SETTLEMENT LEDGER", payRect.left + dp(14f), payRect.top + dp(24f), capPaint)

        // Actual Cash/UPI Paid row (excludes Settlement waiver!)
        val paidPaint = TextPaint(valPaint).apply { color = 0xFF15803D.toInt(); textSize = dp(15.5f) }
        canvas.drawText(if (isBengali) "জমা (Paid)" else "Paid Received", payRect.left + dp(14f), payRect.top + dp(52f), labelPaint)
        val paidStr = formatInr(actualCashPaid)
        canvas.drawText(paidStr, payRect.right - dp(14f) - paidPaint.measureText(paidStr), payRect.top + dp(52f), paidPaint)

        var py = payRect.top + dp(76f)
        if (hasSettlement) {
            val settledPaint = TextPaint(valPaint).apply { color = 0xFFB45309.toInt(); textSize = dp(14.5f) }
            canvas.drawText(
                if (isBengali) "ছাড় / সেটেল" else "Settled / Discount",
                payRect.left + dp(14f),
                py,
                labelPaint
            )
            val setStr = formatInr(settledWaived)
            canvas.drawText(setStr, payRect.right - dp(14f) - settledPaint.measureText(setStr), py, settledPaint)
            py += dp(24f)
        }

        // Recent payments compact lines (up to 3)
        if (paymentRowsCount > 0) {
            val miniPaint = TextPaint(labelPaint).apply { textSize = dp(11.5f) }
            payments.take(paymentRowsCount).forEach { p ->
                val isSetRow = PaymentMode.fromLabel(p.mode) == PaymentMode.SETTLEMENT
                val modeLbl = if (isSetRow) (if (isBengali) "সেটেল ছাড়" else "Settled") else p.mode
                canvas.drawText("${p.date} · $modeLbl", payRect.left + dp(14f), py, miniPaint)
                val pAmt = if (isSetRow) formatInr(p.amount) else "+${formatInr(p.amount)}"
                val pCol = TextPaint(miniPaint).apply {
                    color = if (isSetRow) 0xFFB45309.toInt() else 0xFF15803D.toInt()
                    typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawText(pAmt, payRect.right - dp(14f) - pCol.measureText(pAmt), py, pCol)
                py += dp(19f)
            }
        }

        // High-contrast Payable Due box anchored at bottom of left card
        val dueBoxRect = RectF(payRect.left + dp(12f), payRect.bottom - dp(52f), payRect.right - dp(12f), payRect.bottom - dp(12f))
        canvas.drawRoundRect(dueBoxRect, dp(10f), dp(10f), accentFill)
        val dueLblPaint = TextPaint(valPaint).apply { textSize = dp(15.5f) }
        val dueValPaint = TextPaint(valPaint).apply { textSize = dp(19f); color = dueColor }
        canvas.drawText(if (isBengali) "মোট বাকি" else "Payable Due", dueBoxRect.left + dp(12f), dueBoxRect.top + dp(26f), dueLblPaint)
        val dueStr = formatInr(due)
        canvas.drawText(dueStr, dueBoxRect.right - dp(12f) - dueValPaint.measureText(dueStr), dueBoxRect.top + dp(27f), dueValPaint)

        // Right: Framed Square UPI QR Card
        if (qrBitmap != null) {
            val qrRect = RectF(payRect.right + gap, y, w - outerPad, y + splitH)
            canvas.drawRoundRect(qrRect, dp(14f), dp(14f), accentFill)
            canvas.drawRoundRect(qrRect, dp(14f), dp(14f), tableBorderPaint)

            val qrTitle = if (isBengali) "স্ক্যান ও পে (UPI)" else "SCAN & PAY UPI"
            canvas.drawText(qrTitle, qrRect.centerX() - capPaint.measureText(qrTitle) / 2f, qrRect.top + dp(24f), capPaint)

            val qrSize = dp(122f).roundToInt()
            val qrLeft = (qrRect.centerX() - qrSize / 2f).roundToInt()
            val qrTop = (qrRect.top + (splitH - qrSize) / 2f - dp(4f)).roundToInt()
            canvas.drawRoundRect(RectF(qrLeft - dp(6f), qrTop - dp(6f), qrLeft + qrSize + dp(6f), qrTop + qrSize + dp(6f)), dp(8f), dp(8f), whiteFill)
            canvas.drawBitmap(qrBitmap, null, Rect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), null)

            val amtTag = when {
                due > 0.001 -> formatInr(due)
                hasSettlement -> if (isBengali) "সেটেল · ₹0" else "SETTLED · ₹0"
                else -> if (isBengali) "পরিশোধিত · ₹0" else "PAID · ₹0"
            }
            val qrAmtPaint = TextPaint(valPaint).apply { textSize = dp(13.5f); color = if (due > 0.001) primaryColor else 0xFF15803D.toInt() }
            canvas.drawText(amtTag, qrRect.centerX() - qrAmtPaint.measureText(amtTag) / 2f, qrRect.bottom - dp(14f), qrAmtPaint)
        }
        y += splitH + dp(14f)

        if (hasNote) {
            canvas.drawText("Note: ${bill.note.take(58)}", outerPad + dp(6f), y + dp(16f), labelPaint)
            y += dp(32f)
        }

        drawHandwrittenOwnerSignatureSection(
            canvas = canvas,
            left = outerPad,
            right = w - outerPad,
            topY = y,
            dp = { dp(it) },
            settings = settings,
            billDate = bill.date,
            primaryColor = primaryColor,
            mutedColor = mutedColor
        )

        return bitmap
    }

    /**
     * Brand-new Light Atelier Editorial Form layout for Sakura Ivory, Sky Pearl, and Marigold Cream:
     * Airy tinted pastel paper, ink-friendly luminous light header with dark crisp typography,
     * ornamental corner brackets, 2-column Bill Meta + Customer split box, 2x2 pastel metric tiles,
     * and side-by-side Payment Summary + Square UPI QR card.
     */
    private fun renderLightAtelierStyle(
        settings: BusinessSettings,
        bill: Bill,
        payments: List<Payment>,
        paid: Double,
        due: Double,
        design: BillDesignModel,
        size: BillPrintSize,
        qrMode: String,
        customQrPath: String,
        isBengali: Boolean
    ): Bitmap {
        val w = size.widthPx
        val scale = w / 680f
        fun dp(v: Float) = v * scale
        val outerPad = dp(24f)
        val innerPad = dp(16f)

        val qrBitmap = resolveQrBitmap(settings, due, qrMode, customQrPath)
        val customerBarcodeId = compactCustomerBarcodeId(bill.customerId).takeIf { it.isNotBlank() }
        val customerBarcodeBitmap = customerBarcodeId?.let {
            generateCode128BarcodeBitmap(
                it,
                widthPx = dp(460f).roundToInt().coerceAtLeast(320),
                heightPx = dp(64f).roundToInt().coerceAtLeast(56)
            )
        }
        val hasLand = bill.landAmount > 0.001 || bill.rate > 0.001
        val hasNote = bill.note.isNotBlank()
        val (actualCashPaid, settledWaived) = splitPaidAndSettled(payments, paid)
        val hasSettlement = settledWaived > 0.001
        val paymentRowsCount = minOf(payments.size, 4)

        val primaryColor = design.primaryColorInt
        val pastelEnd = design.secondaryColorInt
        val accentBg = design.accentBgInt
        val paperBg = when (design) {
            BillDesignModel.SAKURA_IVORY -> 0xFFFFFDFD.toInt()
            BillDesignModel.SKY_PEARL -> 0xFFF9FCFF.toInt()
            BillDesignModel.MARIGOLD_CREAM -> 0xFFFFFDF7.toInt()
            else -> Color.WHITE
        }
        val inkColor = 0xFF0F172A.toInt()
        val mutedColor = 0xFF64748B.toInt()
        val cardBorder = 0xFFE2E8F0.toInt()
        val dueColor = if (due > 0.001) 0xFFDC2626.toInt() else 0xFF15803D.toInt()

        // Measure dynamic layout height
        var estH = outerPad * 2f
        estH += dp(122f) // Luminous light gradient header
        if (customerBarcodeBitmap != null) estH += dp(12f) + dp(80f) // Compact Customer ID Barcode right after header
        estH += dp(14f) + dp(156f) // Split 2-column Bill Meta + Customer Form Box
        estH += dp(14f) + if (hasLand) dp(184f) else dp(92f) // 2x2 Pastel Charge Tiles
        estH += dp(14f) + if (qrBitmap != null) dp(if (hasSettlement) 244f else 216f) else dp(if (hasSettlement) 224f else 196f) // Side-by-side Payment Summary + QR
        if (paymentRowsCount > 0) estH += dp(14f) + dp(46f) + paymentRowsCount * dp(28f)
        if (hasNote) estH += dp(12f) + dp(40f)
        estH += dp(16f) + dp(96f) // Handwritten signature + footer
        if (size.isFullA4) {
            estH = max(estH, w * 1.38f)
        }

        val h = estH.roundToInt().coerceAtLeast(760)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(paperBg)

        // Subtle outer pastel hairline frame + L-shaped editorial corner accents
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = primaryColor
            alpha = 85
            strokeWidth = dp(1.6f)
        }
        canvas.drawRoundRect(RectF(dp(10f), dp(10f), w - dp(10f), h - dp(10f)), dp(20f), dp(20f), framePaint)

        val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = primaryColor
            strokeWidth = dp(3.2f)
            strokeCap = Paint.Cap.ROUND
        }
        val cLen = dp(20f)
        val cOff = dp(10f)
        // Top-left & Top-right & Bottom-left & Bottom-right brackets
        canvas.drawLine(cOff, cOff + cLen, cOff, cOff + dp(6f), cornerPaint)
        canvas.drawLine(cOff + dp(6f), cOff, cOff + cLen, cOff, cornerPaint)
        canvas.drawLine(w - cOff - cLen, cOff, w - cOff - dp(6f), cOff, cornerPaint)
        canvas.drawLine(w - cOff, cOff + dp(6f), w - cOff, cOff + cLen, cornerPaint)
        canvas.drawLine(cOff, h - cOff - cLen, cOff, h - cOff - dp(6f), cornerPaint)
        canvas.drawLine(cOff + dp(6f), h - cOff, cOff + cLen, h - cOff, cornerPaint)
        canvas.drawLine(w - cOff - cLen, h - cOff, w - cOff - dp(6f), h - cOff, cornerPaint)
        canvas.drawLine(w - cOff, h - cOff - cLen, w - cOff, h - cOff - dp(6f), cornerPaint)

        // Typography paints
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = dp(26f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF334155.toInt()
            textSize = dp(13.5f)
        }
        val capPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = dp(12f)
            letterSpacing = 0.08f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = dp(13.5f)
        }
        val valPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = dp(15.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val whiteCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val pastelCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentBg }
        val softBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = cardBorder
            strokeWidth = dp(1.3f)
        }
        val accentBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = primaryColor
            alpha = 95
            strokeWidth = dp(1.4f)
        }
        val hairlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF1F5F9.toInt()
            strokeWidth = dp(1.1f)
        }

        var y = outerPad

        // 1. Luminous Light Pastel Gradient Header (printer-friendly, ultra-crisp dark text)
        val headerH = dp(120f)
        val headerRect = RectF(outerPad, y, w - outerPad, y + headerH)
        val headerGradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                headerRect.left,
                headerRect.top,
                headerRect.right,
                headerRect.bottom,
                accentBg,
                pastelEnd,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(headerRect, dp(18f), dp(18f), headerGradPaint)
        canvas.drawRoundRect(headerRect, dp(18f), dp(18f), accentBorderPaint)

        // Subtle decorative translucent orb inside header right
        canvas.save()
        val clipPath = Path().apply { addRoundRect(headerRect, dp(18f), dp(18f), Path.Direction.CW) }
        canvas.clipPath(clipPath)
        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            alpha = 20
        }
        canvas.drawCircle(headerRect.right - dp(24f), headerRect.top + dp(16f), dp(74f), orbPaint)
        // Left vertical accent stripe
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor }
        canvas.drawRect(headerRect.left, headerRect.top, headerRect.left + dp(7f), headerRect.bottom, barPaint)
        canvas.restore()

        // High-contrast status capsule
        val badgeText = when {
            due <= 0.001 -> if (isBengali) "পরিশোধিত" else "PAID"
            isBengali -> "বিল রশিদ"
            else -> "OFFICIAL BILL"
        }
        val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = dp(11f)
            letterSpacing = 0.05f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val badgeW = badgeTextPaint.measureText(badgeText) + dp(22f)
        val badgeH = dp(24f)
        val badgeRect = RectF(
            headerRect.right - innerPad - badgeW,
            headerRect.top + dp(14f),
            headerRect.right - innerPad,
            headerRect.top + dp(14f) + badgeH
        )
        canvas.drawRoundRect(badgeRect, dp(12f), dp(12f), barPaint)
        canvas.drawText(badgeText, badgeRect.left + dp(11f), badgeRect.top + dp(16f), badgeTextPaint)

        val companyName = settings.company.ifBlank { "আশা মিনি শ্যালো" }
        canvas.drawText(companyName, headerRect.left + dp(20f), headerRect.top + dp(40f), titlePaint)
        if (settings.address.isNotBlank()) {
            canvas.drawText(settings.address.take(52), headerRect.left + dp(20f), headerRect.top + dp(66f), subPaint)
        }
        val contactLine = listOfNotNull(
            settings.contact.takeIf { it.isNotBlank() }?.let { "Ph: $it" },
            settings.upi.takeIf { it.isNotBlank() }?.let { "UPI: $it" }
        ).joinToString("  •  ")
        if (contactLine.isNotBlank()) {
            canvas.drawText(contactLine, headerRect.left + dp(20f), headerRect.top + dp(94f), subPaint)
        }
        y += headerH + dp(12f)

        // Compact 1D Customer ID Barcode right after header (completely separate from bottom Payment QR)
        if (customerBarcodeBitmap != null && customerBarcodeId != null) {
            y = drawCompactTopCustomerBarcodeBar(
                canvas = canvas,
                left = outerPad,
                right = w - outerPad,
                topY = y,
                dp = { dp(it) },
                barcodeBitmap = customerBarcodeBitmap,
                customerId = customerBarcodeId,
                primaryColor = primaryColor,
                mutedColor = mutedColor,
                borderPaint = softBorderPaint,
                fillPaint = whiteCardPaint
            )
        }

        // 2. Two-Column Split Form Box: Left = Bill Meta (40%), Right = Customer Profile (60%)
        val colGap = dp(12f)
        val availW = w - outerPad * 2f
        val leftColW = (availW - colGap) * 0.40f
        val rightColW = (availW - colGap) * 0.60f
        val splitH = dp(154f)

        val metaRect = RectF(outerPad, y, outerPad + leftColW, y + splitH)
        canvas.drawRoundRect(metaRect, dp(16f), dp(16f), pastelCardPaint)
        canvas.drawRoundRect(metaRect, dp(16f), dp(16f), accentBorderPaint)

        canvas.drawText(if (isBengali) "বিল তথ্য" else "INVOICE META", metaRect.left + dp(14f), metaRect.top + dp(24f), capPaint)
        canvas.drawText(if (isBengali) "বিল নং" else "Bill Number", metaRect.left + dp(14f), metaRect.top + dp(48f), labelPaint)
        canvas.drawText(bill.billNo.ifBlank { "ASHA" }, metaRect.left + dp(14f), metaRect.top + dp(68f), valPaint)
        canvas.drawLine(metaRect.left + dp(14f), metaRect.top + dp(80f), metaRect.right - dp(14f), metaRect.top + dp(80f), hairlinePaint)
        canvas.drawText(if (isBengali) "তারিখ" else "Bill Date", metaRect.left + dp(14f), metaRect.top + dp(100f), labelPaint)
        canvas.drawText(bill.date, metaRect.left + dp(14f), metaRect.top + dp(120f), valPaint)
        if (bill.season.isNotBlank()) {
            val sPillPaint = TextPaint(capPaint).apply { textSize = dp(11f) }
            canvas.drawText(
                "${if (isBengali) "সিজন" else "Season"}: ${bill.season.take(18)}",
                metaRect.left + dp(14f),
                metaRect.top + dp(142f),
                sPillPaint
            )
        }

        val custRect = RectF(metaRect.right + colGap, y, w - outerPad, y + splitH)
        canvas.drawRoundRect(custRect, dp(16f), dp(16f), whiteCardPaint)
        canvas.drawRoundRect(custRect, dp(16f), dp(16f), softBorderPaint)
        canvas.drawText(if (isBengali) "গ্রাহকের বিবরণ" else "BILLED TO", custRect.left + dp(14f), custRect.top + dp(24f), capPaint)
        val custNamePaint = TextPaint(valPaint).apply { textSize = dp(18f) }
        canvas.drawText(bill.customerName.take(24), custRect.left + dp(14f), custRect.top + dp(52f), custNamePaint)

        val phoneTxt = "${if (isBengali) "ফোন" else "Phone"}: ${bill.phone.ifBlank { "—" }}"
        val villTxt = "${if (isBengali) "গ্রাম" else "Village"}: ${bill.village.ifBlank { "—" }}"
        val addrTxt = "${if (isBengali) "ঠিকানা" else "Address"}: ${bill.address.take(26).ifBlank { "—" }}"
        canvas.drawText(phoneTxt, custRect.left + dp(14f), custRect.top + dp(80f), subPaint)
        canvas.drawText(villTxt, custRect.left + dp(14f), custRect.top + dp(104f), subPaint)
        canvas.drawText(addrTxt, custRect.left + dp(14f), custRect.top + dp(128f), labelPaint)
        y += splitH + dp(14f)

        // 3. 2x2 Pastel Charge Metric Tiles (Brand-New Form Design)
        if (hasLand) {
            val gridHeaderH = dp(24f)
            canvas.drawText(if (isBengali) "জমি ও বিলের বিবরণ" else "CHARGE DETAILS", outerPad + dp(4f), y + dp(16f), capPaint)
            val tileTop = y + gridHeaderH + dp(4f)
            val tileGap = dp(10f)
            val tileW = (availW - tileGap) / 2f
            val tileH = dp(70f)

            fun drawMetricTile(
                col: Int,
                row: Int,
                tileLabel: String,
                tileValue: String,
                highlight: Boolean = false
            ) {
                val left = outerPad + col * (tileW + tileGap)
                val top = tileTop + row * (tileH + tileGap)
                val rect = RectF(left, top, left + tileW, top + tileH)
                canvas.drawRoundRect(rect, dp(14f), dp(14f), if (highlight) pastelCardPaint else whiteCardPaint)
                canvas.drawRoundRect(rect, dp(14f), dp(14f), if (highlight) accentBorderPaint else softBorderPaint)
                canvas.drawText(tileLabel, rect.left + dp(14f), rect.top + dp(24f), labelPaint)
                val vp = TextPaint(valPaint).apply {
                    textSize = if (highlight) dp(18.5f) else dp(15.5f)
                    color = if (highlight) primaryColor else inkColor
                }
                canvas.drawText(tileValue, rect.left + dp(14f), rect.top + dp(52f), vp)
            }

            drawMetricTile(0, 0, if (isBengali) "সিজন" else "Season", bill.season.ifBlank { "Regular" })
            drawMetricTile(
                1,
                0,
                if (isBengali) "জমির পরিমাণ" else "Land",
                "${String.format(Locale.US, "%.2f", bill.bigha)} Bigha (${formatQty(bill.landAmount)} ${bill.unit})"
            )
            drawMetricTile(
                0,
                1,
                if (isBengali) "রেট" else "Rate",
                if (isBengali) "${formatInr(bill.rate)}/বিঘা" else "${formatInr(bill.rate)}/Bigha"
            )
            drawMetricTile(
                1,
                1,
                if (isBengali) "বর্তমান বিল" else "Current Bill",
                formatInr(bill.current),
                highlight = true
            )
            y += dp(184f) + dp(14f)
        } else {
            val recH = dp(86f)
            val recRect = RectF(outerPad, y, w - outerPad, y + recH)
            canvas.drawRoundRect(recRect, dp(14f), dp(14f), whiteCardPaint)
            canvas.drawRoundRect(recRect, dp(14f), dp(14f), softBorderPaint)
            canvas.drawText(if (isBengali) "জমি ও বিলের বিবরণ" else "CHARGE DETAILS", recRect.left + dp(14f), recRect.top + dp(26f), capPaint)
            canvas.drawText(if (isBengali) "জের বাকি হিসাব" else "Previous Due Recovery Bill", recRect.left + dp(14f), recRect.top + dp(58f), valPaint)
            y += recH + dp(14f)
        }

        // 4. Side-by-Side Payment Summary Box + Square UPI QR Card
        val sumBlockH = if (qrBitmap != null) dp(if (hasSettlement) 240f else 212f) else dp(if (hasSettlement) 220f else 192f)
        val sumW = if (qrBitmap != null) (availW - colGap) * 0.60f else availW
        val sumRect = RectF(outerPad, y, outerPad + sumW, y + sumBlockH)
        canvas.drawRoundRect(sumRect, dp(16f), dp(16f), whiteCardPaint)
        canvas.drawRoundRect(sumRect, dp(16f), dp(16f), accentBorderPaint)

        canvas.drawText(if (isBengali) "পেমেন্ট সারসংক্ষেপ" else "PAYMENT SUMMARY", sumRect.left + dp(14f), sumRect.top + dp(26f), capPaint)

        fun drawSumLine(rowY: Float, k: String, v: String, vColor: Int = inkColor) {
            val vp = TextPaint(valPaint).apply { color = vColor }
            canvas.drawText(k, sumRect.left + dp(14f), rowY, labelPaint)
            val vw = vp.measureText(v)
            canvas.drawText(v, sumRect.right - dp(14f) - vw, rowY, vp)
            canvas.drawLine(sumRect.left + dp(14f), rowY + dp(10f), sumRect.right - dp(14f), rowY + dp(10f), hairlinePaint)
        }

        var sy = sumRect.top + dp(52f)
        drawSumLine(sy, if (isBengali) "জের বাকি" else "Previous Due", formatInr(bill.previousDue))
        sy += dp(30f)
        drawSumLine(sy, if (isBengali) "মোট মূল্য" else "Total Amount", formatInr(bill.allTotal))
        sy += dp(30f)
        drawSumLine(sy, if (isBengali) "জমা" else "Paid", formatInr(actualCashPaid), vColor = 0xFF15803D.toInt())
        if (hasSettlement) {
            sy += dp(30f)
            drawSumLine(sy, if (isBengali) "ছাড় / সেটেল" else "Settled / Discount", formatInr(settledWaived), vColor = 0xFFB45309.toInt())
        }

        // Highlighted Payable Due pill banner at bottom of summary card
        val duePillRect = RectF(
            sumRect.left + dp(12f),
            sumRect.bottom - dp(54f),
            sumRect.right - dp(12f),
            sumRect.bottom - dp(12f)
        )
        canvas.drawRoundRect(duePillRect, dp(12f), dp(12f), pastelCardPaint)
        val dueLabelPaint = TextPaint(valPaint).apply { textSize = dp(16f) }
        val dueAmtPaint = TextPaint(valPaint).apply {
            textSize = dp(19.5f)
            color = dueColor
        }
        val dueLabelText = if (isBengali) "মোট বাকি" else "Payable Due"
        canvas.drawText(dueLabelText, duePillRect.left + dp(12f), duePillRect.top + dp(27f), dueLabelPaint)
        val dueValStr = formatInr(due)
        val dvw = dueAmtPaint.measureText(dueValStr)
        canvas.drawText(dueValStr, duePillRect.right - dp(12f) - dvw, duePillRect.top + dp(28f), dueAmtPaint)

        // Right UPI QR Card (if available)
        if (qrBitmap != null) {
            val qrRect = RectF(sumRect.right + colGap, y, w - outerPad, y + sumBlockH)
            canvas.drawRoundRect(qrRect, dp(16f), dp(16f), pastelCardPaint)
            canvas.drawRoundRect(qrRect, dp(16f), dp(16f), accentBorderPaint)

            val qrCap = if (isBengali) "স্ক্যান ও পে (UPI)" else "SCAN & PAY"
            val qcw = capPaint.measureText(qrCap)
            canvas.drawText(qrCap, qrRect.centerX() - qcw / 2f, qrRect.top + dp(24f), capPaint)

            val qrSize = dp(126f).roundToInt()
            val qrLeft = (qrRect.centerX() - qrSize / 2f).roundToInt()
            val qrTop = (qrRect.top + dp(34f)).roundToInt()
            // White quiet zone behind QR
            canvas.drawRoundRect(
                RectF(qrLeft - dp(6f), qrTop - dp(6f), qrLeft + qrSize + dp(6f), qrTop + qrSize + dp(6f)),
                dp(10f),
                dp(10f),
                whiteCardPaint
            )
            canvas.drawBitmap(qrBitmap, null, Rect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), null)

            val payAmt = if (due > 0.001) formatInr(due) else formatInr(bill.allTotal)
            val qrAmtPaint = TextPaint(valPaint).apply {
                textSize = dp(14.5f)
                color = primaryColor
            }
            val qaw = qrAmtPaint.measureText(payAmt)
            canvas.drawText(payAmt, qrRect.centerX() - qaw / 2f, qrRect.bottom - dp(14f), qrAmtPaint)
        }
        y += sumBlockH + dp(14f)

        // 5. Recent Payments mini strip
        if (paymentRowsCount > 0) {
            val rpH = dp(44f) + paymentRowsCount * dp(26f)
            val rpRect = RectF(outerPad, y, w - outerPad, y + rpH)
            canvas.drawRoundRect(rpRect, dp(14f), dp(14f), whiteCardPaint)
            canvas.drawRoundRect(rpRect, dp(14f), dp(14f), softBorderPaint)
            canvas.drawText("RECENT PAYMENTS", rpRect.left + dp(14f), rpRect.top + dp(24f), capPaint)
            var ry = rpRect.top + dp(48f)
            payments.take(paymentRowsCount).forEachIndexed { idx, p ->
                canvas.drawText("${p.date} · ${p.mode}", rpRect.left + dp(14f), ry, labelPaint)
                val amtStr = "+${formatInr(p.amount)}"
                val pValPaint = TextPaint(valPaint).apply {
                    textSize = dp(14.5f)
                    color = 0xFF15803D.toInt()
                }
                val aw = pValPaint.measureText(amtStr)
                canvas.drawText(amtStr, rpRect.right - dp(14f) - aw, ry, pValPaint)
                if (idx < paymentRowsCount - 1) {
                    canvas.drawLine(rpRect.left + dp(14f), ry + dp(9f), rpRect.right - dp(14f), ry + dp(9f), hairlinePaint)
                }
                ry += dp(26f)
            }
            y += rpH + dp(14f)
        }

        if (hasNote) {
            canvas.drawText("Note: ${bill.note.take(58)}", outerPad + dp(6f), y + dp(18f), labelPaint)
            y += dp(36f)
        }

        // 6. Handwritten Proprietor Signature + Date
        drawHandwrittenOwnerSignatureSection(
            canvas = canvas,
            left = outerPad,
            right = w - outerPad,
            topY = y,
            dp = { dp(it) },
            settings = settings,
            billDate = bill.date,
            primaryColor = primaryColor,
            mutedColor = mutedColor
        )

        return bitmap
    }

    /**
     * Renders the beloved sectioned rounded-card bill style inspired by the user's reference photo
     * (Emerald/Gold/Midnight/Sapphire/Ruby/Bengali/QR-Focused/Teal/Saffron/Amethyst) + handwritten signature.
     */
    private fun renderSectionedCardStyle(
        settings: BusinessSettings,
        bill: Bill,
        payments: List<Payment>,
        paid: Double,
        due: Double,
        design: BillDesignModel,
        size: BillPrintSize,
        qrMode: String,
        customQrPath: String,
        isBengali: Boolean
    ): Bitmap {
        val w = size.widthPx
        val scale = w / 680f
        fun dp(v: Float) = v * scale
        val outerPad = dp(24f)
        val innerPad = dp(18f)

        val qrBitmap = resolveQrBitmap(settings, due, qrMode, customQrPath)
        val customerBarcodeId = compactCustomerBarcodeId(bill.customerId).takeIf { it.isNotBlank() }
        val customerBarcodeBitmap = customerBarcodeId?.let {
            generateCode128BarcodeBitmap(
                it,
                widthPx = dp(460f).roundToInt().coerceAtLeast(320),
                heightPx = dp(64f).roundToInt().coerceAtLeast(56)
            )
        }
        val isQrHero = design == BillDesignModel.QR_FOCUSED
        val hasLand = bill.landAmount > 0.001 || bill.rate > 0.001
        val hasNote = bill.note.isNotBlank()
        val (actualCashPaid, settledWaived) = splitPaidAndSettled(payments, paid)
        val hasSettlement = settledWaived > 0.001
        val paymentRowsCount = minOf(payments.size, 4)

        val primaryColor = design.primaryColorInt
        val secondaryColor = design.secondaryColorInt
        val accentBg = design.accentBgInt
        val inkColor = 0xFF111827.toInt()
        val mutedColor = 0xFF64748B.toInt()
        val borderColor = 0xFFE2E8F0.toInt()
        val dueColor = if (due > 0.001) 0xFFDC2626.toInt() else 0xFF15803D.toInt()

        // Measure dynamic total height accurately
        val isEmeraldPro = design == BillDesignModel.PREMIUM_GREEN
        var estH = outerPad * 2f
        estH += if (isEmeraldPro) dp(154f) else dp(130f) // Top colored brand banner
        if (customerBarcodeBitmap != null) {
            estH += dp(12f) + dp(80f) // Compact Customer ID barcode right after header
        }
        estH += dp(14f) + dp(72f) // Bill No & Date twin pills
        if (isQrHero && qrBitmap != null) {
            estH += dp(14f) + dp(236f)
        }
        estH += dp(14f) + dp(184f) // Customer Details card
        estH += dp(14f) + if (hasLand) dp(184f) else dp(104f) // Charge Details card
        estH += dp(14f) + dp(if (hasSettlement) 218f else 186f) // Payment Summary tinted card
        if (paymentRowsCount > 0) estH += dp(14f) + dp(48f) + paymentRowsCount * dp(28f)
        if (hasNote) estH += dp(12f) + dp(44f)
        if (!isQrHero && qrBitmap != null) {
            estH += dp(14f) + dp(236f) // Payment QR card
        }
        estH += dp(16f) + dp(96f) // Handwritten signature + date + footer
        if (size.isFullA4) {
            estH = max(estH, w * 1.38f)
        }

        val h = estH.roundToInt().coerceAtLeast(760)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // Outer subtle colored border frame (+ gold inner double frame for luxury editions)
        val isRoyalGold = design == BillDesignModel.ROYAL_GOLD
        val isMidnight = design == BillDesignModel.MIDNIGHT_PREMIUM
        val hasLuxuryDoubleTrim = isEmeraldPro || isRoyalGold || isMidnight || isBengali ||
            design == BillDesignModel.IMPERIAL_TEAL ||
            design == BillDesignModel.SUNSET_SAFFRON ||
            design == BillDesignModel.AMETHYST_LUXE
        val outerBorderRect = RectF(dp(8f), dp(8f), w - dp(8f), h - dp(8f))
        val outerFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = primaryColor
            strokeWidth = dp(2.4f)
        }
        canvas.drawRoundRect(outerBorderRect, dp(22f), dp(22f), outerFramePaint)
        if (hasLuxuryDoubleTrim) {
            val innerTrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = if (isRoyalGold || isBengali) 0xFFD97706.toInt() else primaryColor
                alpha = 110
                strokeWidth = dp(1f)
            }
            canvas.drawRoundRect(
                RectF(dp(14f), dp(14f), w - dp(14f), h - dp(14f)),
                dp(18f),
                dp(18f),
                innerTrimPaint
            )
        }

        val headerTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = dp(28f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isRoyalGold) 0xFFFDE68A.toInt() else 0xFFE2F5EC.toInt()
            textSize = dp(14.5f)
        }
        val sectionCapPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = dp(13.5f)
            letterSpacing = 0.04f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = dp(15.5f)
        }
        val valPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = dp(16.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val boxStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = if (isRoyalGold) 0xFFFDE68A.toInt() else borderColor
            strokeWidth = dp(1.4f)
        }
        val hairlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF1F5F9.toInt()
            strokeWidth = dp(1.2f)
        }

        var y = outerPad

        // 1. Top Gradient Brand Banner — Upgraded Royal Centered Header for Emerald Crest Pro
        val bannerH = if (isEmeraldPro) dp(150f) else dp(126f)
        val bannerRect = RectF(outerPad, y, w - outerPad, y + bannerH)
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = if (isEmeraldPro) {
                LinearGradient(
                    bannerRect.left,
                    bannerRect.top,
                    bannerRect.right,
                    bannerRect.bottom,
                    intArrayOf(0xFF033B2A.toInt(), 0xFF0B7A4B.toInt(), 0xFF045D3B.toInt()),
                    floatArrayOf(0f, 0.52f, 1f),
                    Shader.TileMode.CLAMP
                )
            } else {
                LinearGradient(
                    bannerRect.left,
                    bannerRect.top,
                    bannerRect.right,
                    bannerRect.bottom,
                    primaryColor,
                    secondaryColor,
                    Shader.TileMode.CLAMP
                )
            }
        }
        canvas.drawRoundRect(bannerRect, dp(18f), dp(18f), bannerPaint)

        // Subtle gold/platinum status ribbon pill on top-right of banner
        val badgeText = when {
            due <= 0.001 -> if (isBengali) "পরিশোধিত" else "PAID"
            isBengali -> "বিল রশিদ"
            else -> "INVOICE"
        }
        val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isRoyalGold || isEmeraldPro) 0xFF1F2937.toInt() else Color.WHITE
            textSize = dp(11f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val badgeW = badgeTextPaint.measureText(badgeText) + dp(20f)
        val badgeH = dp(23f)
        val badgeRect = RectF(
            bannerRect.right - innerPad - badgeW,
            bannerRect.top + dp(14f),
            bannerRect.right - innerPad,
            bannerRect.top + dp(14f) + badgeH
        )
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isRoyalGold || isEmeraldPro) 0xFFFDE68A.toInt() else 0x33FFFFFF
        }
        canvas.drawRoundRect(badgeRect, dp(12f), dp(12f), badgeBgPaint)
        canvas.drawText(
            badgeText,
            badgeRect.left + dp(10f),
            badgeRect.top + dp(16f),
            badgeTextPaint
        )

        val companyName = settings.company.ifBlank { "আশা মিনি শ্যালো" }
        val contactUpi = listOfNotNull(
            settings.contact.takeIf { it.isNotBlank() }?.let { "Ph: $it" },
            settings.upi.takeIf { it.isNotBlank() }?.let { "UPI: $it" }
        ).joinToString("  •  ")

        if (isEmeraldPro) {
            // Ornamental gold inner crest frame & luminous radial center sheen
            canvas.save()
            val clipBanner = Path().apply { addRoundRect(bannerRect, dp(18f), dp(18f), Path.Direction.CW) }
            canvas.clipPath(clipBanner)
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 18
            }
            canvas.drawCircle(bannerRect.centerX(), bannerRect.top + dp(40f), dp(135f), glowPaint)
            canvas.restore()

            val goldLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFBBF24.toInt()
                alpha = 195
                strokeWidth = dp(1.6f)
            }
            canvas.drawRoundRect(
                RectF(bannerRect.left + dp(7f), bannerRect.top + dp(7f), bannerRect.right - dp(7f), bannerRect.bottom - dp(7f)),
                dp(13f),
                dp(13f),
                Paint(goldLinePaint).apply { style = Paint.Style.STROKE; strokeWidth = dp(1.1f); alpha = 115 }
            )

            // Top-left subtle gold tag
            val crestTagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFDE68A.toInt()
                textSize = dp(10.5f)
                letterSpacing = 0.14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(
                if (isBengali) "অফিশিয়াল বিল" else "OFFICIAL CREST",
                bannerRect.left + innerPad,
                bannerRect.top + dp(30f),
                crestTagPaint
            )

            // Centered Hero Company Name in Premium Mid-Position
            val centeredTitlePaint = TextPaint(headerTitlePaint).apply {
                textSize = dp(31f)
            }
            val titleW = centeredTitlePaint.measureText(companyName)
            canvas.drawText(companyName, bannerRect.centerX() - titleW / 2f, bannerRect.top + dp(74f), centeredTitlePaint)

            // Short centered golden accent dash right beneath the company name
            canvas.drawLine(
                bannerRect.centerX() - dp(36f),
                bannerRect.top + dp(86f),
                bannerRect.centerX() + dp(36f),
                bannerRect.top + dp(86f),
                goldLinePaint
            )

            // Centered Address & Contact/UPI lines
            if (settings.address.isNotBlank()) {
                val addrStr = settings.address.take(54)
                val aw = headerSubPaint.measureText(addrStr)
                canvas.drawText(addrStr, bannerRect.centerX() - aw / 2f, bannerRect.top + dp(108f), headerSubPaint)
            }
            if (contactUpi.isNotBlank()) {
                val cw = headerSubPaint.measureText(contactUpi)
                canvas.drawText(contactUpi, bannerRect.centerX() - cw / 2f, bannerRect.top + dp(131f), headerSubPaint)
            }
        } else {
            canvas.drawText(companyName, bannerRect.left + innerPad, bannerRect.top + dp(42f), headerTitlePaint)
            if (settings.address.isNotBlank()) {
                canvas.drawText(settings.address.take(54), bannerRect.left + innerPad, bannerRect.top + dp(72f), headerSubPaint)
            }
            if (contactUpi.isNotBlank()) {
                canvas.drawText(contactUpi, bannerRect.left + innerPad, bannerRect.top + dp(100f), headerSubPaint)
            }
        }
        y += bannerH + dp(12f)

        // Compact 1D Customer ID Barcode right after header (well separated from Payment QR!)
        if (customerBarcodeBitmap != null && customerBarcodeId != null) {
            y = drawCompactTopCustomerBarcodeBar(
                canvas = canvas,
                left = outerPad,
                right = w - outerPad,
                topY = y,
                dp = { dp(it) },
                barcodeBitmap = customerBarcodeBitmap,
                customerId = customerBarcodeId,
                primaryColor = primaryColor,
                mutedColor = mutedColor,
                borderPaint = boxStrokePaint
            )
        }

        // 2. Twin Bill No & Date Cards
        val gap = dp(12f)
        val halfW = (w - outerPad * 2f - gap) / 2f
        val pillH = dp(70f)
        val leftPill = RectF(outerPad, y, outerPad + halfW, y + pillH)
        val rightPill = RectF(outerPad + halfW + gap, y, w - outerPad, y + pillH)
        canvas.drawRoundRect(leftPill, dp(14f), dp(14f), boxStrokePaint)
        canvas.drawRoundRect(rightPill, dp(14f), dp(14f), boxStrokePaint)

        canvas.drawText(if (isBengali) "বিল নং (Bill No)" else "Bill No", leftPill.left + dp(14f), leftPill.top + dp(24f), labelPaint)
        canvas.drawText(bill.billNo.ifBlank { "ASHA" }, leftPill.left + dp(14f), leftPill.top + dp(52f), valPaint)
        canvas.drawText(if (isBengali) "তারিখ (Date)" else "Date", rightPill.left + dp(14f), rightPill.top + dp(24f), labelPaint)
        canvas.drawText(bill.date, rightPill.left + dp(14f), rightPill.top + dp(52f), valPaint)
        y += pillH + dp(14f)

        fun drawCardKeyVal(cardLeft: Float, cardRight: Float, rowY: Float, k: String, v: String, drawLine: Boolean = true, vColor: Int = inkColor, boldKey: Boolean = false) {
            val kp = if (boldKey) valPaint else labelPaint
            val vp = TextPaint(valPaint).apply { color = vColor }
            canvas.drawText(k, cardLeft + dp(16f), rowY, kp)
            val vw = vp.measureText(v)
            canvas.drawText(v, cardRight - dp(16f) - vw, rowY, vp)
            if (drawLine) {
                canvas.drawLine(cardLeft + dp(16f), rowY + dp(12f), cardRight - dp(16f), rowY + dp(12f), hairlinePaint)
            }
        }

        fun drawQrCardBlock(atY: Float): Float {
            if (qrBitmap == null) return atY
            val qrH = dp(236f)
            val qrRect = RectF(outerPad, atY, w - outerPad, atY + qrH)
            canvas.drawRoundRect(qrRect, dp(16f), dp(16f), boxStrokePaint)

            val qrTitle = if (isBengali) "কিউআর স্ক্যান করে পে করুন (UPI)" else "PAYMENT QR"
            val capW = sectionCapPaint.measureText(qrTitle)
            canvas.drawText(qrTitle, qrRect.centerX() - capW / 2f, qrRect.top + dp(26f), sectionCapPaint)

            val qrSize = dp(146f).roundToInt()
            val qrLeft = (qrRect.centerX() - qrSize / 2f).roundToInt()
            val qrTop = (qrRect.top + dp(38f)).roundToInt()
            canvas.drawBitmap(qrBitmap, null, Rect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), null)

            val scanLabel = if (due > 0.001) "Scan & Pay ${formatInr(due)}" else "Scan & Pay ${formatInr(bill.allTotal)}"
            val payTitlePaint = TextPaint(valPaint).apply {
                textSize = dp(18f)
                color = inkColor
            }
            val sw = payTitlePaint.measureText(scanLabel)
            canvas.drawText(scanLabel, qrRect.centerX() - sw / 2f, qrRect.bottom - dp(18f), payTitlePaint)
            return atY + qrH + dp(14f)
        }

        if (isQrHero) {
            y = drawQrCardBlock(y)
        }

        // 3. CUSTOMER DETAILS Card
        val custCardH = dp(184f)
        val custRect = RectF(outerPad, y, w - outerPad, y + custCardH)
        canvas.drawRoundRect(custRect, dp(16f), dp(16f), boxStrokePaint)
        canvas.drawText(if (isBengali) "গ্রাহকের বিবরণ (CUSTOMER)" else "CUSTOMER DETAILS", custRect.left + dp(16f), custRect.top + dp(26f), sectionCapPaint)

        var cy = custRect.top + dp(56f)
        drawCardKeyVal(custRect.left, custRect.right, cy, if (isBengali) "নাম" else "Name", bill.customerName.take(28))
        cy += dp(36f)
        drawCardKeyVal(custRect.left, custRect.right, cy, if (isBengali) "ফোন" else "Phone", bill.phone.ifBlank { "—" })
        cy += dp(36f)
        drawCardKeyVal(custRect.left, custRect.right, cy, if (isBengali) "গ্রাম" else "Village", bill.village.ifBlank { "—" })
        cy += dp(36f)
        drawCardKeyVal(custRect.left, custRect.right, cy, if (isBengali) "ঠিকানা" else "Address", bill.address.take(30).ifBlank { "—" }, drawLine = false)
        y += custCardH + dp(14f)

        // 4. CHARGE DETAILS Card
        val chargeH = if (hasLand) dp(184f) else dp(104f)
        val chargeRect = RectF(outerPad, y, w - outerPad, y + chargeH)
        canvas.drawRoundRect(chargeRect, dp(16f), dp(16f), boxStrokePaint)
        canvas.drawText(if (isBengali) "জমি ও বিলের বিবরণ" else "CHARGE DETAILS", chargeRect.left + dp(16f), chargeRect.top + dp(26f), sectionCapPaint)

        var chy = chargeRect.top + dp(56f)
        if (hasLand) {
            drawCardKeyVal(chargeRect.left, chargeRect.right, chy, if (isBengali) "সিজন" else "Season", bill.season.ifBlank { "—" })
            chy += dp(36f)
            drawCardKeyVal(
                chargeRect.left,
                chargeRect.right,
                chy,
                if (isBengali) "জমির পরিমাণ" else "Land",
                "${String.format(Locale.US, "%.2f", bill.bigha)} Bigha (${formatQty(bill.landAmount)} ${bill.unit})"
            )
            chy += dp(36f)
            drawCardKeyVal(
                chargeRect.left,
                chargeRect.right,
                chy,
                if (isBengali) "রেট" else "Rate",
                if (isBengali) "${formatInr(bill.rate)}/বিঘা" else "${formatInr(bill.rate)}/Bigha"
            )
            chy += dp(36f)
            drawCardKeyVal(chargeRect.left, chargeRect.right, chy, if (isBengali) "বর্তমান বিল" else "Current Bill", formatInr(bill.current), drawLine = false)
        } else {
            drawCardKeyVal(chargeRect.left, chargeRect.right, chy, if (isBengali) "বিলের ধরন" else "Bill Type", if (isBengali) "জের বাকি হিসাব" else "Previous Due Recovery", drawLine = false)
        }
        y += chargeH + dp(14f)

        // 5. PAYMENT SUMMARY Mint/Accent Card
        val paySumH = dp(if (hasSettlement) 218f else 186f)
        val paySumRect = RectF(outerPad, y, w - outerPad, y + paySumH)
        val accentFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentBg }
        val accentBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = primaryColor
            alpha = 65
            strokeWidth = dp(1.4f)
        }
        canvas.drawRoundRect(paySumRect, dp(16f), dp(16f), accentFillPaint)
        canvas.drawRoundRect(paySumRect, dp(16f), dp(16f), accentBorderPaint)
        canvas.drawText(if (isBengali) "পেমেন্ট সারসংক্ষেপ" else "PAYMENT SUMMARY", paySumRect.left + dp(16f), paySumRect.top + dp(26f), sectionCapPaint)

        var py = paySumRect.top + dp(56f)
        drawCardKeyVal(paySumRect.left, paySumRect.right, py, if (isBengali) "জের বাকি" else "Previous Due", formatInr(bill.previousDue), drawLine = false)
        py += dp(32f)
        drawCardKeyVal(paySumRect.left, paySumRect.right, py, if (isBengali) "মোট মূল্য" else "Total Amount", formatInr(bill.allTotal), drawLine = false)
        py += dp(32f)
        drawCardKeyVal(paySumRect.left, paySumRect.right, py, if (isBengali) "জমা" else "Paid", formatInr(actualCashPaid), drawLine = false, vColor = 0xFF15803D.toInt())
        if (hasSettlement) {
            py += dp(32f)
            drawCardKeyVal(paySumRect.left, paySumRect.right, py, if (isBengali) "ছাড় / সেটেল" else "Settled / Discount", formatInr(settledWaived), drawLine = false, vColor = 0xFFB45309.toInt())
        }
        py += dp(38f)

        val dueTitlePaint = TextPaint(valPaint).apply { textSize = dp(20f) }
        val dueValPaint = TextPaint(valPaint).apply {
            textSize = dp(22f)
            color = dueColor
        }
        val dueLabelText = if (isBengali) "মোট বাকি" else "Payable Due"
        canvas.drawText(dueLabelText, paySumRect.left + dp(16f), py, dueTitlePaint)
        val dvw = dueValPaint.measureText(formatInr(due))
        canvas.drawText(formatInr(due), paySumRect.right - dp(16f) - dvw, py, dueValPaint)
        y += paySumH + dp(14f)

        // Optional Recent Payments mini list
        if (paymentRowsCount > 0) {
            val rpH = dp(48f) + paymentRowsCount * dp(28f)
            val rpRect = RectF(outerPad, y, w - outerPad, y + rpH)
            canvas.drawRoundRect(rpRect, dp(14f), dp(14f), boxStrokePaint)
            canvas.drawText("RECENT PAYMENTS", rpRect.left + dp(16f), rpRect.top + dp(24f), sectionCapPaint)
            var ry = rpRect.top + dp(52f)
            payments.take(paymentRowsCount).forEachIndexed { idx, p ->
                val isSetRow = PaymentMode.fromLabel(p.mode) == PaymentMode.SETTLEMENT
                val modeTxt = if (isSetRow) (if (isBengali) "সেটেল ছাড়" else "Settled / Discount") else p.mode
                drawCardKeyVal(
                    rpRect.left,
                    rpRect.right,
                    ry,
                    "${p.date} · $modeTxt",
                    if (isSetRow) formatInr(p.amount) else "+${formatInr(p.amount)}",
                    drawLine = idx < paymentRowsCount - 1,
                    vColor = if (isSetRow) 0xFFB45309.toInt() else 0xFF15803D.toInt()
                )
                ry += dp(28f)
            }
            y += rpH + dp(14f)
        }

        if (hasNote) {
            canvas.drawText("Note: ${bill.note.take(58)}", outerPad + dp(6f), y + dp(18f), labelPaint)
            y += dp(38f)
        }

        if (!isQrHero) {
            y = drawQrCardBlock(y)
        }

        // 6. Bengali Thank You / Reminder Footer + Handwritten Owner Signature with Date
        drawHandwrittenOwnerSignatureSection(
            canvas = canvas,
            left = outerPad,
            right = w - outerPad,
            topY = y,
            dp = { dp(it) },
            settings = settings,
            billDate = bill.date,
            primaryColor = primaryColor,
            mutedColor = mutedColor
        )

        return bitmap
    }

    /**
     * Renders Classic Official & Monochrome Thermal POS receipts + Handwritten Owner Signature & Date.
     */
    private fun renderClassicOrThermalStyle(
        settings: BusinessSettings,
        bill: Bill,
        payments: List<Payment>,
        paid: Double,
        due: Double,
        design: BillDesignModel,
        size: BillPrintSize,
        qrMode: String,
        customQrPath: String,
        isBengali: Boolean
    ): Bitmap {
        val w = size.widthPx
        val scale = w / 680f
        fun dp(v: Float) = v * scale
        val pad = dp(26f)
        val isThermal = design == BillDesignModel.THERMAL_COMPACT

        val qrBitmap = resolveQrBitmap(settings, due, qrMode, customQrPath)
        val customerBarcodeId = compactCustomerBarcodeId(bill.customerId).takeIf { it.isNotBlank() }
        val customerBarcodeBitmap = customerBarcodeId?.let {
            generateCode128BarcodeBitmap(
                it,
                widthPx = dp(440f).roundToInt().coerceAtLeast(300),
                heightPx = dp(60f).roundToInt().coerceAtLeast(54)
            )
        }
        val hasLand = bill.landAmount > 0.001 || bill.rate > 0.001
        val hasNote = bill.note.isNotBlank()
        val (actualCashPaid, settledWaived) = splitPaidAndSettled(payments, paid)
        val hasSettlement = settledWaived > 0.001
        val paymentRowsCount = minOf(payments.size, 5)

        var estH = dp(220f)
        estH += dp(140f)
        estH += if (hasLand) dp(185f) else dp(92f)
        estH += dp(if (hasSettlement) 186f else 155f)
        if (paymentRowsCount > 0) estH += dp(64f) + paymentRowsCount * dp(30f)
        if (qrBitmap != null) estH += dp(175f)
        if (customerBarcodeBitmap != null) estH += dp(92f)
        if (hasNote) estH += dp(48f)
        estH += dp(115f) // Handwritten owner signature + date
        if (size.isFullA4) {
            estH = max(estH, w * 1.38f)
        }

        val h = estH.roundToInt().coerceAtLeast(660)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val primaryColor = design.primaryColorInt
        val accentBg = design.accentBgInt
        val inkColor = 0xFF13221D.toInt()
        val mutedColor = 0xFF5D6F68.toInt()
        val hairlineColor = 0xFFDDE7E2.toInt()
        val dueColor = if (due > 0.001) 0xFFC44C4C.toInt() else 0xFF14764E.toInt()

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isThermal) Color.BLACK else Color.WHITE
            textSize = dp(26f)
            typeface = Typeface.create(if (isThermal) Typeface.MONOSPACE else Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = dp(18f)
            typeface = Typeface.create(if (isThermal) Typeface.MONOSPACE else Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = dp(16.5f)
            typeface = if (isThermal) Typeface.MONOSPACE else Typeface.DEFAULT
        }
        val mutedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = dp(14.5f)
            typeface = if (isThermal) Typeface.MONOSPACE else Typeface.DEFAULT
        }
        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = dp(13.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isThermal) 0xFF333333.toInt() else hairlineColor
            strokeWidth = dp(1.5f)
            if (isThermal) pathEffect = DashPathEffect(floatArrayOf(dp(8f), dp(6f)), 0f)
        }

        var y = pad

        if (!isThermal) {
            val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = primaryColor
                strokeWidth = dp(3f)
            }
            canvas.drawRoundRect(RectF(dp(10f), dp(10f), w - dp(10f), h - dp(10f)), dp(16f), dp(16f), framePaint)
        }

        val headerH = dp(142f)
        val headerRect = RectF(pad, y, w - pad, y + headerH)
        if (isThermal) {
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.BLACK
                strokeWidth = dp(2f)
            }
            canvas.drawRect(headerRect, stroke)
        } else {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor }
            canvas.drawRoundRect(headerRect, dp(12f), dp(12f), bgPaint)
        }

        val companyName = settings.company.ifBlank { "আশা মিনি শ্যালো" }
        canvas.drawText(companyName, headerRect.left + dp(16f), headerRect.top + dp(40f), titlePaint)
        val headerSubPaint = TextPaint(mutedPaint).apply {
            color = if (isThermal) Color.DKGRAY else 0xFFD2EBE0.toInt()
            textSize = dp(14f)
        }
        val ownerLine = listOfNotNull(
            settings.owner.takeIf { it.isNotBlank() }?.let { "Prop: $it" },
            settings.contact.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        canvas.drawText(ownerLine, headerRect.left + dp(16f), headerRect.top + dp(66f), headerSubPaint)
        canvas.drawText(settings.address.take(56), headerRect.left + dp(16f), headerRect.top + dp(90f), headerSubPaint)

        val tagText = "${bill.billNo}  •  ${bill.date}"
        val tagBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isThermal) 0xFFE5E7EB.toInt() else 0x33FFFFFF
        }
        val tagRect = RectF(headerRect.left + dp(16f), headerRect.top + dp(102f), headerRect.right - dp(16f), headerRect.bottom - dp(10f))
        canvas.drawRoundRect(tagRect, dp(8f), dp(8f), tagBg)
        val tagPaint = TextPaint(bodyBoldPaint).apply {
            color = if (isThermal) Color.BLACK else Color.WHITE
            textSize = dp(14.5f)
        }
        canvas.drawText(tagText, tagRect.left + dp(10f), tagRect.top + dp(20f), tagPaint)

        y += headerH + dp(12f)

        // Compact 1D Customer ID Barcode right after header (separate from Payment QR at bottom)
        if (customerBarcodeBitmap != null && customerBarcodeId != null) {
            val topBcBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = if (isThermal) Color.BLACK else hairlineColor
                strokeWidth = dp(1.2f)
            }
            y = drawCompactTopCustomerBarcodeBar(
                canvas = canvas,
                left = pad,
                right = w - pad,
                topY = y,
                dp = { dp(it) },
                barcodeBitmap = customerBarcodeBitmap,
                customerId = customerBarcodeId,
                primaryColor = primaryColor,
                mutedColor = mutedColor,
                borderPaint = topBcBorder
            )
        }

        // Customer Box
        val custBoxH = dp(104f)
        val custRect = RectF(pad, y, w - pad, y + custBoxH)
        val softBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isThermal) 0xFFFAFAFA.toInt() else accentBg
        }
        canvas.drawRoundRect(custRect, dp(12f), dp(12f), softBoxPaint)
        canvas.drawText("BILL TO CUSTOMER", custRect.left + dp(14f), custRect.top + dp(22f), badgePaint)
        val custTitlePaint = TextPaint(bodyBoldPaint).apply { textSize = dp(19f) }
        canvas.drawText(bill.customerName, custRect.left + dp(14f), custRect.top + dp(50f), custTitlePaint)
        val custMeta = listOfNotNull(
            bill.phone.takeIf { it.isNotBlank() }?.let { "Ph: $it" },
            bill.village.takeIf { it.isNotBlank() }?.let { "Vill: $it" }
        ).joinToString(" | ").ifBlank { "Registered Customer" }
        canvas.drawText(custMeta, custRect.left + dp(14f), custRect.top + dp(76f), mutedPaint)

        y += custBoxH + dp(16f)

        canvas.drawText("PARTICULARS", pad, y, badgePaint)
        y += dp(10f)
        canvas.drawLine(pad, y, w - pad, y, linePaint)
        y += dp(24f)

        fun drawKV(label: String, value: String) {
            canvas.drawText(label, pad, y, bodyPaint)
            val vw = bodyBoldPaint.measureText(value)
            canvas.drawText(value, w - pad - vw, y, bodyBoldPaint)
            y += dp(12f)
            canvas.drawLine(pad, y, w - pad, y, linePaint)
            y += dp(24f)
        }

        if (hasLand) {
            if (bill.season.isNotBlank()) {
                drawKV(if (isBengali) "সিজন" else "Season", bill.season)
            }
            drawKV(
                if (isBengali) "জমির পরিমাণ" else "Land (${bill.unit.uppercase()})",
                "${formatQty(bill.landAmount)} ${bill.unit} (${String.format(Locale.US, "%.2f", bill.bigha)} Bigha)"
            )
            drawKV(if (isBengali) "রেট" else "Rate / Bigha", formatInr(bill.rate))
            drawKV(if (isBengali) "বর্তমান বিল" else "Current Bill", formatInr(bill.current))
        } else {
            drawKV(if (isBengali) "বিলের ধরন" else "Bill Type", if (isBengali) "জের বাকি হিসাব" else "Previous Due Recovery")
        }
        drawKV(if (isBengali) "জের বাকি" else "Previous Due", formatInr(bill.previousDue))

        // Summary box
        val sumH = dp(if (hasSettlement) 156f else 124f)
        val sumRect = RectF(pad, y, w - pad, y + sumH)
        val sumFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (isThermal) 0xFFF3F4F6.toInt() else accentBg }
        canvas.drawRoundRect(sumRect, dp(12f), dp(12f), sumFill)
        var sy = sumRect.top + dp(28f)
        fun drawSumRow(lbl: String, valText: String, colorInt: Int, big: Boolean = false) {
            val lp = TextPaint(bodyBoldPaint).apply { textSize = if (big) dp(19f) else dp(16f) }
            val vp = TextPaint(bodyBoldPaint).apply {
                textSize = if (big) dp(20.5f) else dp(17f)
                color = colorInt
            }
            canvas.drawText(lbl, sumRect.left + dp(14f), sy, lp)
            val vw = vp.measureText(valText)
            canvas.drawText(valText, sumRect.right - dp(14f) - vw, sy, vp)
            sy += dp(30f)
        }
        drawSumRow(if (isBengali) "মোট মূল্য" else "Grand Total", formatInr(bill.allTotal), primaryColor, big = true)
        drawSumRow(if (isBengali) "জমা" else "Paid", formatInr(actualCashPaid), 0xFF14764E.toInt())
        if (hasSettlement) {
            drawSumRow(
                if (isBengali) "ছাড় / সেটেল" else "Settled / Discount",
                formatInr(settledWaived),
                if (isThermal) Color.BLACK else 0xFFB45309.toInt()
            )
        }
        drawSumRow(
            if (isBengali) "মোট বাকি" else if (due > 0.001) "Balance Due" else if (hasSettlement) "Settled Balance" else "Paid in Full",
            formatInr(due),
            dueColor,
            big = true
        )
        y += sumH + dp(16f)

        if (paymentRowsCount > 0) {
            canvas.drawText("RECENT PAYMENTS", pad, y, badgePaint)
            y += dp(10f)
            canvas.drawLine(pad, y, w - pad, y, linePaint)
            y += dp(22f)
            payments.take(paymentRowsCount).forEach { p ->
                val isSetRow = PaymentMode.fromLabel(p.mode) == PaymentMode.SETTLEMENT
                val modeTxt = if (isSetRow) (if (isBengali) "সেটেল ছাড়" else "Settled") else p.mode
                canvas.drawText("${p.date} · $modeTxt", pad, y, mutedPaint)
                val rw = bodyBoldPaint.measureText(formatInr(p.amount))
                canvas.drawText(formatInr(p.amount), w - pad - rw, y, bodyBoldPaint)
                y += dp(24f)
            }
            y += dp(6f)
        }

        if (qrBitmap != null) {
            val qrBoxH = dp(148f)
            val qrRect = RectF(pad, y, w - pad, y + qrBoxH)
            val qrBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = hairlineColor
                strokeWidth = dp(1.5f)
            }
            canvas.drawRoundRect(qrRect, dp(12f), dp(12f), qrBorder)
            val qrSize = dp(116f).roundToInt()
            val ql = (qrRect.left + dp(14f)).roundToInt()
            val qt = (qrRect.top + dp(16f)).roundToInt()
            canvas.drawBitmap(qrBitmap, null, Rect(ql, qt, ql + qrSize, qt + qrSize), null)
            val tx = ql + qrSize + dp(14f)
            canvas.drawText("SCAN TO PAY (UPI)", tx, qrRect.top + dp(38f), badgePaint)
            canvas.drawText(settings.payee.ifBlank { settings.company }, tx, qrRect.top + dp(66f), bodyBoldPaint)
            if (settings.upi.isNotBlank()) {
                canvas.drawText(settings.upi, tx, qrRect.top + dp(92f), mutedPaint)
            }
            if (due > 0.001) {
                val ap = TextPaint(bodyBoldPaint).apply { color = dueColor }
                canvas.drawText("Payable: ${formatInr(due)}", tx, qrRect.top + dp(118f), ap)
            }
            y += qrBoxH + dp(14f)
        }

        if (hasNote) {
            canvas.drawText("Note: ${bill.note.take(58)}", pad, y, mutedPaint)
            y += dp(28f)
        }

        canvas.drawLine(pad, y, w - pad, y, linePaint)
        y += dp(8f)

        drawHandwrittenOwnerSignatureSection(
            canvas = canvas,
            left = pad,
            right = w - pad,
            topY = y,
            dp = { dp(it) },
            settings = settings,
            billDate = bill.date,
            primaryColor = primaryColor,
            mutedColor = mutedColor
        )

        return bitmap
    }

    /**
     * Draws a realistic fountain-pen handwritten proprietor signature with date on every bill image.
     */
    private fun drawHandwrittenOwnerSignatureSection(
        canvas: Canvas,
        left: Float,
        right: Float,
        topY: Float,
        dp: (Float) -> Float,
        settings: BusinessSettings,
        billDate: String,
        primaryColor: Int,
        mutedColor: Int
    ) {
        val ownerName = settings.owner.ifBlank { "SK EKRAMUL HAQUE" }
        val leftNotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = dp(12.5f)
        }
        val ownerMetaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF334155.toInt()
            textSize = dp(13f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Left side: Bengali polite footer & proprietor credit
        canvas.drawText("আপনাকে পুরো বিল শীঘ্রই পরিশোধের জন্য বলা হচ্ছে", left + dp(4f), topY + dp(26f), leftNotePaint)
        canvas.drawText("Owner: $ownerName · Thank you", left + dp(4f), topY + dp(48f), ownerMetaPaint)
        canvas.drawText("Signed Date: $billDate", left + dp(4f), topY + dp(68f), leftNotePaint)

        // Right side: Handwritten Pen Signature Stroke + Cursive Serif-Italic Name + Signature Line
        val sigRight = right - dp(6f)
        val sigCenterX = sigRight - dp(105f)
        val sigBaseY = topY + dp(44f)

        val penPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xFF1E3A8A.toInt() // Classic fountain pen ink navy
            strokeWidth = dp(2.2f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Expressive multi-loop handwritten signature path
        val sigPath = Path().apply {
            moveTo(sigCenterX - dp(68f), sigBaseY - dp(6f))
            cubicTo(
                sigCenterX - dp(54f), sigBaseY - dp(32f),
                sigCenterX - dp(34f), sigBaseY - dp(28f),
                sigCenterX - dp(40f), sigBaseY - dp(4f)
            )
            cubicTo(
                sigCenterX - dp(46f), sigBaseY + dp(10f),
                sigCenterX - dp(24f), sigBaseY - dp(22f),
                sigCenterX - dp(12f), sigBaseY - dp(6f)
            )
            cubicTo(
                sigCenterX - dp(2f), sigBaseY - dp(18f),
                sigCenterX + dp(14f), sigBaseY - dp(20f),
                sigCenterX + dp(18f), sigBaseY - dp(5f)
            )
            cubicTo(
                sigCenterX + dp(26f), sigBaseY + dp(4f),
                sigCenterX + dp(44f), sigBaseY - dp(22f),
                sigCenterX + dp(62f), sigBaseY - dp(10f)
            )
            // Classic underline flourish
            moveTo(sigCenterX - dp(64f), sigBaseY + dp(4f))
            quadTo(sigCenterX + dp(4f), sigBaseY - dp(1f), sigCenterX + dp(72f), sigBaseY + dp(2f))
        }
        canvas.drawPath(sigPath, penPaint)

        // Stylized handwritten-serif name tucked right over baseline
        val sigTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1E3A8A.toInt()
            textSize = dp(13.5f)
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        }
        val shortSig = ownerName.split(" ").take(2).joinToString(" ")
        val sw = sigTextPaint.measureText(shortSig)
        canvas.drawText(shortSig, sigCenterX - sw / 2f, sigBaseY - dp(16f), sigTextPaint)

        // Signature rule & label + date
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            strokeWidth = dp(1f)
        }
        canvas.drawLine(sigCenterX - dp(82f), sigBaseY + dp(11f), sigRight, sigBaseY + dp(11f), rulePaint)

        val sigCaptionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = dp(11.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cap = "Proprietor Sign · $billDate"
        val cw = sigCaptionPaint.measureText(cap)
        canvas.drawText(cap, sigRight - cw, sigBaseY + dp(26f), sigCaptionPaint)
    }

    /**
     * Draws the bill onto a print/PDF page Canvas honoring 1/4th A4 (top-left filled, 3/4 blank),
     * 4-in-1 A4 Grid (2x2 copies), full A4, or exact thermal roll proportions.
     */
    fun drawBillOnPrintPage(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        billBitmap: Bitmap,
        size: BillPrintSize
    ) {
        canvas.drawColor(Color.WHITE)
        val filterPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        when {
            size.isHalfA4 -> {
                // Exactly 1/2 of A4 (top half filled), bottom half left blank
                val margin = (pageWidth * 0.03f).roundToInt()
                val halfH = pageHeight / 2
                val availW = (pageWidth - margin * 2).coerceAtLeast(1).toFloat()
                val availH = (halfH - margin * 2).coerceAtLeast(1).toFloat()
                val scale = min(availW / billBitmap.width.toFloat(), availH / billBitmap.height.toFloat())
                val drawW = (billBitmap.width * scale).roundToInt()
                val drawH = (billBitmap.height * scale).roundToInt()
                val left = margin + ((availW - drawW) / 2f).roundToInt()
                val top = margin + ((availH - drawH) / 2f).roundToInt()
                canvas.drawBitmap(billBitmap, null, Rect(left, top, left + drawW, top + drawH), filterPaint)

                // Light dashed horizontal fold / cut line separating top 1/2 from blank bottom half
                val cropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFCBD5E1.toInt()
                    strokeWidth = 1.2f
                    pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
                }
                canvas.drawLine(0f, halfH.toFloat(), pageWidth.toFloat(), halfH.toFloat(), cropPaint)
            }

            size.isQuarterA4 -> {
                // Exactly 1/4th of A4 (top-left quadrant), remaining 3/4ths left blank
                val margin = (pageWidth * 0.025f).roundToInt()
                val quadW = pageWidth / 2
                val quadH = pageHeight / 2
                val availW = (quadW - margin * 2).coerceAtLeast(1).toFloat()
                val availH = (quadH - margin * 2).coerceAtLeast(1).toFloat()
                val scale = min(availW / billBitmap.width.toFloat(), availH / billBitmap.height.toFloat())
                val drawW = (billBitmap.width * scale).roundToInt()
                val drawH = (billBitmap.height * scale).roundToInt()
                val left = margin + ((availW - drawW) / 2f).roundToInt()
                val top = margin
                canvas.drawBitmap(billBitmap, null, Rect(left, top, left + drawW, top + drawH), filterPaint)

                // Light dashed crop guide separating the 1/4th quadrant so paper can be cut cleanly
                val cropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFCBD5E1.toInt()
                    strokeWidth = 1f
                    pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
                }
                canvas.drawLine(quadW.toFloat(), 0f, quadW.toFloat(), quadH.toFloat(), cropPaint)
                canvas.drawLine(0f, quadH.toFloat(), quadW.toFloat(), quadH.toFloat(), cropPaint)
            }

            size.isFourUpA4 -> {
                // 4 bills divided across one full A4 page (2x2 grid)
                val quadW = pageWidth / 2
                val quadH = pageHeight / 2
                val margin = (pageWidth * 0.022f).roundToInt()
                val availW = (quadW - margin * 2).coerceAtLeast(1).toFloat()
                val availH = (quadH - margin * 2).coerceAtLeast(1).toFloat()
                val scale = min(availW / billBitmap.width.toFloat(), availH / billBitmap.height.toFloat())
                val drawW = (billBitmap.width * scale).roundToInt()
                val drawH = (billBitmap.height * scale).roundToInt()

                for (row in 0..1) {
                    for (col in 0..1) {
                        val cellLeft = col * quadW
                        val cellTop = row * quadH
                        val left = cellLeft + margin + ((availW - drawW) / 2f).roundToInt()
                        val top = cellTop + margin
                        canvas.drawBitmap(billBitmap, null, Rect(left, top, left + drawW, top + drawH), filterPaint)
                    }
                }

                // Subtle cut grid lines dividing the A4 page into 4 quarters
                val cropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFCBD5E1.toInt()
                    strokeWidth = 1f
                    pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
                }
                canvas.drawLine(quadW.toFloat(), 0f, quadW.toFloat(), pageHeight.toFloat(), cropPaint)
                canvas.drawLine(0f, quadH.toFloat(), pageWidth.toFloat(), quadH.toFloat(), cropPaint)
            }

            else -> {
                val availW = pageWidth.coerceAtLeast(1).toFloat()
                val availH = pageHeight.coerceAtLeast(1).toFloat()
                val scale = min(availW / billBitmap.width.toFloat(), availH / billBitmap.height.toFloat())
                val drawW = (billBitmap.width * scale).roundToInt()
                val drawH = (billBitmap.height * scale).roundToInt()
                val left = ((availW - drawW) / 2f).roundToInt()
                canvas.drawBitmap(billBitmap, null, Rect(left, 0, left + drawW, drawH), filterPaint)
            }
        }
    }

    fun writeBillPdfToStream(
        outputStream: OutputStream,
        settings: BusinessSettings,
        bill: Bill,
        payments: List<Payment>,
        paid: Double,
        due: Double,
        design: BillDesignModel,
        size: BillPrintSize,
        qrMode: String,
        customQrPath: String,
        bengaliLabels: Boolean = false
    ) {
        val billBitmap = renderBillBitmap(
            settings = settings,
            bill = bill,
            payments = payments,
            paid = paid,
            due = due,
            design = design,
            size = size,
            qrMode = qrMode,
            customQrPath = customQrPath,
            bengaliLabels = bengaliLabels
        )
        val pdfWidth = size.pdfWidthPt
        val pdfHeight = when {
            size.isHalfA4 || size.isQuarterA4 || size.isFourUpA4 || size.isFullA4 -> 842
            else -> ((billBitmap.height.toFloat() / billBitmap.width.toFloat()) * pdfWidth)
                .roundToInt()
                .coerceAtLeast(300)
        }

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, 1).create()
        val page = pdf.startPage(pageInfo)
        drawBillOnPrintPage(
            canvas = page.canvas,
            pageWidth = pdfWidth,
            pageHeight = pdfHeight,
            billBitmap = billBitmap,
            size = size
        )
        pdf.finishPage(page)
        pdf.writeTo(outputStream)
        pdf.close()
    }

    fun writeBillPngToStream(outputStream: OutputStream, bitmap: Bitmap) {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    }

    /**
     * Zero-permission direct export to public Downloads/ASHA_Bills/ on Android 10+ (API 29+).
     * Returns Result.success(Uri) so users can download PDF invoices directly on mobile.
     */
    fun saveBillPdfToDownloads(
        context: Context,
        settings: BusinessSettings,
        bill: Bill,
        payments: List<Payment>,
        paid: Double,
        due: Double,
        design: BillDesignModel,
        size: BillPrintSize,
        qrMode: String,
        customQrPath: String,
        bengaliLabels: Boolean = false
    ): Result<Uri> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return Result.failure(IllegalStateException("Use system file picker on Android 9 or earlier"))
        }
        val safeBill = bill.billNo.ifBlank { "Bill" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val safeName = bill.customerName.ifBlank { "Customer" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "ASHA_${safeBill}_${safeName}_${bill.date}.pdf"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ASHA_Bills")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        var uri: Uri? = null
        return runCatching {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create Downloads entry")
            val targetUri = uri!!
            resolver.openOutputStream(targetUri)?.use { out ->
                writeBillPdfToStream(
                    outputStream = out,
                    settings = settings,
                    bill = bill,
                    payments = payments,
                    paid = paid,
                    due = due,
                    design = design,
                    size = size,
                    qrMode = qrMode,
                    customQrPath = customQrPath,
                    bengaliLabels = bengaliLabels
                )
            } ?: error("Failed to open download output stream")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(targetUri, values, null, null)
            targetUri
        }.onFailure {
            uri?.let { partial -> runCatching { resolver.delete(partial, null, null) } }
        }
    }

    /**
     * Zero-permission direct PNG bill image save into public Downloads/ASHA_Bills/ on Android 10+ (API 29+).
     */
    fun saveBillPngToDownloads(
        context: Context,
        bitmap: Bitmap,
        bill: Bill
    ): Result<Uri> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return Result.failure(IllegalStateException("Use system file picker on Android 9 or earlier"))
        }
        val safeBill = bill.billNo.ifBlank { "Bill" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val safeName = bill.customerName.ifBlank { "Customer" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "ASHA_${safeBill}_${safeName}_${bill.date}.png"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "image/png")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ASHA_Bills")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        var uri: Uri? = null
        return runCatching {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create Downloads entry")
            val targetUri = uri!!
            resolver.openOutputStream(targetUri)?.use { out ->
                writeBillPngToStream(out, bitmap)
            } ?: error("Failed to open download output stream")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(targetUri, values, null, null)
            targetUri
        }.onFailure {
            uri?.let { partial -> runCatching { resolver.delete(partial, null, null) } }
        }
    }

    private val shareScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun shareBillImageWithReminder(
        context: Context,
        settings: BusinessSettings,
        bill: Bill,
        payments: List<Payment>,
        paid: Double,
        due: Double,
        design: BillDesignModel,
        size: BillPrintSize,
        qrMode: String,
        customQrPath: String,
        bengaliLabels: Boolean = false
    ) {
        val appContext = context.applicationContext
        shareScope.launch {
            val uri = runCatching {
                withContext(Dispatchers.IO) {
                    renderCachedBillImageUri(
                        context = appContext,
                        settings = settings,
                        bill = bill,
                        payments = payments,
                        paid = paid,
                        due = due,
                        design = design,
                        size = size,
                        qrMode = qrMode,
                        customQrPath = customQrPath,
                        bengaliLabels = bengaliLabels
                    )
                }
            }.getOrElse {
                android.widget.Toast.makeText(appContext, "Could not prepare bill image", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            val reminderMsg = buildBillReminderMessage(settings, bill, due)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, reminderMsg)
                putExtra(Intent.EXTRA_SUBJECT, "Bill ${bill.billNo} - ${bill.customerName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Bill Image & Reminder").apply {
                if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(chooser) }
        }
    }

    fun sendBillImageReminderToWhatsApp(
        context: Context,
        settings: BusinessSettings,
        bill: Bill,
        payments: List<Payment>,
        paid: Double,
        due: Double,
        design: BillDesignModel = BillDesignModel.PREMIUM_GREEN,
        size: BillPrintSize = BillPrintSize.THERMAL_80,
        qrMode: String = "Dynamic amount QR",
        customQrPath: String = "",
        bengaliLabels: Boolean = false
    ) {
        val appContext = context.applicationContext
        shareScope.launch {
            val uri = runCatching {
                withContext(Dispatchers.IO) {
                    renderCachedBillImageUri(
                        context = appContext,
                        settings = settings,
                        bill = bill,
                        payments = payments,
                        paid = paid,
                        due = due,
                        design = design,
                        size = size,
                        qrMode = qrMode,
                        customQrPath = customQrPath,
                        bengaliLabels = bengaliLabels
                    )
                }
            }.getOrElse {
                android.widget.Toast.makeText(appContext, "Could not prepare bill image", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            val reminderMsg = buildBillReminderMessage(settings, bill, due)
            val waDigits = InvoicePrinter.normalizeWhatsAppDigits(bill.phone)
            val jid = if (waDigits.isNotBlank()) "$waDigits@s.whatsapp.net" else null

            val baseIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, reminderMsg)
                putExtra(Intent.EXTRA_SUBJECT, "Bill ${bill.billNo} - ${bill.customerName}")
                if (jid != null) {
                    putExtra("jid", jid)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pm = appContext.packageManager
            val waPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
            for (pkg in waPackages) {
                val targeted = Intent(baseIntent).setPackage(pkg)
                if (targeted.resolveActivity(pm) != null) {
                    val launched = runCatching { context.startActivity(targeted) }.isSuccess
                    if (launched) return@launch
                }
            }

            if (waDigits.isBlank()) {
                android.widget.Toast.makeText(
                    appContext,
                    "No phone number saved; choose app to share bill image",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, reminderMsg)
                putExtra(Intent.EXTRA_SUBJECT, "Bill ${bill.billNo} - ${bill.customerName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(chooserIntent, "Send Bill Image via WhatsApp").apply {
                if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(chooser) }
        }
    }

    private fun renderCachedBillImageUri(
        context: Context,
        settings: BusinessSettings,
        bill: Bill,
        payments: List<Payment>,
        paid: Double,
        due: Double,
        design: BillDesignModel,
        size: BillPrintSize,
        qrMode: String,
        customQrPath: String,
        bengaliLabels: Boolean = false
    ): Uri {
        val bitmap = renderBillBitmap(
            settings = settings,
            bill = bill,
            payments = payments,
            paid = paid,
            due = due,
            design = design,
            size = size,
            qrMode = qrMode,
            customQrPath = customQrPath,
            bengaliLabels = bengaliLabels
        )
        val cacheDir = File(context.cacheDir, "bills").apply { mkdirs() }
        val file = File(cacheDir, "ASHA_${bill.billNo.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun buildBillReminderMessage(
        settings: BusinessSettings,
        bill: Bill,
        due: Double
    ): String {
        val company = settings.company.ifBlank { "আশা মিনি শ্যালো" }
        return buildString {
            append("Hello ${bill.customerName}, ")
            append("this is a payment reminder from $company for Bill ${bill.billNo} (${bill.date}). ")
            if (due > 0.001) {
                append("Pending due amount: ${formatInr(due)}. ")
            } else {
                append("Bill Total: ${formatInr(bill.allTotal)} (Paid). ")
            }
            if (settings.upi.isNotBlank()) {
                append("Pay via UPI: ${settings.upi}. ")
            }
            append("Thank you!")
        }
    }

    private fun formatQty(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }
}
