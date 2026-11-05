package com.example.personalfinanceapp.feature.reminder

import com.example.personalfinanceapp.core.database.ReminderDao
import com.example.personalfinanceapp.core.database.ReminderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeReminderDao : ReminderDao {

    private val remindersFlow = MutableStateFlow<List<ReminderEntity>>(emptyList())

    override fun getRemindersForUser(userId: String): Flow<List<ReminderEntity>> {
        return remindersFlow.asStateFlow().map { list ->
            list.filter { it.userId == userId }
        }
    }

    override suspend fun getRemindersListForUser(userId: String): List<ReminderEntity> {
        return remindersFlow.value.filter { it.userId == userId }
    }

    override suspend fun insertReminder(reminder: ReminderEntity) {
        val current = remindersFlow.value.filterNot { it.id == reminder.id }
        remindersFlow.value = current + reminder
    }

    override suspend fun markDelivered(id: String) {
        remindersFlow.value = remindersFlow.value.map {
            if (it.id == id) it.copy(isDelivered = true) else it
        }
    }

    override suspend fun deleteUserReminders(userId: String) {
        remindersFlow.value = remindersFlow.value.filterNot { it.userId == userId }
    }

    override suspend fun deleteReminder(id: String) {
        remindersFlow.value = remindersFlow.value.filterNot { it.id == id }
    }

    override suspend fun deleteRecurringReminders(userId: String, recurringExpenseId: String) {
        remindersFlow.value = remindersFlow.value.filterNot {
            it.userId == userId && it.recurringExpenseId == recurringExpenseId
        }
    }
}
