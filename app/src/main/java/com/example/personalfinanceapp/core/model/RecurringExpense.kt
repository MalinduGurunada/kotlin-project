
package com.example.personalfinanceapp.core.model

import java.time.LocalDate
import java.time.Instant

enum class RecurrenceFrequency {
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class RecurringStatus {
    ACTIVE,
    PAUSED,
    CANCELLED
}

enum class RecurringOccurrenceStatus {
    PENDING,
    CONFIRMED,
    SKIPPED
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
    val status: RecurringStatus = RecurringStatus.ACTIVE,
    val paymentChannel: PaymentChannel = PaymentChannel.BANK,
    val channelLabel: String? = null,
    val reminderEnabled: Boolean = false,
    val reminderLeadDays: Long = 0L
) {
    val active: Boolean
        get() = status == RecurringStatus.ACTIVE
}

data class RecurringOccurrence(
    val id: DocumentId,
    val recurringExpenseId: DocumentId,
    val dueDate: LocalDate,
    val status: RecurringOccurrenceStatus = RecurringOccurrenceStatus.PENDING,
    val expenseId: DocumentId? = null,
    val actionAt: Instant? = null
)
