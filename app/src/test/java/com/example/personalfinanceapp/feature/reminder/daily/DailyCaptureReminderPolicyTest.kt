package com.example.personalfinanceapp.feature.reminder.daily

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyCaptureReminderPolicyTest {

    @Test
    fun nextTriggerUsesTodayWhenSelectedTimeIsStillAhead() {
        val trigger = DailyCaptureReminderSchedulePolicy.nextTrigger(
            now = Instant.parse("2026-11-05T10:00:00Z"),
            zoneId = ZoneOffset.UTC,
            timeMinutes = 20 * 60
        )

        assertEquals(Instant.parse("2026-11-05T20:00:00Z"), trigger)
    }

    @Test
    fun nextTriggerMovesToTomorrowAfterSelectedTime() {
        val trigger = DailyCaptureReminderSchedulePolicy.nextTrigger(
            now = Instant.parse("2026-11-05T20:00:00Z"),
            zoneId = ZoneOffset.UTC,
            timeMinutes = 20 * 60
        )

        assertEquals(Instant.parse("2026-11-06T20:00:00Z"), trigger)
    }

    @Test
    fun expenseRecordedTodaySuppressesReminder() {
        val delivered = DailyCaptureReminderDeliveryPolicy.claimDeliveryIfAllowed(
            scheduledUserId = "user-1",
            currentUserId = "user-1",
            reminderEnabled = true,
            notificationsEnabled = true,
            notificationPermissionGranted = true,
            hasExpenseToday = true,
            claimDelivery = { true }
        )

        assertFalse(delivered)
    }

    @Test
    fun oneDayCanBeClaimedOnlyOnce() {
        var claimed = false
        val claim = {
            if (claimed) {
                false
            } else {
                claimed = true
                true
            }
        }

        val first = eligibleDelivery(claim)
        val second = eligibleDelivery(claim)

        assertTrue(first)
        assertFalse(second)
    }

    @Test
    fun selectedTimeIsFormattedWithAmAndPm() {
        assertEquals("12:05 AM", formatDailyCaptureReminderTime(5))
        assertEquals("12:30 PM", formatDailyCaptureReminderTime(12 * 60 + 30))
        assertEquals("8:15 PM", formatDailyCaptureReminderTime(20 * 60 + 15))
    }

    private fun eligibleDelivery(claim: () -> Boolean) =
        DailyCaptureReminderDeliveryPolicy.claimDeliveryIfAllowed(
            scheduledUserId = "user-1",
            currentUserId = "user-1",
            reminderEnabled = true,
            notificationsEnabled = true,
            notificationPermissionGranted = true,
            hasExpenseToday = false,
            claimDelivery = claim
        )
}
