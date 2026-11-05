package com.example.personalfinanceapp.feature.reminder.daily

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.personalfinanceapp.MainActivity
import com.example.personalfinanceapp.R
import com.example.personalfinanceapp.feature.expense.ExpenseRepository
import com.example.personalfinanceapp.feature.notification.NotificationPreferenceStore
import com.example.personalfinanceapp.feature.reminder.ReminderNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class DailyCaptureReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: DailyCaptureReminderScheduler
    @Inject lateinit var reminderStore: DailyCaptureReminderStore
    @Inject lateinit var deliveryStore: DailyCaptureReminderDeliveryStore
    @Inject lateinit var notificationPreferenceStore: NotificationPreferenceStore
    @Inject lateinit var expenseRepository: ExpenseRepository
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var clock: Clock

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DailyCaptureReminderScheduler.ACTION_DAILY_CAPTURE_REMINDER) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                deliverIfAllowed(context, intent)
            } finally {
                scheduler.rescheduleForCurrentUser()
                pendingResult.finish()
            }
        }
    }

    private suspend fun deliverIfAllowed(context: Context, intent: Intent) {
        val scheduledUserId = intent.getStringExtra(DailyCaptureReminderScheduler.EXTRA_USER_ID)
        val currentUserId = auth.currentUser?.uid
        if (scheduledUserId.isNullOrBlank() || scheduledUserId != currentUserId) return

        val settings = reminderStore.getSettings(scheduledUserId)
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(clock.withZone(zoneId))
        val scheduledDate = intent.getStringExtra(
            DailyCaptureReminderScheduler.EXTRA_SCHEDULED_DATE
        )
        if (scheduledDate != today.toString()) return

        val expensesToday = withTimeoutOrNull(EXPENSE_CHECK_TIMEOUT_MILLIS) {
            expenseRepository.observeExpenses(today, today.plusDays(1)).first()
        } ?: return
        val permissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

        val shouldDeliver = DailyCaptureReminderDeliveryPolicy.claimDeliveryIfAllowed(
            scheduledUserId = scheduledUserId,
            currentUserId = currentUserId,
            reminderEnabled = settings.enabled,
            notificationsEnabled = notificationPreferenceStore.isEnabled(scheduledUserId),
            notificationPermissionGranted = permissionGranted,
            hasExpenseToday = expensesToday.isNotEmpty(),
            claimDelivery = {
                deliveryStore.claimDelivery(scheduledUserId, today.toString())
            }
        )
        if (!shouldDeliver) return

        ReminderNotificationHelper.createNotificationChannel(context)
        val quickAddIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(DailyCaptureNotificationTarget.EXTRA_TYPE, DailyCaptureNotificationTarget.TYPE)
            putExtra(DailyCaptureNotificationTarget.EXTRA_USER_ID, scheduledUserId)
        }
        val quickAddPendingIntent = PendingIntent.getActivity(
            context,
            QUICK_ADD_REQUEST_CODE,
            quickAddIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(
            context,
            ReminderNotificationHelper.CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Record today's expenses")
            .setContentText("No expense has been recorded today. Add one while it is fresh.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(quickAddPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Quick add expense",
                quickAddPendingIntent
            )
            .build()
        NotificationManagerCompat.from(context).notify(
            DAILY_CAPTURE_NOTIFICATION_ID,
            notification
        )
    }

    private companion object {
        const val EXPENSE_CHECK_TIMEOUT_MILLIS = 10_000L
        const val QUICK_ADD_REQUEST_CODE = 5_502
        const val DAILY_CAPTURE_NOTIFICATION_ID = 5_503
    }
}
