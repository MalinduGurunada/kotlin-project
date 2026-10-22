package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import javax.inject.Inject

class FirestoreIncomeRepository internal constructor(
    private val authRepository: AuthRepository,
    private val observer: IncomeFirestoreObserver,
    private val writeGateway: IncomeFirestoreWriteGateway
) : IncomeRepository {

    @Inject
    constructor(
        authRepository: AuthRepository,
        firestore: FirebaseFirestore
    ) : this(
        authRepository = authRepository,
        observer = IncomeFirestoreObserver(authRepository, firestore),
        writeGateway = FirebaseIncomeFirestoreWriteGateway(firestore)
    )

    override fun observeIncomes(
        startDate: LocalDate,
        endDateExclusive: LocalDate
    ): Flow<List<Income>> = observer.observeIncomes(startDate, endDateExclusive)

    override suspend fun saveIncome(income: Income): Result<Unit> = repositoryResult {
        IncomeValidator.validate(income).getOrThrow()
        val fields = IncomeFirestoreMapper.toFirestoreMap(income)
        val authenticatedUid = requireAuthenticatedUid()

        writeGateway.setIncome(
            authenticatedUid = authenticatedUid,
            incomeId = income.id,
            fields = fields
        )
    }

    override suspend fun deleteIncome(id: DocumentId): Result<Unit> = repositoryResult {
        validateIncomeDocumentId(id)
        val authenticatedUid = requireAuthenticatedUid()

        writeGateway.deleteIncomeIfExists(
            authenticatedUid = authenticatedUid,
            incomeId = id
        )
    }

    private suspend fun requireAuthenticatedUid(): UserId {
        val uid = authRepository.observeAuthState().first()
        check(!uid.isNullOrBlank()) {
            "User is not authenticated"
        }
        return uid
    }

    private suspend inline fun repositoryResult(
        crossinline operation: suspend () -> Unit
    ): Result<Unit> {
        return try {
            operation()
            Result.success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}

internal interface IncomeFirestoreWriteGateway {

    suspend fun setIncome(
        authenticatedUid: UserId,
        incomeId: DocumentId,
        fields: Map<String, Any>
    )

    suspend fun deleteIncomeIfExists(
        authenticatedUid: UserId,
        incomeId: DocumentId
    )
}

private class FirebaseIncomeFirestoreWriteGateway(
    private val firestore: FirebaseFirestore
) : IncomeFirestoreWriteGateway {

    override suspend fun setIncome(
        authenticatedUid: UserId,
        incomeId: DocumentId,
        fields: Map<String, Any>
    ) {
        validateIncomeDocumentId(incomeId)
        firestore.incomeDocument(authenticatedUid, incomeId)
            .set(fields)
            .awaitCompletion()
    }

    override suspend fun deleteIncomeIfExists(
        authenticatedUid: UserId,
        incomeId: DocumentId
    ) {
        validateIncomeDocumentId(incomeId)
        val document = firestore.incomeDocument(authenticatedUid, incomeId)

        firestore.runTransaction { transaction ->
            if (!transaction.get(document).exists()) {
                throw NoSuchElementException("Income not found")
            }
            transaction.delete(document)
            Unit
        }.awaitCompletion()
    }
}

private fun FirebaseFirestore.incomeDocument(
    authenticatedUid: UserId,
    incomeId: DocumentId
) = collection(USERS_COLLECTION)
    .document(authenticatedUid)
    .collection(INCOMES_COLLECTION)
    .document(incomeId)

private suspend fun Task<*>.awaitCompletion() {
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { completedTask ->
            when {
                completedTask.isCanceled -> continuation.cancel(
                    CancellationException("Firebase task was cancelled")
                )
                completedTask.isSuccessful -> continuation.resume(Unit) { _, _, _ -> }
                else -> {
                    val exception = completedTask.exception
                        ?: IllegalStateException("Firebase task failed without an exception")
                    continuation.resumeWith(Result.failure(exception))
                }
            }
        }
    }
}

private fun validateIncomeDocumentId(incomeId: DocumentId) {
    require(incomeId.isNotBlank()) {
        "Income ID cannot be empty"
    }
    require(incomeId != "." && incomeId != "..") {
        "Income ID is not a valid Firestore document ID"
    }
    require('/' !in incomeId) {
        "Income ID cannot contain Firestore path separators"
    }
    require(!RESERVED_DOCUMENT_ID_PATTERN.matches(incomeId)) {
        "Income ID uses a reserved Firestore document ID format"
    }

    val encodedByteCount = try {
        StandardCharsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .encode(CharBuffer.wrap(incomeId))
            .remaining()
    } catch (exception: CharacterCodingException) {
        throw IllegalArgumentException(
            "Income ID must contain valid UTF-8 characters",
            exception
        )
    }

    require(encodedByteCount <= MAX_DOCUMENT_ID_BYTES) {
        "Income ID exceeds the Firestore document ID size limit"
    }
}

private const val USERS_COLLECTION = "users"
private const val INCOMES_COLLECTION = "incomes"
private const val MAX_DOCUMENT_ID_BYTES = 1_500
private val RESERVED_DOCUMENT_ID_PATTERN = Regex("__.*__")
