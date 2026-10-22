package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

/**
 * Observes the authenticated user's income documents for one date window.
 *
 * A real empty snapshot is emitted as an empty list. Firestore and mapping failures terminate the
 * flow instead of being converted to empty data. When an authenticated UID is replaced or signed
 * out, an explicit empty list clears the previous account's values before any new listener result.
 * Initial authenticated collection waits for the first Firestore snapshot, leaving loading and
 * retry presentation to the eventual IncomeViewModel integration.
 */
class IncomeFirestoreObserver internal constructor(
    private val authRepository: AuthRepository,
    private val snapshotSource: IncomeFirestoreSnapshotSource
) {

    constructor(
        authRepository: AuthRepository,
        firestore: FirebaseFirestore
    ) : this(authRepository, FirebaseIncomeFirestoreSnapshotSource(firestore))

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeIncomes(
        startDate: LocalDate,
        endDateExclusive: LocalDate
    ): Flow<List<Income>> {
        require(startDate.isBefore(endDateExclusive)) {
            "Start date must be before end date"
        }

        val startDateInclusiveIso = startDate.toString()
        val endDateExclusiveIso = endDateExclusive.toString()

        return observeIdentityChanges()
            .flatMapLatest { identity ->
                val uid = identity.uid
                when {
                    uid == null -> flowOf(emptyList())
                    identity.mustClearPreviousUser -> flow {
                        emit(emptyList())
                        emitAll(
                            observeAuthenticatedUser(
                                IncomeFirestoreQuery(
                                    authenticatedUid = uid,
                                    startDateInclusiveIso = startDateInclusiveIso,
                                    endDateExclusiveIso = endDateExclusiveIso
                                )
                            )
                        )
                    }
                    else -> observeAuthenticatedUser(
                        query = IncomeFirestoreQuery(
                            authenticatedUid = uid,
                            startDateInclusiveIso = startDateInclusiveIso,
                            endDateExclusiveIso = endDateExclusiveIso
                        )
                    )
                }
            }
            .buffer(capacity = 0)
    }

    private fun observeIdentityChanges(): Flow<IdentityChange> = flow {
        var lastAuthenticatedUid: UserId? = null
        var signedOutSinceAuthenticated = false
        var currentUid: UserId? = null

        authRepository.observeAuthState()
            .distinctUntilChanged()
            .collect { uid ->
                val mustClearPreviousUser = uid != null &&
                    lastAuthenticatedUid != null &&
                    (uid != lastAuthenticatedUid || signedOutSinceAuthenticated)

                if (uid == null) {
                    if (lastAuthenticatedUid != null) {
                        signedOutSinceAuthenticated = true
                    }
                } else {
                    lastAuthenticatedUid = uid
                    signedOutSinceAuthenticated = false
                }
                currentUid = uid

                emit(
                    IdentityChange(
                        uid = uid,
                        mustClearPreviousUser = mustClearPreviousUser
                    )
                )
            }

        if (currentUid != null) {
            emit(IdentityChange(uid = null, mustClearPreviousUser = false))
        }
    }

    private fun observeAuthenticatedUser(
        query: IncomeFirestoreQuery
    ): Flow<List<Income>> = callbackFlow {
        val registration = snapshotSource.listen(query) snapshot@{ result ->
            val documents = result.getOrElse { exception ->
                close(exception)
                return@snapshot
            }

            val incomes = try {
                documents.map { document ->
                    IncomeFirestoreMapper.fromFirestoreMap(
                        documentId = document.id,
                        fields = document.fields
                    )
                }
            } catch (exception: IncomeFirestoreMappingException) {
                close(exception)
                return@snapshot
            }

            trySend(incomes)
        }

        awaitClose {
            registration.remove()
        }
    }.distinctUntilChanged()
}

private data class IdentityChange(
    val uid: UserId?,
    val mustClearPreviousUser: Boolean
)

internal data class IncomeFirestoreQuery(
    val authenticatedUid: UserId,
    val startDateInclusiveIso: String,
    val endDateExclusiveIso: String,
    val ordering: List<IncomeFirestoreOrder> = listOf(
        IncomeFirestoreOrder.OCCURRED_DATE_ASCENDING,
        IncomeFirestoreOrder.DOCUMENT_ID_ASCENDING
    )
)

internal enum class IncomeFirestoreOrder {
    OCCURRED_DATE_ASCENDING,
    DOCUMENT_ID_ASCENDING
}

internal data class IncomeFirestoreDocument(
    val id: String,
    val fields: Map<String, Any?>
)

internal fun interface IncomeFirestoreListenerRegistration {
    fun remove()
}

internal fun interface IncomeFirestoreSnapshotSource {
    fun listen(
        query: IncomeFirestoreQuery,
        onSnapshot: (Result<List<IncomeFirestoreDocument>>) -> Unit
    ): IncomeFirestoreListenerRegistration
}

private class FirebaseIncomeFirestoreSnapshotSource(
    private val firestore: FirebaseFirestore
) : IncomeFirestoreSnapshotSource {

    override fun listen(
        query: IncomeFirestoreQuery,
        onSnapshot: (Result<List<IncomeFirestoreDocument>>) -> Unit
    ): IncomeFirestoreListenerRegistration {
        var firestoreQuery: Query = firestore
            .collection(USERS_COLLECTION)
            .document(query.authenticatedUid)
            .collection(INCOMES_COLLECTION)
            .whereGreaterThanOrEqualTo(OCCURRED_DATE_FIELD, query.startDateInclusiveIso)
            .whereLessThan(OCCURRED_DATE_FIELD, query.endDateExclusiveIso)

        query.ordering.forEach { order ->
            firestoreQuery = when (order) {
                IncomeFirestoreOrder.OCCURRED_DATE_ASCENDING ->
                    firestoreQuery.orderBy(OCCURRED_DATE_FIELD, Query.Direction.ASCENDING)
                IncomeFirestoreOrder.DOCUMENT_ID_ASCENDING ->
                    firestoreQuery.orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
            }
        }

        val listener = firestoreQuery.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                onSnapshot(Result.failure(exception))
                return@addSnapshotListener
            }
            if (snapshot == null) {
                onSnapshot(
                    Result.failure(
                        IllegalStateException("Income listener returned no snapshot")
                    )
                )
                return@addSnapshotListener
            }

            val documents = snapshot.documents.map { document ->
                IncomeFirestoreDocument(
                    id = document.id,
                    fields = document.data ?: emptyMap()
                )
            }
            onSnapshot(Result.success(documents))
        }

        return IncomeFirestoreListenerRegistration {
            listener.remove()
        }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val INCOMES_COLLECTION = "incomes"
        const val OCCURRED_DATE_FIELD = "occurredDate"
    }
}
