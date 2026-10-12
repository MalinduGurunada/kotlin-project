
package com.example.personalfinanceapp.feature.recurring

import com.example.personalfinanceapp.core.model.RecurrenceFrequency
import com.example.personalfinanceapp.core.model.RecurringExpense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FakeRecurringRepositoryTest {

    private fun createRecurringExpense(
        id: String = "internet-001"
    ): RecurringExpense {
        return RecurringExpense(
            id = id,
            title = "Internet Bill",
            categoryId = "bills",
            expectedAmountMinor = 350_000L,
            frequency = RecurrenceFrequency.MONTHLY,
            intervalCount = 1L,
            anchorDate = LocalDate.of(2026, 10, 15),
            startDate = LocalDate.of(2026, 10, 15),
            reminderEnabled = true,
            reminderLeadDays = 2L
        )
    }

    @Test
    fun initialRecurringExpenseListIsEmpty() = runTest {
        val repository = FakeRecurringRepository()

        val expenses = repository
            .observeRecurringExpenses()
            .first()

        assertTrue(expenses.isEmpty())
    }

    @Test
    fun saveRecurringExpenseSuccessfully() = runTest {
        val repository = FakeRecurringRepository()

        val result = repository.saveRecurringExpense(
            createRecurringExpense()
        )

        assertTrue(result.isSuccess)

        val expenses = repository
            .observeRecurringExpenses()
            .first()

        assertEquals(1, expenses.size)
        assertEquals("Internet Bill", expenses.first().title)
    }

    @Test
    fun updateRecurringExpenseWithoutDuplicate() = runTest {
        val repository = FakeRecurringRepository()

        val expense = createRecurringExpense()

        repository.saveRecurringExpense(expense)

        val updatedExpense = expense.copy(
            title = "Home Internet",
            expectedAmountMinor = 400_000L
        )

        val result = repository.saveRecurringExpense(
            updatedExpense
        )

        assertTrue(result.isSuccess)

        val expenses = repository
            .observeRecurringExpenses()
            .first()

        assertEquals(1, expenses.size)
        assertEquals("Home Internet", expenses.first().title)
        assertEquals(400_000L, expenses.first().expectedAmountMinor)
    }

    @Test
    fun pauseRecurringExpenseSuccessfully() = runTest {
        val repository = FakeRecurringRepository()

        repository.saveRecurringExpense(
            createRecurringExpense()
        )

        val result = repository.setRecurringExpenseActive(
            id = "internet-001",
            active = false
        )

        assertTrue(result.isSuccess)

        val expense = repository
            .observeRecurringExpenses()
            .first()
            .first()

        assertFalse(expense.active)
    }

    @Test
    fun reactivateRecurringExpenseSuccessfully() = runTest {
        val repository = FakeRecurringRepository()

        repository.saveRecurringExpense(
            createRecurringExpense()
        )

        repository.setRecurringExpenseActive(
            id = "internet-001",
            active = false
        )

        val result = repository.setRecurringExpenseActive(
            id = "internet-001",
            active = true
        )

        assertTrue(result.isSuccess)

        val expense = repository
            .observeRecurringExpenses()
            .first()
            .first()

        assertTrue(expense.active)
    }

    @Test
    fun rejectBlankTitle() = runTest {
        val repository = FakeRecurringRepository()

        val expense = createRecurringExpense().copy(
            title = "   "
        )

        val result = repository.saveRecurringExpense(expense)

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectZeroExpectedAmount() = runTest {
        val repository = FakeRecurringRepository()

        val expense = createRecurringExpense().copy(
            expectedAmountMinor = 0L
        )

        val result = repository.saveRecurringExpense(expense)

        assertTrue(result.isFailure)

        assertTrue(
            repository.observeRecurringExpenses().first().isEmpty()
        )
    }

    @Test
    fun rejectInvalidIntervalCount() = runTest {
        val repository = FakeRecurringRepository()

        val expense = createRecurringExpense().copy(
            intervalCount = 0L
        )

        val result = repository.saveRecurringExpense(expense)

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectEndDateBeforeStartDate() = runTest {
        val repository = FakeRecurringRepository()

        val expense = createRecurringExpense().copy(
            endDate = LocalDate.of(2026, 10, 1)
        )

        val result = repository.saveRecurringExpense(expense)

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectNegativeReminderLeadDays() = runTest {
        val repository = FakeRecurringRepository()

        val expense = createRecurringExpense().copy(
            reminderLeadDays = -1L
        )

        val result = repository.saveRecurringExpense(expense)

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectUpdatingMissingRecurringExpense() = runTest {
        val repository = FakeRecurringRepository()

        val result = repository.setRecurringExpenseActive(
            id = "missing-id",
            active = false
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun trimRecurringExpenseTitle() = runTest {
        val repository = FakeRecurringRepository()

        val expense = createRecurringExpense().copy(
            title = "  Internet Bill  "
        )

        repository.saveRecurringExpense(expense)

        val savedExpense = repository
            .observeRecurringExpenses()
            .first()
            .first()

        assertEquals("Internet Bill", savedExpense.title)
    }
}
