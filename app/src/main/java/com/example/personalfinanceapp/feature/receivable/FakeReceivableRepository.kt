
package com.example.personalfinanceapp.feature.receivable

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Receivable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal

class FakeReceivableRepository : ReceivableRepository {

    private val receivables =
        MutableStateFlow<List<Receivable>>(emptyList())

    private val mutex = Mutex()

    override fun observeReceivables(): Flow<List<Receivable>> {
        return receivables.asStateFlow()
    }

    override suspend fun saveReceivable(
        receivable: Receivable
    ): Result<Unit> = mutex.withLock {

        val title = receivable.title.trim()
        val projectReference = receivable.projectReference.trim()
        val amountText = receivable.expectedOriginalDecimal.trim()

        if (
            receivable.id.isBlank() ||
            title.isBlank() ||
            projectReference.isBlank()
        ) {
            return@withLock Result.failure(
                IllegalArgumentException(
                    "ID, title and project reference cannot be blank"
                )
            )
        }

        val amount = amountText.toBigDecimalOrNull()

        if (amount == null || amount <= BigDecimal.ZERO) {
            return@withLock Result.failure(
                IllegalArgumentException(
                    "Expected amount must be a positive number"
                )
            )
        }

        val current = receivables.value

        val updatedReceivable = receivable.copy(
            title = title,
            projectReference = projectReference,
            expectedOriginalDecimal = amountText
        )

        val existingIndex = current.indexOfFirst {
            it.id == receivable.id
        }

        receivables.value = if (existingIndex >= 0) {

            current.toMutableList().apply {
                set(existingIndex, updatedReceivable)
            }

        } else {

            current + updatedReceivable
        }

        Result.success(Unit)
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
}
