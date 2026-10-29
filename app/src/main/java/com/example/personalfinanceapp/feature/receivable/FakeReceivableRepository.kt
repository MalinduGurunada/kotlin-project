
package com.example.personalfinanceapp.feature.receivable

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Receivable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal

class FakeReceivableRepository(
    private val ownerId: String = "fake-owner"
) : ReceivableRepository {

    private val receivables =
        MutableStateFlow<List<Receivable>>(emptyList())

    private val mutex = Mutex()

    override fun observeReceivableSnapshots(): Flow<ReceivableObservationSnapshot> =
        receivables.asStateFlow().map { ReceivableObservationSnapshot.forOwner(ownerId, it) }

    override suspend fun saveReceivable(
        receivable: Receivable
    ): Result<Unit> = mutex.withLock {
        val normalized = ReceivableValidator.normalize(receivable).getOrElse {
            return@withLock Result.failure(it)
        }

        val current = receivables.value

        val existingIndex = current.indexOfFirst {
            it.id == normalized.id
        }

        if (existingIndex >= 0 && current[existingIndex].currency != normalized.currency) {
            return@withLock Result.failure(
                IllegalArgumentException("Receivable currency cannot change after creation")
            )
        }

        receivables.value = if (existingIndex >= 0) {

            current.toMutableList().apply {
                set(existingIndex, normalized)
            }

        } else {

            current + normalized
        }

        Result.success(Unit)
    }

    override suspend fun saveReceivableForOwner(
        expectedOwnerId: String,
        receivable: Receivable
    ): Result<Unit> = if (expectedOwnerId == ownerId) {
        saveReceivable(receivable)
    } else {
        Result.failure(IllegalStateException("Authenticated user changed before receivable could be saved"))
    }

    override suspend fun setReceivableArchived(
        id: DocumentId,
        archived: Boolean
    ): Result<Unit> = mutex.withLock {

        val current = receivables.value

        if (current.none { it.id == id }) {
            return@withLock Result.failure(
                NoSuchElementException(
                    "Receivable not found"
                )
            )
        }

        receivables.value = current.map { receivable ->
            if (receivable.id == id) {
                receivable.copy(archived = archived)
            } else {
                receivable
            }
        }

        Result.success(Unit)
    }

    override suspend fun setReceivableArchivedForOwner(
        expectedOwnerId: String,
        id: DocumentId,
        archived: Boolean
    ): Result<Unit> = if (expectedOwnerId == ownerId) {
        setReceivableArchived(id, archived)
    } else {
        Result.failure(IllegalStateException("Authenticated user changed before receivable could be updated"))
    }

    override suspend fun getReceivableForOwner(
        expectedOwnerId: String,
        id: DocumentId
    ): Result<Receivable> = mutex.withLock {
        if (expectedOwnerId != ownerId) {
            return@withLock Result.failure(IllegalStateException("Authenticated user changed"))
        }
        receivables.value.firstOrNull { it.id == id }
            ?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Receivable not found"))
    }
}
