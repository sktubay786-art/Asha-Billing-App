package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.auth.AuthScreen
import com.example.auth.OwnerAccessControl
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.firebase.BillingRepository
import com.example.data.firebase.OfflineModeManager
import com.example.notifications.DueReminderScheduler
import com.example.security.AppPreferencesStore
import com.example.security.AppThemeMode
import com.example.security.BiometricGateScreen
import com.example.security.ThemeColorOption
import com.example.security.checkBiometricAvailability
import com.example.ui.components.ProvideAshaHaptics
import com.example.ui.components.ProvideSafeTextToolbar
import com.example.ui.navigation.AshaAppShell
import com.example.ui.theme.AshaBillingTheme
import com.example.ui.viewmodel.BillingViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : FragmentActivity() {
    private val pendingNotificationBillId = mutableStateOf<String?>(null)

    private val prefsStore by lazy { AppPreferencesStore(applicationContext) }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingNotificationBillId.value = intent?.getStringExtra(DueReminderScheduler.EXTRA_BILL_ID)
        lifecycleScope.launch(Dispatchers.IO) {
            DueReminderScheduler.ensureChannel(applicationContext)
            DueReminderScheduler.rescheduleAll(applicationContext)
        }

        setContent {
            val localPrefs by prefsStore.state.collectAsStateWithLifecycle()
            val accent = ThemeColorOption.fromKey(localPrefs.themeColorKey)
            val isDark = when (localPrefs.themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            AshaBillingTheme(
                darkTheme = isDark,
                primaryColor = Color(accent.primaryHex),
                deepColor = Color(accent.deepHex),
                lightContainerColor = Color(accent.lightContainerHex),
                darkPrimaryColor = Color(accent.darkPrimaryHex)
            ) {
                ProvideSafeTextToolbar {
                    ProvideAshaHaptics(enabled = localPrefs.hapticFeedbackEnabled) {
                        val windowSizeClass = calculateWindowSizeClass(this)
                        val isWideLayout = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                        AppNavigation(
                            isWideLayout = isWideLayout,
                            prefsStore = prefsStore,
                            pendingBillId = pendingNotificationBillId.value,
                            onConsumePendingBillId = { pendingNotificationBillId.value = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val billId = intent.getStringExtra(DueReminderScheduler.EXTRA_BILL_ID)
        if (!billId.isNullOrBlank()) {
            pendingNotificationBillId.value = billId
        }
    }
}

@Composable
fun AppNavigation(
    isWideLayout: Boolean,
    prefsStore: AppPreferencesStore,
    pendingBillId: String? = null,
    onConsumePendingBillId: () -> Unit = {},
    auth: FirebaseAuth = Firebase.auth
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val localPrefs by prefsStore.state.collectAsStateWithLifecycle()
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var unlockedUid by remember { mutableStateOf<String?>(null) }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            prefsStore = prefsStore,
            authScope = scope,
            onAuthSuccess = {
                unlockedUid = auth.currentUser?.uid
            }
        )
    } else {
        val sessionUnlocked = unlockedUid == user.uid
        val needsBiometricCheck = localPrefs.biometricEnabled &&
            localPrefs.biometricOnLogin &&
            !sessionUnlocked
        val biometricAvailable by produceState(initialValue = false, context, needsBiometricCheck) {
            value = if (needsBiometricCheck) {
                withContext(Dispatchers.IO) { checkBiometricAvailability(context).isAvailable }
            } else {
                false
            }
        }
        val requiresLoginGate = needsBiometricCheck && biometricAvailable

        if (requiresLoginGate) {
            val accent = ThemeColorOption.fromKey(localPrefs.themeColorKey)
            BiometricGateScreen(
                moduleTitle = "Unlock ASHA Billing",
                moduleDescription = "Signed in as ${OwnerAccessControl.ownerIdentityLabel(user)}. Verify to continue.",
                primaryColor = Color(accent.primaryHex),
                secondaryActionLabel = stringResourceSafeSignOut(),
                onSecondaryAction = {
                    unlockedUid = null
                    signOut(credentialManager, scope)
                },
                onUnlocked = { unlockedUid = user.uid }
            )
        } else {
            val isDeviceOnline by remember(context) {
                OfflineModeManager.observeNetworkOnline(context).flowOn(Dispatchers.IO)
            }.collectAsStateWithLifecycle(initialValue = true)

            val workspaceId = OwnerAccessControl.workspaceIdFor(user)
            key(user.uid, workspaceId) {
                val viewModel: BillingViewModel = viewModel(
                    key = "${user.uid}_$workspaceId",
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY]) {
                                "APPLICATION_KEY missing from CreationExtras"
                            }
                            val databaseId = app.getString(R.string.firestore_database_id)
                            val db = FirebaseFirestore.getInstance(databaseId)
                            OfflineModeManager.configurePersistentCache(db)
                            BillingViewModel(
                                repository = BillingRepository(db),
                                currentUserId = workspaceId
                            )
                        }
                    }
                )
                LaunchedEffect(localPrefs.offlineModeEnabled, isDeviceOnline) {
                    viewModel.syncOfflineState(
                        manualOffline = localPrefs.offlineModeEnabled,
                        deviceOnline = isDeviceOnline
                    )
                }
                AshaAppShell(
                    viewModel = viewModel,
                    prefsStore = prefsStore,
                    userEmail = OwnerAccessControl.ownerIdentityLabel(user),
                    canEditAppInfo = OwnerAccessControl.isAuthorizedAppEditor(user),
                    isWideLayout = isWideLayout,
                    pendingNotificationBillId = pendingBillId,
                    onConsumePendingBillId = onConsumePendingBillId,
                    onLockWorkspace = { unlockedUid = null }
                )
            }
        }
    }
}

@Composable
private fun stringResourceSafeSignOut(): String =
    androidx.compose.ui.res.stringResource(R.string.switch_google_account)
