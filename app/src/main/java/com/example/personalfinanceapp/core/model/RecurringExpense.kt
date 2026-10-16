
package com.example.personalfinanceapp.core.model

import java.time.LocalDate

enum class RecurrenceFrequency {
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class RecurringExpense(
    val id: DocumentId,
    val title: String,
    val categoryId: DocumentId,
    val expectedAmountMinor: Long,
    val frequency: RecurrenceFrequency,
    val intervalCount: Long = 1L,
    val anchorDate: LocalDate,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val active: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderLeadDays: Long = 0L
)
