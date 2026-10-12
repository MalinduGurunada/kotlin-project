
package com.example.personalfinanceapp.feature.recurring

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.RecurringExpense
import kotlinx.coroutines.flow.Flow

interface RecurringRepository {

    fun observeRecurringExpenses(): Flow<List<RecurringExpense>>

    suspend fun saveRecurringExpense(
        recurringExpense: RecurringExpense
    ): Result<Unit>

    suspend fun setRecurringExpenseActive(
        id: DocumentId,
        active: Boolean
    ): Result<Unit>
}
