
package com.example.personalfinanceapp.feature.expense

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.UserId
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ExpenseRepository {

    fun observeExpenses(
        startDate: LocalDate,
        endDateExclusive: LocalDate
    ): Flow<List<Expense>>

    suspend fun saveExpense(
        expense: Expense
    ): Result<Unit>

    suspend fun saveExpenseForOwner(
        expectedOwnerId: UserId,
        expense: Expense
    ): Result<Unit> = Result.failure(
        UnsupportedOperationException("Owner-bound expense writes are not supported")
    )

    suspend fun deleteExpense(
        id: DocumentId
    ): Result<Unit>

    suspend fun deleteExpenseForOwner(
        expectedOwnerId: UserId,
        id: DocumentId
    ): Result<Unit> = Result.failure(
        UnsupportedOperationException("Owner-bound expense deletes are not supported")
    )
}
