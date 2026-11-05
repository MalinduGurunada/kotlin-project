package com.example.personalfinanceapp.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY dueDateIso ASC")
    fun getRemindersForUser(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY dueDateIso ASC")
    suspend fun getRemindersListForUser(userId: String): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isDelivered = 1 WHERE id = :id")
    suspend fun markDelivered(id: String)

    @Query("DELETE FROM reminders WHERE userId = :userId")
    suspend fun deleteUserReminders(userId: String)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: String)

    @Query("DELETE FROM reminders WHERE userId = :userId AND recurringExpenseId = :recurringExpenseId")
    suspend fun deleteRecurringReminders(userId: String, recurringExpenseId: String)
}
