
package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

class FakeIncomeRepository : IncomeRepository {

    private val incomes = MutableStateFlow<List<Income>>(emptyList())

    private val mutex = Mutex()

    override fun observeIncomes(
        startDate: LocalDate,
        endDateExclusive: LocalDate
    ): Flow<List<Income>> {

        require(startDate.isBefore(endDateExclusive)) {
            "Start date must be before end date"
        }

        return incomes.map { allIncomes ->

            allIncomes.filter { income ->
                !income.occurredDate.isBefore(startDate) &&
                        income.occurredDate.isBefore(endDateExclusive)
            }.sortedWith(
                compareBy<Income> { it.occurredDate }
                    .thenBy { it.id }
            )

        }.distinctUntilChanged()
    }

    override fun observeIncomeSnapshots(
        startDate: LocalDate,
        endDateExclusive: LocalDate
    ): Flow<IncomeObservationSnapshot> = observeIncomes(startDate, endDateExclusive)
        .map { records ->
            IncomeObservationSnapshot.forOwner(FAKE_OBSERVATION_OWNER_ID, records)
        }

    override suspend fun saveIncome(
        income: Income
    ): Result<Unit> = saveValidatedIncome(income)

    override suspend fun saveIncomeForOwner(
        expectedOwnerId: UserId,
        income: Income
    ): Result<Unit> {
        if (expectedOwnerId.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Expected income owner cannot be empty")
            )
        }
        return saveValidatedIncome(income)
    }

    private suspend fun saveValidatedIncome(
        income: Income
    ): Result<Unit> {
        val validationResult = IncomeValidator.validate(income)

        if (validationResult.isFailure) {
            return validationResult
        }

        return mutex.withLock {
            val current = incomes.value

            val existingIndex = current.indexOfFirst {
                it.id == income.id
            }

            incomes.value = if (existingIndex >= 0) {

                current.toMutableList().apply {
                    set(existingIndex, income)
                }

            } else {

                current + income
            }

            Result.success(Unit)
        }
    }

    override suspend fun deleteIncome(
        id: DocumentId
    ): Result<Unit> = deleteIncomeInternal(id)

    override suspend fun deleteIncomeForOwner(
        expectedOwnerId: UserId,
        id: DocumentId
    ): Result<Unit> {
        if (expectedOwnerId.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Expected income owner cannot be empty")
            )
        }
        return deleteIncomeInternal(id)
    }

    private suspend fun deleteIncomeInternal(
        id: DocumentId
    ): Result<Unit> = mutex.withLock {

        val current = incomes.value

        if (current.none { it.id == id }) {
            return@withLock Result.failure(
                NoSuchElementException("Income not found")
            )
        }

        incomes.value = current.filter {
            it.id != id
        }

        Result.success(Unit)
    }

    private companion object {
        const val FAKE_OBSERVATION_OWNER_ID = "fake-income-owner"
    }
}
