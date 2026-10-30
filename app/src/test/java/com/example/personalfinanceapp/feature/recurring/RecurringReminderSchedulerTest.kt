package com.example.personalfinanceapp.feature.recurring

import com.example.personalfinanceapp.core.model.RecurrenceFrequency
import com.example.personalfinanceapp.core.model.RecurringExpense
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurringReminderSchedulerTest {

    @Test
    fun reminderUsesLeadDaysAndStableOccurrenceId() {
        val schedule = RecurringExpense(
            id = "electricity",
            title = "CEB Electricity Bill",
            categoryId = "utilities",
            expectedAmountMinor = 12_500_00L,
            frequency = RecurrenceFrequency.MONTHLY,
            anchorDate = LocalDate.of(2026, 10, 31),
            startDate = LocalDate.of(2026, 10, 31),
            reminderEnabled = true,
            reminderLeadDays = 2L
        )

        val reminder = RecurringReminderScheduler.buildReminder(
            "user-1",
            schedule,
            LocalDate.of(2026, 10, 31)
        )

        assertEquals("2026-10-29", reminder.remindOnIso)
        assertEquals("electricity_2026-10-31", reminder.recurringOccurrenceId)
        assertEquals("reminder_electricity_2026-10-31", reminder.id)
    }
}
