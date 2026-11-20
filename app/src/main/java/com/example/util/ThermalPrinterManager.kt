package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.Bill
import com.example.data.model.BusinessSettings
import com.example.data.model.Payment
import com.example.security.AppLocalPreferences
import com.example.security.ThermalConnectionType
import com.example.security.ThermalPaperWidth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import kotlin.math.roundToInt

data class PairedBtPrinter(
    val name: String,
    val address: String,
    val isLikelyPrinter: Boolean
)

/**
 * Universal ESC/POS Thermal Printer engine supporting:
 * - Any Bluetooth Classic (SPP) thermal printer (58mm, 80mm, 110mm/4-inch, or custom mm)
 * - Any Wi-Fi / LAN TCP/IP thermal printer (default port 9100 or custom port)
 *
 * Uses chunked GS v 0 raster bitmap printing so Bengali fonts, 1D Code 128 barcodes,
 * and UPI QR codes print crisply on virtually every ESC/POS thermal printer brand
 * (Epson, Star, Xprinter, Munbyn, Rongta, TVS, Rugtek, Bluprints, Gprinter, Hoin, etc.).
 */
object ThermalPrinterManager {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBluetoothAvailable(context: Context): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter != null
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun listPairedBluetoothDevices(context: Context): List<PairedBtPrinter> {
        if (!hasBluetoothPermission(context)) return emptyList()
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter = manager?.adapter ?: return emptyList()
        val bonded: Set<BluetoothDevice> = runCatching { adapter.bondedDevices }.getOrNull().orEmpty()
        return bonded.map { dev ->
            val rawName = runCatching { dev.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Bluetooth Device"
            val majorClass = runCatching { dev.bluetoothClass?.majorDeviceClass }.getOrNull()
            val nameHints = listOf("print", "pos", "mtp", "rp", "xp", "bt", "thermal", "rpp", "pt-", "gp-", "hoin", "tvs", "epson", "star", "bluprints", "munbyn")
            val likely = majorClass == 0x0600 || nameHints.any { rawName.contains(it, ignoreCase = true) }
            PairedBtPrinter(
                name = rawName,
                address = dev.address.orEmpty(),
                isLikelyPrinter = likely
            )
        }.sortedWith(
            compareByDescending<PairedBtPrinter> { it.isLikelyPrinter }
                .thenBy { it.name.lowercase() }
        )
    }

    fun resolveDotsWidth(prefs: AppLocalPreferences): Int {
        val rawDots = when (prefs.printerPaperWidth) {
            ThermalPaperWidth.MM_58 -> 384
            ThermalPaperWidth.MM_80 -> 576
            ThermalPaperWidth.MM_110 -> 832
            ThermalPaperWidth.CUSTOM -> {
                val clampedMm = prefs.printerCustomWidthMm.coerceIn(38, 120)
                // Standard 203 DPI thermal head = ~8 dots/mm, subtract ~4mm printable margin
                ((clampedMm - 4).coerceAtLeast(32) * 8)
            }
        }
        // Must be a multiple of 8 pixels for ESC/POS GS v 0 byte alignment
        return (rawDots / 8) * 8
    }

    fun isConfigured(prefs: AppLocalPreferences): Boolean = when (prefs.printerConnectionType) {
        ThermalConnectionType.BLUETOOTH -> prefs.printerBluetoothAddress.isNotBlank()
        ThermalConnectionType.WIFI_TCP -> prefs.printerWifiHost.isNotBlank() && prefs.printerWifiPort in 1..65535
        ThermalConnectionType.SYSTEM_PRINT -> false
    }

    fun configuredSummary(prefs: AppLocalPreferences): String = when (prefs.printerConnectionType) {
        ThermalConnectionType.BLUETOOTH -> {
            if (prefs.printerBluetoothAddress.isBlank()) "Bluetooth · Not paired"
            else "BT · ${prefs.printerBluetoothName.ifBlank { prefs.printerBluetoothAddress }} (${prefs.printerPaperWidth.displayLabel(prefs.printerCustomWidthMm)})"
        }
        ThermalConnectionType.WIFI_TCP -> {
            if (prefs.printerWifiHost.isBlank()) "Wi-Fi · Enter printer IP"
            else "Wi-Fi · ${prefs.printerWifiHost}:${prefs.printerWifiPort} (${prefs.printerPaperWidth.displayLabel(prefs.printerCustomWidthMm)})"
        }
        ThermalConnectionType.SYSTEM_PRINT -> "Android System Print"
    }

    suspend fun printBillDirect(
        context: Context,
        prefs: AppLocalPreferences,
        settings: BusinessSettings,
        bill: Bill,
        payments: List<Payment>,
        paid: Double,
        due: Double,
        design: BillDesignModel,
        qrMode: String,
        customQrPath: String,
        bengaliLabels: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val targetSize = if (prefs.printerPaperWidth == ThermalPaperWidth.MM_58) {
                BillPrintSize.THERMAL_58
            } else {
                BillPrintSize.THERMAL_80
            }
            val billBitmap = BillDocumentEngine.renderBillBitmap(
                settings = settings,
                bill = bill,
                payments = payments,
                paid = paid,
                due = due,
                design = design,
                size = targetSize,
                qrMode = qrMode,
                customQrPath = customQrPath,
                bengaliLabels = bengaliLabels
            )
            val dotsWidth = resolveDotsWidth(prefs)
            val payload = encodeBitmapToEscPos(
                source = billBitmap,
                targetDotsWidth = dotsWidth,
                threshold = prefs.printerDensity.coerceIn(70, 210),
                autoCut = prefs.printerAutoCut
            )
            sendRawBytes(context, prefs, payload)
            "Printed ${bill.billNo} to ${configuredSummary(prefs)}"
        }
    }

    suspend fun printTestSlip(
        context: Context,
        prefs: AppLocalPreferences,
        companyName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val dotsWidth = resolveDotsWidth(prefs)
            val slipBitmap = renderTestSlipBitmap(
                dotsWidth = dotsWidth,
                companyName = companyName.ifBlank { "ASHA Mini Shallow" },
                printerSummary = configuredSummary(prefs)
            )
            val payload = encodeBitmapToEscPos(
                source = slipBitmap,
                targetDotsWidth = dotsWidth,
                threshold = prefs.printerDensity.coerceIn(70, 210),
                autoCut = prefs.printerAutoCut
            )
            sendRawBytes(context, prefs, payload)
            "Test slip printed successfully!"
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendRawBytes(context: Context, prefs: AppLocalPreferences, data: ByteArray) {
        when (prefs.printerConnectionType) {
            ThermalConnectionType.BLUETOOTH -> {
                if (!hasBluetoothPermission(context)) {
                    throw SecurityException("Bluetooth permission not granted. Allow Nearby Devices in Settings.")
                }
                val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = manager?.adapter
                    ?: throw IllegalStateException("Bluetooth is not supported on this device.")
                if (!adapter.isEnabled) {
                    throw IllegalStateException("Please turn on Bluetooth first.")
                }
                val address = prefs.printerBluetoothAddress.trim()
                if (address.isBlank()) {
                    throw IllegalStateException("Select a paired Bluetooth thermal printer in Settings.")
                }
                val device = adapter.getRemoteDevice(address)
                runCatching { adapter.cancelDiscovery() }
                val socket = runCatching {
                    device.createRfcommSocketToServiceRecord(SPP_UUID)
                }.getOrElse {
                    device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                }
                socket.use { btSocket ->
                    btSocket.connect()
                    writeInChunks(btSocket.outputStream, data)
                }
            }

            ThermalConnectionType.WIFI_TCP -> {
                val host = prefs.printerWifiHost.trim()
                val port = prefs.printerWifiPort.coerceIn(1, 65535)
                if (host.isBlank()) {
                    throw IllegalStateException("Enter your Wi-Fi / LAN printer IP address in Settings.")
                }
                Socket().use { tcpSocket ->
                    tcpSocket.connect(InetSocketAddress(host, port), 5000)
                    tcpSocket.soTimeout = 10_000
                    writeInChunks(tcpSocket.getOutputStream(), data)
                }
            }

            ThermalConnectionType.SYSTEM_PRINT -> {
                throw IllegalStateException("Select Bluetooth or Wi-Fi Thermal Printer in Settings first.")
            }
        }
    }

    private fun writeInChunks(output: OutputStream, data: ByteArray) {
        // Chunked writes prevent buffer overflows on compact portable 58mm/80mm BT printers
        val chunkSize = 1024
        var offset = 0
        while (offset < data.size) {
            val len = minOf(chunkSize, data.size - offset)
            output.write(data, offset, len)
            output.flush()
            offset += len
            if (offset < data.size) {
                Thread.sleep(14)
            }
        }
        Thread.sleep(120)
    }

    /**
     * Converts any Android [Bitmap] into universal ESC/POS `GS v 0` raster commands,
     * sliced in 128-row horizontal bands for maximum compatibility across all brands.
     */
    fun encodeBitmapToEscPos(
        source: Bitmap,
        targetDotsWidth: Int,
        threshold: Int = 148,
        autoCut: Boolean = true
    ): ByteArray {
        val alignedWidth = ((targetDotsWidth.coerceAtLeast(256)) / 8) * 8
        val scale = alignedWidth.toFloat() / source.width.coerceAtLeast(1).toFloat()
        val targetHeight = (source.height * scale).roundToInt().coerceAtLeast(1)

        val scaled = Bitmap.createBitmap(alignedWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaled)
        canvas.drawColor(Color.WHITE)
        val srcRect = android.graphics.Rect(0, 0, source.width, source.height)
        val dstRect = android.graphics.Rect(0, 0, alignedWidth, targetHeight)
        val drawPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(source, srcRect, dstRect, drawPaint)

        val pixels = IntArray(alignedWidth * targetHeight)
        scaled.getPixels(pixels, 0, alignedWidth, 0, 0, alignedWidth, targetHeight)
        scaled.recycle()

        val bytesPerRow = alignedWidth / 8
        val out = ByteArrayOutputStream()

        // ESC @ -> Initialize printer
        out.write(byteArrayOf(0x1B, 0x40))
        // ESC a 1 -> Center align
        out.write(byteArrayOf(0x1B, 0x61, 0x01))

        val bandHeight = 128
        var yStart = 0
        while (yStart < targetHeight) {
            val sliceHeight = minOf(bandHeight, targetHeight - yStart)
            // GS v 0 m xL xH yL yH
            out.write(
                byteArrayOf(
                    0x1D,
                    0x76,
                    0x30,
                    0x00,
                    (bytesPerRow and 0xFF).toByte(),
                    ((bytesPerRow shr 8) and 0xFF).toByte(),
                    (sliceHeight and 0xFF).toByte(),
                    ((sliceHeight shr 8) and 0xFF).toByte()
                )
            )
            for (y in yStart until (yStart + sliceHeight)) {
                val rowOffset = y * alignedWidth
                for (byteIdx in 0 until bytesPerRow) {
                    var b = 0
                    val xBase = byteIdx * 8
                    for (bit in 0 until 8) {
                        val px = pixels[rowOffset + xBase + bit]
                        val r = (px shr 16) and 0xFF
                        val g = (px shr 8) and 0xFF
                        val bl = px and 0xFF
                        val lum = (0.299 * r + 0.587 * g + 0.114 * bl)
                        if (lum < threshold) {
                            b = b or (1 shl (7 - bit))
                        }
                    }
                    out.write(b)
                }
            }
            yStart += sliceHeight
        }

        // Feed 4 lines cleanly
        out.write(byteArrayOf(0x1B, 0x64, 0x04))
        // Optional partial paper cut (GS V 66 0)
        if (autoCut) {
            out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        }
        return out.toByteArray()
    }

    private fun renderTestSlipBitmap(
        dotsWidth: Int,
        companyName: String,
        printerSummary: String
    ): Bitmap {
        val width = dotsWidth.coerceAtLeast(384)
        val height = 270
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = (width * 0.068f).coerceIn(22f, 38f)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = (width * 0.044f).coerceIn(16f, 26f)
        }
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 3f
        }

        val cx = width / 2f
        canvas.drawLine(12f, 16f, width - 12f, 16f, linePaint)
        canvas.drawText(companyName, cx, 62f, titlePaint)
        canvas.drawText("THERMAL PRINTER CONNECTED", cx, 102f, bodyPaint)
        canvas.drawText("বাংলা প্রিন্ট টেস্ট · সিজন: বর্ষা / খরা", cx, 142f, bodyPaint)
        canvas.drawText(printerSummary, cx, 182f, bodyPaint)
        canvas.drawText("Width: ${width}px · ESC/POS Ready", cx, 218f, bodyPaint)
        canvas.drawLine(12f, 246f, width - 12f, 246f, linePaint)
        return bmp
    }
}
