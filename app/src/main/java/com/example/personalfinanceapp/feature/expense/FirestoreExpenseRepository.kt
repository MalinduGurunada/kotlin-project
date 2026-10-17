package com.example.personalfinanceapp.feature.expense

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.SpendingClass
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
    ): Flow<List<Expense>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val collectionRef = firestore
            .collection("users")
            .document(uid)
            .collection("expenses")

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val items = snapshot.documents.mapNotNull { doc ->
                runCatching {
                    val categoryId = doc.getString("categoryId") ?: ""
                    val categoryNameSnapshot = doc.getString("categoryNameSnapshot") ?: ""
                    val spendingClass = doc.getString("spendingClass")?.let {
                        SpendingClass.valueOf(it)
                    } ?: SpendingClass.DISCRETIONARY
                    val paymentChannel = doc.getString("paymentChannel")?.let {
                        PaymentChannel.valueOf(it)
                    } ?: PaymentChannel.CASH
                    val occurredDateStr = doc.getString("occurredDate") ?: ""
                    val occurredDate = LocalDate.parse(occurredDateStr)

                    val originalAmountDecimal = doc.getString("originalAmountDecimal") ?: "0"
                    val originalCurrency = doc.getString("originalCurrency")?.let {
                        CurrencyCode.valueOf(it)
                    } ?: CurrencyCode.LKR
                    val rateLkrPerUnitDecimal = doc.getString("rateLkrPerUnitDecimal") ?: "1"
                    val baseAmountMinor = doc.getLong("baseAmountMinor") ?: 0L
                    val rateSource = doc.getString("rateSource")?.let {
                        RateSource.valueOf(it)
                    } ?: RateSource.LKR_FIXED
                    val rateDateStr = doc.getString("rateDate") ?: occurredDateStr
                    val rateDate = LocalDate.parse(rateDateStr)

                    val note = doc.getString("note")
                    val channelLabel = doc.getString("channelLabel")

                    Expense(
                        id = doc.id,
                        categoryId = categoryId,
                        categoryNameSnapshot = categoryNameSnapshot,
                        spendingClass = spendingClass,
                        paymentChannel = paymentChannel,
                        money = Money(
                            originalAmountDecimal = originalAmountDecimal,
                            originalCurrency = originalCurrency,
                            rateLkrPerUnitDecimal = rateLkrPerUnitDecimal,
                            baseAmountMinor = baseAmountMinor,
                            rateSource = rateSource,
                            rateDate = rateDate
                        ),
                        occurredDate = occurredDate,
                        note = note,
                        channelLabel = channelLabel
                    )
                }.getOrNull()
            }.filter { expense ->
                !expense.occurredDate.isBefore(startDate) &&
                        expense.occurredDate.isBefore(endDateExclusive)
            }.sortedWith(
                compareBy<Expense> { it.occurredDate }.thenBy { it.id }
            )

            trySend(items)
        }

        awaitClose {
            listener.remove()
        }
    }.distinctUntilChanged()

    override suspend fun saveExpense(
        expense: Expense
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val uid = currentUserId
        if (uid == null) {
            continuation.resume(Result.failure(IllegalStateException("User is not authenticated")))
            return@suspendCancellableCoroutine
        }

        if (expense.id.isBlank()) {
            continuation.resume(Result.failure(IllegalArgumentException("Expense ID cannot be blank")))
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
            "channelLabel" to expense.channelLabel
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
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val uid = currentUserId
        if (uid == null) {
            continuation.resume(Result.failure(IllegalStateException("User is not authenticated")))
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
