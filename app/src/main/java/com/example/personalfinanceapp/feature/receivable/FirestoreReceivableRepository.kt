package com.example.personalfinanceapp.feature.receivable

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Receivable
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class FirestoreReceivableRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : ReceivableRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeReceivableSnapshots(): Flow<ReceivableObservationSnapshot> =
        authRepository.observeAuthState()
            .distinctUntilChanged()
            .flatMapLatest { uid ->
                if (uid.isNullOrBlank()) flowOf(ReceivableObservationSnapshot.signedOut())
                else observeOwner(uid).map { ReceivableObservationSnapshot.forOwner(uid, it) }
            }
            .buffer(0)

    override suspend fun saveReceivable(receivable: Receivable): Result<Unit> =
        saveInternal(null, receivable)

    override suspend fun saveReceivableForOwner(
        expectedOwnerId: UserId,
        receivable: Receivable
    ): Result<Unit> = saveInternal(expectedOwnerId, receivable)

    private suspend fun saveInternal(expectedOwnerId: UserId?, receivable: Receivable): Result<Unit> =
        repositoryResult {
            val fields = ReceivableFirestoreMapper.toFirestoreMap(receivable)
            val uid = requireOwner(expectedOwnerId)
            val reference = document(uid, receivable.id)
            firestore.runTransaction { transaction ->
                val existing = transaction.get(reference)
                if (existing.exists() && existing.getString("currency") != fields["currency"]) {
                    throw IllegalArgumentException("Receivable currency cannot change after creation")
                }
                transaction.set(reference, fields)
                Unit
            }.awaitCompletion()
            requireOwner(uid)
        }

    override suspend fun setReceivableArchived(id: DocumentId, archived: Boolean): Result<Unit> =
        archiveInternal(null, id, archived)

    override suspend fun setReceivableArchivedForOwner(
        expectedOwnerId: UserId,
        id: DocumentId,
        archived: Boolean
    ): Result<Unit> = archiveInternal(expectedOwnerId, id, archived)

    private suspend fun archiveInternal(
        expectedOwnerId: UserId?,
        id: DocumentId,
        archived: Boolean
    ): Result<Unit> = repositoryResult {
        validateReceivableDocumentId(id)
        val uid = requireOwner(expectedOwnerId)
        val reference = document(uid, id)
        firestore.runTransaction { transaction ->
            if (!transaction.get(reference).exists()) throw NoSuchElementException("Receivable not found")
            transaction.update(reference, "archived", archived)
            Unit
        }.awaitCompletion()
        requireOwner(uid)
    }

    override suspend fun getReceivableForOwner(
        expectedOwnerId: UserId,
        id: DocumentId
    ): Result<Receivable> = try {
        validateReceivableDocumentId(id)
        val uid = requireOwner(expectedOwnerId)
        val snapshot = document(uid, id).get().awaitDocument()
        requireOwner(uid)
        if (!snapshot.exists()) Result.failure(NoSuchElementException("Receivable not found"))
        else Result.success(ReceivableFirestoreMapper.fromFirestoreMap(snapshot.id, snapshot.data.orEmpty()))
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    private fun observeOwner(uid: UserId): Flow<List<Receivable>> = callbackFlow {
        val registration = firestore.collection(USERS).document(uid).collection(RECEIVABLES)
            .orderBy("expectedDate", Query.Direction.ASCENDING)
            .orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else if (snapshot == null) {
                    close(IllegalStateException("Receivable listener returned no snapshot"))
                } else {
                    trySend(snapshot.documents.map {
                        ReceivableFirestoreMapper.fromFirestoreMap(it.id, it.data.orEmpty())
                    })
                }
            }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    private suspend fun requireOwner(expectedOwnerId: UserId?): UserId {
        if (expectedOwnerId != null) require(expectedOwnerId.isNotBlank()) {
            "Expected receivable owner cannot be blank"
        }
        val uid = authRepository.observeAuthState().first()
        check(!uid.isNullOrBlank()) { "User is not authenticated" }
        if (expectedOwnerId != null) check(uid == expectedOwnerId) {
            "Authenticated user changed during receivable operation"
        }
        return uid
    }

    private fun document(uid: UserId, id: DocumentId) = firestore.collection(USERS)
        .document(uid).collection(RECEIVABLES).document(id)

    private suspend inline fun repositoryResult(crossinline block: suspend () -> Unit): Result<Unit> = try {
        block(); Result.success(Unit)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    private companion object {
        const val USERS = "users"
        const val RECEIVABLES = "receivables"
    }
}

internal fun validateReceivableDocumentId(id: DocumentId) {
    require(id.isNotBlank()) { "Receivable ID cannot be empty" }
    require(id != "." && id != ".." && '/' !in id && !Regex("__.*__").matches(id)) {
        "Receivable ID is not a valid Firestore document ID"
    }
    val bytes = StandardCharsets.UTF_8.newEncoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .encode(CharBuffer.wrap(id)).remaining()
    require(bytes <= 1_500) { "Receivable ID exceeds the Firestore limit" }
}

private suspend fun Task<*>.awaitCompletion() = suspendCancellableCoroutine<Unit> { continuation ->
    addOnCompleteListener { task ->
        when {
            task.isCanceled -> continuation.cancel(CancellationException("Firebase task was cancelled"))
            task.isSuccessful -> continuation.resume(Unit) { _, _, _ -> }
            else -> continuation.resumeWith(Result.failure(task.exception ?: IllegalStateException("Firebase task failed")))
        }
    }
}

private suspend fun Task<DocumentSnapshot>.awaitDocument(): DocumentSnapshot =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            when {
                task.isCanceled -> continuation.cancel(CancellationException("Firebase task was cancelled"))
                task.isSuccessful -> continuation.resume(task.result) { _, _, _ -> }
                else -> continuation.resumeWith(Result.failure(task.exception ?: IllegalStateException("Firebase task failed")))
            }
        }
    }
