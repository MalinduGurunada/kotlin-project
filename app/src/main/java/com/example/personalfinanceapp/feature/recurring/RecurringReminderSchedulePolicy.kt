package com.example.personalfinanceapp.feature.recurring

import com.example.personalfinanceapp.core.database.ReminderEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

internal object RecurringReminderSchedulePolicy {

    fun triggerAtMillis(
        reminder: ReminderEntity,
        wasDelivered: Boolean,
        now: Instant,
        zoneId: ZoneId
    ): Long? {
        if (wasDelivered) return null
        if (reminder.reminderTimeMinutes !in 0 until MINUTES_PER_DAY) return null
        val scheduled = runCatching {
            LocalDate.parse(reminder.remindOnIso)
                .atTime(
                    LocalTime.of(
                        reminder.reminderTimeMinutes / MINUTES_PER_HOUR,
                        reminder.reminderTimeMinutes % MINUTES_PER_HOUR
                    )
                )
                .atZone(zoneId)
                .toInstant()
        }.getOrNull() ?: return null
        return scheduled.takeIf { it.isAfter(now) }?.toEpochMilli()
    }

    private const val MINUTES_PER_HOUR = 60
    private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
}
