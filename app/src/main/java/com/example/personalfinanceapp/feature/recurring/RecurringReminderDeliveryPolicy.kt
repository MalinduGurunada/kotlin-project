package com.example.personalfinanceapp.feature.recurring

internal object RecurringReminderDeliveryPolicy {

    fun canDeliver(
        scheduledUserId: String?,
        currentUserId: String?,
        notificationPermissionGranted: Boolean
    ): Boolean =
        notificationPermissionGranted &&
            !scheduledUserId.isNullOrBlank() &&
            scheduledUserId == currentUserId

    fun claimDeliveryIfAllowed(
        scheduledUserId: String?,
        currentUserId: String?,
        notificationPermissionGranted: Boolean,
        claimDelivery: () -> Boolean
    ): Boolean =
        canDeliver(
            scheduledUserId = scheduledUserId,
            currentUserId = currentUserId,
            notificationPermissionGranted = notificationPermissionGranted
        ) && claimDelivery()
}
