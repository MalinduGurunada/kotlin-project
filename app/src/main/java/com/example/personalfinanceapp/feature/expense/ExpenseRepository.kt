
package com.example.personalfinanceapp.feature.expense

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Expense
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

    suspend fun deleteExpense(
        id: DocumentId
    ): Result<Unit>
}
