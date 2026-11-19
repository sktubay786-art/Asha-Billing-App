package com.example.security

import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.ui.components.BmwPrecisionStripe
import com.example.ui.theme.BmwTelemetryStyle
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

enum class AppThemeMode(val key: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromKey(key: String): AppThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class ThemeColorOption(
    val key: String,
    val label: String,
    val primaryHex: Long,
    val deepHex: Long
) {
    EMERALD("emerald", "Asha Emerald", 0xFF0B6B50, 0xFF084B3D),
    TEAL("teal", "Ocean Teal", 0xFF0F766E, 0xFF115E59),
    FOREST("forest", "Deep Forest", 0xFF15803D, 0xFF14532D),
    SAPPHIRE("sapphire", "Royal Blue", 0xFF0369A1, 0xFF0C4A6E);

    companion object {
        fun fromKey(key: String): ThemeColorOption =
            entries.firstOrNull { it.key == key } ?: EMERALD
    }
}

data class AppLocalPreferences(
    val biometricEnabled: Boolean = false,
    val protectBilling: Boolean = true,
    val protectCalculator: Boolean = true,
    val biometricOnLogin: Boolean = true,
    val autoLoginEnabled: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val themeColorKey: String = ThemeColorOption.EMERALD.key,
    val qrMode: String = "Dynamic amount QR",
    val qrImagePath: String = "",
    val defaultPrintSize: String = "Thermal · 80 mm",
    val defaultBillTemplate: String = "Premium Green Bill",
    val bengaliBillMode: Boolean = false
)

class AppPreferencesStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppLocalPreferences> = _state.asStateFlow()

    private fun load(): AppLocalPreferences = AppLocalPreferences(
        biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false),
        protectBilling = prefs.getBoolean(KEY_PROTECT_BILLING, true),
        protectCalculator = prefs.getBoolean(KEY_PROTECT_CALCULATOR, true),
        biometricOnLogin = prefs.getBoolean(KEY_BIOMETRIC_ON_LOGIN, true),
        autoLoginEnabled = prefs.getBoolean(KEY_AUTO_LOGIN, true),
        themeMode = AppThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.key) ?: AppThemeMode.SYSTEM.key),
        themeColorKey = prefs.getString(KEY_THEME_COLOR, ThemeColorOption.EMERALD.key)
            ?: ThemeColorOption.EMERALD.key,
        qrMode = prefs.getString(KEY_QR_MODE, "Dynamic amount QR") ?: "Dynamic amount QR",
        qrImagePath = prefs.getString(KEY_QR_IMAGE_PATH, "") ?: "",
        defaultPrintSize = prefs.getString(KEY_PRINT_SIZE, "Thermal · 80 mm") ?: "Thermal · 80 mm",
        defaultBillTemplate = prefs.getString(KEY_BILL_TEMPLATE, "Premium Green Bill")
            ?: "Premium Green Bill",
        bengaliBillMode = prefs.getBoolean(KEY_BENGALI_BILL_MODE, false)
    )

    fun update(transform: (AppLocalPreferences) -> AppLocalPreferences) {
        val next = transform(_state.value)
        _state.value = next
        prefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, next.biometricEnabled)
            .putBoolean(KEY_PROTECT_BILLING, next.protectBilling)
            .putBoolean(KEY_PROTECT_CALCULATOR, next.protectCalculator)
            .putBoolean(KEY_BIOMETRIC_ON_LOGIN, next.biometricOnLogin)
            .putBoolean(KEY_AUTO_LOGIN, next.autoLoginEnabled)
            .putString(KEY_THEME_MODE, next.themeMode.key)
            .putString(KEY_THEME_COLOR, next.themeColorKey)
            .putString(KEY_QR_MODE, next.qrMode)
            .putString(KEY_QR_IMAGE_PATH, next.qrImagePath)
            .putString(KEY_PRINT_SIZE, next.defaultPrintSize)
            .putString(KEY_BILL_TEMPLATE, next.defaultBillTemplate)
            .putBoolean(KEY_BENGALI_BILL_MODE, next.bengaliBillMode)
            .apply()
    }

    fun saveCustomQrFromUri(uri: Uri): String? {
        return try {
            val outFile = File(appContext.filesDir, "asha_custom_qr.jpg")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            update { it.copy(qrImagePath = outFile.absolutePath) }
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun removeCustomQr() {
        val path = _state.value.qrImagePath
        if (path.isNotBlank()) {
            runCatching { File(path).delete() }
        }
        update { it.copy(qrImagePath = "") }
    }

    companion object {
        private const val PREFS_NAME = "asha_billing_local_prefs"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PROTECT_BILLING = "protect_billing"
        private const val KEY_PROTECT_CALCULATOR = "protect_calculator"
        private const val KEY_BIOMETRIC_ON_LOGIN = "biometric_on_login"
        private const val KEY_AUTO_LOGIN = "auto_login_enabled"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_THEME_COLOR = "theme_color_key"
        private const val KEY_QR_MODE = "qr_mode"
        private const val KEY_QR_IMAGE_PATH = "qr_image_path"
        private const val KEY_PRINT_SIZE = "default_print_size"
        private const val KEY_BILL_TEMPLATE = "default_bill_template"
        private const val KEY_BENGALI_BILL_MODE = "bengali_bill_mode"
    }
}

fun loadBitmapFromPath(path: String): ImageBitmap? {
    if (path.isBlank()) return null
    val file = File(path)
    if (!file.exists()) return null
    return runCatching {
        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
    }.getOrNull()
}

sealed class BiometricStatus(val message: String, val isAvailable: Boolean) {
    data object Ready : BiometricStatus("Fingerprint / Face Unlock ready", true)
    data object NotEnrolled : BiometricStatus("No fingerprint, face, or screen lock enrolled on this device", false)
    data object Unavailable : BiometricStatus("Biometric hardware unavailable on this device", false)
}

fun checkBiometricAvailability(context: Context): BiometricStatus {
    val manager = BiometricManager.from(context)
    val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    } else {
        BIOMETRIC_WEAK
    }
    return when (manager.canAuthenticate(authenticators)) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Ready
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NotEnrolled
        else -> BiometricStatus.Unavailable
    }
}

tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

fun launchBiometricPrompt(
    context: Context,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit = {}
) {
    val activity = context.findFragmentActivity()
    if (activity == null) {
        onError("Unable to open biometric prompt from this context.")
        return
    }
    val status = checkBiometricAvailability(activity)
    if (!status.isAvailable) {
        onError(status.message)
        return
    }

    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onCancelled()
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Biometric not recognized. Try again.")
            }
        }
    )

    val builder = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setConfirmationRequired(false)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        builder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
    } else {
        builder.setAllowedAuthenticators(BIOMETRIC_WEAK)
        builder.setNegativeButtonText("Cancel")
    }

    prompt.authenticate(builder.build())
}

@Composable
fun BiometricGateScreen(
    moduleTitle: String,
    moduleDescription: String,
    primaryColor: Color = EmeraldPrimary,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var triggeredAutoPrompt by remember(moduleTitle) { mutableStateOf(false) }
    var appeared by remember { mutableStateOf(false) }

    val cardScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "biometric_card_scale"
    )
    val pulseTransition = rememberInfiniteTransition(label = "bio_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bio_pulse_scale"
    )

    fun triggerPrompt() {
        errorMessage = null
        launchBiometricPrompt(
            context = context,
            title = moduleTitle,
            subtitle = "Verify fingerprint, face, or device PIN to continue",
            onSuccess = onUnlocked,
            onError = { msg -> errorMessage = msg },
            onCancelled = { errorMessage = "Verification cancelled. Tap below to unlock." }
        )
    }

    LaunchedEffect(moduleTitle) {
        appeared = true
        if (!triggeredAutoPrompt) {
            triggeredAutoPrompt = true
            triggerPrompt()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        primaryColor.copy(alpha = 0.08f)
                    )
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .scale(cardScale),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BmwPrecisionStripe(
                    height = 3.dp,
                    modifier = Modifier.width(80.dp)
                )

                Text(
                    text = "BIOMETRIC SHIELD",
                    style = BmwTelemetryStyle,
                    color = primaryColor
                )

                Box(
                    modifier = Modifier.size(92.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.12f))
                    )
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(EmeraldMint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = "Biometric fingerprint sensor",
                            tint = primaryColor,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Text(
                    text = moduleTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = moduleDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { triggerPrompt() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("biometric_unlock_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.biometric_unlock_cta),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("biometric_secondary_button")
                    ) {
                        Text(secondaryActionLabel, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
