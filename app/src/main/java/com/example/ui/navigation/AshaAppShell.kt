package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.signOut
import com.example.data.model.Bill
import com.example.data.model.Customer
import com.example.data.model.Payment
import com.example.data.model.formatInr
import com.example.security.AppPreferencesStore
import com.example.security.AppThemeMode
import com.example.security.BiometricGateScreen
import com.example.security.ThemeColorOption
import com.example.security.checkBiometricAvailability
import com.example.ui.components.AnimatedBmwPrecisionStripe
import com.example.ui.components.AnimatedIosNavBadge
import com.example.ui.components.BmwPrecisionBadge
import com.example.ui.components.BmwPrecisionStripe
import com.example.ui.components.EditBillDialog
import com.example.ui.components.EditPaymentDialog
import com.example.ui.components.InvoiceDetailDialog
import com.example.ui.components.IosAppLogoMark
import com.example.ui.components.PremiumBillIcon
import com.example.ui.components.PremiumCalculatorIcon
import com.example.ui.components.PremiumHistoryIcon
import com.example.ui.components.PremiumHomeIcon
import com.example.ui.components.PremiumPayIcon
import com.example.ui.components.PremiumSettingsIcon
import com.example.ui.components.PremiumUserIcon
import com.example.ui.components.PulsingLiveDot
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CustomerFormDialog
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MonthlyAnalyticsScreen
import com.example.ui.screens.NewBillScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.PaymentsScreen
import com.example.ui.screens.SettleCustomerDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UserChatLedgerScreen
import com.example.ui.screens.WbsedclScreen
import com.example.ui.theme.CrimsonDue
import com.example.ui.viewmodel.BillingViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.BillDesignModel
import com.example.util.BillPrintSize
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
sealed interface AppRoute {
    @Serializable data object Overview : AppRoute
    @Serializable data object Customers : AppRoute
    @Serializable data object NewBill : AppRoute
    @Serializable data object Payments : AppRoute
    @Serializable data object History : AppRoute
    @Serializable data object Expenses : AppRoute
    @Serializable data object MonthlyAnalytics : AppRoute
    @Serializable data object Calculator : AppRoute
    @Serializable data object Wbsedcl : AppRoute
    @Serializable data object Settings : AppRoute
}

private data class NavItemSpec(
    val route: AppRoute,
    val label: String,
    val icon: ImageVector,
    val accentColor: Color,
    val tag: String
)

private val V103BottomDestinations = listOf(
    NavItemSpec(AppRoute.Overview, "Home", Icons.Rounded.Home, Color(0xFF0B6B50), "nav_overview"),
    NavItemSpec(AppRoute.Customers, "Users", Icons.Rounded.People, Color(0xFF007AFF), "nav_customers"),
    NavItemSpec(AppRoute.NewBill, "Bill", Icons.AutoMirrored.Rounded.ReceiptLong, Color(0xFF5856D6), "nav_new_bill"),
    NavItemSpec(AppRoute.Payments, "Pay", Icons.Rounded.CurrencyRupee, Color(0xFF34C759), "nav_payments"),
    NavItemSpec(AppRoute.History, "History", Icons.Rounded.History, Color(0xFFAF52DE), "nav_history"),
    NavItemSpec(AppRoute.Calculator, "Calculator", Icons.Rounded.Calculate, Color(0xFFFF9500), "nav_calculator"),
    NavItemSpec(AppRoute.Settings, "Settings", Icons.Rounded.Settings, Color(0xFF64748B), "nav_settings")
)

@Composable
fun AshaAppShell(
    viewModel: BillingViewModel,
    prefsStore: AppPreferencesStore,
    userEmail: String?,
    canEditAppInfo: Boolean = true,
    isWideLayout: Boolean,
    pendingNotificationBillId: String? = null,
    onConsumePendingBillId: () -> Unit = {},
    onLockWorkspace: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cloudStatus by viewModel.cloudStatus.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val selectedInvoiceId by viewModel.selectedInvoiceId.collectAsStateWithLifecycle()
    val preselectedCustomerId by viewModel.preselectedCustomerId.collectAsStateWithLifecycle()
    val preselectedBillId by viewModel.preselectedBillId.collectAsStateWithLifecycle()
    val calcState by viewModel.calcState.collectAsStateWithLifecycle()
    val localPrefs by prefsStore.state.collectAsStateWithLifecycle()

    val accent = ThemeColorOption.fromKey(localPrefs.themeColorKey)
    val primaryAccent = Color(accent.primaryHex)
    val deepAccent = Color(accent.deepHex)
    val biometricAvailable = remember(context) { checkBiometricAvailability(context).isAvailable }
    val isDarkCurrently = when (localPrefs.themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Per-module session unlock states for Biometric Security Layer
    var billingUnlocked by remember(userEmail) { mutableStateOf(false) }
    var calculatorUnlocked by remember(userEmail) { mutableStateOf(false) }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val currentSectionLabel = when {
        currentDest?.hasRoute(AppRoute.Overview::class) == true -> "Home"
        currentDest?.hasRoute(AppRoute.Customers::class) == true -> "Users"
        currentDest?.hasRoute(AppRoute.NewBill::class) == true -> "New Bill"
        currentDest?.hasRoute(AppRoute.Payments::class) == true -> "Pay"
        currentDest?.hasRoute(AppRoute.History::class) == true -> "History"
        currentDest?.hasRoute(AppRoute.Expenses::class) == true -> "Expenses"
        currentDest?.hasRoute(AppRoute.MonthlyAnalytics::class) == true -> "P&L Chart"
        currentDest?.hasRoute(AppRoute.Calculator::class) == true -> "Calculator"
        currentDest?.hasRoute(AppRoute.Wbsedcl::class) == true -> "WBSEDCL"
        currentDest?.hasRoute(AppRoute.Settings::class) == true -> "Settings"
        else -> "Home"
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var openAddCustomerOnNextVisit by remember { mutableStateOf(false) }
    var openPaymentHistoryOnNextVisit by remember { mutableStateOf(false) }
    var previewDraftBill by remember { mutableStateOf<Bill?>(null) }
    var previewDraftDesign by remember { mutableStateOf<BillDesignModel?>(null) }
    var previewDraftSize by remember { mutableStateOf<BillPrintSize?>(null) }
    var bengaliBillMode by remember { mutableStateOf(false) }

    // WhatsApp-style full-screen user ledger chat state
    var activeChatCustomerId by remember { mutableStateOf<String?>(null) }
    var editingChatCustomer by remember { mutableStateOf<Customer?>(null) }
    var settlingChatCustomer by remember { mutableStateOf<Customer?>(null) }
    var deletingChatCustomer by remember { mutableStateOf<Customer?>(null) }

    // Bill & Payment undo / fix / delete modal state
    var editingBillTarget by remember { mutableStateOf<Bill?>(null) }
    var deletingBillTarget by remember { mutableStateOf<Bill?>(null) }
    var editingPaymentTarget by remember { mutableStateOf<Payment?>(null) }
    var deletingPaymentTarget by remember { mutableStateOf<Payment?>(null) }

    LaunchedEffect(Unit) {
        viewModel.toastMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(pendingNotificationBillId, uiState) {
        val targetId = pendingNotificationBillId
        val data = (uiState as? UiState.Success)?.data
        if (!targetId.isNullOrBlank() && data != null) {
            if (data.bills.any { it.id == targetId }) {
                viewModel.openInvoice(targetId)
            }
            onConsumePendingBillId()
        }
    }

    fun navigateTo(route: AppRoute) {
        activeChatCustomerId = null
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val activeChatCustomer = (uiState as? UiState.Success)?.data?.customers
        ?.firstOrNull { it.id == activeChatCustomerId }
    val inFullChat = activeChatCustomer != null

    val isBillingLockedNow = localPrefs.biometricEnabled &&
        biometricAvailable &&
        localPrefs.protectBilling &&
        !billingUnlocked

    val isCalculatorLockedNow = localPrefs.biometricEnabled &&
        biometricAvailable &&
        localPrefs.protectCalculator &&
        !calculatorUnlocked

    val defaultDesignModel = BillDesignModel.fromLabel(localPrefs.defaultBillTemplate)
    val defaultPrintSize = BillPrintSize.fromLabel(localPrefs.defaultPrintSize)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (!inFullChat) {
                val dashboardData = (uiState as? UiState.Success)?.data

                V103EmeraldHeaderBanner(
                    bismillahArabic = "بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيْمِ",
                    headerTitle = "ASHA Mini Shallow",
                    activeSection = currentSectionLabel,
                    totalOutstanding = dashboardData?.totalOutstanding ?: 0.0,
                    netProfit = dashboardData?.netCollectedProfit ?: 0.0,
                    cloudStatus = cloudStatus,
                    isSyncing = isSyncing,
                    biometricEnabled = localPrefs.biometricEnabled && biometricAvailable,
                    canEditAppInfo = canEditAppInfo,
                    primaryColor = primaryAccent,
                    deepColor = deepAccent,
                    onOpenMonthlyAnalytics = { navigateTo(AppRoute.MonthlyAnalytics) },
                    onOpenUsers = { navigateTo(AppRoute.Customers) },
                    onSyncClick = { viewModel.syncNow() },
                    onLockClick = {
                        billingUnlocked = false
                        calculatorUnlocked = false
                        if (localPrefs.biometricOnLogin) {
                            onLockWorkspace()
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Protected modules locked")
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!inFullChat) {
                V103FloatingBottomBar(
                    isWideLayout = isWideLayout,
                    primaryColor = primaryAccent,
                    isRouteSelected = { route ->
                        currentDest?.hasRoute(route::class) == true
                    },
                    onSelectRoute = { route -> navigateTo(route) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (inFullChat) PaddingValues() else innerPadding)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryAccent)
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { viewModel.syncNow() }) {
                                Text(stringResource(R.string.sync_now))
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    val dashboardData = state.data
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = if (inFullChat) 900.dp else 1080.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        if (activeChatCustomer != null) {
                            if (isBillingLockedNow) {
                                BiometricGateScreen(
                                    moduleTitle = "Unlock Customer Ledger",
                                    moduleDescription = "Biometric security is enabled for billing ledgers.",
                                    primaryColor = primaryAccent,
                                    secondaryActionLabel = "Back",
                                    onSecondaryAction = { activeChatCustomerId = null },
                                    onUnlocked = { billingUnlocked = true }
                                )
                            } else {
                                UserChatLedgerScreen(
                                    customer = activeChatCustomer,
                                    data = dashboardData,
                                    defaultBillDesign = defaultDesignModel,
                                    defaultPrintSize = defaultPrintSize,
                                    qrMode = localPrefs.qrMode,
                                    customQrPath = localPrefs.qrImagePath,
                                    bengaliLabels = bengaliBillMode,
                                    onBack = { activeChatCustomerId = null },
                                    onNewBill = {
                                        activeChatCustomerId = null
                                        viewModel.selectCustomerForNewBill(activeChatCustomer.id)
                                        navigateTo(AppRoute.NewBill)
                                    },
                                    onRecordBillPayment = { bill, amount, currentDue, mode, receivedIn, date, note, onSaved ->
                                        viewModel.recordPayment(
                                            bill = bill,
                                            amount = amount,
                                            currentDue = currentDue,
                                            mode = mode,
                                            receivedIn = receivedIn,
                                            date = date,
                                            note = note,
                                            onSaved = onSaved
                                        )
                                    },
                                    onSettleOverallAmount = { amount, onSettled ->
                                        viewModel.settleCustomer(activeChatCustomer, amount, dashboardData, onSettled)
                                    },
                                    onEditCustomer = { editingChatCustomer = activeChatCustomer },
                                    onSettleCustomer = {
                                        if (dashboardData.dueForCustomer(activeChatCustomer.id) <= 0.001) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Customer has no outstanding balance")
                                            }
                                        } else {
                                            settlingChatCustomer = activeChatCustomer
                                        }
                                    },
                                    onDeleteCustomer = { deletingChatCustomer = activeChatCustomer },
                                    onViewInvoice = { billId -> viewModel.openInvoice(billId) },
                                    onEditBill = { editingBillTarget = it },
                                    onDeleteBill = { deletingBillTarget = it },
                                    onEditPayment = { editingPaymentTarget = it },
                                    onDeletePayment = { deletingPaymentTarget = it },
                                    onSendChatMessage = { text, onSent ->
                                        viewModel.sendChatMessage(activeChatCustomer.id, text, onSent)
                                    },
                                    onUpdateChatMessage = { msg, newText, onDone ->
                                        viewModel.updateChatMessage(msg.id, newText) { ok ->
                                            if (ok) onDone()
                                        }
                                    },
                                    onDeleteChatMessage = { msgId ->
                                        viewModel.deleteChatMessage(msgId)
                                    }
                                )
                            }
                        } else {
                            NavHost(
                                navController = navController,
                                startDestination = AppRoute.Overview,
                                enterTransition = {
                                    fadeIn(animationSpec = tween(240)) +
                                        scaleIn(initialScale = 0.965f, animationSpec = tween(240)) +
                                        slideInVertically(animationSpec = tween(250)) { it / 28 }
                                },
                                exitTransition = {
                                    fadeOut(animationSpec = tween(160)) +
                                        scaleOut(targetScale = 0.985f, animationSpec = tween(160))
                                },
                                popEnterTransition = {
                                    fadeIn(animationSpec = tween(220)) +
                                        scaleIn(initialScale = 0.975f, animationSpec = tween(220))
                                },
                                popExitTransition = {
                                    fadeOut(animationSpec = tween(150))
                                }
                            ) {
                                composable<AppRoute.Overview> {
                                    OverviewScreen(
                                        data = dashboardData,
                                        isWideLayout = isWideLayout,
                                        isDarkTheme = isDarkCurrently,
                                        defaultBillDesign = defaultDesignModel,
                                        defaultPrintSize = defaultPrintSize,
                                        qrMode = localPrefs.qrMode,
                                        customQrPath = localPrefs.qrImagePath,
                                        onToggleTheme = {
                                            prefsStore.update {
                                                it.copy(
                                                    themeMode = if (isDarkCurrently) AppThemeMode.LIGHT else AppThemeMode.DARK
                                                )
                                            }
                                        },
                                        onCreateBill = { navigateTo(AppRoute.NewBill) },
                                        onOpenUsers = { navigateTo(AppRoute.Customers) },
                                        onOpenCustomerChat = { cust -> activeChatCustomerId = cust.id },
                                        onReceivePayment = { navigateTo(AppRoute.Payments) },
                                        onOpenCalculator = { navigateTo(AppRoute.Calculator) },
                                        onOpenWbsedcl = { navigateTo(AppRoute.Wbsedcl) },
                                        onOpenExpenses = { navigateTo(AppRoute.Expenses) },
                                        onOpenMonthlyAnalytics = { navigateTo(AppRoute.MonthlyAnalytics) },
                                        onViewHistory = { navigateTo(AppRoute.History) },
                                        onViewInvoice = { billId ->
                                            previewDraftDesign = null
                                            previewDraftSize = null
                                            viewModel.openInvoice(billId)
                                        }
                                    )
                                }

                                composable<AppRoute.Customers> {
                                    BackHandler { navigateTo(AppRoute.Overview) }
                                    CustomersScreen(
                                        data = dashboardData,
                                        openAddDialogImmediately = openAddCustomerOnNextVisit,
                                        onConsumeAddDialogFlag = { openAddCustomerOnNextVisit = false },
                                        onOpenCustomerChat = { cust -> activeChatCustomerId = cust.id },
                                        onSaveCustomer = { id, name, phone, village, address, openingDue, landArea, landOwnerType, landOwnerName, onComplete ->
                                            viewModel.saveCustomer(
                                                existingId = id,
                                                name = name,
                                                phone = phone,
                                                village = village,
                                                address = address,
                                                openingDue = openingDue,
                                                landArea = landArea,
                                                landOwnerType = landOwnerType,
                                                landOwnerName = landOwnerName,
                                                onComplete = onComplete
                                            )
                                        }
                                    )
                                }

                                composable<AppRoute.NewBill> {
                                    BackHandler { navigateTo(AppRoute.Overview) }
                                    if (isBillingLockedNow) {
                                        BiometricGateScreen(
                                            moduleTitle = "Billing Module Locked",
                                            moduleDescription = "Verify fingerprint or face unlock to create bills and access invoices.",
                                            primaryColor = primaryAccent,
                                            secondaryActionLabel = "Back to Home",
                                            onSecondaryAction = { navigateTo(AppRoute.Overview) },
                                            onUnlocked = { billingUnlocked = true }
                                        )
                                    } else {
                                        NewBillScreen(
                                            data = dashboardData,
                                            nextBillNo = viewModel.nextBillNumber(dashboardData.bills),
                                            preselectedCustomerId = preselectedCustomerId,
                                            defaultBillDesign = localPrefs.defaultBillTemplate,
                                            defaultPrintSize = localPrefs.defaultPrintSize,
                                            qrMode = localPrefs.qrMode,
                                            customQrPath = localPrefs.qrImagePath,
                                            defaultBengaliMode = bengaliBillMode,
                                            onToggleBengaliMode = { bengaliBillMode = it },
                                            onConsumePreselectedCustomer = { viewModel.consumePreselectedCustomer() },
                                            onPreviewBill = { draft, design, size ->
                                                previewDraftDesign = design
                                                previewDraftSize = size
                                                previewDraftBill = draft
                                            },
                                            onSaveExistingCustomerBill = { customer, season, date, qty, unit, rate, initialPaid, mode, receivedIn, note, previousDueOverride, onCreated ->
                                                viewModel.createBill(
                                                    data = dashboardData,
                                                    customer = customer,
                                                    season = season,
                                                    date = date,
                                                    landAmount = qty,
                                                    unit = unit,
                                                    ratePerBigha = rate,
                                                    initialPaid = initialPaid,
                                                    initialMode = mode,
                                                    initialReceivedIn = receivedIn,
                                                    note = note,
                                                    previousDueOverride = previousDueOverride,
                                                    onCreated = { newId, newBillNo -> onCreated(newId, newBillNo) }
                                                )
                                            },
                                            onSaveManualCustomerBill = { name, phone, village, address, prevDue, season, date, qty, unit, rate, initialPaid, mode, receivedIn, note, onCreated ->
                                                viewModel.createManualCustomerAndBill(
                                                    data = dashboardData,
                                                    customerName = name,
                                                    phone = phone,
                                                    village = village,
                                                    address = address,
                                                    manualPreviousDue = prevDue,
                                                    season = season,
                                                    date = date,
                                                    landAmount = qty,
                                                    unit = unit,
                                                    ratePerBigha = rate,
                                                    initialPaid = initialPaid,
                                                    initialMode = mode,
                                                    initialReceivedIn = receivedIn,
                                                    note = note,
                                                    onCreated = { newId, newBillNo -> onCreated(newId, newBillNo) }
                                                )
                                            }
                                        )
                                    }
                                }

                                composable<AppRoute.Payments> {
                                    BackHandler { navigateTo(AppRoute.Overview) }
                                    if (isBillingLockedNow) {
                                        BiometricGateScreen(
                                            moduleTitle = "Payments Module Locked",
                                            moduleDescription = "Verify fingerprint or face unlock to receive payments or view payment history.",
                                            primaryColor = primaryAccent,
                                            secondaryActionLabel = "Back to Home",
                                            onSecondaryAction = { navigateTo(AppRoute.Overview) },
                                            onUnlocked = { billingUnlocked = true }
                                        )
                                    } else {
                                        PaymentsScreen(
                                            data = dashboardData,
                                            preselectedBillId = preselectedBillId,
                                            onConsumePreselectedBill = { viewModel.consumePreselectedBill() },
                                            initialShowHistory = openPaymentHistoryOnNextVisit,
                                            onConsumeShowHistoryFlag = { openPaymentHistoryOnNextVisit = false },
                                            onViewInvoice = { billId -> viewModel.openInvoice(billId) },
                                            onSavePayment = { bill, amount, currentDue, mode, receivedIn, date, note, onSaved ->
                                                viewModel.recordPayment(
                                                    bill = bill,
                                                    amount = amount,
                                                    currentDue = currentDue,
                                                    mode = mode,
                                                    receivedIn = receivedIn,
                                                    date = date,
                                                    note = note,
                                                    onSaved = onSaved
                                                )
                                            },
                                            onEditPayment = { editingPaymentTarget = it },
                                            onDeletePayment = { deletingPaymentTarget = it },
                                            defaultPrintSize = defaultPrintSize
                                        )
                                    }
                                }

                                composable<AppRoute.History> {
                                    BackHandler { navigateTo(AppRoute.Overview) }
                                    if (isBillingLockedNow) {
                                        BiometricGateScreen(
                                            moduleTitle = "History Module Locked",
                                            moduleDescription = "Verify fingerprint or face unlock to view bills and payment history.",
                                            primaryColor = primaryAccent,
                                            secondaryActionLabel = "Back to Home",
                                            onSecondaryAction = { navigateTo(AppRoute.Overview) },
                                            onUnlocked = { billingUnlocked = true }
                                        )
                                    } else {
                                        HistoryScreen(
                                            data = dashboardData,
                                            defaultBillDesign = defaultDesignModel,
                                            defaultPrintSize = defaultPrintSize,
                                            qrMode = localPrefs.qrMode,
                                            customQrPath = localPrefs.qrImagePath,
                                            onOpenMonthlyAnalytics = { navigateTo(AppRoute.MonthlyAnalytics) },
                                            onOpenExpenses = { navigateTo(AppRoute.Expenses) },
                                            onViewInvoice = { billId ->
                                                previewDraftDesign = null
                                                previewDraftSize = null
                                                viewModel.openInvoice(billId)
                                            },
                                            onEditBill = { editingBillTarget = it },
                                            onDeleteBill = { deletingBillTarget = it },
                                            onEditPayment = { editingPaymentTarget = it },
                                            onDeletePayment = { deletingPaymentTarget = it },
                                            bengaliLabels = bengaliBillMode
                                        )
                                    }
                                }

                                composable<AppRoute.Expenses> {
                                    BackHandler { navigateTo(AppRoute.Overview) }
                                    ExpensesScreen(
                                        data = dashboardData,
                                        onSaveExpense = { existingId, title, category, amount, date, paidMode, note, onComplete ->
                                            viewModel.saveExpense(
                                                existingId = existingId,
                                                title = title,
                                                category = category,
                                                amount = amount,
                                                date = date,
                                                paidMode = paidMode,
                                                note = note,
                                                onComplete = onComplete
                                            )
                                        },
                                        onDeleteExpense = { expenseId ->
                                            viewModel.deleteExpense(expenseId)
                                        },
                                        onOpenMonthlyAnalytics = { navigateTo(AppRoute.MonthlyAnalytics) }
                                    )
                                }

                                composable<AppRoute.MonthlyAnalytics> {
                                    BackHandler { navigateTo(AppRoute.Overview) }
                                    MonthlyAnalyticsScreen(
                                        data = dashboardData,
                                        onOpenExpenses = { navigateTo(AppRoute.Expenses) }
                                    )
                                }

                                composable<AppRoute.Calculator> {
                                    BackHandler { navigateTo(AppRoute.Overview) }
                                    if (isCalculatorLockedNow) {
                                        BiometricGateScreen(
                                            moduleTitle = "Calculator Locked",
                                            moduleDescription = "Biometric security is enabled for the Calculator module.",
                                            primaryColor = primaryAccent,
                                            secondaryActionLabel = "Back to Home",
                                            onSecondaryAction = { navigateTo(AppRoute.Overview) },
                                            onUnlocked = { calculatorUnlocked = true }
                                        )
                                    } else {
                                        CalculatorScreen(
                                            calcState = calcState,
                                            onKeyPress = { key -> viewModel.onCalcKey(key) }
                                        )
                                    }
                                }

                                composable<AppRoute.Wbsedcl> {
                                    BackHandler { navigateTo(AppRoute.Overview) }
                                    WbsedclScreen(
                                        data = dashboardData,
                                        onSaveConsumer = { existingId, label, consumerNo, installationNote, onComplete ->
                                            viewModel.saveWbsedclConsumer(
                                                existingId = existingId,
                                                label = label,
                                                consumerNo = consumerNo,
                                                installationNote = installationNote,
                                                onComplete = onComplete
                                            )
                                        },
                                        onDeleteConsumer = { consumerId ->
                                            viewModel.deleteWbsedclConsumer(consumerId)
                                        },
                                        onSavePayment = { existingId, consumerId, consumerNo, consumerLabel, billMonth, amountPaid, date, note, onComplete ->
                                            viewModel.saveWbsedclPayment(
                                                existingId = existingId,
                                                consumerId = consumerId,
                                                consumerNo = consumerNo,
                                                consumerLabel = consumerLabel,
                                                billMonth = billMonth,
                                                amountPaid = amountPaid,
                                                date = date,
                                                note = note,
                                                onComplete = onComplete
                                            )
                                        },
                                        onDeletePayment = { paymentId ->
                                            viewModel.deleteWbsedclPayment(paymentId)
                                        }
                                    )
                                }

                                composable<AppRoute.Settings> {
                                    BackHandler { navigateTo(AppRoute.Overview) }
                                    SettingsScreen(
                                        data = dashboardData,
                                        prefsStore = prefsStore,
                                        userEmail = userEmail,
                                        canEditAppInfo = canEditAppInfo,
                                        cloudStatus = cloudStatus,
                                        isSyncing = isSyncing,
                                        onSaveSettings = { updated -> viewModel.saveSettings(updated) },
                                        onSyncNow = { viewModel.syncNow() },
                                        onSignOut = {
                                            signOut(credentialManager, scope)
                                        }
                                    )
                                }
                            }
                        }

                        val activeInvoice = selectedInvoiceId?.let { id ->
                            dashboardData.bills.firstOrNull { it.id == id }
                        } ?: previewDraftBill

                        if (activeInvoice != null) {
                            InvoiceDetailDialog(
                                bill = activeInvoice,
                                data = dashboardData,
                                initialDesignLabel = (previewDraftDesign ?: defaultDesignModel).label,
                                initialPrintSizeLabel = (previewDraftSize ?: defaultPrintSize).label,
                                qrMode = localPrefs.qrMode,
                                customQrPath = localPrefs.qrImagePath,
                                initialBengaliMode = bengaliBillMode,
                                onToggleBengaliMode = { bengaliBillMode = it },
                                onEditBill = if (activeInvoice.id.isNotBlank()) {
                                    { editingBillTarget = it }
                                } else null,
                                onDeleteBill = if (activeInvoice.id.isNotBlank()) {
                                    { deletingBillTarget = it }
                                } else null,
                                onEditPayment = { editingPaymentTarget = it },
                                onDeletePayment = { deletingPaymentTarget = it },
                                onDismiss = {
                                    previewDraftBill = null
                                    previewDraftDesign = null
                                    previewDraftSize = null
                                    viewModel.openInvoice(null)
                                }
                            )
                        }

                        editingChatCustomer?.let { cust ->
                            CustomerFormDialog(
                                initial = cust,
                                onDismiss = { editingChatCustomer = null },
                                onSave = { name, phone, village, address, openingDue, landArea, landOwnerType, landOwnerName ->
                                    viewModel.saveCustomer(
                                        existingId = cust.id,
                                        name = name,
                                        phone = phone,
                                        village = village,
                                        address = address,
                                        openingDue = openingDue,
                                        landArea = landArea,
                                        landOwnerType = landOwnerType,
                                        landOwnerName = landOwnerName
                                    ) { success ->
                                        if (success) editingChatCustomer = null
                                    }
                                }
                            )
                        }

                        settlingChatCustomer?.let { cust ->
                            SettleCustomerDialog(
                                customer = cust,
                                totalDue = dashboardData.dueForCustomer(cust.id),
                                onDismiss = { settlingChatCustomer = null },
                                onConfirm = { amount ->
                                    viewModel.settleCustomer(cust, amount, dashboardData) {
                                        settlingChatCustomer = null
                                    }
                                }
                            )
                        }

                        deletingChatCustomer?.let { cust ->
                            AlertDialog(
                                onDismissRequest = { deletingChatCustomer = null },
                                title = { Text("Delete ${cust.name}?") },
                                text = {
                                    Text("Delete ${cust.name} and all associated invoices? This cannot be undone.")
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            viewModel.deleteCustomer(cust, dashboardData)
                                            deletingChatCustomer = null
                                            activeChatCustomerId = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonDue)
                                    ) {
                                        Text(stringResource(R.string.delete))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { deletingChatCustomer = null }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }
                            )
                        }

                        editingBillTarget?.let { targetBill ->
                            EditBillDialog(
                                bill = targetBill,
                                onDismiss = { editingBillTarget = null },
                                onConfirm = { season, date, landAmount, unit, ratePerBigha, previousDue, note ->
                                    viewModel.updateBill(
                                        original = targetBill,
                                        season = season,
                                        date = date,
                                        landAmount = landAmount,
                                        unit = unit,
                                        ratePerBigha = ratePerBigha,
                                        previousDue = previousDue,
                                        note = note
                                    ) { ok ->
                                        if (ok) editingBillTarget = null
                                    }
                                }
                            )
                        }

                        deletingBillTarget?.let { targetBill ->
                            AlertDialog(
                                onDismissRequest = { deletingBillTarget = null },
                                title = { Text("Undo / Delete ${targetBill.billNo}?") },
                                text = {
                                    Text(
                                        "Delete invoice ${targetBill.billNo} for ${targetBill.customerName} (${formatInr(targetBill.allTotal)}) and any payments recorded against it?"
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            viewModel.deleteBill(targetBill, dashboardData) { ok ->
                                                if (ok) {
                                                    if (selectedInvoiceId == targetBill.id) {
                                                        viewModel.openInvoice(null)
                                                    }
                                                    deletingBillTarget = null
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonDue)
                                    ) {
                                        Text("Undo & Delete")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { deletingBillTarget = null }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }
                            )
                        }

                        editingPaymentTarget?.let { targetPayment ->
                            val linkedBillNo = dashboardData.bills
                                .firstOrNull { it.id == targetPayment.billId }?.billNo
                                ?: "Payment"
                            EditPaymentDialog(
                                payment = targetPayment,
                                billNo = linkedBillNo,
                                onDismiss = { editingPaymentTarget = null },
                                onConfirm = { amount, mode, receivedIn, date, note ->
                                    viewModel.updatePayment(
                                        original = targetPayment,
                                        amount = amount,
                                        mode = mode,
                                        receivedIn = receivedIn,
                                        date = date,
                                        note = note
                                    ) { ok ->
                                        if (ok) editingPaymentTarget = null
                                    }
                                }
                            )
                        }

                        deletingPaymentTarget?.let { targetPayment ->
                            val linkedBillNo = dashboardData.bills
                                .firstOrNull { it.id == targetPayment.billId }?.billNo
                                ?: "Customer Ledger"
                            AlertDialog(
                                onDismissRequest = { deletingPaymentTarget = null },
                                title = { Text("Undo Payment?") },
                                text = {
                                    Text(
                                        "Remove payment of ${formatInr(targetPayment.amount)} (${targetPayment.mode}) from $linkedBillNo? The customer's balance will automatically recalculate."
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            viewModel.deletePayment(targetPayment.id) { ok ->
                                                if (ok) deletingPaymentTarget = null
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonDue)
                                    ) {
                                        Text("Undo Payment")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { deletingPaymentTarget = null }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V103EmeraldHeaderBanner(
    bismillahArabic: String,
    headerTitle: String,
    activeSection: String,
    totalOutstanding: Double,
    netProfit: Double,
    cloudStatus: String,
    isSyncing: Boolean,
    biometricEnabled: Boolean,
    canEditAppInfo: Boolean,
    primaryColor: Color,
    deepColor: Color,
    onOpenMonthlyAnalytics: () -> Unit,
    onOpenUsers: () -> Unit,
    onSyncClick: () -> Unit,
    onLockClick: () -> Unit
) {
    val isProfit = netProfit >= 0.0
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
            .background(Brush.linearGradient(listOf(primaryColor, deepColor)))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = size.width * 0.34f,
                center = Offset(size.width * 0.90f, size.height * 0.32f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = size.width * 0.22f,
                center = Offset(size.width * 0.14f, size.height * 0.95f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    IosAppLogoMark(size = 46.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = bismillahArabic,
                            fontSize = 12.sp,
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.25.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            AnimatedContent(
                                targetState = activeSection,
                                transitionSpec = {
                                    (fadeIn(tween(180)) + scaleIn(initialScale = 0.92f, animationSpec = tween(180)))
                                        .togetherWith(fadeOut(tween(120)))
                                },
                                label = "header_section_badge"
                            ) { sectionLabel ->
                                BmwPrecisionBadge(label = sectionLabel)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Clean Frosted Glass Header Dock (Lock & Sync only; P&L Chart, Expenses, Theme live inside app sections)
                val syncSpinTransition = rememberInfiniteTransition(label = "header_sync_spin")
                val syncAngle by syncSpinTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(950, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "sync_rotation"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (biometricEnabled) {
                        HeaderGlassIconButton(
                            icon = Icons.Rounded.Lock,
                            contentDescription = "Lock protected modules",
                            highlighted = false,
                            tag = "topbar_lock_button",
                            onClick = onLockClick
                        )
                    }
                    HeaderGlassIconButton(
                        icon = Icons.Rounded.Sync,
                        contentDescription = stringResource(R.string.sync_now),
                        highlighted = isSyncing,
                        rotationDegrees = if (isSyncing) syncAngle else 0f,
                        tag = "topbar_sync_button",
                        onClick = onSyncClick
                    )
                }
            }

            AnimatedBmwPrecisionStripe(height = 2.dp)

            // Live Ticker & Status Capsules
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.22f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        PulsingLiveDot(
                            color = if (isSyncing) Color(0xFFFDE047) else Color(0xFF4ADE80),
                            size = 6.dp
                        )
                        Text(
                            text = if (isSyncing) "Syncing…" else cloudStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    onClick = onOpenUsers,
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.22f)
                ) {
                    Text(
                        text = "Due ${formatInr(totalOutstanding)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (totalOutstanding > 0.001) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    onClick = onOpenMonthlyAnalytics,
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.22f),
                    modifier = Modifier.testTag("topbar_pnl_pill")
                ) {
                    Text(
                        text = "Net ${if (isProfit) "+" else "-"}${formatInr(abs(netProfit))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isProfit) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (!canEditAppInfo) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = "Info Locked",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderGlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    highlighted: Boolean,
    rotationDegrees: Float = 0f,
    tag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (highlighted) Color.White.copy(alpha = 0.34f)
                else Color.White.copy(alpha = 0.17f)
            )
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .rotate(rotationDegrees)
        )
    }
}

@Composable
private fun V103FloatingBottomBar(
    isWideLayout: Boolean,
    primaryColor: Color,
    isRouteSelected: (AppRoute) -> Boolean,
    onSelectRoute: (AppRoute) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 12.dp,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                V103BottomDestinations.forEach { item ->
                    val selected = isRouteSelected(item.route)
                    val activeAccent = if (item.route == AppRoute.Overview) primaryColor else item.accentColor
                    val labelColor by animateColorAsState(
                        targetValue = if (selected) activeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "nav_label_color"
                    )
                    val displayLabel = if (!isWideLayout && item.label == "Calculator") {
                        "Calc"
                    } else {
                        item.label
                    }
                    val iconSizeDp = if (isWideLayout) 39.dp else 34.dp
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (selected) 18.dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "nav_indicator_width"
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .minimumInteractiveComponentSize()
                            .clickable { onSelectRoute(item.route) }
                            .padding(vertical = 4.dp)
                            .testTag(item.tag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        when (item.route) {
                            AppRoute.Overview -> PremiumHomeIcon(
                                size = iconSizeDp,
                                active = selected,
                                primaryColor = primaryColor
                            )
                            AppRoute.Customers -> PremiumUserIcon(
                                size = iconSizeDp,
                                active = selected
                            )
                            AppRoute.NewBill -> PremiumBillIcon(
                                size = iconSizeDp,
                                active = selected
                            )
                            AppRoute.Payments -> PremiumPayIcon(
                                size = iconSizeDp,
                                active = selected
                            )
                            AppRoute.History -> PremiumHistoryIcon(
                                size = iconSizeDp,
                                active = selected
                            )
                            AppRoute.Calculator -> PremiumCalculatorIcon(
                                size = iconSizeDp,
                                active = selected
                            )
                            AppRoute.Settings -> PremiumSettingsIcon(
                                size = iconSizeDp,
                                active = selected
                            )
                            else -> AnimatedIosNavBadge(
                                icon = item.icon,
                                contentDescription = item.label,
                                selected = selected,
                                activeColor = activeAccent,
                                inactiveBg = MaterialTheme.colorScheme.surfaceVariant,
                                inactiveTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = iconSizeDp,
                                iconSize = if (isWideLayout) 20.dp else 18.dp
                            )
                        }
                        Text(
                            text = displayLabel,
                            fontSize = if (isWideLayout) 11.sp else 9.5.sp,
                            lineHeight = 12.sp,
                            letterSpacing = if (selected) 0.25.sp else 0.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = labelColor,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .width(indicatorWidth)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(activeAccent)
                        )
                    }
                }
            }
        }
    }
}
