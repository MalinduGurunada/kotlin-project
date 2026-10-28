
package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class IncomeObservationSnapshot private constructor(
    val ownerId: UserId?,
    val incomes: List<Income>
) {
    companion object {
        fun signedOut(): IncomeObservationSnapshot = IncomeObservationSnapshot(
            ownerId = null,
            incomes = emptyList()
        )

        fun forOwner(
            ownerId: UserId,
            incomes: List<Income>
        ): IncomeObservationSnapshot {
            require(ownerId.isNotBlank()) {
                "Income observation owner cannot be blank"
            }
            return IncomeObservationSnapshot(
                ownerId = ownerId,
                incomes = incomes.toList()
            )
        }
    }
}

interface IncomeRepository {

    fun observeIncomeSnapshots(
        startDate: LocalDate,
        endDateExclusive: LocalDate
    ): Flow<IncomeObservationSnapshot>

    fun observeIncomes(
        startDate: LocalDate,
        endDateExclusive: LocalDate
    ): Flow<List<Income>> = observeIncomeSnapshots(startDate, endDateExclusive)
        .map { snapshot -> snapshot.incomes }

    suspend fun saveIncome(
        income: Income
    ): Result<Unit>

    suspend fun saveIncomeForOwner(
        expectedOwnerId: UserId,
        income: Income
    ): Result<Unit>

    suspend fun deleteIncome(
        id: DocumentId
    ): Result<Unit>

    suspend fun deleteIncomeForOwner(
        expectedOwnerId: UserId,
        id: DocumentId
    ): Result<Unit>
}

sealed class IncomeDeleteException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    class PermissionDenied(cause: Throwable? = null) : IncomeDeleteException(
        message = "Income deletion permission denied",
        cause = cause
    )

    class NetworkUnavailable(cause: Throwable? = null) : IncomeDeleteException(
        message = "Income deletion network unavailable",
        cause = cause
    )
}
