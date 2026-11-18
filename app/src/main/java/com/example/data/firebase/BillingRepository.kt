package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.auth.OwnerAccessControl
import com.example.data.model.Bill
import com.example.data.model.BusinessSettings
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.Payment
import com.example.data.model.PaymentMode
import com.example.data.model.todayIsoDate
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
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
import kotlin.math.max
import kotlin.math.min

class BillingRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

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
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(settings.toUpdateMap()).await()
            } else {
                docRef.set(settings.toCreateMap(uid)).await()
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
                docRef.update(customer.toUpdateMap()).await()
            } else {
                docRef.set(customer.toCreateMap(uid)).await()
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

            val clampedInitial = min(bill.allTotal, max(0.0, initialPaid))
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

            batch.commit().await()
            Result.success(billRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
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
            paymentRef.set(payment.toCreateMap(uid)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
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

            var left = amount
            val batch = db.batch()
            val today = todayIsoDate()

            for ((bill, billDue) in dueBillsWithDue.sortedBy { it.first.date }) {
                if (left <= 0.001) break
                if (billDue <= 0.001) continue
                val part = min(left, billDue)
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
                left -= part
            }

            if (left > 0.001 && currentOpeningDue > 0.0) {
                val newOpening = max(0.0, currentOpeningDue - left)
                val customerRef = userDoc(uid).collection("customers").document(customerId)
                batch.update(
                    customerRef,
                    mapOf(
                        "openingDue" to newOpening,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
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
        paymentIds: List<String>
    ): Result<Unit> {
        val path = "users/$userId/customers/$customerId"
        return try {
            val uid = requireAuthorizedUid(userId)
            val refs = buildList {
                paymentIds.forEach { add(userDoc(uid).collection("payments").document(it)) }
                billIds.forEach { add(userDoc(uid).collection("bills").document(it)) }
                add(userDoc(uid).collection("customers").document(customerId))
            }
            refs.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().await()
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
                docRef.update(expense.toUpdateMap()).await()
            } else {
                docRef.set(expense.toCreateMap(uid)).await()
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
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun forceSync(userId: String): Result<Unit> {
        val path = "users/$userId/customers"
        return try {
            val uid = requireAuthorizedUid(userId)
            userDoc(uid).collection("settings").document("profile").get(Source.SERVER).await()
            userDoc(uid).collection("customers").get(Source.SERVER).await()
            userDoc(uid).collection("bills").get(Source.SERVER).await()
            userDoc(uid).collection("payments").get(Source.SERVER).await()
            userDoc(uid).collection("expenses").get(Source.SERVER).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }
}
