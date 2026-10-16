
package com.example.personalfinanceapp.feature.recurring

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.RecurringExpense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeRecurringRepository : RecurringRepository {

    private val recurringExpenses =
        MutableStateFlow<List<RecurringExpense>>(emptyList())

    private val mutex = Mutex()

    override fun observeRecurringExpenses(): Flow<List<RecurringExpense>> {
        return recurringExpenses.asStateFlow()
    }

    override suspend fun saveRecurringExpense(
        recurringExpense: RecurringExpense
    ): Result<Unit> = mutex.withLock {

        if (
            recurringExpense.id.isBlank() ||
            recurringExpense.title.isBlank() ||
            recurringExpense.categoryId.isBlank()
        ) {
            return@withLock Result.failure(
                IllegalArgumentException(
                    "ID, title and category cannot be blank"
                )
            )
        }

        if (recurringExpense.expectedAmountMinor <= 0L) {
            return@withLock Result.failure(
                IllegalArgumentException(
                    "Expected amount must be positive"
                )
            )
        }

        if (recurringExpense.intervalCount <= 0L) {
            return@withLock Result.failure(
                IllegalArgumentException(
                    "Interval count must be positive"
                )
            )
        }

        if (
            recurringExpense.endDate != null &&
            recurringExpense.endDate.isBefore(recurringExpense.startDate)
        ) {
            return@withLock Result.failure(
                IllegalArgumentException(
                    "End date cannot be before start date"
                )
            )
        }

        if (recurringExpense.reminderLeadDays < 0L) {
            return@withLock Result.failure(
                IllegalArgumentException(
                    "Reminder lead days cannot be negative"
                )
            )
        }

        val current = recurringExpenses.value

        val existingIndex = current.indexOfFirst {
            it.id == recurringExpense.id
        }

        val updatedExpense = recurringExpense.copy(
            title = recurringExpense.title.trim()
        )

        recurringExpenses.value = if (existingIndex >= 0) {

            current.toMutableList().apply {
                set(existingIndex, updatedExpense)
            }

        } else {

            current + updatedExpense
        }

        Result.success(Unit)
    }

    override suspend fun setRecurringExpenseActive(
        id: DocumentId,
        active: Boolean
    ): Result<Unit> = mutex.withLock {

        val current = recurringExpenses.value

        if (current.none { it.id == id }) {
            return@withLock Result.failure(
                NoSuchElementException(
                    "Recurring expense not found"
                )
            )
        }

        recurringExpenses.value = current.map { expense ->
            if (expense.id == id) {
                expense.copy(active = active)
            } else {
                expense
            }
        }

        Result.success(Unit)
    }
}
