package com.example.personalfinanceapp.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val category: String,
    val amountMinor: Long,
    val dueDateIso: String,
    val remindOnIso: String = dueDateIso,
    val reminderTimeMinutes: Int = 9 * 60,
    val isDelivered: Boolean = false,
    val reminderType: String = "RECURRING_BILL",
    val recurringExpenseId: String? = null,
    val recurringOccurrenceId: String? = null
)
