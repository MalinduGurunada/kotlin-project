package com.example.personalfinanceapp.feature.reminder

import com.example.personalfinanceapp.core.database.ReminderDao
import com.example.personalfinanceapp.core.database.ReminderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao
) {

    fun observeReminders(userId: String): Flow<List<ReminderEntity>> {
        return reminderDao.getRemindersForUser(userId)
    }

    suspend fun addReminder(reminder: ReminderEntity): Result<Unit> {
        return try {
            reminderDao.insertReminder(reminder)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markDelivered(id: String): Result<Unit> {
        return try {
            reminderDao.markDelivered(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearUserRemindersOnLogout(userId: String): Result<Unit> {
        return try {
            reminderDao.deleteUserReminders(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
