package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.BillingRepository
import com.example.data.model.Bill
import com.example.data.model.BillingDashboardData
import com.example.data.model.BusinessSettings
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.LandUnit
import com.example.data.model.Payment
import com.example.data.model.PaymentMode
import com.example.data.model.calculateBigha
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
    val freshResult: Boolean = false
)

class BillingViewModel(
    private val repository: BillingRepository,
    private val currentUserId: String
) : ViewModel() {

    val uiState: StateFlow<UiState<BillingDashboardData>> = combine(
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

    fun saveCustomer(
        existingId: String = "",
        name: String,
        phone: String,
        village: String,
        address: String,
        openingDue: Double,
        landArea: String = "",
        landOwnerType: String = "Own",
        landOwnerName: String = "",
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _cloudStatus.value = "Saving to cloud…"
            val normalizedType = if (landOwnerType.equals("Other", ignoreCase = true)) "Other" else "Own"
            val result = repository.saveCustomer(
                currentUserId,
                Customer(
                    id = existingId,
                    userId = currentUserId,
                    name = name.trim(),
                    phone = phone.trim(),
                    village = village.trim(),
                    address = address.trim(),
                    openingDue = openingDue.coerceAtLeast(0.0),
                    landArea = landArea.trim(),
                    landOwnerType = normalizedType,
                    landOwnerName = if (normalizedType == "Other") landOwnerName.trim() else ""
                )
            )
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Customer saved")
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
            val result = repository.deleteCustomerAndData(
                userId = currentUserId,
                customerId = customer.id,
                billIds = billIds,
                paymentIds = paymentIds
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
        val currentAmount = bigha * ratePerBigha
        val previousDue = (previousDueOverride ?: data.dueForCustomer(customer.id)).coerceAtLeast(0.0)
        val grandTotal = currentAmount + previousDue

        if (grandTotal <= 0.0) {
            _toastMessages.tryEmit("Enter a valid land quantity, rate, or previous due")
            return
        }

        val billNo = "ASHA-${Year.now().value}-${(data.bills.size + 1).toString().padStart(4, '0')}"
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
            rate = ratePerBigha,
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
                initialPaid = initialPaid,
                initialMode = initialMode,
                initialReceivedIn = initialReceivedIn
            )
            if (result.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Invoice saved successfully")
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
        val bigha = calculateBigha(landAmount, unit)
        val currentAmount = bigha * ratePerBigha
        val prevDue = manualPreviousDue.coerceAtLeast(0.0)
        val grandTotal = currentAmount + prevDue
        if (grandTotal <= 0.0) {
            _toastMessages.tryEmit("Enter a valid land quantity, rate, or previous due")
            return
        }

        viewModelScope.launch {
            _cloudStatus.value = "Saving customer & bill…"
            val customerResult = repository.saveCustomer(
                userId = currentUserId,
                customer = Customer(
                    userId = currentUserId,
                    name = cleanName,
                    phone = phone.trim(),
                    village = village.trim(),
                    address = address.trim(),
                    openingDue = 0.0
                )
            )
            val newCustomerId = customerResult.getOrNull()
            if (customerResult.isFailure || newCustomerId.isNullOrBlank()) {
                _cloudStatus.value = "Cloud save failed"
                _toastMessages.tryEmit(
                    customerResult.exceptionOrNull()?.localizedMessage ?: "Failed to save new customer"
                )
                return@launch
            }

            val billNo = "ASHA-${Year.now().value}-${(data.bills.size + 1).toString().padStart(4, '0')}"
            val bill = Bill(
                userId = currentUserId,
                billNo = billNo,
                date = date.ifBlank { todayIsoDate() },
                customerId = newCustomerId,
                customerName = cleanName,
                phone = phone.trim(),
                address = address.trim(),
                village = village.trim(),
                season = season.trim(),
                landAmount = landAmount,
                unit = unit.key,
                bigha = bigha,
                rate = ratePerBigha,
                current = currentAmount,
                previousDue = prevDue,
                allTotal = grandTotal,
                note = note.trim()
            )
            val billResult = repository.createBill(
                userId = currentUserId,
                bill = bill,
                initialPaid = initialPaid,
                initialMode = initialMode,
                initialReceivedIn = initialReceivedIn
            )
            if (billResult.isSuccess) {
                _cloudStatus.value = "Cloud synced"
                _toastMessages.tryEmit("Customer & invoice saved")
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
        if (amount <= 0.0 || amount > currentDue + 0.001) {
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
                amount = amount,
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

    fun onCalcKey(key: String) {
        val current = _calcState.value
        when (key) {
            "AC" -> {
                _calcState.value = CalculatorUiState()
            }
            "⌫" -> {
                val nextExpr = if (current.freshResult) "" else current.expression.dropLast(1)
                _calcState.value = CalculatorUiState(
                    expression = nextExpr,
                    historyLine = if (current.freshResult) "" else current.historyLine,
                    display = nextExpr.ifEmpty { "0" },
                    freshResult = false
                )
            }
            "=" -> {
                if (current.expression.isBlank()) return
                try {
                    val value = SafeCalculator.evaluate(current.expression)
                    val formatted = SafeCalculator.formatResult(value)
                    _calcState.value = CalculatorUiState(
                        expression = formatted,
                        historyLine = "${current.expression} =",
                        display = formatted,
                        freshResult = true
                    )
                } catch (e: IllegalArgumentException) {
                    _calcState.value = current.copy(
                        display = e.message ?: "Error",
                        freshResult = true
                    )
                }
            }
            else -> {
                val isOperator = key in setOf("+", "−", "×", "÷", "%")
                val baseExpr = if (current.freshResult && !isOperator) "" else current.expression
                val nextExpr = baseExpr + key
                _calcState.value = CalculatorUiState(
                    expression = nextExpr,
                    historyLine = if (current.freshResult && !isOperator) "" else current.historyLine,
                    display = nextExpr,
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
