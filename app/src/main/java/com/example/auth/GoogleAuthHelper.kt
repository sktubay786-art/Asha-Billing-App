package com.example.auth

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.security.AppPreferencesStore
import com.example.security.checkBiometricAvailability
import com.example.security.findFragmentActivity
import com.example.security.launchBiometricPrompt
import com.example.ui.components.BmwPrecisionStripe
import com.example.ui.theme.BmwTelemetryStyle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "Auth"

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val activityContext = context.findFragmentActivity() ?: context
            val response = withTimeoutOrNull(2500L) {
                credentialManager.getCredential(activityContext, request)
            }
            val cred = response?.credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                withContext(NonCancellable) {
                    Firebase.auth.signInWithCredential(
                        GoogleAuthProvider.getCredential(token, null)
                    ).await()
                    onAuthSuccess()
                }
            } else {
                onUnauthenticated()
            }
        } catch (e: CancellationException) {
            if (Firebase.auth.currentUser != null) {
                onAuthSuccess()
            }
            throw e
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    forceAccountPicker: Boolean = false,
    onAuthCancelled: (String) -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing.")
        return
    }
    scope.launch {
        try {
            if (forceAccountPicker) {
                runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
            }
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()
            val activityContext = context.findFragmentActivity() ?: context
            val cred = credentialManager.getCredential(activityContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                withContext(NonCancellable) {
                    Firebase.auth.signInWithCredential(
                        GoogleAuthProvider.getCredential(token, null)
                    ).await()
                    onAuthSuccess()
                }
            } else {
                onAuthError("Unexpected credential type returned.")
            }
        } catch (e: CancellationException) {
            if (Firebase.auth.currentUser != null) {
                onAuthSuccess()
            }
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled("Sign-in cancelled. Choose your authorized Google email account.")
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google account available on device: ${e.message}", e)
            onAuthError("No Google account found on this device. Add sktubay786@gmail.com or gamingtubay@gmail.com in Android Settings > Accounts.")
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed.")
        }
    }
}

fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun AuthScreen(
    prefsStore: AppPreferencesStore,
    onAuthSuccess: () -> Unit,
    authScope: CoroutineScope? = null
) {
    val context = LocalContext.current
    val fallbackScope = rememberCoroutineScope()
    val scope = authScope ?: fallbackScope
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val localPrefs by prefsStore.state.collectAsStateWithLifecycle()
    val biometricStatus = remember(context) { checkBiometricAvailability(context) }

    var emailInput by remember { mutableStateOf("sktubay786@gmail.com") }
    var isLoading by remember { mutableStateOf(false) }
    var isAttemptingAuto by remember { mutableStateOf(localPrefs.autoLoginEnabled) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    var appeared by remember { mutableStateOf(false) }

    val cardScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.90f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "auth_card_scale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "auth_card_alpha"
    )

    LaunchedEffect(Unit) {
        appeared = true
        if (localPrefs.autoLoginEnabled) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isAttemptingAuto = false
                    onAuthSuccess()
                },
                onUnauthenticated = {
                    isAttemptingAuto = false
                },
                scope = scope
            )
        } else {
            isAttemptingAuto = false
        }
    }

    fun triggerGoogleSignIn(forcePicker: Boolean) {
        val entered = emailInput.trim()
        if (entered.isNotEmpty() && !OwnerAccessControl.isAllowedOwnerEmail(entered)) {
            errorMessage = "Only sktubay786@gmail.com or gamingtubay@gmail.com have owner access."
            return
        }
        errorMessage = null
        statusNotice = null
        isLoading = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
            },
            scope = scope,
            forceAccountPicker = forcePicker,
            onAuthCancelled = { msg ->
                isLoading = false
                errorMessage = msg
            }
        )
    }

    fun triggerBiometricVerifiedLogin() {
        val entered = emailInput.trim()
        if (entered.isNotEmpty() && !OwnerAccessControl.isAllowedOwnerEmail(entered)) {
            errorMessage = "Select an authorized owner email first."
            return
        }
        errorMessage = null
        statusNotice = null
        if (!biometricStatus.isAvailable) {
            errorMessage = biometricStatus.message
            return
        }
        launchBiometricPrompt(
            context = context,
            title = "Biometric Owner Sign-In",
            subtitle = "Verify fingerprint or face to enable Biometric Shield and sign in",
            onSuccess = {
                prefsStore.update { it.copy(biometricEnabled = true, biometricOnLogin = true) }
                statusNotice = "Fingerprint verified. Completing Google Email sign-in…"
                triggerGoogleSignIn(forcePicker = false)
            },
            onError = { msg -> errorMessage = msg },
            onCancelled = { statusNotice = "Fingerprint verification cancelled." }
        )
    }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020B1F))
    ) {
        // Deep Midnight-to-Cyan Atmospheric Spotlight + Floating 3D Glossy Spheres Backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1CC5FF).copy(alpha = 0.72f),
                        Color(0xFF0A4DA6).copy(alpha = 0.85f),
                        Color(0xFF03193F),
                        Color(0xFF010817)
                    ),
                    center = Offset(w * 0.50f, h * 0.34f),
                    radius = maxOf(w, h) * 0.78f
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.34f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.84f, h * 0.28f),
                    radius = w * 0.52f
                ),
                radius = w * 0.52f,
                center = Offset(w * 0.84f, h * 0.28f)
            )

            fun drawGlossy3dSphere(center: Offset, radius: Float, label: String) {
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f),
                    radius = radius * 1.04f,
                    center = Offset(center.x, center.y + radius * 0.14f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF5CE1FF),
                            Color(0xFF0C74E4),
                            Color(0xFF042B78),
                            Color(0xFF021138)
                        ),
                        center = Offset(center.x - radius * 0.25f, center.y - radius * 0.35f),
                        radius = radius * 1.55f
                    ),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        start = Offset(center.x, center.y - radius),
                        end = Offset(center.x, center.y + radius * 0.1f)
                    ),
                    radius = radius * 0.92f,
                    center = center
                )
                val fontSizeSp = (radius * 0.34f).toSp()
                val measured = textMeasurer.measure(
                    text = label,
                    style = TextStyle(
                        fontSize = fontSizeSp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp,
                        color = Color.White.copy(alpha = 0.90f)
                    )
                )
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = center.x - measured.size.width / 2f,
                        y = center.y - measured.size.height / 2f
                    )
                )
            }

            drawGlossy3dSphere(Offset(w * 0.21f, h * 0.32f), w * 0.17f, "আশা")
            drawGlossy3dSphere(Offset(w * 0.80f, h * 0.35f), w * 0.23f, "AS")
            drawGlossy3dSphere(Offset(w * 0.15f, h * 0.56f), w * 0.13f, "AS")
            drawGlossy3dSphere(Offset(w * 0.56f, h * 0.72f), w * 0.13f, "আশা")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيْمِ",
                fontSize = 13.sp,
                color = Color(0xFFBAE6FD),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Sleek Translucent Frosted Cobalt Glass Card matching reference image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 390.dp)
                    .scale(cardScale)
                    .alpha(cardAlpha)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color(0xFF00A3FF),
                        spotColor = Color(0xFF00143D)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.26f),
                                Color(0xFF0D47A1).copy(alpha = 0.44f),
                                Color(0xFF041C4C).copy(alpha = 0.64f)
                            )
                        )
                    )
                    .border(
                        width = 1.25.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.72f),
                                Color(0xFF7DD3FC).copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.24f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 26.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Bold Geometric Brand Title matching reference
                    Text(
                        text = "ASHA",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.0.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    BmwPrecisionStripe(
                        height = 2.5.dp,
                        modifier = Modifier.width(86.dp)
                    )
                    Text(
                        text = "আশা মিনি শ্যালো · OWNER ACCESS",
                        style = BmwTelemetryStyle,
                        color = Color(0xFFBAE6FD),
                        modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                    )

                    // White Circular Avatar/Logo Badge inside glass card
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(74.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_asha_logo),
                                contentDescription = "ASHA Mini Shallow Emblem",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Bottom-underlined translucent Owner Email field (Google Email Login only)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = "Owner Email Address",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (emailInput.isEmpty()) {
                                Text(
                                    text = "Your Google email",
                                    color = Color.White.copy(alpha = 0.68f),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            BasicTextField(
                                value = emailInput,
                                onValueChange = {
                                    emailInput = it
                                    errorMessage = null
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                cursorBrush = SolidColor(Color.White),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("email_login_input")
                            )
                        }
                        if (OwnerAccessControl.isAllowedOwnerEmail(emailInput)) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = "Authorized Owner Email",
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 8.dp),
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.72f)
                    )

                    // Quick owner email switcher chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OwnerAccessControl.ALLOWED_OWNER_EMAILS.forEach { allowedEmail ->
                            val selected = emailInput.trim().equals(allowedEmail, ignoreCase = true)
                            Surface(
                                onClick = {
                                    emailInput = allowedEmail
                                    errorMessage = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) {
                                    Color.White.copy(alpha = 0.24f)
                                } else {
                                    Color.White.copy(alpha = 0.08f)
                                },
                                border = BorderStroke(
                                    width = if (selected) 1.dp else 0.5.dp,
                                    color = Color.White.copy(alpha = if (selected) 0.78f else 0.25f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = allowedEmail.substringBefore("@"),
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Biometric / Fingerprint Security Bar (keeps Fingerprint login prominent & accessible)
                    Surface(
                        onClick = { triggerBiometricVerifiedLogin() },
                        enabled = !isLoading && !isAttemptingAuto,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.11f),
                        border = BorderStroke(
                            width = 0.9.dp,
                            color = if (localPrefs.biometricEnabled && biometricStatus.isAvailable) {
                                Color(0xFF4ADE80).copy(alpha = 0.65f)
                            } else {
                                Color.White.copy(alpha = 0.30f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("biometric_login_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Fingerprint,
                                    contentDescription = "Fingerprint Biometric Login",
                                    tint = if (biometricStatus.isAvailable) Color(0xFF7DD3FC) else Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Fingerprint / Biometric Sign-In",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (biometricStatus.isAvailable) {
                                        if (localPrefs.biometricEnabled) "Biometric Shield active · Tap to verify"
                                        else "Tap to verify fingerprint & sign in"
                                    } else {
                                        biometricStatus.message
                                    },
                                    fontSize = 10.5.sp,
                                    color = Color(0xFFBAE6FD),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Remember Me Checkbox + Switch Account Link (matches reference row)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    prefsStore.update { it.copy(autoLoginEnabled = !it.autoLoginEnabled) }
                                }
                                .padding(vertical = 6.dp)
                                .testTag("auto_login_switch")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (localPrefs.autoLoginEnabled) Color.White else Color.Transparent)
                                    .border(1.2.dp, Color.White, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (localPrefs.autoLoginEnabled) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Remember me enabled",
                                        tint = Color(0xFF0444A6),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Remember me",
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.92f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "Switch Account",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.92f),
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable(enabled = !isLoading && !isAttemptingAuto) {
                                    triggerGoogleSignIn(forcePicker = true)
                                }
                                .padding(vertical = 6.dp)
                                .testTag("switch_google_account_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Luminous White Pill Primary CTA ("Log In with Google") matching reference
                    Button(
                        onClick = { triggerGoogleSignIn(forcePicker = false) },
                        enabled = !isLoading && !isAttemptingAuto,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF0A4DA6),
                            disabledContainerColor = Color.White.copy(alpha = 0.65f),
                            disabledContentColor = Color(0xFF0A4DA6)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 10.dp,
                            pressedElevation = 4.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading || isAttemptingAuto) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF0A4DA6),
                                strokeWidth = 2.2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isAttemptingAuto) stringResource(R.string.checking_session) else stringResource(R.string.signing_in),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AlternateEmail,
                                contentDescription = null,
                                tint = Color(0xFF0A4DA6),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sign_in_with_google),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                letterSpacing = 0.35.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "GOOGLE EMAIL AUTHENTICATION · CLOUD SYNC",
                        style = BmwTelemetryStyle,
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center
                    )

                    AnimatedVisibility(visible = !statusNotice.isNullOrBlank(), enter = fadeIn(), exit = fadeOut()) {
                        Surface(
                            color = Color(0xFF0EA5E9).copy(alpha = 0.28f),
                            border = BorderStroke(1.dp, Color(0xFF7DD3FC).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text(
                                text = statusNotice.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = !errorMessage.isNullOrBlank(), enter = fadeIn(), exit = fadeOut()) {
                        Surface(
                            color = Color(0xFFDC2626).copy(alpha = 0.34f),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.65f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
