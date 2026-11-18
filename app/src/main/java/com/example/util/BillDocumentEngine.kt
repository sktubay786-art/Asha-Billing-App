package com.example.util

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
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.Bill
import com.example.data.model.BusinessSettings
import com.example.data.model.Payment
import com.example.data.model.formatInr
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URLEncoder
import java.util.Locale
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
    ROSE_VELVET(
        label = "Ruby Velvet Executive",
        tagline = "Rich velvet plum-crimson card with warm rose summary",
        primaryColorInt = 0xFF9F1239.toInt(),
        secondaryColorInt = 0xFF4C0519.toInt(),
        accentBgInt = 0xFFFFF1F2.toInt()
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
    THERMAL_80("Thermal · 80 mm", 680, 227),
    THERMAL_58("Thermal · 58 mm", 500, 164),
    A4_HALF("A4 · 1/2 Page", 760, 595, isHalfA4 = true),
    A4_QUARTER("A4 · 1/4 Page", 680, 595, isQuarterA4 = true),
    A4_FOUR_UP("A4 · 4x Grid", 680, 595, isFourUpA4 = true),
    A4_INVOICE("A4 · Full Page", 840, 595, isFullA4 = true),
    COMPACT_A6("Compact A6 · Normal", 580, 298);

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

    fun generateQrBitmap(content: String, sizePx: Int = 260): Bitmap? {
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
            return generateQrBitmap(uri, 260)
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
        customQrPath: String = ""
    ): Bitmap {
        return when (design) {
            BillDesignModel.PREMIUM_GREEN,
            BillDesignModel.ROYAL_GOLD,
            BillDesignModel.MIDNIGHT_PREMIUM,
            BillDesignModel.SAPPHIRE_GLASS,
            BillDesignModel.ROSE_VELVET,
            BillDesignModel.BENGALI_HERITAGE,
            BillDesignModel.QR_FOCUSED -> renderSectionedCardStyle(
                settings = settings,
                bill = bill,
                payments = payments,
                paid = paid,
                due = due,
                design = design,
                size = size,
                qrMode = qrMode,
                customQrPath = customQrPath
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
                customQrPath = customQrPath
            )
        }
    }

    /**
     * Renders the beloved sectioned rounded-card bill style inspired by the user's reference photo
     * (Emerald/Gold/Midnight/Ocean/Bengali/QR-Focused variants) + handwritten owner signature & date.
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
        customQrPath: String
    ): Bitmap {
        val w = size.widthPx
        val scale = w / 680f
        fun dp(v: Float) = v * scale
        val outerPad = dp(24f)
        val innerPad = dp(18f)

        val qrBitmap = resolveQrBitmap(settings, due, qrMode, customQrPath)
        val isBengali = design == BillDesignModel.BENGALI_HERITAGE
        val isQrHero = design == BillDesignModel.QR_FOCUSED
        val hasLand = bill.landAmount > 0.001 || bill.rate > 0.001
        val hasNote = bill.note.isNotBlank()
        val paymentRowsCount = minOf(payments.size, 4)

        val primaryColor = design.primaryColorInt
        val secondaryColor = design.secondaryColorInt
        val accentBg = design.accentBgInt
        val inkColor = 0xFF111827.toInt()
        val mutedColor = 0xFF64748B.toInt()
        val borderColor = 0xFFE2E8F0.toInt()
        val dueColor = if (due > 0.001) 0xFFDC2626.toInt() else 0xFF15803D.toInt()

        // Measure dynamic total height accurately
        var estH = outerPad * 2f
        estH += dp(130f) // Top colored brand banner
        estH += dp(14f) + dp(72f) // Bill No & Date twin pills
        if (isQrHero && qrBitmap != null) {
            estH += dp(14f) + dp(236f)
        }
        estH += dp(14f) + dp(184f) // Customer Details card
        estH += dp(14f) + if (hasLand) dp(184f) else dp(104f) // Charge Details card
        estH += dp(14f) + dp(186f) // Payment Summary tinted card
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
        val hasLuxuryDoubleTrim = isRoyalGold || isMidnight || isBengali || design == BillDesignModel.ROSE_VELVET
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

        // 1. Top Gradient Brand Banner with subtle luxury accent badge
        val bannerH = dp(126f)
        val bannerRect = RectF(outerPad, y, w - outerPad, y + bannerH)
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                bannerRect.left,
                bannerRect.top,
                bannerRect.right,
                bannerRect.bottom,
                primaryColor,
                secondaryColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(bannerRect, dp(18f), dp(18f), bannerPaint)

        // Subtle gold/platinum status ribbon pill on top-right of banner
        val badgeText = when {
            due <= 0.001 -> if (isBengali) "পরিশোধিত" else "PAID"
            isBengali -> "বিল রশিদ"
            else -> "INVOICE"
        }
        val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isRoyalGold) 0xFF451A03.toInt() else Color.WHITE
            textSize = dp(11.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val badgeW = badgeTextPaint.measureText(badgeText) + dp(20f)
        val badgeH = dp(24f)
        val badgeRect = RectF(
            bannerRect.right - innerPad - badgeW,
            bannerRect.top + dp(16f),
            bannerRect.right - innerPad,
            bannerRect.top + dp(16f) + badgeH
        )
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isRoyalGold) 0xFFFBBF24.toInt() else 0x33FFFFFF
        }
        canvas.drawRoundRect(badgeRect, dp(12f), dp(12f), badgeBgPaint)
        canvas.drawText(
            badgeText,
            badgeRect.left + dp(10f),
            badgeRect.top + dp(16.5f),
            badgeTextPaint
        )

        val companyName = settings.company.ifBlank { "আশা মিনি শ্যালো" }
        canvas.drawText(companyName, bannerRect.left + innerPad, bannerRect.top + dp(42f), headerTitlePaint)
        if (settings.address.isNotBlank()) {
            canvas.drawText(settings.address.take(54), bannerRect.left + innerPad, bannerRect.top + dp(72f), headerSubPaint)
        }
        val contactUpi = listOfNotNull(
            settings.contact.takeIf { it.isNotBlank() }?.let { "Ph: $it" },
            settings.upi.takeIf { it.isNotBlank() }?.let { "UPI: $it" }
        ).joinToString("  •  ")
        if (contactUpi.isNotBlank()) {
            canvas.drawText(contactUpi, bannerRect.left + innerPad, bannerRect.top + dp(100f), headerSubPaint)
        }
        y += bannerH + dp(14f)

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
            drawCardKeyVal(chargeRect.left, chargeRect.right, chy, if (isBengali) "মরশুম (Season)" else "Season", bill.season.ifBlank { "—" })
            chy += dp(36f)
            drawCardKeyVal(
                chargeRect.left,
                chargeRect.right,
                chy,
                if (isBengali) "জমির পরিমাণ" else "Land",
                "${String.format(Locale.US, "%.2f", bill.bigha)} Bigha (${formatQty(bill.landAmount)} ${bill.unit})"
            )
            chy += dp(36f)
            drawCardKeyVal(chargeRect.left, chargeRect.right, chy, if (isBengali) "দর / বিঘা" else "Rate", "${formatInr(bill.rate)}/বিঘা")
            chy += dp(36f)
            drawCardKeyVal(chargeRect.left, chargeRect.right, chy, if (isBengali) "বর্তমান বিল" else "Current Bill", formatInr(bill.current), drawLine = false)
        } else {
            drawCardKeyVal(chargeRect.left, chargeRect.right, chy, "Bill Type", "Previous Due Recovery", drawLine = false)
        }
        y += chargeH + dp(14f)

        // 5. PAYMENT SUMMARY Mint/Accent Card
        val paySumH = dp(186f)
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
        canvas.drawText(if (isBengali) "হিসাব সারসংক্ষেপ" else "PAYMENT SUMMARY", paySumRect.left + dp(16f), paySumRect.top + dp(26f), sectionCapPaint)

        var py = paySumRect.top + dp(58f)
        drawCardKeyVal(paySumRect.left, paySumRect.right, py, if (isBengali) "আগের বাকি" else "Previous Due", formatInr(bill.previousDue), drawLine = false)
        py += dp(34f)
        drawCardKeyVal(paySumRect.left, paySumRect.right, py, if (isBengali) "মোট বিল" else "Total Amount", formatInr(bill.allTotal), drawLine = false)
        py += dp(34f)
        drawCardKeyVal(paySumRect.left, paySumRect.right, py, if (isBengali) "জমা (Paid)" else "Paid / Joma", formatInr(paid), drawLine = false, vColor = 0xFF15803D.toInt())
        py += dp(40f)

        val dueTitlePaint = TextPaint(valPaint).apply { textSize = dp(20f) }
        val dueValPaint = TextPaint(valPaint).apply {
            textSize = dp(22f)
            color = dueColor
        }
        val dueLabelText = if (isBengali) "মোট বাকি (Due)" else "Payable Due"
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
                drawCardKeyVal(
                    rpRect.left,
                    rpRect.right,
                    ry,
                    "${p.date} · ${p.mode}",
                    "+${formatInr(p.amount)}",
                    drawLine = idx < paymentRowsCount - 1,
                    vColor = 0xFF15803D.toInt()
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
        customQrPath: String
    ): Bitmap {
        val w = size.widthPx
        val scale = w / 680f
        fun dp(v: Float) = v * scale
        val pad = dp(26f)
        val isThermal = design == BillDesignModel.THERMAL_COMPACT

        val qrBitmap = resolveQrBitmap(settings, due, qrMode, customQrPath)
        val hasLand = bill.landAmount > 0.001 || bill.rate > 0.001
        val hasNote = bill.note.isNotBlank()
        val paymentRowsCount = minOf(payments.size, 5)

        var estH = dp(220f)
        estH += dp(140f)
        estH += if (hasLand) dp(185f) else dp(92f)
        estH += dp(155f)
        if (paymentRowsCount > 0) estH += dp(64f) + paymentRowsCount * dp(30f)
        if (qrBitmap != null) estH += dp(175f)
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

        y += headerH + dp(16f)

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
            drawKV("Land (${bill.unit.uppercase()})", "${formatQty(bill.landAmount)} ${bill.unit} (${String.format(Locale.US, "%.2f", bill.bigha)} Bigha)")
            drawKV("Rate / Bigha", formatInr(bill.rate))
            drawKV("Current Bill", formatInr(bill.current))
        } else {
            drawKV("Bill Type", "Previous Due Recovery")
        }
        drawKV("Previous Due", formatInr(bill.previousDue))

        // Summary box
        val sumH = dp(124f)
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
        drawSumRow("Grand Total", formatInr(bill.allTotal), primaryColor, big = true)
        drawSumRow("Paid", formatInr(paid), 0xFF14764E.toInt())
        drawSumRow(if (due > 0.001) "Balance Due" else "Paid in Full", formatInr(due), dueColor, big = true)
        y += sumH + dp(16f)

        if (paymentRowsCount > 0) {
            canvas.drawText("RECENT PAYMENTS", pad, y, badgePaint)
            y += dp(10f)
            canvas.drawLine(pad, y, w - pad, y, linePaint)
            y += dp(22f)
            payments.take(paymentRowsCount).forEach { p ->
                canvas.drawText("${p.date} · ${p.mode}", pad, y, mutedPaint)
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
        val filterPaint = Paint(Paint.FILTER_BITMAP_FLAG)

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
        customQrPath: String
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
            customQrPath = customQrPath
        )
        val pdfWidth = size.pdfWidthPt
        val pdfHeight = when {
            size.isHalfA4 || size.isQuarterA4 || size.isFourUpA4 || size.isFullA4 -> 842 // Exact ISO A4 height in pt (595x842)
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
        customQrPath: String
    ) {
        val uri = renderCachedBillImageUri(
            context = context,
            settings = settings,
            bill = bill,
            payments = payments,
            paid = paid,
            due = due,
            design = design,
            size = size,
            qrMode = qrMode,
            customQrPath = customQrPath
        )
        val reminderMsg = buildBillReminderMessage(settings, bill, due)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, reminderMsg)
            putExtra(Intent.EXTRA_SUBJECT, "Bill ${bill.billNo} - ${bill.customerName}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Bill Image & Reminder"))
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
        customQrPath: String = ""
    ) {
        val uri = renderCachedBillImageUri(
            context = context,
            settings = settings,
            bill = bill,
            payments = payments,
            paid = paid,
            due = due,
            design = design,
            size = size,
            qrMode = qrMode,
            customQrPath = customQrPath
        )
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
        }

        val pm = context.packageManager
        val waPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in waPackages) {
            val targeted = Intent(baseIntent).setPackage(pkg)
            if (targeted.resolveActivity(pm) != null) {
                runCatching {
                    context.startActivity(targeted)
                    return
                }
            }
        }

        if (waDigits.isBlank()) {
            android.widget.Toast.makeText(
                context,
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
        context.startActivity(Intent.createChooser(chooserIntent, "Send Bill Image via WhatsApp"))
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
        customQrPath: String
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
            customQrPath = customQrPath
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
