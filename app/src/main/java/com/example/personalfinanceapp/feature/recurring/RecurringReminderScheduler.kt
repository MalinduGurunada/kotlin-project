package com.example.personalfinanceapp.feature.recurring

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.personalfinanceapp.core.database.ReminderEntity
import com.example.personalfinanceapp.core.model.RecurringExpense
import com.example.personalfinanceapp.feature.reminder.RoomReminderRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

class RecurringReminderScheduler @Inject constructor(
    private val reminderRepository: RoomReminderRepository,
    private val firebaseAuth: FirebaseAuth,
    @param:ApplicationContext private val context: Context
) {
    suspend fun sync(
        recurringExpense: RecurringExpense,
        dueDates: List<LocalDate>
    ): Result<Unit> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("User is not authenticated"))
        reminderRepository.observeReminders(uid).first()
            .filter { it.recurringExpenseId == recurringExpense.id }
            .forEach(::cancelAlarm)
        reminderRepository.removeRecurringReminders(uid, recurringExpense.id).getOrElse {
            return Result.failure(it)
        }
        if (!recurringExpense.active || !recurringExpense.reminderEnabled) {
            return Result.success(Unit)
        }
        dueDates.distinct().forEach { dueDate ->
            val reminder = buildReminder(uid, recurringExpense, dueDate)
            reminderRepository.addReminder(reminder).getOrElse { return Result.failure(it) }
            scheduleAlarm(reminder)
        }
        return Result.success(Unit)
    }

    suspend fun cancelOccurrence(recurringExpenseId: String, dueDate: LocalDate): Result<Unit> {
        val id = reminderId(recurringExpenseId, dueDate)
        cancelAlarmById(id)
        return reminderRepository.removeReminder(id)
    }

    private fun scheduleAlarm(reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val scheduled = LocalDate.parse(reminder.remindOnIso)
            .atTime(LocalTime.of(9, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val triggerAt = maxOf(scheduled, System.currentTimeMillis() + 5_000L)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(reminder)
        )
    }

    private fun cancelAlarm(reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(reminder))
    }

    private fun cancelAlarmById(id: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, RecurringReminderAlarmReceiver::class.java).setAction(id)
        val pending = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    private fun pendingIntent(reminder: ReminderEntity): PendingIntent {
        val intent = Intent(context, RecurringReminderAlarmReceiver::class.java)
            .setAction(reminder.id)
            .putExtra(RecurringReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            .putExtra(RecurringReminderAlarmReceiver.EXTRA_TITLE, reminder.title)
            .putExtra(RecurringReminderAlarmReceiver.EXTRA_AMOUNT_MINOR, reminder.amountMinor)
            .putExtra(RecurringReminderAlarmReceiver.EXTRA_DUE_DATE, reminder.dueDateIso)
        return PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        fun reminderId(recurringExpenseId: String, dueDate: LocalDate): String =
            "reminder_${RecurringScheduleCalculator.occurrenceId(recurringExpenseId, dueDate)}"

        fun buildReminder(
            userId: String,
            recurringExpense: RecurringExpense,
            dueDate: LocalDate
        ): ReminderEntity {
            val occurrenceId = RecurringScheduleCalculator.occurrenceId(
                recurringExpense.id,
                dueDate
            )
            return ReminderEntity(
                id = reminderId(recurringExpense.id, dueDate),
                userId = userId,
                title = recurringExpense.title,
                category = recurringExpense.categoryId,
                amountMinor = recurringExpense.expectedAmountMinor,
                dueDateIso = dueDate.toString(),
                remindOnIso = dueDate.minusDays(recurringExpense.reminderLeadDays).toString(),
                recurringExpenseId = recurringExpense.id,
                recurringOccurrenceId = occurrenceId
            )
        }
    }
}
