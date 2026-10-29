package com.example.personalfinanceapp.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReminderDao {

    private val remindersFlow = MutableStateFlow<List<ReminderEntity>>(emptyList())

    fun getRemindersForUser(userId: String): Flow<List<ReminderEntity>> {
        return remindersFlow.asStateFlow()
    }

    suspend fun insertReminder(reminder: ReminderEntity) {
        val current = remindersFlow.value
        if (current.none { it.id == reminder.id }) {
            remindersFlow.value = current + reminder
        }
    }

    suspend fun markDelivered(id: String) {
        remindersFlow.value = remindersFlow.value.map {
            if (it.id == id) it.copy(isDelivered = true) else it
        }
    }

    suspend fun deleteUserReminders(userId: String) {
        remindersFlow.value = remindersFlow.value.filter { it.userId != userId }
    }
}
