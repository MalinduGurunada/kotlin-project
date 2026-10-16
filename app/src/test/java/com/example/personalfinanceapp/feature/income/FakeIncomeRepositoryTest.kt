
package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.RateSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FakeIncomeRepositoryTest {

    private val startDate = LocalDate.of(2026, 10, 1)
    private val endDate = LocalDate.of(2026, 11, 1)

    private fun createIncome(
        id: String,
        date: LocalDate = LocalDate.of(2026, 10, 12)
    ): Income {
        return Income(
            id = id,
            source = IncomeSource.SALARY,
            entryKind = IncomeEntryKind.RECEIPT,
            money = Money(
                originalAmountDecimal = "1000.00",
                originalCurrency = CurrencyCode.LKR,
                rateLkrPerUnitDecimal = "1",
                baseAmountMinor = 100_000L,
                rateSource = RateSource.LKR_FIXED,
                rateDate = date
            ),
            occurredDate = date
        )
    }

    @Test
    fun initialIncomeListIsEmpty() = runTest {
        val repository = FakeIncomeRepository()

        val incomes = repository.observeIncomes(
            startDate,
            endDate
        ).first()

        assertTrue(incomes.isEmpty())
    }

    @Test
    fun saveIncomeSuccessfully() = runTest {
        val repository = FakeIncomeRepository()

        val income = createIncome("income-001")

        val result = repository.saveIncome(income)

        assertTrue(result.isSuccess)

        val incomes = repository.observeIncomes(
            startDate,
            endDate
        ).first()

        assertEquals(1, incomes.size)
        assertEquals("income-001", incomes.first().id)
    }

    @Test
    fun updateIncomeWithoutCreatingDuplicate() = runTest {
        val repository = FakeIncomeRepository()

        val income = createIncome("income-001")

        repository.saveIncome(income)

        val updatedIncome = income.copy(
            note = "Updated salary"
        )

        val result = repository.saveIncome(updatedIncome)

        assertTrue(result.isSuccess)

        val incomes = repository.observeIncomes(
            startDate,
            endDate
        ).first()

        assertEquals(1, incomes.size)
        assertEquals("Updated salary", incomes.first().note)
    }

    @Test
    fun filterIncomeByDateRange() = runTest {
        val repository = FakeIncomeRepository()

        repository.saveIncome(
            createIncome("income-001", LocalDate.of(2026, 10, 1))
        )

        repository.saveIncome(
            createIncome("income-002", LocalDate.of(2026, 10, 31))
        )

        repository.saveIncome(
            createIncome("income-003", LocalDate.of(2026, 11, 1))
        )

        val incomes = repository.observeIncomes(
            startDate,
            endDate
        ).first()

        assertEquals(2, incomes.size)
        assertEquals(
            listOf("income-001", "income-002"),
            incomes.map { it.id }
        )
    }

    @Test
    fun deleteIncomeSuccessfully() = runTest {
        val repository = FakeIncomeRepository()

        repository.saveIncome(createIncome("income-001"))

        val result = repository.deleteIncome("income-001")

        assertTrue(result.isSuccess)

        val incomes = repository.observeIncomes(
            startDate,
            endDate
        ).first()

        assertTrue(incomes.isEmpty())
    }

    @Test
    fun rejectBlankIncomeId() = runTest {
        val repository = FakeIncomeRepository()

        val income = createIncome("")

        val result = repository.saveIncome(income)

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectDeletingMissingIncome() = runTest {
        val repository = FakeIncomeRepository()

        val result = repository.deleteIncome("missing-id")

        assertTrue(result.isFailure)
    }
}
