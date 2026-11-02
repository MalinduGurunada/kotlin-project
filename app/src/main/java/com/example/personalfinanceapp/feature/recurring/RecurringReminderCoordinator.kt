package com.example.personalfinanceapp.feature.recurring

import com.example.personalfinanceapp.core.model.RecurringExpense
import java.time.LocalDate

interface RecurringReminderCoordinator {

    suspend fun sync(
        recurringExpense: RecurringExpense,
        dueDates: List<LocalDate>
    ): Result<Unit>

    suspend fun cancelOccurrence(
        recurringExpenseId: String,
        dueDate: LocalDate
    ): Result<Unit>
}
