
package com.example.personalfinanceapp.feature.recurring

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.RecurringOccurrence
import com.example.personalfinanceapp.core.model.RecurringOccurrenceStatus
import com.example.personalfinanceapp.core.model.RecurringExpense
import com.example.personalfinanceapp.core.model.RecurringStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate

class FakeRecurringRepository : RecurringRepository {

    private val recurringExpenses =
        MutableStateFlow<List<RecurringExpense>>(emptyList())

    private val occurrences =
        MutableStateFlow<List<RecurringOccurrence>>(emptyList())

    private val mutex = Mutex()

    override fun observeRecurringExpenses(): Flow<List<RecurringExpense>> {
        return recurringExpenses.asStateFlow()
    }

    override fun observeOccurrences(): Flow<List<RecurringOccurrence>> =
        occurrences.asStateFlow()

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

    override suspend fun setRecurringExpenseStatus(
        id: DocumentId,
        status: RecurringStatus
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
                expense.copy(status = status)
            } else {
                expense
            }
        }

        Result.success(Unit)
    }

    override suspend fun confirmPayment(
        recurringExpenseId: DocumentId,
        dueDate: LocalDate,
        paidDate: LocalDate,
        amountMinor: Long
    ): Result<DocumentId> = mutex.withLock {
        if (amountMinor <= 0L) {
            return@withLock Result.failure(IllegalArgumentException("Payment details are invalid"))
        }
        if (recurringExpenses.value.none { it.id == recurringExpenseId }) {
            return@withLock Result.failure(NoSuchElementException("Recurring expense not found"))
        }

        val occurrenceId = RecurringScheduleCalculator.occurrenceId(recurringExpenseId, dueDate)
        val existing = occurrences.value.firstOrNull { it.id == occurrenceId }
        if (existing?.status == RecurringOccurrenceStatus.SKIPPED) {
            return@withLock Result.failure(IllegalStateException("Skipped occurrence cannot be confirmed"))
        }
        val expenseId = existing?.expenseId
            ?: RecurringScheduleCalculator.expenseId(recurringExpenseId, dueDate)
        val confirmed = RecurringOccurrence(
            id = occurrenceId,
            recurringExpenseId = recurringExpenseId,
            dueDate = dueDate,
            status = RecurringOccurrenceStatus.CONFIRMED,
            expenseId = expenseId,
            actionAt = Instant.now()
        )
        occurrences.value = occurrences.value.filterNot { it.id == occurrenceId } + confirmed
        Result.success(expenseId)
    }

    override suspend fun skipOccurrence(
        recurringExpenseId: DocumentId,
        dueDate: LocalDate
    ): Result<Unit> = mutex.withLock {
        if (recurringExpenses.value.none { it.id == recurringExpenseId }) {
            return@withLock Result.failure(NoSuchElementException("Recurring expense not found"))
        }
        val occurrenceId = RecurringScheduleCalculator.occurrenceId(recurringExpenseId, dueDate)
        val existing = occurrences.value.firstOrNull { it.id == occurrenceId }
        if (existing?.status == RecurringOccurrenceStatus.CONFIRMED) {
            return@withLock Result.failure(IllegalStateException("Confirmed occurrence cannot be skipped"))
        }
        if (existing?.status == RecurringOccurrenceStatus.SKIPPED) {
            return@withLock Result.success(Unit)
        }
        val skipped = RecurringOccurrence(
            id = occurrenceId,
            recurringExpenseId = recurringExpenseId,
            dueDate = dueDate,
            status = RecurringOccurrenceStatus.SKIPPED,
            actionAt = Instant.now()
        )
        occurrences.value = occurrences.value.filterNot { it.id == occurrenceId } + skipped
        Result.success(Unit)
    }
}
