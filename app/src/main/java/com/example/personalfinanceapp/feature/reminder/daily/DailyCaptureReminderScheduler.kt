package com.example.personalfinanceapp.feature.reminder.daily

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyCaptureReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val store: DailyCaptureReminderStore,
    private val clock: Clock
) {
    private val started = AtomicBoolean(false)
    private val authStateListener = FirebaseAuth.AuthStateListener {
        rescheduleForCurrentUser()
    }

    fun start() {
        if (started.compareAndSet(false, true)) {
            auth.addAuthStateListener(authStateListener)
        }
        rescheduleForCurrentUser()
    }

    @Synchronized
    fun setEnabled(userId: String, enabled: Boolean) {
        store.setEnabled(userId, enabled)
        rescheduleForCurrentUser()
    }

    @Synchronized
    fun setTimeMinutes(userId: String, timeMinutes: Int) {
        store.setTimeMinutes(userId, timeMinutes)
        rescheduleForCurrentUser()
    }

    @Synchronized
    fun rescheduleForCurrentUser() {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            cancel()
            return
        }
        val settings = store.getSettings(userId)
        if (!settings.enabled) {
            cancel()
            return
        }

        val zoneId = ZoneId.systemDefault()
        val trigger = DailyCaptureReminderSchedulePolicy.nextTrigger(
            now = clock.instant(),
            zoneId = zoneId,
            timeMinutes = settings.timeMinutes
        )
        val intent = Intent(context, DailyCaptureReminderAlarmReceiver::class.java)
            .setAction(ACTION_DAILY_CAPTURE_REMINDER)
            .putExtra(EXTRA_USER_ID, userId)
            .putExtra(EXTRA_SCHEDULED_DATE, trigger.atZone(zoneId).toLocalDate().toString())
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            trigger.toEpochMilli(),
            pendingIntent
        )
    }

    @Synchronized
    fun cancel() {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, DailyCaptureReminderAlarmReceiver::class.java)
                .setAction(ACTION_DAILY_CAPTURE_REMINDER),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    companion object {
        internal const val ACTION_DAILY_CAPTURE_REMINDER =
            "com.example.personalfinanceapp.action.DAILY_CAPTURE_REMINDER"
        internal const val EXTRA_USER_ID = "daily_capture_scheduled_user_id"
        internal const val EXTRA_SCHEDULED_DATE = "daily_capture_scheduled_date"
        private const val REQUEST_CODE = 5_501
    }
}
