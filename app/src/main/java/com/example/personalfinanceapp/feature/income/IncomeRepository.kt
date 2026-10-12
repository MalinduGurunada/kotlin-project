
package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Income
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface IncomeRepository {

    fun observeIncomes(
        startDate: LocalDate,
        endDateExclusive: LocalDate
    ): Flow<List<Income>>

    suspend fun saveIncome(
        income: Income
    ): Result<Unit>

    suspend fun deleteIncome(
        id: DocumentId
    ): Result<Unit>
}
