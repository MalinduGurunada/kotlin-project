package com.example.personalfinanceapp.feature.expense

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.feature.auth.accountScopedData
import com.example.personalfinanceapp.feature.auth.observeUserIds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.resume

class FirestoreExpenseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : ExpenseRepository {

    private val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override fun observeExpenses(
        startDate: LocalDate,
        endDateExclusive: LocalDate
    ): Flow<List<Expense>> = firebaseAuth.observeUserIds()
        .accountScopedData(emptyList()) { uid ->
            observeExpensesForOwner(uid, startDate, endDateExclusive)
        }

    private fun observeExpensesForOwner(
        uid: String,
        startDate: LocalDate,
        endDateExclusive: LocalDate
    ): Flow<List<Expense>> = callbackFlow {

        val collectionRef = firestore
            .collection("users")
            .document(uid)
            .collection("expenses")

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot == null) {
                close(IllegalStateException("Expense listener returned no snapshot"))
                return@addSnapshotListener
            }

            val items = try {
                snapshot.documents.mapNotNull { doc ->
                    ExpenseFirestoreMapper.fromFirestoreMapInRange(
                        documentId = doc.id,
                        fields = doc.data.orEmpty(),
                        startDate = startDate,
                        endDateExclusive = endDateExclusive
                    )
                }.sortedWith(
                    compareBy<Expense> { it.occurredDate }.thenBy { it.id }
                )
            } catch (exception: ExpenseFirestoreMappingException) {
                close(exception)
                return@addSnapshotListener
            }

            trySend(items)
        }

        awaitClose {
            listener.remove()
        }
    }.distinctUntilChanged()

    override suspend fun saveExpense(
        expense: Expense
    ): Result<Unit> = saveExpenseInternal(expectedOwnerId = null, expense = expense)

    override suspend fun saveExpenseForOwner(
        expectedOwnerId: UserId,
        expense: Expense
    ): Result<Unit> = saveExpenseInternal(expectedOwnerId, expense)

    private suspend fun saveExpenseInternal(
        expectedOwnerId: UserId?,
        expense: Expense
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val uid = currentUserId
        if (uid == null) {
            continuation.resume(Result.failure(IllegalStateException("User is not authenticated")))
            return@suspendCancellableCoroutine
        }
        if (expectedOwnerId != null && uid != expectedOwnerId) {
            continuation.resume(
                Result.failure(
                    IllegalStateException("Authenticated user changed before expense could be written")
                )
            )
            return@suspendCancellableCoroutine
        }

        if (expense.id.isBlank()) {
            continuation.resume(Result.failure(IllegalArgumentException("Expense ID cannot be blank")))
            return@suspendCancellableCoroutine
        }
        if (expense.money.baseAmountMinor <= 0L) {
            continuation.resume(Result.failure(IllegalArgumentException("Expense amount must be positive")))
            return@suspendCancellableCoroutine
        }

        val docRef = firestore
            .collection("users")
            .document(uid)
            .collection("expenses")
            .document(expense.id)

        val data = hashMapOf(
            "id" to expense.id,
            "userId" to uid,
            "categoryId" to expense.categoryId,
            "categoryNameSnapshot" to expense.categoryNameSnapshot,
            "spendingClass" to expense.spendingClass.name,
            "paymentChannel" to expense.paymentChannel.name,
            "originalAmountDecimal" to expense.money.originalAmountDecimal,
            "originalCurrency" to expense.money.originalCurrency.name,
            "rateLkrPerUnitDecimal" to expense.money.rateLkrPerUnitDecimal,
            "baseAmountMinor" to expense.money.baseAmountMinor,
            "rateSource" to expense.money.rateSource.name,
            "rateDate" to expense.money.rateDate.toString(),
            "occurredDate" to expense.occurredDate.toString(),
            "note" to expense.note,
            "channelLabel" to expense.channelLabel,
            "recurringExpenseId" to expense.recurringExpenseId,
            "recurringOccurrenceId" to expense.recurringOccurrenceId
        )

        docRef.set(data)
            .addOnSuccessListener {
                continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                continuation.resume(Result.failure(e))
            }
    }

    override suspend fun deleteExpense(
        id: DocumentId
    ): Result<Unit> = deleteExpenseInternal(expectedOwnerId = null, id = id)

    override suspend fun deleteExpenseForOwner(
        expectedOwnerId: UserId,
        id: DocumentId
    ): Result<Unit> = deleteExpenseInternal(expectedOwnerId, id)

    private suspend fun deleteExpenseInternal(
        expectedOwnerId: UserId?,
        id: DocumentId
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val uid = currentUserId
        if (uid == null) {
            continuation.resume(Result.failure(IllegalStateException("User is not authenticated")))
            return@suspendCancellableCoroutine
        }
        if (expectedOwnerId != null && uid != expectedOwnerId) {
            continuation.resume(
                Result.failure(
                    IllegalStateException("Authenticated user changed before expense could be deleted")
                )
            )
            return@suspendCancellableCoroutine
        }

        val docRef = firestore
            .collection("users")
            .document(uid)
            .collection("expenses")
            .document(id)

        docRef.delete()
            .addOnSuccessListener {
                continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                continuation.resume(Result.failure(e))
            }
    }
}
