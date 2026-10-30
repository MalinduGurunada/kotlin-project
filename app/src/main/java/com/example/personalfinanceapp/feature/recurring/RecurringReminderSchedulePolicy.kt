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
        val scheduled = runCatching {
            LocalDate.parse(reminder.remindOnIso)
                .atTime(LocalTime.of(9, 0))
                .atZone(zoneId)
                .toInstant()
        }.getOrNull() ?: return null
        return scheduled.takeIf { it.isAfter(now) }?.toEpochMilli()
    }
}
