package com.example.personalfinanceapp.feature.reminder.daily

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

internal object DailyCaptureReminderSchedulePolicy {
    fun nextTrigger(
        now: Instant,
        zoneId: ZoneId,
        timeMinutes: Int
    ): Instant {
        require(timeMinutes in 0 until DailyCaptureReminderSettings.MINUTES_PER_DAY)
        val localTime = LocalTime.of(timeMinutes / 60, timeMinutes % 60)
        val nowAtZone = now.atZone(zoneId)
        var candidate = ZonedDateTime.of(nowAtZone.toLocalDate(), localTime, zoneId)
        if (!candidate.toInstant().isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        return candidate.toInstant()
    }
}

internal object DailyCaptureReminderDeliveryPolicy {
    fun claimDeliveryIfAllowed(
        scheduledUserId: String?,
        currentUserId: String?,
        reminderEnabled: Boolean,
        notificationsEnabled: Boolean,
        notificationPermissionGranted: Boolean,
        hasExpenseToday: Boolean,
        claimDelivery: () -> Boolean
    ): Boolean =
        !scheduledUserId.isNullOrBlank() &&
            scheduledUserId == currentUserId &&
            reminderEnabled &&
            notificationsEnabled &&
            notificationPermissionGranted &&
            !hasExpenseToday &&
            claimDelivery()
}

fun formatDailyCaptureReminderTime(timeMinutes: Int): String {
    require(timeMinutes in 0 until DailyCaptureReminderSettings.MINUTES_PER_DAY)
    val hour24 = timeMinutes / 60
    val minute = timeMinutes % 60
    val suffix = if (hour24 < 12) "AM" else "PM"
    val hour12 = when (val value = hour24 % 12) {
        0 -> 12
        else -> value
    }
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, suffix)
}

data class DailyCaptureNotificationTarget(val userId: String) {
    companion object {
        internal const val EXTRA_TYPE = "daily_capture_notification_type"
        internal const val EXTRA_USER_ID = "daily_capture_notification_user_id"
        internal const val TYPE = "daily_expense_capture"

        fun fromIntent(intent: android.content.Intent?): DailyCaptureNotificationTarget? {
            intent ?: return null
            if (intent.getStringExtra(EXTRA_TYPE) != TYPE) return null
            val userId = intent.getStringExtra(EXTRA_USER_ID)
                ?.trim()
                ?.takeIf { it.isNotEmpty() && it.length <= 128 }
                ?: return null
            return DailyCaptureNotificationTarget(userId)
        }
    }
}
