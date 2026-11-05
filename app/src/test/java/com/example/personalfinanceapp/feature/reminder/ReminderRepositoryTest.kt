package com.example.personalfinanceapp.feature.reminder

import com.example.personalfinanceapp.core.database.ReminderDao
import com.example.personalfinanceapp.core.database.ReminderEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReminderRepositoryTest {

    private lateinit var reminderDao: ReminderDao
    private lateinit var repository: RoomReminderRepository

    @Before
    fun setUp() {
        reminderDao = ReminderDao()
        repository = RoomReminderRepository(reminderDao)
    }

    @Test
    fun addAndObserveUserReminders() = runTest {
        val reminder = ReminderEntity(
            id = "rem-1",
            userId = "user-100",
            title = "Electricity Bill",
            category = "Utilities",
            amountMinor = 4850_00L,
            dueDateIso = "2026-10-31"
        )

        val result = repository.addReminder(reminder)
        assertTrue(result.isSuccess)

        val list = repository.observeReminders("user-100").first()
        assertEquals(1, list.size)
        assertEquals("Electricity Bill", list.first().title)
    }

    @Test
    fun clearRemindersOnLogout() = runTest {
        val reminder = ReminderEntity(
            id = "rem-1",
            userId = "user-100",
            title = "Tuition Fee",
            category = "Education",
            amountMinor = 15000_00L,
            dueDateIso = "2026-10-28"
        )

        repository.addReminder(reminder)
        repository.clearUserRemindersOnLogout("user-100")

        val list = repository.observeReminders("user-100").first()
        assertTrue(list.isEmpty())
    }
}
