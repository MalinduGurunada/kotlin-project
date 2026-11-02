package com.example.personalfinanceapp.feature.recurring

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringReminderDeliveryPolicyTest {

    @Test
    fun currentOwnerWithPermissionCanReceiveReminder() {
        assertTrue(
            RecurringReminderDeliveryPolicy.canDeliver(
                scheduledUserId = "user-a",
                currentUserId = "user-a",
                notificationPermissionGranted = true
            )
        )
    }

    @Test
    fun deniedPermissionBlocksReminder() {
        assertFalse(
            RecurringReminderDeliveryPolicy.canDeliver(
                scheduledUserId = "user-a",
                currentUserId = "user-a",
                notificationPermissionGranted = false
            )
        )
    }

    @Test
    fun logoutBlocksPreviouslyScheduledReminder() {
        assertFalse(
            RecurringReminderDeliveryPolicy.canDeliver(
                scheduledUserId = "user-a",
                currentUserId = null,
                notificationPermissionGranted = true
            )
        )
    }

    @Test
    fun accountSwitchBlocksPreviousOwnersReminder() {
        assertFalse(
            RecurringReminderDeliveryPolicy.canDeliver(
                scheduledUserId = "user-a",
                currentUserId = "user-b",
                notificationPermissionGranted = true
            )
        )
    }

    @Test
    fun reminderWithoutOwnerIsRejected() {
        assertFalse(
            RecurringReminderDeliveryPolicy.canDeliver(
                scheduledUserId = null,
                currentUserId = "user-a",
                notificationPermissionGranted = true
            )
        )
    }

    @Test
    fun sameOccurrenceCanProduceOnlyOneNotification() {
        var claimed = false
        var notificationCount = 0
        val claim = {
            if (claimed) {
                false
            } else {
                claimed = true
                true
            }
        }

        repeat(2) {
            if (RecurringReminderDeliveryPolicy.claimDeliveryIfAllowed(
                    scheduledUserId = "user-a",
                    currentUserId = "user-a",
                    notificationPermissionGranted = true,
                    claimDelivery = claim
                )
            ) {
                notificationCount += 1
            }
        }

        assertEquals(1, notificationCount)
    }
}
