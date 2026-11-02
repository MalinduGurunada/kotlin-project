package com.example.personalfinanceapp.feature.recurring

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.personalfinanceapp.core.database.ReminderEntity
import com.example.personalfinanceapp.core.model.RecurringExpense
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.feature.auth.AuthPostSignOutCleanup
import com.example.personalfinanceapp.feature.reminder.RoomReminderRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringReminderScheduler @Inject constructor(
    private val reminderRepository: RoomReminderRepository,
    private val firebaseAuth: FirebaseAuth,
    private val deliveryStore: RecurringReminderDeliveryStore,
    private val clock: Clock,
    @param:ApplicationContext private val context: Context
) : AuthPostSignOutCleanup, RecurringReminderCoordinator {

    private val syncMutex = Mutex()

    override suspend fun sync(
        recurringExpense: RecurringExpense,
        dueDates: List<LocalDate>
    ): Result<Unit> = syncMutex.withLock {
        val uid = firebaseAuth.currentUser?.uid
            ?: return@withLock Result.failure(IllegalStateException("User is not authenticated"))
        val existing = reminderRepository.observeReminders(uid).first()
            .filter { it.recurringExpenseId == recurringExpense.id }
        existing.forEach(::cancelAlarm)
        reminderRepository.removeRecurringReminders(uid, recurringExpense.id).getOrElse {
            return@withLock Result.failure(it)
        }
        if (!recurringExpense.active || !recurringExpense.reminderEnabled) {
            return@withLock Result.success(Unit)
        }
        val deliveredIds = existing.filter { it.isDelivered }.mapTo(mutableSetOf()) { it.id }
        for (dueDate in dueDates.distinct()) {
            val reminder = buildReminder(uid, recurringExpense, dueDate)
            val wasDelivered = reminder.id in deliveredIds ||
                deliveryStore.wasDelivered(uid, reminder.id)
            val triggerAt = RecurringReminderSchedulePolicy.triggerAtMillis(
                reminder = reminder,
                wasDelivered = wasDelivered,
                now = clock.instant(),
                zoneId = ZoneId.systemDefault()
            ) ?: continue
            reminderRepository.addReminder(reminder).getOrElse {
                return@withLock Result.failure(it)
            }
            scheduleAlarm(reminder, triggerAt)
        }
        Result.success(Unit)
    }

    override suspend fun cancelOccurrence(
        recurringExpenseId: String,
        dueDate: LocalDate
    ): Result<Unit> {
        val id = reminderId(recurringExpenseId, dueDate)
        cancelAlarmById(id)
        return reminderRepository.removeReminder(id)
    }

    override suspend fun clearForUser(userId: UserId): Result<Unit> {
        val reminders = runCatching {
            reminderRepository.observeReminders(userId).first()
        }.getOrElse { return Result.failure(it) }

        reminders.forEach(::cancelAlarm)
        return reminderRepository.clearUserRemindersOnLogout(userId)
    }

    private fun scheduleAlarm(reminder: ReminderEntity, triggerAt: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(reminder)
        )
    }

    private fun cancelAlarm(reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(reminder)
        alarmManager.cancel(pending)
        pending.cancel()
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
            .putExtra(RecurringReminderAlarmReceiver.EXTRA_USER_ID, reminder.userId)
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
