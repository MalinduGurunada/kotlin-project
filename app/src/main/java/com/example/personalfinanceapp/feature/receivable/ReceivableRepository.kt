
package com.example.personalfinanceapp.feature.receivable

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Receivable
import kotlinx.coroutines.flow.Flow

interface ReceivableRepository {

    fun observeReceivables(): Flow<List<Receivable>>

    suspend fun saveReceivable(
        receivable: Receivable
    ): Result<Unit>

    suspend fun setReceivableArchived(
        id: DocumentId,
        archived: Boolean
    ): Result<Unit>
}
