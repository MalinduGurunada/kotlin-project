package com.example.personalfinanceapp.core.database

data class ReminderEntity(
    val id: String,
    val userId: String,
    val title: String,
    val category: String,
    val amountMinor: Long,
    val dueDateIso: String,
    val remindOnIso: String = dueDateIso,
    val isDelivered: Boolean = false,
    val reminderType: String = "RECURRING_BILL",
    val recurringExpenseId: String? = null,
    val recurringOccurrenceId: String? = null
)
