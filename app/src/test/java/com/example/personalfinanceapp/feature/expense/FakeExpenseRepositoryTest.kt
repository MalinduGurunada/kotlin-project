
package com.example.personalfinanceapp.feature.expense

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.SpendingClass
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FakeExpenseRepositoryTest {

    private val startDate = LocalDate.of(2026, 10, 1)
    private val endDate = LocalDate.of(2026, 11, 1)

    private fun createExpense(
        id: String,
        date: LocalDate = LocalDate.of(2026, 10, 12)
    ): Expense {
        return Expense(
            id = id,
            categoryId = "food",
            categoryNameSnapshot = "Food",
            spendingClass = SpendingClass.DISCRETIONARY,
            paymentChannel = PaymentChannel.CASH,
            money = Money(
                originalAmountDecimal = "1500.00",
                originalCurrency = CurrencyCode.LKR,
                rateLkrPerUnitDecimal = "1",
                baseAmountMinor = 150_000L,
                rateSource = RateSource.LKR_FIXED,
                rateDate = date
            ),
            occurredDate = date,
            note = "Lunch"
        )
    }

    @Test
    fun initialExpenseListIsEmpty() = runTest {
        val repository = FakeExpenseRepository()

        val expenses = repository.observeExpenses(
            startDate,
            endDate
        ).first()

        assertTrue(expenses.isEmpty())
    }

    @Test
    fun saveExpenseSuccessfully() = runTest {
        val repository = FakeExpenseRepository()

        val expense = createExpense("expense-001")

        val result = repository.saveExpense(expense)

        assertTrue(result.isSuccess)

        val expenses = repository.observeExpenses(
            startDate,
            endDate
        ).first()

        assertEquals(1, expenses.size)
        assertEquals("expense-001", expenses.first().id)
    }

    @Test
    fun updateExpenseWithoutCreatingDuplicate() = runTest {
        val repository = FakeExpenseRepository()

        val expense = createExpense("expense-001")

        repository.saveExpense(expense)

        val updatedExpense = expense.copy(
            note = "Dinner"
        )

        val result = repository.saveExpense(updatedExpense)

        assertTrue(result.isSuccess)

        val expenses = repository.observeExpenses(
            startDate,
            endDate
        ).first()

        assertEquals(1, expenses.size)
        assertEquals("Dinner", expenses.first().note)
    }

    @Test
    fun filterExpensesByDateRange() = runTest {
        val repository = FakeExpenseRepository()

        repository.saveExpense(
            createExpense(
                "expense-001",
                LocalDate.of(2026, 10, 1)
            )
        )

        repository.saveExpense(
            createExpense(
                "expense-002",
                LocalDate.of(2026, 10, 31)
            )
        )

        repository.saveExpense(
            createExpense(
                "expense-003",
                LocalDate.of(2026, 11, 1)
            )
        )

        val expenses = repository.observeExpenses(
            startDate,
            endDate
        ).first()

        assertEquals(2, expenses.size)

        assertEquals(
            listOf("expense-001", "expense-002"),
            expenses.map { it.id }
        )
    }

    @Test
    fun deleteExpenseSuccessfully() = runTest {
        val repository = FakeExpenseRepository()

        repository.saveExpense(
            createExpense("expense-001")
        )

        val result = repository.deleteExpense("expense-001")

        assertTrue(result.isSuccess)

        val expenses = repository.observeExpenses(
            startDate,
            endDate
        ).first()

        assertTrue(expenses.isEmpty())
    }

    @Test
    fun rejectBlankExpenseId() = runTest {
        val repository = FakeExpenseRepository()

        val expense = createExpense("")

        val result = repository.saveExpense(expense)

        assertTrue(result.isFailure)

        val expenses = repository.observeExpenses(
            startDate,
            endDate
        ).first()

        assertTrue(expenses.isEmpty())
    }

    @Test
    fun rejectDeletingMissingExpense() = runTest {
        val repository = FakeExpenseRepository()

        val result = repository.deleteExpense("missing-id")

        assertTrue(result.isFailure)
    }
}
