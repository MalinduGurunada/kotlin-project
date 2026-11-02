package com.example.personalfinanceapp.feature.recurring

import com.example.personalfinanceapp.core.model.RecurrenceFrequency
import com.example.personalfinanceapp.core.model.RecurringExpense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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

    @Test
    fun expiredReminderIsNotScheduledAgain() {
        val reminder = reminder(remindOnIso = "2026-10-29")

        val triggerAt = RecurringReminderSchedulePolicy.triggerAtMillis(
            reminder = reminder,
            wasDelivered = false,
            now = Instant.parse("2026-10-30T12:00:00Z"),
            zoneId = ZoneOffset.UTC
        )

        assertNull(triggerAt)
    }

    @Test
    fun deliveredReminderIsNotScheduledAfterRefreshOrRestart() {
        val reminder = reminder(remindOnIso = "2026-10-31")

        val triggerAt = RecurringReminderSchedulePolicy.triggerAtMillis(
            reminder = reminder,
            wasDelivered = true,
            now = Instant.parse("2026-10-30T12:00:00Z"),
            zoneId = ZoneOffset.UTC
        )

        assertNull(triggerAt)
    }

    @Test
    fun futureUndeliveredReminderKeepsItsOriginalTriggerTime() {
        val reminder = reminder(remindOnIso = "2026-10-31")

        val triggerAt = RecurringReminderSchedulePolicy.triggerAtMillis(
            reminder = reminder,
            wasDelivered = false,
            now = Instant.parse("2026-10-30T12:00:00Z"),
            zoneId = ZoneOffset.UTC
        )

        assertEquals(
            Instant.parse("2026-10-31T09:00:00Z").toEpochMilli(),
            triggerAt
        )
    }

    private fun reminder(remindOnIso: String) =
        RecurringReminderScheduler.buildReminder(
            userId = "user-1",
            recurringExpense = RecurringExpense(
                id = "electricity",
                title = "CEB Electricity Bill",
                categoryId = "utilities",
                expectedAmountMinor = 12_500_00L,
                frequency = RecurrenceFrequency.MONTHLY,
                anchorDate = LocalDate.of(2026, 10, 31),
                startDate = LocalDate.of(2026, 10, 31),
                reminderEnabled = true
            ),
            dueDate = LocalDate.of(2026, 10, 31)
        ).copy(remindOnIso = remindOnIso)
}
