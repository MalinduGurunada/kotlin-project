
package com.example.personalfinanceapp.feature.recurring

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.RecurringOccurrence
import com.example.personalfinanceapp.core.model.RecurringExpense
import com.example.personalfinanceapp.core.model.RecurringStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RecurringRepository {

    fun observeRecurringExpenses(): Flow<List<RecurringExpense>>

    fun observeOccurrences(): Flow<List<RecurringOccurrence>>

    suspend fun saveRecurringExpense(
        recurringExpense: RecurringExpense
    ): Result<Unit>

    suspend fun setRecurringExpenseStatus(
        id: DocumentId,
        status: RecurringStatus
    ): Result<Unit>

    suspend fun setRecurringExpenseActive(
        id: DocumentId,
        active: Boolean
    ): Result<Unit> = setRecurringExpenseStatus(
        id,
        if (active) RecurringStatus.ACTIVE else RecurringStatus.PAUSED
    )

    suspend fun confirmPayment(
        recurringExpenseId: DocumentId,
        dueDate: LocalDate,
        paidDate: LocalDate,
        amountMinor: Long
    ): Result<DocumentId>

    suspend fun skipOccurrence(
        recurringExpenseId: DocumentId,
        dueDate: LocalDate
    ): Result<Unit>
}
