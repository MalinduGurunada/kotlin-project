package com.example.personalfinanceapp.feature.recurring

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.RecurrenceFrequency
import com.example.personalfinanceapp.core.model.RecurringExpense
import com.example.personalfinanceapp.core.model.RecurringOccurrence
import com.example.personalfinanceapp.core.model.RecurringOccurrenceStatus
import com.example.personalfinanceapp.core.model.RecurringStatus
import com.example.personalfinanceapp.core.model.SpendingClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.resume

class FirestoreRecurringRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : RecurringRepository {

    override fun observeRecurringExpenses(): Flow<List<RecurringExpense>> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = recurringCollection(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                close(error ?: IllegalStateException("Unable to load recurring commitments"))
                return@addSnapshotListener
            }
            trySend(snapshot.documents.mapNotNull(::toRecurringExpense).sortedBy { it.title })
        }
        awaitClose { listener.remove() }
    }.distinctUntilChanged()

    override fun observeOccurrences(): Flow<List<RecurringOccurrence>> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = occurrenceCollection(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                close(error ?: IllegalStateException("Unable to load recurring occurrences"))
                return@addSnapshotListener
            }
            trySend(snapshot.documents.mapNotNull(::toOccurrence).sortedBy { it.dueDate })
        }
        awaitClose { listener.remove() }
    }.distinctUntilChanged()

    override suspend fun saveRecurringExpense(
        recurringExpense: RecurringExpense
    ): Result<Unit> = repositoryResult {
        validate(recurringExpense)
        val uid = requireUserId()
        recurringCollection(uid).document(recurringExpense.id)
            .set(recurringExpense.toFirestoreMap(uid))
            .awaitResult()
    }

    override suspend fun setRecurringExpenseStatus(
        id: DocumentId,
        status: RecurringStatus
    ): Result<Unit> = repositoryResult {
        require(id.isNotBlank()) { "Recurring expense ID cannot be blank" }
        val uid = requireUserId()
        val reference = recurringCollection(uid).document(id)
        firestore.runTransaction { transaction ->
            if (!transaction.get(reference).exists()) {
                throw NoSuchElementException("Recurring expense not found")
            }
            transaction.update(reference, "status", status.name)
        }.awaitResult()
    }

    override suspend fun confirmPayment(
        recurringExpenseId: DocumentId,
        dueDate: LocalDate,
        paidDate: LocalDate,
        amountMinor: Long
    ): Result<DocumentId> {
        if (recurringExpenseId.isBlank() || amountMinor <= 0L) {
            return Result.failure(IllegalArgumentException("Payment details are invalid"))
        }
        val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val occurrenceId = RecurringScheduleCalculator.occurrenceId(recurringExpenseId, dueDate)
        val expenseId = RecurringScheduleCalculator.expenseId(recurringExpenseId, dueDate)
        val recurringRef = recurringCollection(uid).document(recurringExpenseId)
        val occurrenceRef = occurrenceCollection(uid).document(occurrenceId)
        val expenseRef = firestore.collection("users").document(uid)
            .collection("expenses").document(expenseId)

        return suspendCancellableCoroutine { continuation ->
            firestore.runTransaction { transaction ->
                val recurring = transaction.get(recurringRef)
                if (!recurring.exists()) throw NoSuchElementException("Recurring expense not found")
                val categoryId = recurring.getString("categoryId").orEmpty()
                val categoryRef = firestore.collection("users").document(uid)
                    .collection("categories").document(categoryId)
                val category = transaction.get(categoryRef)
                if (!category.exists()) throw NoSuchElementException("Category not found")
                val occurrence = transaction.get(occurrenceRef)
                val expense = transaction.get(expenseRef)

                when (occurrence.getString("status")) {
                    RecurringOccurrenceStatus.CONFIRMED.name ->
                        return@runTransaction occurrence.getString("expenseId") ?: expenseId
                    RecurringOccurrenceStatus.SKIPPED.name ->
                        throw IllegalStateException("Skipped occurrence cannot be confirmed")
                }
                if (expense.exists()) {
                    throw IllegalStateException("An expense already uses this occurrence ID")
                }

                val paymentChannel = recurring.getString("paymentChannel")
                    ?.let { runCatching { PaymentChannel.valueOf(it) }.getOrNull() }
                    ?: PaymentChannel.BANK
                val categoryName = category.getString("name").orEmpty()
                val spendingClass = category.getString("defaultSpendingClass")
                    ?.let { runCatching { SpendingClass.valueOf(it) }.getOrNull() }
                    ?: SpendingClass.COMMITTED
                val amountDecimal = BigDecimal.valueOf(amountMinor, 2).setScale(2).toPlainString()

                transaction.set(
                    expenseRef,
                    hashMapOf(
                        "id" to expenseId,
                        "userId" to uid,
                        "categoryId" to categoryId,
                        "categoryNameSnapshot" to categoryName,
                        "spendingClass" to spendingClass.name,
                        "paymentChannel" to paymentChannel.name,
                        "originalAmountDecimal" to amountDecimal,
                        "originalCurrency" to "LKR",
                        "rateLkrPerUnitDecimal" to "1",
                        "baseAmountMinor" to amountMinor,
                        "rateSource" to "LKR_FIXED",
                        "rateDate" to paidDate.toString(),
                        "occurredDate" to paidDate.toString(),
                        "note" to "Recurring payment: ${recurring.getString("title").orEmpty()}",
                        "channelLabel" to recurring.getString("channelLabel"),
                        "recurringExpenseId" to recurringExpenseId,
                        "recurringOccurrenceId" to occurrenceId
                    )
                )
                transaction.set(
                    occurrenceRef,
                    hashMapOf(
                        "id" to occurrenceId,
                        "userId" to uid,
                        "recurringExpenseId" to recurringExpenseId,
                        "dueDate" to dueDate.toString(),
                        "status" to RecurringOccurrenceStatus.CONFIRMED.name,
                        "expenseId" to expenseId,
                        "actionAt" to FieldValue.serverTimestamp()
                    )
                )
                expenseId
            }.addOnSuccessListener { id ->
                if (continuation.isActive) continuation.resume(Result.success(id))
            }.addOnFailureListener { error ->
                if (continuation.isActive) continuation.resume(Result.failure(error))
            }
        }
    }

    override suspend fun skipOccurrence(
        recurringExpenseId: DocumentId,
        dueDate: LocalDate
    ): Result<Unit> = repositoryResult {
        require(recurringExpenseId.isNotBlank()) { "Recurring expense ID cannot be blank" }
        val uid = requireUserId()
        val recurringRef = recurringCollection(uid).document(recurringExpenseId)
        val occurrenceId = RecurringScheduleCalculator.occurrenceId(recurringExpenseId, dueDate)
        val occurrenceRef = occurrenceCollection(uid).document(occurrenceId)
        firestore.runTransaction { transaction ->
            if (!transaction.get(recurringRef).exists()) {
                throw NoSuchElementException("Recurring expense not found")
            }
            val existing = transaction.get(occurrenceRef)
            when (existing.getString("status")) {
                RecurringOccurrenceStatus.CONFIRMED.name ->
                    throw IllegalStateException("Confirmed occurrence cannot be skipped")
                RecurringOccurrenceStatus.SKIPPED.name -> return@runTransaction Unit
            }
            transaction.set(
                occurrenceRef,
                hashMapOf(
                    "id" to occurrenceId,
                    "userId" to uid,
                    "recurringExpenseId" to recurringExpenseId,
                    "dueDate" to dueDate.toString(),
                    "status" to RecurringOccurrenceStatus.SKIPPED.name,
                    "expenseId" to null,
                    "actionAt" to FieldValue.serverTimestamp()
                )
            )
        }.awaitResult()
    }

    private fun recurringCollection(uid: String) = firestore
        .collection("users").document(uid).collection("recurringExpenses")

    private fun occurrenceCollection(uid: String) = firestore
        .collection("users").document(uid).collection("recurringOccurrences")

    private fun requireUserId(): String = firebaseAuth.currentUser?.uid
        ?: throw IllegalStateException("User is not authenticated")

    private suspend inline fun <T> repositoryResult(crossinline operation: suspend () -> T): Result<T> =
        try {
            Result.success(operation())
        } catch (error: Exception) {
            Result.failure(error)
        }

    private fun validate(item: RecurringExpense) {
        require(item.id.isNotBlank()) { "ID cannot be blank" }
        require(item.title.isNotBlank()) { "Title cannot be blank" }
        require(item.categoryId.isNotBlank()) { "Category cannot be blank" }
        require(item.expectedAmountMinor > 0L) { "Expected amount must be positive" }
        require(item.intervalCount > 0L) { "Interval count must be positive" }
        require(item.endDate == null || !item.endDate.isBefore(item.startDate)) {
            "End date cannot be before start date"
        }
        require(item.reminderLeadDays >= 0L) { "Reminder lead days cannot be negative" }
    }
}

private fun RecurringExpense.toFirestoreMap(uid: String): Map<String, Any?> = hashMapOf(
    "id" to id,
    "userId" to uid,
    "title" to title.trim(),
    "categoryId" to categoryId,
    "expectedAmountMinor" to expectedAmountMinor,
    "frequency" to frequency.name,
    "intervalCount" to intervalCount,
    "anchorDate" to anchorDate.toString(),
    "startDate" to startDate.toString(),
    "endDate" to endDate?.toString(),
    "status" to status.name,
    "paymentChannel" to paymentChannel.name,
    "channelLabel" to channelLabel?.trim()?.takeIf { it.isNotEmpty() },
    "reminderEnabled" to reminderEnabled,
    "reminderLeadDays" to reminderLeadDays
)

private fun toRecurringExpense(document: DocumentSnapshot): RecurringExpense? = runCatching {
    RecurringExpense(
        id = document.id,
        title = document.getString("title").orEmpty(),
        categoryId = document.getString("categoryId").orEmpty(),
        expectedAmountMinor = document.getLong("expectedAmountMinor") ?: 0L,
        frequency = RecurrenceFrequency.valueOf(document.getString("frequency").orEmpty()),
        intervalCount = document.getLong("intervalCount") ?: 1L,
        anchorDate = LocalDate.parse(document.getString("anchorDate")),
        startDate = LocalDate.parse(document.getString("startDate")),
        endDate = document.getString("endDate")?.let(LocalDate::parse),
        status = RecurringStatus.valueOf(document.getString("status") ?: RecurringStatus.ACTIVE.name),
        paymentChannel = PaymentChannel.valueOf(
            document.getString("paymentChannel") ?: PaymentChannel.BANK.name
        ),
        channelLabel = document.getString("channelLabel"),
        reminderEnabled = document.getBoolean("reminderEnabled") ?: false,
        reminderLeadDays = document.getLong("reminderLeadDays") ?: 0L
    )
}.getOrNull()

private fun toOccurrence(document: DocumentSnapshot): RecurringOccurrence? = runCatching {
    RecurringOccurrence(
        id = document.id,
        recurringExpenseId = document.getString("recurringExpenseId").orEmpty(),
        dueDate = LocalDate.parse(document.getString("dueDate")),
        status = RecurringOccurrenceStatus.valueOf(document.getString("status").orEmpty()),
        expenseId = document.getString("expenseId"),
        actionAt = document.getTimestamp("actionAt")?.toDate()?.toInstant() ?: Instant.EPOCH
    )
}.getOrNull()

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWith(Result.failure(error))
        }
    }
