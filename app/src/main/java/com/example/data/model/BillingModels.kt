package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@Keep
@IgnoreExtraProperties
data class BusinessSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val company: String = "আশা মিনি শ্যালো",
    val owner: String = "SK EKRAMUL HAQUE",
    val contact: String = "9564061920",
    val address: String = "Raghunathpur, Chaklachipur, Ghatal, Paschim Medinipur, 721232",
    val upi: String = "8710065540@axl",
    val payee: String = "SK ENAMUL HAQUE",
    val country: String = "+91",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "company" to company.trim().take(160),
        "owner" to owner.trim().take(120),
        "contact" to contact.trim().take(40),
        "address" to address.trim().take(300),
        "upi" to upi.trim().take(120),
        "payee" to payee.trim().take(120),
        "country" to country.trim().ifBlank { "+91" }.take(12),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "company" to company.trim().take(160),
        "owner" to owner.trim().take(120),
        "contact" to contact.trim().take(40),
        "address" to address.trim().take(300),
        "upi" to upi.trim().take(120),
        "payee" to payee.trim().take(120),
        "country" to country.trim().ifBlank { "+91" }.take(12),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val village: String = "",
    val address: String = "",
    val openingDue: Double = 0.0,
    val landArea: String = "",
    val landUnit: String = LandUnit.DECIMAL.key,
    val landOwnerType: String = "Own",
    val landOwnerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun normalizedOwnerType(): String =
        if (landOwnerType.equals("Other", ignoreCase = true)) "Other" else "Own"

    fun normalizedLandUnit(): LandUnit = LandUnit.fromKey(landUnit)

    fun landSummaryLabel(): String {
        val rawArea = landArea.trim()
        val unitLabel = normalizedLandUnit().label
        val area = when {
            rawArea.isBlank() -> ""
            rawArea.contains("decimal", ignoreCase = true) ||
                rawArea.contains("katha", ignoreCase = true) ||
                rawArea.contains("bigha", ignoreCase = true) ||
                rawArea.contains("শতক") ||
                rawArea.contains("কাঠা") ||
                rawArea.contains("বিঘা") -> rawArea
            else -> "$rawArea $unitLabel"
        }
        val owner = if (normalizedOwnerType() == "Other") {
            landOwnerName.trim().ifBlank { "Other Owner" }
        } else {
            "Own"
        }
        return when {
            area.isNotBlank() && normalizedOwnerType() == "Other" -> "$area · Owner: $owner"
            area.isNotBlank() -> "$area · Own"
            normalizedOwnerType() == "Other" -> "Owner: $owner"
            else -> ""
        }
    }

    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "name" to name.trim().take(120),
        "phone" to phone.trim().take(40),
        "village" to village.trim().take(120),
        "address" to address.trim().take(250),
        "openingDue" to roundMoney(max(0.0, openingDue)),
        "landArea" to landArea.trim().take(120),
        "landUnit" to normalizedLandUnit().key,
        "landOwnerType" to normalizedOwnerType(),
        "landOwnerName" to (if (normalizedOwnerType() == "Other") landOwnerName.trim().take(120) else ""),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.trim().take(120),
        "phone" to phone.trim().take(40),
        "village" to village.trim().take(120),
        "address" to address.trim().take(250),
        "openingDue" to roundMoney(max(0.0, openingDue)),
        "landArea" to landArea.trim().take(120),
        "landUnit" to normalizedLandUnit().key,
        "landOwnerType" to normalizedOwnerType(),
        "landOwnerName" to (if (normalizedOwnerType() == "Other") landOwnerName.trim().take(120) else ""),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class Bill(
    @DocumentId val id: String = "",
    val userId: String = "",
    val billNo: String = "",
    val date: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val phone: String = "",
    val address: String = "",
    val village: String = "",
    val season: String = "",
    val landAmount: Double = 0.0,
    val unit: String = LandUnit.DECIMAL.key,
    val bigha: Double = 0.0,
    val rate: Double = 0.0,
    val current: Double = 0.0,
    val previousDue: Double = 0.0,
    val allTotal: Double = 0.0,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "billNo" to billNo.trim().take(60),
        "date" to date.trim().take(24),
        "customerId" to customerId,
        "customerName" to customerName.trim().take(120),
        "phone" to phone.trim().take(40),
        "address" to address.trim().take(250),
        "village" to village.trim().take(120),
        "season" to season.trim().take(120),
        "landAmount" to max(0.0, landAmount),
        "unit" to LandUnit.fromKey(unit).key,
        "bigha" to max(0.0, bigha),
        "rate" to max(0.0, rate),
        "current" to roundMoney(max(0.0, current)),
        "previousDue" to roundMoney(max(0.0, previousDue)),
        "allTotal" to roundMoney(allTotal),
        "note" to note.trim().take(500),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "date" to date.trim().take(24),
        "season" to season.trim().take(120),
        "landAmount" to max(0.0, landAmount),
        "unit" to LandUnit.fromKey(unit).key,
        "bigha" to max(0.0, bigha),
        "rate" to roundMoney(max(0.0, rate)),
        "current" to roundMoney(max(0.0, current)),
        "previousDue" to roundMoney(max(0.0, previousDue)),
        "allTotal" to roundMoney(max(0.0, allTotal)),
        "note" to note.trim().take(500),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class Payment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val billId: String = "",
    val customerId: String = "",
    val date: String = "",
    val amount: Double = 0.0,
    val mode: String = PaymentMode.CASH.label,
    val receivedIn: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "billId" to billId,
        "customerId" to customerId,
        "date" to date.trim().take(24),
        "amount" to roundMoney(amount),
        "mode" to PaymentMode.fromLabel(mode).label,
        "receivedIn" to receivedIn.trim().take(80),
        "note" to note.trim().take(500),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "date" to date.trim().take(24),
        "amount" to roundMoney(amount),
        "mode" to PaymentMode.fromLabel(mode).label,
        "receivedIn" to receivedIn.trim().take(80),
        "note" to note.trim().take(500),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val text: String = "",
    val sender: String = "ME",
    val date: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "customerId" to customerId,
        "text" to text.trim().take(1000),
        "sender" to if (sender == "CUSTOMER") "CUSTOMER" else "ME",
        "date" to date.trim().ifBlank { todayIsoDate() }.take(24),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "text" to text.trim().take(1000),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

enum class ExpenseCategory(val label: String, val colorHex: Long) {
    FUEL("Fuel", 0xFFEA580C),
    MAINTENANCE("Maintenance", 0xFF0284C7),
    ELECTRICITY("Electricity", 0xFFEAB308),
    SALARY("Salary", 0xFF7C3AED),
    PARTS("Parts", 0xFF0B6B50),
    OTHER("Other", 0xFF64748B);

    companion object {
        fun fromLabel(raw: String): ExpenseCategory =
            entries.firstOrNull { it.label.equals(raw, ignoreCase = true) } ?: OTHER
    }
}

@Keep
@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = ExpenseCategory.FUEL.label,
    val amount: Double = 0.0,
    val date: String = "",
    val paidMode: String = PaymentMode.CASH.label,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "title" to title.trim().take(160),
        "category" to ExpenseCategory.fromLabel(category).label,
        "amount" to max(0.0, amount),
        "date" to date.trim().take(24),
        "paidMode" to normalizeExpenseMode(paidMode),
        "note" to note.trim().take(500),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "title" to title.trim().take(160),
        "category" to ExpenseCategory.fromLabel(category).label,
        "amount" to max(0.0, amount),
        "date" to date.trim().take(24),
        "paidMode" to normalizeExpenseMode(paidMode),
        "note" to note.trim().take(500),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun normalizeExpenseMode(raw: String): String {
        val parsed = PaymentMode.fromLabel(raw)
        return if (parsed == PaymentMode.SETTLEMENT) PaymentMode.OTHER.label else parsed.label
    }
}

@Keep
@IgnoreExtraProperties
data class WbsedclConsumer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val consumerNo: String = "",
    val label: String = "",
    val installationNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "consumerNo" to consumerNo.trim().take(40),
        "label" to label.trim().take(80),
        "installationNote" to installationNote.trim().take(200),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "consumerNo" to consumerNo.trim().take(40),
        "label" to label.trim().take(80),
        "installationNote" to installationNote.trim().take(200),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class WbsedclBillPayment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val consumerNo: String = "",
    val consumerLabel: String = "",
    val billMonth: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "consumerNo" to consumerNo.trim().take(40),
        "consumerLabel" to consumerLabel.trim().take(80),
        "billMonth" to billMonth.trim().take(40),
        "amount" to roundMoney(max(0.0, amount)),
        "date" to date.trim().ifBlank { todayIsoDate() }.take(24),
        "note" to note.trim().take(500),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "consumerNo" to consumerNo.trim().take(40),
        "consumerLabel" to consumerLabel.trim().take(80),
        "billMonth" to billMonth.trim().take(40),
        "amount" to roundMoney(max(0.0, amount)),
        "date" to date.trim().ifBlank { todayIsoDate() }.take(24),
        "note" to note.trim().take(500),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

enum class LandUnit(val key: String, val label: String, val perBigha: Double) {
    DECIMAL("decimal", "Decimal", 40.0),
    KATHA("katha", "Katha", 20.0),
    BIGHA("bigha", "Bigha", 1.0);

    companion object {
        fun fromKey(raw: String): LandUnit =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: DECIMAL
    }
}

enum class PaymentMode(val label: String) {
    CASH("Cash"),
    UPI("UPI"),
    BANK_TRANSFER("Bank transfer"),
    OTHER("Other"),
    SETTLEMENT("Settlement");

    companion object {
        val selectableModes: List<PaymentMode> = listOf(CASH, UPI, BANK_TRANSFER, OTHER)

        fun fromLabel(raw: String): PaymentMode =
            entries.firstOrNull { it.label.equals(raw, ignoreCase = true) } ?: CASH
    }
}

enum class BillStatus(val label: String) {
    DUE("Due"),
    PARTIAL("Partial"),
    PAID("Paid")
}

data class BillingDashboardData(
    val settings: BusinessSettings = BusinessSettings(),
    val customers: List<Customer> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val wbsedclConsumers: List<WbsedclConsumer> = emptyList(),
    val wbsedclPayments: List<WbsedclBillPayment> = emptyList()
) {
    private val paymentsByBill: Map<String, List<Payment>> = payments.groupBy { it.billId }

    fun chatMessagesForCustomer(customerId: String): List<ChatMessage> =
        chatMessages
            .filter { it.customerId == customerId }
            .sortedWith(compareBy<ChatMessage> { it.date }.thenBy { it.createdAt?.toDate()?.time ?: 0L })

    fun paymentsForBill(billId: String): List<Payment> =
        paymentsByBill[billId].orEmpty().sortedBy { it.date }

    fun paidForBill(billId: String): Double =
        roundMoney(paymentsByBill[billId].orEmpty().sumOf { it.amount })

    fun dueForBill(bill: Bill): Double =
        roundMoney(max(0.0, bill.allTotal - paidForBill(bill.id)))

    fun statusForBill(bill: Bill): BillStatus {
        val due = dueForBill(bill)
        val paid = paidForBill(bill.id)
        return when {
            due <= 0.009 -> BillStatus.PAID
            paid > 0.0 -> BillStatus.PARTIAL
            else -> BillStatus.DUE
        }
    }

    fun billsForCustomer(customerId: String): List<Bill> =
        bills.filter { it.customerId == customerId }.sortedByDescending { it.date }

    fun dueForCustomer(customerId: String): Double {
        val opening = customers.firstOrNull { it.id == customerId }?.openingDue ?: 0.0
        val billsDue = bills.filter { it.customerId == customerId }.sumOf { dueForBill(it) }
        return roundMoney(opening + billsDue)
    }

    val totalBilled: Double
        get() = roundMoney(bills.sumOf { it.allTotal })

    val totalCollected: Double
        get() = roundMoney(bills.sumOf { paidForBill(it.id) })

    val totalOutstanding: Double
        get() = roundMoney(customers.sumOf { dueForCustomer(it.id) })

    val totalExpenses: Double
        get() = roundMoney(expenses.sumOf { it.amount })

    val netCollectedProfit: Double
        get() = roundMoney(totalCollected - totalExpenses)

    val projectedNetProfit: Double
        get() = roundMoney(totalBilled - totalExpenses)

    val totalWbsedclSpent: Double
        get() = roundMoney(wbsedclPayments.sumOf { it.amount })

    val collectionPercent: Int
        get() = if (totalBilled <= 0.0) 0 else ((totalCollected / totalBilled) * 100.0).toInt().coerceIn(0, 100)
}

fun roundMoney(v: Double): Double = kotlin.math.round(v * 100.0) / 100.0

fun calculateBigha(amount: Double, unit: LandUnit): Double {
    if (amount <= 0.0) return 0.0
    return amount / unit.perBigha
}

fun formatInr(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
    return "₹${formatter.format(amount)}"
}

fun todayIsoDate(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

fun formatTodayHeader(): String = LocalDate.now().format(
    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.forLanguageTag("en-IN"))
)

fun customerInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (parts.isEmpty()) return "?"
    return parts.take(2).joinToString("") { it.first().uppercaseChar().toString() }
}
