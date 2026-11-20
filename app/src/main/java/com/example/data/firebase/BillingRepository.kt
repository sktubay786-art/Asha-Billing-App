package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.auth.OwnerAccessControl
import com.example.data.model.Bill
import com.example.data.model.BusinessSettings
import com.example.data.model.ChatMessage
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.Payment
import com.example.data.model.PaymentMode
import com.example.data.model.WbsedclBillPayment
import com.example.data.model.WbsedclConsumer
import com.example.data.model.roundMoney
import com.example.data.model.todayIsoDate
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max
import kotlin.math.min

class BillingRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    @Volatile
    private var offlineActive: Boolean = false

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    suspend fun setOfflineMode(manualOffline: Boolean, deviceOnline: Boolean) {
        offlineActive = manualOffline || !deviceOnline
        runCatching {
            if (manualOffline) {
                db.disableNetwork().await()
            } else if (deviceOnline) {
                db.enableNetwork().await()
            }
        }
    }

    private suspend fun <T> Task<T>.awaitWrite(): T? {
        return if (offlineActive) {
            null
        } else {
            withTimeoutOrNull(2500L) { await() }
        }
    }

    private fun requireAuthorizedUid(expectedUserId: String): String {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
        val actualWorkspaceId = OwnerAccessControl.workspaceIdFor(currentUser)
        check(actualWorkspaceId == expectedUserId) { "Signed-in user changed; refusing cross-user access." }
        return expectedUserId
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeSettings(userId: String): Flow<BusinessSettings> = flow {
        val uid = requireAuthorizedUid(userId)
        val docRef = userDoc(uid).collection("settings").document("profile")
        val path = docRef.path
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        BusinessSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: BusinessSettings(userId = uid)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeCustomers(userId: String): Flow<List<Customer>> = flow {
        val uid = requireAuthorizedUid(userId)
        val colRef = userDoc(uid).collection("customers")
        val path = colRef.path
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Customer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeBills(userId: String): Flow<List<Bill>> = flow {
        val uid = requireAuthorizedUid(userId)
        val colRef = userDoc(uid).collection("bills")
        val path = colRef.path
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Bill::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.date }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observePayments(userId: String): Flow<List<Payment>> = flow {
        val uid = requireAuthorizedUid(userId)
        val colRef = userDoc(uid).collection("payments")
        val path = colRef.path
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Payment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.date }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeExpenses(userId: String): Flow<List<Expense>> = flow {
        val uid = requireAuthorizedUid(userId)
        val colRef = userDoc(uid).collection("expenses")
        val path = colRef.path
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Expense::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.date }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeChatMessages(userId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireAuthorizedUid(userId)
        val colRef = userDoc(uid).collection("chatMessages")
        val path = colRef.path
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(compareBy<ChatMessage> { it.date }.thenBy { it.createdAt?.toDate()?.time ?: 0L })
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeWbsedclConsumers(userId: String): Flow<List<WbsedclConsumer>> = flow {
        val uid = requireAuthorizedUid(userId)
        val colRef = userDoc(uid).collection("wbsedclConsumers")
        val path = colRef.path
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WbsedclConsumer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.label.lowercase() }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeWbsedclPayments(userId: String): Flow<List<WbsedclBillPayment>> = flow {
        val uid = requireAuthorizedUid(userId)
        val colRef = userDoc(uid).collection("wbsedclPayments")
        val path = colRef.path
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WbsedclBillPayment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.date }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        op: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.let { OwnerAccessControl.workspaceIdFor(it) } == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    suspend fun saveSettings(userId: String, settings: BusinessSettings): Result<Unit> {
        var docPath = "users/$userId/settings/profile"
        return try {
            val uid = requireAuthorizedUid(userId)
            val docRef = userDoc(uid).collection("settings").document("profile")
            docPath = docRef.path
            val exists = if (offlineActive) {
                runCatching { docRef.get(Source.CACHE).await().exists() }.getOrDefault(false)
            } else {
                withTimeoutOrNull(2500L) { docRef.get().await().exists() } ?: false
            }
            if (exists) {
                docRef.update(settings.toUpdateMap()).awaitWrite()
            } else {
                docRef.set(settings.toCreateMap(uid)).awaitWrite()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveCustomer(userId: String, customer: Customer): Result<String> {
        var docPath = "users/$userId/customers"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(customer.name.trim().isNotEmpty()) { "Customer name is required." }
            require(customer.openingDue >= 0.0) { "Opening due cannot be negative." }
            val colRef = userDoc(uid).collection("customers")
            val docRef = if (customer.id.isNotBlank()) colRef.document(customer.id) else colRef.document()
            docPath = docRef.path
            if (customer.id.isNotBlank()) {
                docRef.update(customer.toUpdateMap()).awaitWrite()
            } else {
                docRef.set(customer.toCreateMap(uid)).awaitWrite()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (customer.id.isNotBlank()) OperationType.UPDATE else OperationType.CREATE,
                docPath
            )
            Result.failure(e)
        }
    }

    suspend fun createBill(
        userId: String,
        bill: Bill,
        initialPaid: Double,
        initialMode: PaymentMode = PaymentMode.CASH,
        initialReceivedIn: String = PaymentMode.CASH.label
    ): Result<String> {
        var docPath = "users/$userId/bills"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(bill.customerId.isNotBlank()) { "Choose a customer." }
            require(bill.allTotal > 0.0) { "Enter a valid land quantity and rate." }

            val billRef = userDoc(uid).collection("bills").document()
            docPath = billRef.path
            val batch = db.batch()
            batch.set(billRef, bill.toCreateMap(uid))

            val clampedInitial = roundMoney(min(bill.allTotal, max(0.0, initialPaid)))
            if (clampedInitial > 0.001) {
                val paymentRef = userDoc(uid).collection("payments").document()
                val initialPayment = Payment(
                    billId = billRef.id,
                    customerId = bill.customerId,
                    date = bill.date.ifBlank { todayIsoDate() },
                    amount = clampedInitial,
                    mode = initialMode.label,
                    receivedIn = initialReceivedIn.trim().ifBlank { initialMode.label },
                    note = "Initial payment"
                )
                batch.set(paymentRef, initialPayment.toCreateMap(uid))
            }

            batch.commit().awaitWrite()
            Result.success(billRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateBill(userId: String, bill: Bill): Result<Unit> {
        var docPath = "users/$userId/bills/${bill.id}"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(bill.id.isNotBlank()) { "Bill ID is required." }
            require(bill.allTotal > 0.0) { "Total amount must be greater than 0." }
            val docRef = userDoc(uid).collection("bills").document(bill.id)
            docPath = docRef.path
            docRef.update(bill.toUpdateMap()).awaitWrite()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteBillAndPayments(
        userId: String,
        billId: String,
        paymentIds: List<String>
    ): Result<Unit> {
        val path = "users/$userId/bills/$billId"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(billId.isNotBlank()) { "Bill ID is required." }
            val refs = buildList {
                paymentIds.forEach { add(userDoc(uid).collection("payments").document(it)) }
                add(userDoc(uid).collection("bills").document(billId))
            }
            refs.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().awaitWrite()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun recordPayment(userId: String, payment: Payment): Result<Unit> {
        var docPath = "users/$userId/payments"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(payment.billId.isNotBlank()) { "Select an invoice." }
            require(payment.customerId.isNotBlank()) { "Missing customer reference." }
            require(payment.amount > 0.0) { "Enter a valid payment amount." }

            val paymentRef = userDoc(uid).collection("payments").document()
            docPath = paymentRef.path
            paymentRef.set(payment.toCreateMap(uid)).awaitWrite()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePayment(userId: String, payment: Payment): Result<Unit> {
        var docPath = "users/$userId/payments/${payment.id}"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(payment.id.isNotBlank()) { "Payment ID is required." }
            require(payment.amount > 0.0) { "Payment amount must be greater than 0." }
            val docRef = userDoc(uid).collection("payments").document(payment.id)
            docPath = docRef.path
            docRef.update(payment.toUpdateMap()).awaitWrite()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePayment(userId: String, paymentId: String): Result<Unit> {
        var docPath = "users/$userId/payments/$paymentId"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(paymentId.isNotBlank()) { "Payment ID is required." }
            val docRef = userDoc(uid).collection("payments").document(paymentId)
            docPath = docRef.path
            docRef.delete().awaitWrite()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun settleCustomer(
        userId: String,
        customerId: String,
        amount: Double,
        dueBillsWithDue: List<Pair<Bill, Double>>,
        currentOpeningDue: Double
    ): Result<Unit> {
        val path = "users/$userId/customers/$customerId"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(customerId.isNotBlank()) { "Customer ID is required." }
            require(amount > 0.0) { "Settlement amount is invalid." }

            var left = roundMoney(amount)
            val batch = db.batch()
            val today = todayIsoDate()

            for ((bill, billDue) in dueBillsWithDue.sortedBy { it.first.date }) {
                if (left <= 0.001) break
                if (billDue <= 0.001) continue
                val part = roundMoney(min(left, billDue))
                val paymentRef = userDoc(uid).collection("payments").document()
                val settlement = Payment(
                    billId = bill.id,
                    customerId = customerId,
                    date = today,
                    amount = part,
                    mode = PaymentMode.SETTLEMENT.label,
                    receivedIn = "Adjusted",
                    note = "Balance settlement adjustment"
                )
                batch.set(paymentRef, settlement.toCreateMap(uid))
                left = roundMoney(max(0.0, left - part))
            }

            if (left > 0.001 && currentOpeningDue > 0.0) {
                val newOpening = roundMoney(max(0.0, currentOpeningDue - left))
                val customerRef = userDoc(uid).collection("customers").document(customerId)
                batch.update(
                    customerRef,
                    mapOf(
                        "openingDue" to newOpening,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().awaitWrite()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomerAndData(
        userId: String,
        customerId: String,
        billIds: List<String>,
        paymentIds: List<String>,
        chatMessageIds: List<String> = emptyList()
    ): Result<Unit> {
        val path = "users/$userId/customers/$customerId"
        return try {
            val uid = requireAuthorizedUid(userId)
            val refs = buildList {
                chatMessageIds.forEach { add(userDoc(uid).collection("chatMessages").document(it)) }
                paymentIds.forEach { add(userDoc(uid).collection("payments").document(it)) }
                billIds.forEach { add(userDoc(uid).collection("bills").document(it)) }
                add(userDoc(uid).collection("customers").document(customerId))
            }
            refs.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().awaitWrite()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveExpense(userId: String, expense: Expense): Result<String> {
        var docPath = "users/$userId/expenses"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(expense.title.trim().isNotEmpty()) { "Expense title is required." }
            require(expense.amount > 0.0) { "Expense amount must be greater than 0." }
            val colRef = userDoc(uid).collection("expenses")
            val docRef = if (expense.id.isNotBlank()) colRef.document(expense.id) else colRef.document()
            docPath = docRef.path
            if (expense.id.isNotBlank()) {
                docRef.update(expense.toUpdateMap()).awaitWrite()
            } else {
                docRef.set(expense.toCreateMap(uid)).awaitWrite()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (expense.id.isNotBlank()) OperationType.UPDATE else OperationType.CREATE,
                docPath
            )
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(userId: String, expenseId: String): Result<Unit> {
        var docPath = "users/$userId/expenses/$expenseId"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(expenseId.isNotBlank()) { "Expense ID is required." }
            val docRef = userDoc(uid).collection("expenses").document(expenseId)
            docPath = docRef.path
            docRef.delete().awaitWrite()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(userId: String, message: ChatMessage): Result<String> {
        var docPath = "users/$userId/chatMessages"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(message.customerId.isNotBlank()) { "Customer ID is required." }
            require(message.text.trim().isNotEmpty()) { "Message cannot be empty." }
            val docRef = userDoc(uid).collection("chatMessages").document()
            docPath = docRef.path
            docRef.set(message.toCreateMap(uid)).awaitWrite()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateChatMessage(userId: String, messageId: String, newText: String): Result<Unit> {
        var docPath = "users/$userId/chatMessages/$messageId"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(messageId.isNotBlank()) { "Message ID is required." }
            require(newText.trim().isNotEmpty()) { "Message cannot be empty." }
            val docRef = userDoc(uid).collection("chatMessages").document(messageId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "text" to newText.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitWrite()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteChatMessage(userId: String, messageId: String): Result<Unit> {
        var docPath = "users/$userId/chatMessages/$messageId"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(messageId.isNotBlank()) { "Message ID is required." }
            val docRef = userDoc(uid).collection("chatMessages").document(messageId)
            docPath = docRef.path
            docRef.delete().awaitWrite()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveWbsedclConsumer(userId: String, consumer: WbsedclConsumer): Result<String> {
        var docPath = "users/$userId/wbsedclConsumers"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(consumer.consumerNo.trim().length >= 3) { "Enter a valid WBSEDCL Consumer ID." }
            require(consumer.label.trim().isNotEmpty()) { "Enter a label (e.g. Shallow 1)." }
            val colRef = userDoc(uid).collection("wbsedclConsumers")
            val docRef = if (consumer.id.isNotBlank()) colRef.document(consumer.id) else colRef.document()
            docPath = docRef.path
            if (consumer.id.isNotBlank()) {
                docRef.update(consumer.toUpdateMap()).awaitWrite()
            } else {
                docRef.set(consumer.toCreateMap(uid)).awaitWrite()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (consumer.id.isNotBlank()) OperationType.UPDATE else OperationType.CREATE,
                docPath
            )
            Result.failure(e)
        }
    }

    suspend fun deleteWbsedclConsumer(userId: String, consumerId: String): Result<Unit> {
        var docPath = "users/$userId/wbsedclConsumers/$consumerId"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(consumerId.isNotBlank()) { "Consumer ID is required." }
            val docRef = userDoc(uid).collection("wbsedclConsumers").document(consumerId)
            docPath = docRef.path
            docRef.delete().awaitWrite()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveWbsedclPayment(userId: String, payment: WbsedclBillPayment): Result<String> {
        var docPath = "users/$userId/wbsedclPayments"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(payment.consumerNo.trim().length >= 3) { "Enter a valid Consumer ID." }
            require(payment.billMonth.trim().isNotEmpty()) { "Enter the Bill Month." }
            require(payment.amount > 0.0) { "Amount must be greater than ₹0." }
            val colRef = userDoc(uid).collection("wbsedclPayments")
            val docRef = if (payment.id.isNotBlank()) colRef.document(payment.id) else colRef.document()
            docPath = docRef.path
            if (payment.id.isNotBlank()) {
                docRef.update(payment.toUpdateMap()).awaitWrite()
            } else {
                docRef.set(payment.toCreateMap(uid)).awaitWrite()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (payment.id.isNotBlank()) OperationType.UPDATE else OperationType.CREATE,
                docPath
            )
            Result.failure(e)
        }
    }

    suspend fun deleteWbsedclPayment(userId: String, paymentId: String): Result<Unit> {
        var docPath = "users/$userId/wbsedclPayments/$paymentId"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(paymentId.isNotBlank()) { "Payment ID is required." }
            val docRef = userDoc(uid).collection("wbsedclPayments").document(paymentId)
            docPath = docRef.path
            docRef.delete().awaitWrite()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun forceSync(userId: String): Result<Unit> {
        if (offlineActive) {
            return Result.failure(IllegalStateException("Offline Mode is active — disable Offline Mode or reconnect to sync."))
        }
        val path = "users/$userId/customers"
        return try {
            val uid = requireAuthorizedUid(userId)
            userDoc(uid).collection("settings").document("profile").get(Source.SERVER).await()
            userDoc(uid).collection("customers").get(Source.SERVER).await()
            userDoc(uid).collection("bills").get(Source.SERVER).await()
            userDoc(uid).collection("payments").get(Source.SERVER).await()
            userDoc(uid).collection("expenses").get(Source.SERVER).await()
            userDoc(uid).collection("chatMessages").get(Source.SERVER).await()
            userDoc(uid).collection("wbsedclConsumers").get(Source.SERVER).await()
            userDoc(uid).collection("wbsedclPayments").get(Source.SERVER).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }
}
