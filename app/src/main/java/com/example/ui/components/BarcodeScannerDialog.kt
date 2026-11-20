package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.Customer
import com.example.data.model.compactCustomerBarcodeId
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import com.example.util.BillDocumentEngine
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * Fullscreen CameraX + ML Kit barcode/QR scanner dialog for identifying customers or products
 * directly inside bill creation.
 */
@Composable
fun BarcodeScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalAshaHaptics.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var hasHandledScan by remember { mutableStateOf(false) }
    var showManualCodeEntry by remember { mutableStateOf(false) }
    var manualCodeText by remember { mutableStateOf("") }
    var scannerStatusHint by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF07110D)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    val cameraController = remember(context) {
                        LifecycleCameraController(context).apply {
                            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            // High-resolution analysis frames make 1D Code 128 barcodes lock far faster
                            imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        Size(1280, 720),
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                    )
                                )
                                .build()
                        }
                    }

                    val barcodeScanner = remember {
                        val options = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(
                                Barcode.FORMAT_CODE_128,
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_CODE_39,
                                Barcode.FORMAT_EAN_13,
                                Barcode.FORMAT_EAN_8,
                                Barcode.FORMAT_UPC_A
                            )
                            .build()
                        BarcodeScanning.getClient(options)
                    }

                    DisposableEffect(lifecycleOwner, cameraController, barcodeScanner) {
                        val executor = ContextCompat.getMainExecutor(context)
                        cameraController.setImageAnalysisAnalyzer(
                            executor,
                            MlKitAnalyzer(
                                listOf(barcodeScanner),
                                CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                                executor
                            ) { result ->
                                if (hasHandledScan) return@MlKitAnalyzer
                                val barcodes = result?.getValue(barcodeScanner).orEmpty()
                                if (barcodes.isEmpty()) return@MlKitAnalyzer

                                // Prioritize 1D Customer Barcodes and skip UPI Payment QRs so scanning a bill
                                // doesn't accidentally abort on the bottom "upi://pay" QR code!
                                val customerCandidates = barcodes.filter { b ->
                                    val v = b.rawValue?.trim().orEmpty()
                                    v.isNotBlank() && !v.startsWith("upi://", ignoreCase = true)
                                }
                                val preferred = customerCandidates.firstOrNull { it.format == Barcode.FORMAT_CODE_128 }
                                    ?: customerCandidates.firstOrNull()

                                val raw = preferred?.rawValue?.trim()
                                if (!raw.isNullOrBlank()) {
                                    hasHandledScan = true
                                    scannerStatusHint = null
                                    haptics.confirm()
                                    onBarcodeScanned(raw)
                                } else if (barcodes.any { it.rawValue?.trim()?.startsWith("upi://", ignoreCase = true) == true }) {
                                    scannerStatusHint = "UPI Payment QR ignored — aim at the Customer Barcode near top of bill"
                                }
                            }
                        )
                        cameraController.bindToLifecycle(lifecycleOwner)
                        onDispose {
                            cameraController.clearImageAnalysisAnalyzer()
                            cameraController.unbind()
                            barcodeScanner.close()
                        }
                    }

                    LaunchedEffect(torchEnabled, cameraController) {
                        runCatching { cameraController.enableTorch(torchEnabled) }
                    }

                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                controller = cameraController
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    ScannerReticleOverlay(
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Permission denied / awaiting state
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(28.dp)
                            .widthIn(max = 360.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        IosSquircleIcon(
                            icon = Icons.Rounded.CameraAlt,
                            containerColor = EmeraldPrimary,
                            size = 64.dp,
                            iconSize = 32.dp
                        )
                        Text(
                            text = "Camera Access Needed",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Allow camera access to scan customer QR passes, phone barcodes, or product tags.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f),
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("grant_camera_permission_button")
                        ) {
                            Text("Enable Camera", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Top Header Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.56f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                tint = EmeraldMint,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Scan 1D Barcode / QR",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (hasCameraPermission) {
                            IconButton(
                                onClick = { torchEnabled = !torchEnabled },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (torchEnabled) EmeraldPrimary else Color.Black.copy(alpha = 0.56f)
                                    )
                                    .testTag("scanner_torch_button")
                            ) {
                                Icon(
                                    imageVector = if (torchEnabled) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                                    contentDescription = "Toggle flashlight",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.56f))
                                .testTag("close_scanner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close scanner",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Bottom Guidance & Manual Code Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF0F1D17).copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!scannerStatusHint.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = scannerStatusHint!!,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFDE68A),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Point camera at Customer Barcode or QR",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Reads compact CID Barcodes (Code 128), QR & Phone codes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                            OutlinedButton(
                                onClick = { showManualCodeEntry = !showManualCodeEntry },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.testTag("scanner_manual_entry_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Keyboard,
                                    contentDescription = null,
                                    tint = EmeraldMint,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (showManualCodeEntry) "Hide" else "Type",
                                    color = Color.White
                                )
                            }
                        }

                        if (showManualCodeEntry) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = manualCodeText,
                                    onValueChange = { manualCodeText = it },
                                    placeholder = { Text("Phone, Bill No, or QR code", color = Color.White.copy(alpha = 0.5f)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("scanner_manual_code_input")
                                )
                                Button(
                                    onClick = {
                                        val clean = manualCodeText.trim()
                                        if (clean.isNotEmpty() && !hasHandledScan) {
                                            hasHandledScan = true
                                            onBarcodeScanned(clean)
                                        }
                                    },
                                    enabled = manualCodeText.isNotBlank(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("scanner_manual_apply_button")
                                ) {
                                    Text("Apply", fontWeight = FontWeight.Bold)
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
private fun ScannerReticleOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
    val lineProgress by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_progress"
    )

    Canvas(modifier = modifier) {
        val frameW = (size.width * 0.80f).coerceAtMost(330.dp.toPx())
        val frameH = (size.height * 0.34f).coerceIn(190.dp.toPx(), 260.dp.toPx())
        val left = (size.width - frameW) / 2f
        val top = (size.height - frameH) / 2f - 28.dp.toPx()
        val right = left + frameW
        val bottom = top + frameH

        // Dim surrounding viewport softly
        drawRect(color = Color.Black.copy(alpha = 0.42f))

        val cornerLen = 30.dp.toPx()
        val strokeW = 4.dp.toPx()
        val bracketColor = Color(0xFF4ADE80)

        // Top-Left
        drawLine(bracketColor, Offset(left, top), Offset(left + cornerLen, top), strokeW, StrokeCap.Round)
        drawLine(bracketColor, Offset(left, top), Offset(left, top + cornerLen), strokeW, StrokeCap.Round)
        // Top-Right
        drawLine(bracketColor, Offset(right - cornerLen, top), Offset(right, top), strokeW, StrokeCap.Round)
        drawLine(bracketColor, Offset(right, top), Offset(right, top + cornerLen), strokeW, StrokeCap.Round)
        // Bottom-Left
        drawLine(bracketColor, Offset(left, bottom - cornerLen), Offset(left, bottom), strokeW, StrokeCap.Round)
        drawLine(bracketColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW, StrokeCap.Round)
        // Bottom-Right
        drawLine(bracketColor, Offset(right - cornerLen, bottom), Offset(right, bottom), strokeW, StrokeCap.Round)
        drawLine(bracketColor, Offset(right, bottom - cornerLen), Offset(right, bottom), strokeW, StrokeCap.Round)

        // Animated laser line inside reticle
        val laserY = top + frameH * lineProgress
        drawLine(
            color = Color(0xFF22C55E),
            start = Offset(left + 12.dp.toPx(), laserY),
            end = Offset(right - 12.dp.toPx(), laserY),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Generates and displays a compact horizontal 1D Code 128 Barcode encoding ONLY the short Customer ID
 * so any customer can be scanned instantly for chat or billing.
 */
@Composable
fun CustomerQrPassDialog(
    customer: Customer,
    onDismiss: () -> Unit
) {
    val compactId = remember(customer.id) { compactCustomerBarcodeId(customer.id) }
    val barcodeBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, compactId) {
        value = withContext(Dispatchers.Default) {
            BillDocumentEngine.generateCode128BarcodeBitmap(compactId, widthPx = 540, heightPx = 140)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IosSquircleIcon(
                            icon = Icons.Rounded.QrCodeScanner,
                            containerColor = EmeraldPrimary,
                            size = 34.dp,
                            iconSize = 18.dp
                        )
                        Column {
                            Text(
                                text = "Customer Barcode ID",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Compact Code 128 · Fast Scan",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                barcodeBitmap?.let { bmp ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Code 128 barcode for ${customer.name}",
                                contentScale = ContentScale.Fit,
                                filterQuality = FilterQuality.None,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(78.dp)
                            )
                            Text(
                                text = "CID-$compactId",
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }

                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                val meta = listOfNotNull(
                    customer.phone.takeIf { it.isNotBlank() },
                    customer.village.takeIf { it.isNotBlank() },
                    customer.landSummaryLabel().takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
