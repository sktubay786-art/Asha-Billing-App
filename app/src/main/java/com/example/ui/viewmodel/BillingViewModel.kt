package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.OwnerAccessControl
import com.example.data.firebase.BillingRepository
import com.example.data.model.Bill
import com.example.data.model.BillingDashboardData
import com.example.data.model.BusinessSettings
import com.example.data.model.ChatMessage
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.LandUnit
import com.example.data.model.Payment
import com.example.data.model.PaymentMode
import com.example.data.model.WbsedclBillPayment
import com.example.data.model.WbsedclConsumer
import com.example.data.model.calculateBigha
import com.example.data.model.formatInr
import com.example.data.model.roundMoney
import com.example.data.model.todayIsoDate
import com.example.util.SafeCalculator
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Year

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class CalculatorUiState(
    val expression: String = "",
    val historyLine: String = "",
    val display: String = "0",
    val livePreview: String = "",
    val freshResult: Boolean = false,
    val tape: List<Pair<String, String>> = emptyList(),
    val memoryValue: Double = 0.0
)

class BillingViewModel(
    private val repository: BillingRepository,
    private val currentUserId: String
) : ViewModel() {

    val uiState: StateFlow<UiState<BillingDashboardData>> = combine(
        combine(
            repository.observeSettings(currentUserId),
            repository.observeCustomers(currentUserId),
            repository.observeBills(currentUserId),
            repository.observePayments(currentUserId),
            repository.observeExpenses(currentUserId)
        ) { settings, customers, bills, payments, expenses ->
            BillingDashboardData(
                settings = settings,
                customers = customers,
                bills = bills,
                payments = payments,
                expenses = expenses
            )
        },
        repository.observeChatMessages(currentUserId),
        repository.observeWbsedclConsumers(currentUserId),
        repository.observeWbsedclPayments(currentUserId)
    ) { partial, chatMessages, wbsedclConsumers, wbsedclPayments ->
        partial.copy(
            chatMessages = chatMessages,
            wbsedclConsumers = wbsedclConsumers,
            wbsedclPayments = wbsedclPayments
        )
    }
        .map<BillingDashboardData, UiState<BillingDashboardData>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing billing data", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Failed to load billing workspace."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toastMessages = _toastMessages.asSharedFlow()

    private val _cloudStatus = MutableStateFlow("Cloud synced")
    val cloudStatus: StateFlow<String> = _cloudStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _selectedInvoiceId = MutableStateFlow<String?>(null)
    val selectedInvoiceId: StateFlow<String?> = _selectedInvoiceId.asStateFlow()

    private val _preselectedCustomerId = MutableStateFlow<String?>(null)
    val preselectedCustomerId: StateFlow<String?> = _preselectedCustomerId.asStateFlow()

    private val _preselectedBillId = MutableStateFlow<String?>(null)
    val preselectedBillId: StateFlow<String?> = _preselectedBillId.asStateFlow()

    private val _calcState = MutableStateFlow(CalculatorUiState())
    val calcState: StateFlow<CalculatorUiState> = _calcState.asStateFlow()

    fun syncOfflineState(manualOffline: Boolean, deviceOnline: Boolean) {
        viewModelScope.launch {
            repository.setOfflineMode(manualOffline = manualOffline, deviceOnline = deviceOnline)
            _cloudStatus.value = when {
                manualOffline -> "Offline Mode · Local cache"
                !deviceOnline -> "No internet · Offline cache"
                else -> "Cloud synced"
            }
        }
    }

    fun openInvoice(billId: String?) {
        _selectedInvoiceId.value = billId
    }

    fun selectCustomerForNewBill(customerId: String) {
        _preselectedCustomerId.value = customerId
    }

    fun consumePreselectedCustomer(): String? {
        val id = _preselectedCustomerId.value
        _preselectedCustomerId.value = null
        return id
    }

    fun selectBillForPayment(billId: String) {
        _preselectedBillId.value = billId
    }

    fun consumePreselectedBill(): String? {
        val id = _preselectedBillId.value
        _preselectedBillId.value = null
        return id
    }

    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _cloudStatus.value = "Syncing with cloud…"
            val result = repository.forceSync(currentUserId)
            _isSyncing.value = false
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Cloud data synced")
            } else {
                _cloudStatus.value = "Sync failed"
                _toastMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Cloud sync failed"
                )
            }
        }
    }

    fun nextBillNumber(bills: List<Bill>): String {
        val year = Year.now().value
        val regex = Regex("""^ASHA-$year-(\d+)$""")
        val maxSeq = bills
            .mapNotNull { regex.matchEntire(it.billNo.trim())?.groupValues?.getOrNull(1)?.toIntOrNull() }
            .maxOrNull() ?: 0
        return "ASHA-$year-${(maxSeq + 1).toString().padStart(4, '0')}"
    }

    fun findMatchingCustomer(
        customers: List<Customer>,
        name: String,
        phone: String,
        village: String
    ): Customer? {
        val phone10 = OwnerAccessControl.normalizeIndianPhone10(phone)
        if (phone10.length == 10) {
            customers.firstOrNull {
                OwnerAccessControl.normalizeIndianPhone10(it.phone) == phone10
            }?.let { return it }
        }
        val cleanName = name.trim().lowercase()
        val cleanVillage = village.trim().lowercase()
        if (cleanName.isBlank()) return null
        val nameMatches = customers.filter { it.name.trim().lowercase() == cleanName }
        if (nameMatches.isEmpty()) return null
        if (cleanVillage.isNotBlank()) {
            nameMatches.firstOrNull { it.village.trim().lowercase() == cleanVillage }?.let { return it }
            nameMatches.firstOrNull { it.village.isBlank() }?.let { return it }
            return null
        }
        return if (nameMatches.size == 1) nameMatches.first() else null
    }

    fun saveCustomer(
        existingId: String = "",
        name: String,
        phone: String,
        village: String,
        address: String,
        openingDue: Double,
        landArea: String = "",
        landUnit: LandUnit = LandUnit.DECIMAL,
        landOwnerType: String = "Own",
        landOwnerName: String = "",
        existingCustomers: List<Customer> = (uiState.value as? UiState.Success)?.data?.customers.orEmpty(),
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _cloudStatus.value = "Saving to cloud…"
            val normalizedType = if (landOwnerType.equals("Other", ignoreCase = true)) "Other" else "Own"
            val matchedExisting = if (existingId.isBlank()) {
                findMatchingCustomer(existingCustomers, name, phone, village)
            } else {
                null
            }
            val targetId = existingId.ifBlank { matchedExisting?.id.orEmpty() }
            val mergedOpening = if (existingId.isBlank() && matchedExisting != null) {
                roundMoney(matchedExisting.openingDue + openingDue.coerceAtLeast(0.0))
            } else {
                roundMoney(openingDue.coerceAtLeast(0.0))
            }

            val result = repository.saveCustomer(
                currentUserId,
                Customer(
                    id = targetId,
                    userId = currentUserId,
                    name = name.trim(),
                    phone = phone.trim().ifBlank { matchedExisting?.phone.orEmpty() },
                    village = village.trim().ifBlank { matchedExisting?.village.orEmpty() },
                    address = address.trim().ifBlank { matchedExisting?.address.orEmpty() },
                    openingDue = mergedOpening,
                    landArea = landArea.trim().ifBlank { matchedExisting?.landArea.orEmpty() },
                    landUnit = if (landArea.isNotBlank()) landUnit.key else (matchedExisting?.landUnit ?: landUnit.key),
                    landOwnerType = normalizedType,
                    landOwnerName = if (normalizedType == "Other") {
                        landOwnerName.trim().ifBlank { matchedExisting?.landOwnerName.orEmpty() }
                    } else ""
                )
            )
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit(
                    if (matchedExisting != null && existingId.isBlank()) {
                        "Updated existing user: ${matchedExisting.name}"
                    } else {
                        "Customer saved"
                    }
                )
                onComplete(true)
            } else {
                _cloudStatus.value = "Cloud save failed"
                _toastMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to save customer"
                )
                onComplete(false)
            }
        }
    }

    fun deleteCustomer(customer: Customer, data: BillingDashboardData) {
        viewModelScope.launch {
            _cloudStatus.value = "Deleting…"
            val custBills = data.bills.filter { it.customerId == customer.id }
            val billIds = custBills.map { it.id }
            val paymentIds = data.payments.filter { it.customerId == customer.id }.map { it.id }
            val chatMessageIds = data.chatMessages.filter { it.customerId == customer.id }.map { it.id }
            val result = repository.deleteCustomerAndData(
                userId = currentUserId,
                customerId = customer.id,
                billIds = billIds,
                paymentIds = paymentIds,
                chatMessageIds = chatMessageIds
            )
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Customer and invoices deleted")
            } else {
                _cloudStatus.value = "Delete failed"
                _toastMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to delete customer"
                )
            }
        }
    }

    fun createBill(
        data: BillingDashboardData,
        customer: Customer,
        season: String,
        date: String,
        landAmount: Double,
        unit: LandUnit,
        ratePerBigha: Double,
        initialPaid: Double,
        initialMode: PaymentMode = PaymentMode.CASH,
        initialReceivedIn: String = PaymentMode.CASH.label,
        note: String,
        previousDueOverride: Double? = null,
        onCreated: (billId: String, billNo: String) -> Unit
    ) {
        val bigha = calculateBigha(landAmount, unit)
        val currentAmount = roundMoney(bigha * ratePerBigha)
        val previousDue = roundMoney((previousDueOverride ?: data.dueForCustomer(customer.id)).coerceAtLeast(0.0))
        val grandTotal = roundMoney(currentAmount + previousDue)

        if (grandTotal <= 0.0) {
            _toastMessages.tryEmit("Enter a valid land quantity, rate, or previous due")
            return
        }

        val billNo = nextBillNumber(data.bills)
        val bill = Bill(
            userId = currentUserId,
            billNo = billNo,
            date = date.ifBlank { todayIsoDate() },
            customerId = customer.id,
            customerName = customer.name,
            phone = customer.phone,
            address = customer.address,
            village = customer.village,
            season = season.trim(),
            landAmount = landAmount,
            unit = unit.key,
            bigha = bigha,
            rate = roundMoney(ratePerBigha),
            current = currentAmount,
            previousDue = previousDue,
            allTotal = grandTotal,
            note = note.trim()
        )

        viewModelScope.launch {
            _cloudStatus.value = "Saving bill…"
            val result = repository.createBill(
                userId = currentUserId,
                bill = bill,
                initialPaid = roundMoney(initialPaid),
                initialMode = initialMode,
                initialReceivedIn = initialReceivedIn
            )
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Invoice saved ($billNo)")
                val newId = result.getOrNull().orEmpty()
                if (newId.isNotBlank()) {
                    _selectedInvoiceId.value = newId
                    onCreated(newId, billNo)
                }
            } else {
                _cloudStatus.value = "Cloud save failed"
                _toastMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to save invoice"
                )
            }
        }
    }

    fun createManualCustomerAndBill(
        data: BillingDashboardData,
        customerName: String,
        phone: String,
        village: String,
        address: String,
        manualPreviousDue: Double,
        season: String,
        date: String,
        landAmount: Double,
        unit: LandUnit,
        ratePerBigha: Double,
        initialPaid: Double,
        initialMode: PaymentMode = PaymentMode.CASH,
        initialReceivedIn: String = PaymentMode.CASH.label,
        note: String,
        onCreated: (billId: String, billNo: String) -> Unit
    ) {
        val cleanName = customerName.trim()
        if (cleanName.isEmpty()) {
            _toastMessages.tryEmit("Enter customer name or select an existing customer")
            return
        }
        val matchedCustomer = findMatchingCustomer(data.customers, cleanName, phone, village)
        val bigha = calculateBigha(landAmount, unit)
        val currentAmount = roundMoney(bigha * ratePerBigha)
        val prevDue = if (matchedCustomer != null && manualPreviousDue <= 0.001) {
            roundMoney(data.dueForCustomer(matchedCustomer.id))
        } else {
            roundMoney(manualPreviousDue.coerceAtLeast(0.0))
        }
        val grandTotal = roundMoney(currentAmount + prevDue)
        if (grandTotal <= 0.0) {
            _toastMessages.tryEmit("Enter a valid land quantity, rate, or previous due")
            return
        }

        viewModelScope.launch {
            _cloudStatus.value = if (matchedCustomer != null) {
                "Linking to ${matchedCustomer.name}…"
            } else {
                "Saving customer & bill…"
            }

            val resolvedPhone = phone.trim().ifBlank { matchedCustomer?.phone.orEmpty() }
            val resolvedVillage = village.trim().ifBlank { matchedCustomer?.village.orEmpty() }
            val resolvedAddress = address.trim().ifBlank { matchedCustomer?.address.orEmpty() }

            val customerResult = repository.saveCustomer(
                userId = currentUserId,
                customer = Customer(
                    id = matchedCustomer?.id.orEmpty(),
                    userId = currentUserId,
                    name = matchedCustomer?.name ?: cleanName,
                    phone = resolvedPhone,
                    village = resolvedVillage,
                    address = resolvedAddress,
                    openingDue = matchedCustomer?.openingDue ?: 0.0,
                    landArea = matchedCustomer?.landArea.orEmpty(),
                    landUnit = matchedCustomer?.landUnit ?: unit.key,
                    landOwnerType = matchedCustomer?.landOwnerType ?: "Own",
                    landOwnerName = matchedCustomer?.landOwnerName.orEmpty()
                )
            )
            val resolvedCustomerId = customerResult.getOrNull()
            if (customerResult.isFailure || resolvedCustomerId.isNullOrBlank()) {
                _cloudStatus.value = "Cloud save failed"
                _toastMessages.tryEmit(
                    customerResult.exceptionOrNull()?.localizedMessage ?: "Failed to save customer"
                )
                return@launch
            }

            val billNo = nextBillNumber(data.bills)
            val bill = Bill(
                userId = currentUserId,
                billNo = billNo,
                date = date.ifBlank { todayIsoDate() },
                customerId = resolvedCustomerId,
                customerName = matchedCustomer?.name ?: cleanName,
                phone = resolvedPhone,
                address = resolvedAddress,
                village = resolvedVillage,
                season = season.trim(),
                landAmount = landAmount,
                unit = unit.key,
                bigha = bigha,
                rate = roundMoney(ratePerBigha),
                current = currentAmount,
                previousDue = prevDue,
                allTotal = grandTotal,
                note = note.trim()
            )
            val billResult = repository.createBill(
                userId = currentUserId,
                bill = bill,
                initialPaid = roundMoney(initialPaid),
                initialMode = initialMode,
                initialReceivedIn = initialReceivedIn
            )
            if (billResult.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit(
                    if (matchedCustomer != null) {
                        "Added $billNo to existing user ${matchedCustomer.name}"
                    } else {
                        "Customer & invoice saved ($billNo)"
                    }
                )
                val newId = billResult.getOrNull().orEmpty()
                if (newId.isNotBlank()) {
                    _selectedInvoiceId.value = newId
                    onCreated(newId, billNo)
                }
            } else {
                _cloudStatus.value = "Cloud save failed"
                _toastMessages.tryEmit(
                    billResult.exceptionOrNull()?.localizedMessage ?: "Failed to save invoice"
                )
            }
        }
    }

    fun updateBill(
        original: Bill,
        season: String,
        date: String,
        landAmount: Double,
        unit: LandUnit,
        ratePerBigha: Double,
        previousDue: Double,
        note: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val cleanLand = landAmount.coerceAtLeast(0.0)
        val cleanRate = roundMoney(ratePerBigha.coerceAtLeast(0.0))
        val cleanPrev = roundMoney(previousDue.coerceAtLeast(0.0))
        val bigha = calculateBigha(cleanLand, unit)
        val currentAmount = roundMoney(bigha * cleanRate)
        val grandTotal = roundMoney(currentAmount + cleanPrev)
        if (grandTotal <= 0.0) {
            _toastMessages.tryEmit("Total bill amount must be greater than ₹0")
            onComplete(false)
            return
        }
        val updated = original.copy(
            date = date.ifBlank { original.date },
            season = season.trim(),
            landAmount = cleanLand,
            unit = unit.key,
            bigha = bigha,
            rate = cleanRate,
            current = currentAmount,
            previousDue = cleanPrev,
            allTotal = grandTotal,
            note = note.trim()
        )
        viewModelScope.launch {
            _cloudStatus.value = "Updating bill…"
            val result = repository.updateBill(currentUserId, updated)
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Invoice ${original.billNo} updated")
                onComplete(true)
            } else {
                _cloudStatus.value = "Update failed"
                _toastMessages.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to update bill")
                onComplete(false)
            }
        }
    }

    fun deleteBill(bill: Bill, data: BillingDashboardData, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _cloudStatus.value = "Undoing bill…"
            val paymentIds = data.payments.filter { it.billId == bill.id }.map { it.id }
            val result = repository.deleteBillAndPayments(currentUserId, bill.id, paymentIds)
            if (result.isSuccess) {
                if (_selectedInvoiceId.value == bill.id) {
                    _selectedInvoiceId.value = null
                }
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Invoice ${bill.billNo} deleted")
                onComplete(true)
            } else {
                _cloudStatus.value = "Delete failed"
                _toastMessages.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to delete invoice")
                onComplete(false)
            }
        }
    }

    fun recordPayment(
        bill: Bill,
        amount: Double,
        currentDue: Double,
        mode: PaymentMode,
        receivedIn: String,
        date: String,
        note: String,
        onSaved: () -> Unit
    ) {
        val cleanAmt = roundMoney(amount)
        if (cleanAmt <= 0.0 || cleanAmt > currentDue + 0.01) {
            _toastMessages.tryEmit("Enter an amount within the outstanding balance")
            return
        }
        viewModelScope.launch {
            _cloudStatus.value = "Recording payment…"
            val payment = Payment(
                userId = currentUserId,
                billId = bill.id,
                customerId = bill.customerId,
                date = date.ifBlank { todayIsoDate() },
                amount = cleanAmt,
                mode = mode.label,
                receivedIn = receivedIn.trim().ifBlank { mode.label },
                note = note.trim()
            )
            val result = repository.recordPayment(currentUserId, payment)
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Payment recorded")
                onSaved()
            } else {
                _cloudStatus.value = "Cloud save failed"
                _toastMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to save payment"
                )
            }
        }
    }

    fun settleBillWithDiscount(
        bill: Bill,
        cashReceived: Double,
        currentDue: Double,
        mode: PaymentMode,
        receivedIn: String,
        date: String,
        note: String,
        onSettled: () -> Unit
    ) {
        val cleanDue = roundMoney(currentDue)
        val cleanCash = roundMoney(cashReceived)
        if (cleanDue <= 0.001) {
            _toastMessages.tryEmit("This bill is already paid")
            return
        }
        if (cleanCash < 0.0 || cleanCash > cleanDue + 0.01) {
            _toastMessages.tryEmit("Enter a settlement cash amount between ₹0 and ${formatInr(cleanDue)}")
            return
        }
        val clampedCash = cleanCash.coerceAtMost(cleanDue)
        val discountWaived = roundMoney((cleanDue - clampedCash).coerceAtLeast(0.0))

        viewModelScope.launch {
            _cloudStatus.value = "Settling bill…"
            val result = repository.settleBillWithDiscount(
                userId = currentUserId,
                bill = bill,
                cashReceived = clampedCash,
                discountWaived = discountWaived,
                mode = mode,
                receivedIn = receivedIn,
                date = date,
                note = note
            )
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                val msg = if (discountWaived > 0.001) {
                    "Settled ${bill.billNo}: Collected ${formatInr(clampedCash)} · Waived ${formatInr(discountWaived)} · Due ₹0"
                } else {
                    "Bill ${bill.billNo} fully paid (${formatInr(clampedCash)})"
                }
                _toastMessages.tryEmit(msg)
                onSettled()
            } else {
                _cloudStatus.value = "Cloud save failed"
                _toastMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to settle bill"
                )
            }
        }
    }

    fun updatePayment(
        original: Payment,
        amount: Double,
        mode: PaymentMode,
        receivedIn: String,
        date: String,
        note: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val cleanAmt = roundMoney(amount)
        if (cleanAmt <= 0.0) {
            _toastMessages.tryEmit("Enter a valid payment amount greater than ₹0")
            onComplete(false)
            return
        }
        val updated = original.copy(
            date = date.ifBlank { original.date },
            amount = cleanAmt,
            mode = mode.label,
            receivedIn = receivedIn.trim().ifBlank { mode.label },
            note = note.trim()
        )
        viewModelScope.launch {
            _cloudStatus.value = "Updating payment…"
            val result = repository.updatePayment(currentUserId, updated)
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Payment updated")
                onComplete(true)
            } else {
                _cloudStatus.value = "Update failed"
                _toastMessages.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to update payment")
                onComplete(false)
            }
        }
    }

    fun deletePayment(paymentId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _cloudStatus.value = "Undoing payment…"
            val result = repository.deletePayment(currentUserId, paymentId)
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Payment undone & removed")
                onComplete(true)
            } else {
                _cloudStatus.value = "Undo failed"
                _toastMessages.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to undo payment")
                onComplete(false)
            }
        }
    }

    fun sendChatMessage(customerId: String, text: String, onSent: () -> Unit = {}) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val result = repository.sendChatMessage(
                userId = currentUserId,
                message = ChatMessage(
                    userId = currentUserId,
                    customerId = customerId,
                    text = trimmed,
                    sender = "ME",
                    date = todayIsoDate()
                )
            )
            if (result.isSuccess) {
                onSent()
            } else {
                _toastMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to send message"
                )
            }
        }
    }

    fun updateChatMessage(messageId: String, newText: String, onComplete: (Boolean) -> Unit = {}) {
        if (newText.trim().isEmpty()) return
        viewModelScope.launch {
            val result = repository.updateChatMessage(currentUserId, messageId, newText.trim())
            if (result.isSuccess) {
                _toastMessages.tryEmit("Message updated")
                onComplete(true)
            } else {
                _toastMessages.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to update message")
                onComplete(false)
            }
        }
    }

    fun deleteChatMessage(messageId: String) {
        viewModelScope.launch {
            val result = repository.deleteChatMessage(currentUserId, messageId)
            if (result.isSuccess) {
                _toastMessages.tryEmit("Message deleted")
            } else {
                _toastMessages.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to delete message")
            }
        }
    }

    fun settleCustomer(
        customer: Customer,
        amount: Double,
        data: BillingDashboardData,
        onSettled: () -> Unit
    ) {
        val totalDue = data.dueForCustomer(customer.id)
        if (amount <= 0.0 || amount > totalDue + 0.001) {
            _toastMessages.tryEmit("Settlement amount is invalid")
            return
        }
        val dueBills = data.bills
            .filter { it.customerId == customer.id }
            .map { it to data.dueForBill(it) }
            .filter { it.second > 0.001 }

        viewModelScope.launch {
            _cloudStatus.value = "Recording settlement…"
            val result = repository.settleCustomer(
                userId = currentUserId,
                customerId = customer.id,
                amount = amount,
                dueBillsWithDue = dueBills,
                currentOpeningDue = customer.openingDue
            )
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Settlement adjustment recorded")
                onSettled()
            } else {
                _cloudStatus.value = "Cloud save failed"
                _toastMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to settle customer"
                )
            }
        }
    }

    fun saveSettings(settings: BusinessSettings) {
        viewModelScope.launch {
            _cloudStatus.value = "Saving settings…"
            val result = repository.saveSettings(currentUserId, settings)
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Business profile updated")
            } else {
                _cloudStatus.value = "Cloud save failed"
                _toastMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to update settings"
                )
            }
        }
    }

    fun saveExpense(
        existingId: String = "",
        title: String,
        category: ExpenseCategory,
        amount: Double,
        date: String,
        paidMode: PaymentMode,
        note: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (title.trim().isEmpty() || amount <= 0.0) {
            _toastMessages.tryEmit("Enter a valid expense title and amount")
            onComplete(false)
            return
        }
        viewModelScope.launch {
            _cloudStatus.value = "Saving expense…"
            val expense = Expense(
                id = existingId,
                userId = currentUserId,
                title = title.trim(),
                category = category.label,
                amount = amount,
                date = date.ifBlank { todayIsoDate() },
                paidMode = if (paidMode == PaymentMode.SETTLEMENT) PaymentMode.OTHER.label else paidMode.label,
                note = note.trim()
            )
            val result = repository.saveExpense(currentUserId, expense)
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Expense logged")
                onComplete(true)
            } else {
                _cloudStatus.value = "Cloud save failed"
                _toastMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to save expense"
                )
                onComplete(false)
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            _cloudStatus.value = "Deleting expense…"
            val result = repository.deleteExpense(currentUserId, expenseId)
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Expense deleted")
            } else {
                _cloudStatus.value = "Delete failed"
                _toastMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to delete expense"
                )
            }
        }
    }

    fun saveWbsedclConsumer(
        existingId: String = "",
        label: String,
        consumerNo: String,
        installationNote: String = "",
        onComplete: (Boolean) -> Unit = {}
    ) {
        val cleanLabel = label.trim()
        val cleanConsumerNo = consumerNo.trim()
        if (cleanLabel.isEmpty() || cleanConsumerNo.isEmpty()) {
            _toastMessages.tryEmit("Enter both a label and WBSEDCL Consumer ID")
            onComplete(false)
            return
        }
        viewModelScope.launch {
            _cloudStatus.value = "Saving Consumer ID…"
            val result = repository.saveWbsedclConsumer(
                currentUserId,
                WbsedclConsumer(
                    id = existingId,
                    userId = currentUserId,
                    label = cleanLabel,
                    consumerNo = cleanConsumerNo,
                    installationNote = installationNote.trim()
                )
            )
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit(
                    if (existingId.isBlank()) "Consumer ID saved" else "Consumer ID updated"
                )
                onComplete(true)
            } else {
                _cloudStatus.value = "Cloud save failed"
                _toastMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to save Consumer ID"
                )
                onComplete(false)
            }
        }
    }

    fun deleteWbsedclConsumer(consumerId: String) {
        viewModelScope.launch {
            _cloudStatus.value = "Deleting Consumer ID…"
            val result = repository.deleteWbsedclConsumer(currentUserId, consumerId)
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Consumer ID removed")
            } else {
                _cloudStatus.value = "Delete failed"
                _toastMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to remove Consumer ID"
                )
            }
        }
    }

    fun saveWbsedclPayment(
        existingId: String = "",
        consumerId: String = "",
        consumerNo: String,
        consumerLabel: String = "",
        billMonth: String,
        amountPaid: Double,
        date: String,
        note: String = "",
        onComplete: (Boolean) -> Unit = {}
    ) {
        val cleanConsumerNo = consumerNo.trim()
        val cleanMonth = billMonth.trim()
        val cleanAmount = roundMoney(amountPaid)
        if (cleanConsumerNo.isEmpty() || cleanMonth.isEmpty() || cleanAmount <= 0.0) {
            _toastMessages.tryEmit("Enter Consumer ID, Bill Month, and valid amount")
            onComplete(false)
            return
        }
        viewModelScope.launch {
            _cloudStatus.value = "Saving WBSEDCL payment…"
            val result = repository.saveWbsedclPayment(
                currentUserId,
                WbsedclBillPayment(
                    id = existingId,
                    userId = currentUserId,
                    consumerNo = cleanConsumerNo,
                    consumerLabel = consumerLabel.trim(),
                    billMonth = cleanMonth,
                    amount = cleanAmount,
                    date = date.ifBlank { todayIsoDate() },
                    note = note.trim()
                )
            )
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit(
                    if (existingId.isBlank()) "Electricity bill payment saved" else "Electricity payment updated"
                )
                onComplete(true)
            } else {
                _cloudStatus.value = "Cloud save failed"
                _toastMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to save electricity payment"
                )
                onComplete(false)
            }
        }
    }

    fun deleteWbsedclPayment(paymentId: String) {
        viewModelScope.launch {
            _cloudStatus.value = "Deleting payment…"
            val result = repository.deleteWbsedclPayment(currentUserId, paymentId)
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Electricity payment deleted")
            } else {
                _cloudStatus.value = "Delete failed"
                _toastMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to delete payment"
                )
            }
        }
    }

    fun onCalcKey(key: String) {
        val current = _calcState.value
        val binaryOps = setOf("+", "−", "×", "÷", "%")

        fun computeLivePreview(expr: String): String {
            if (expr.isBlank()) return ""
            val hasOp = expr.any { it in setOf('+', '−', '×', '÷', '%', '(') }
            if (!hasOp) return ""
            return try {
                val value = SafeCalculator.evaluate(expr)
                SafeCalculator.formatResult(value)
            } catch (_: Exception) {
                ""
            }
        }

        fun currentNumericValue(): Double? {
            return try {
                if (current.expression.isNotBlank()) {
                    SafeCalculator.evaluate(current.expression)
                } else {
                    current.display.toDoubleOrNull()
                }
            } catch (_: Exception) {
                current.livePreview.toDoubleOrNull() ?: current.display.toDoubleOrNull()
            }
        }

        when {
            key == "AC" -> {
                _calcState.value = current.copy(
                    expression = "",
                    historyLine = "",
                    display = "0",
                    livePreview = "",
                    freshResult = false
                )
            }
            key == "CLEAR_TAPE" -> {
                _calcState.value = current.copy(tape = emptyList())
            }
            key == "MC" -> {
                _calcState.value = current.copy(memoryValue = 0.0)
            }
            key == "MR" -> {
                val formattedMem = SafeCalculator.formatResult(current.memoryValue)
                val nextExpr = if (current.freshResult || current.expression.isBlank()) {
                    formattedMem
                } else if (current.expression.last().toString() in binaryOps || current.expression.last() == '(') {
                    current.expression + formattedMem
                } else {
                    formattedMem
                }
                _calcState.value = current.copy(
                    expression = nextExpr,
                    display = nextExpr,
                    livePreview = computeLivePreview(nextExpr),
                    freshResult = false
                )
            }
            key == "M+" -> {
                val v = currentNumericValue() ?: return
                _calcState.value = current.copy(memoryValue = roundMoney(current.memoryValue + v))
            }
            key == "M-" -> {
                val v = currentNumericValue() ?: return
                _calcState.value = current.copy(memoryValue = roundMoney(current.memoryValue - v))
            }
            key == "±" -> {
                val v = currentNumericValue() ?: return
                val negated = SafeCalculator.formatResult(-v)
                _calcState.value = current.copy(
                    expression = negated,
                    display = negated,
                    livePreview = "",
                    freshResult = false
                )
            }
            key.startsWith("USE:") -> {
                val raw = key.removePrefix("USE:").trim()
                if (raw.isEmpty()) return
                val nextExpr = if (current.expression.isNotBlank() && !current.freshResult &&
                    (current.expression.last().toString() in binaryOps || current.expression.last() == '(')
                ) {
                    current.expression + raw
                } else {
                    raw
                }
                _calcState.value = current.copy(
                    expression = nextExpr,
                    display = nextExpr,
                    livePreview = computeLivePreview(nextExpr),
                    freshResult = false
                )
            }
            key == "⌫" -> {
                val nextExpr = if (current.freshResult) "" else current.expression.dropLast(1)
                _calcState.value = current.copy(
                    expression = nextExpr,
                    historyLine = if (current.freshResult) "" else current.historyLine,
                    display = nextExpr.ifEmpty { "0" },
                    livePreview = computeLivePreview(nextExpr),
                    freshResult = false
                )
            }
            key == "=" -> {
                if (current.expression.isBlank()) return
                try {
                    val value = SafeCalculator.evaluate(current.expression)
                    val formatted = SafeCalculator.formatResult(value)
                    val updatedTape = (listOf(current.expression to formatted) + current.tape).take(12)
                    _calcState.value = current.copy(
                        expression = formatted,
                        historyLine = "${current.expression} =",
                        display = formatted,
                        livePreview = "",
                        freshResult = true,
                        tape = updatedTape
                    )
                } catch (e: IllegalArgumentException) {
                    _calcState.value = current.copy(
                        display = e.message ?: "Error",
                        livePreview = "",
                        freshResult = true
                    )
                }
            }
            else -> {
                val isOperator = key in binaryOps
                val baseExpr = if (current.freshResult && !isOperator) "" else current.expression
                val nextExpr = when {
                    isOperator && baseExpr.isEmpty() -> {
                        if (key == "−") "−" else return
                    }
                    isOperator && baseExpr.last().toString() in binaryOps -> {
                        baseExpr.dropLast(1) + key
                    }
                    key == "00" && (baseExpr.isEmpty() || baseExpr == "0") -> "0"
                    key == "." && baseExpr.takeLastWhile { it.isDigit() || it == '.' }.contains('.') -> baseExpr
                    else -> baseExpr + key
                }
                _calcState.value = current.copy(
                    expression = nextExpr,
                    historyLine = if (current.freshResult && !isOperator) "" else current.historyLine,
                    display = nextExpr.ifEmpty { "0" },
                    livePreview = computeLivePreview(nextExpr),
                    freshResult = false
                )
            }
        }
    }

    private companion object {
        const val TAG = "BillingVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
