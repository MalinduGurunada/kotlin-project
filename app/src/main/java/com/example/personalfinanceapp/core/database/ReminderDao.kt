package com.example.personalfinanceapp.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class ReminderDao {

    private val remindersFlow = MutableStateFlow<List<ReminderEntity>>(emptyList())

    fun getRemindersForUser(userId: String): Flow<List<ReminderEntity>> {
        return remindersFlow.asStateFlow().map { reminders ->
            reminders.filter { it.userId == userId }
        }
    }

    suspend fun insertReminder(reminder: ReminderEntity) {
        val current = remindersFlow.value
        remindersFlow.value = current.filterNot { it.id == reminder.id } + reminder
    }

    suspend fun markDelivered(id: String) {
        remindersFlow.value = remindersFlow.value.map {
            if (it.id == id) it.copy(isDelivered = true) else it
        }
    }

    suspend fun deleteUserReminders(userId: String) {
        remindersFlow.value = remindersFlow.value.filter { it.userId != userId }
    }

    suspend fun deleteReminder(id: String) {
        remindersFlow.value = remindersFlow.value.filterNot { it.id == id }
    }

    suspend fun deleteRecurringReminders(userId: String, recurringExpenseId: String) {
        remindersFlow.value = remindersFlow.value.filterNot {
            it.userId == userId && it.recurringExpenseId == recurringExpenseId
        }
    }
}
