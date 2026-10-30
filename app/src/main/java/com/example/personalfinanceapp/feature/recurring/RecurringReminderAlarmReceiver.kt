package com.example.personalfinanceapp.feature.recurring

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.personalfinanceapp.R
import com.example.personalfinanceapp.feature.reminder.ReminderNotificationHelper
import com.example.personalfinanceapp.feature.reminder.RoomReminderRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecurringReminderAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var deliveryStore: RecurringReminderDeliveryStore

    @Inject
    lateinit var reminderRepository: RoomReminderRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                deliverIfAllowed(context, intent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun deliverIfAllowed(context: Context, intent: Intent) {
        val permissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val scheduledUserId = intent.getStringExtra(EXTRA_USER_ID)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (!RecurringReminderDeliveryPolicy.claimDeliveryIfAllowed(
                scheduledUserId = scheduledUserId,
                currentUserId = currentUserId,
                notificationPermissionGranted = permissionGranted,
                claimDelivery = {
                    deliveryStore.claimDelivery(checkNotNull(scheduledUserId), id)
                }
            )
        ) return

        reminderRepository.markDelivered(id)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Recurring payment"
        val amountMinor = intent.getLongExtra(EXTRA_AMOUNT_MINOR, 0L)
        val dueDate = intent.getStringExtra(EXTRA_DUE_DATE).orEmpty()
        val amount = BigDecimal.valueOf(amountMinor, 2).setScale(2).toPlainString()

        ReminderNotificationHelper.createNotificationChannel(context)
        val notification = NotificationCompat.Builder(context, ReminderNotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("LKR $amount is due on $dueDate")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
    }

    companion object {
        const val EXTRA_REMINDER_ID = "recurring_reminder_id"
        const val EXTRA_TITLE = "recurring_reminder_title"
        const val EXTRA_AMOUNT_MINOR = "recurring_reminder_amount_minor"
        const val EXTRA_DUE_DATE = "recurring_reminder_due_date"
        const val EXTRA_USER_ID = "recurring_reminder_user_id"
    }
}
