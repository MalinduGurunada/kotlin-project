
package com.example.personalfinanceapp.feature.receivable

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Receivable
import com.example.personalfinanceapp.core.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class ReceivableObservationSnapshot private constructor(
    val ownerId: UserId?,
    val receivables: List<Receivable>
) {
    companion object {
        fun signedOut() = ReceivableObservationSnapshot(null, emptyList())

        fun forOwner(ownerId: UserId, receivables: List<Receivable>): ReceivableObservationSnapshot {
            require(ownerId.isNotBlank()) { "Receivable observation owner cannot be blank" }
            return ReceivableObservationSnapshot(ownerId, receivables.toList())
        }
    }
}

interface ReceivableRepository {

    fun observeReceivableSnapshots(): Flow<ReceivableObservationSnapshot>

    fun observeReceivables(): Flow<List<Receivable>> = observeReceivableSnapshots()
        .map { it.receivables }

    suspend fun saveReceivable(
        receivable: Receivable
    ): Result<Unit>

    suspend fun saveReceivableForOwner(
        expectedOwnerId: UserId,
        receivable: Receivable
    ): Result<Unit>

    suspend fun setReceivableArchived(
        id: DocumentId,
        archived: Boolean
    ): Result<Unit>

    suspend fun setReceivableArchivedForOwner(
        expectedOwnerId: UserId,
        id: DocumentId,
        archived: Boolean
    ): Result<Unit>

    suspend fun getReceivableForOwner(
        expectedOwnerId: UserId,
        id: DocumentId
    ): Result<Receivable> = runCatching {
        require(expectedOwnerId.isNotBlank()) { "Expected receivable owner cannot be blank" }
        val snapshot = observeReceivableSnapshots().first { it.ownerId == expectedOwnerId }
        snapshot.receivables.firstOrNull { it.id == id }
            ?: throw NoSuchElementException("Receivable not found")
    }
}
