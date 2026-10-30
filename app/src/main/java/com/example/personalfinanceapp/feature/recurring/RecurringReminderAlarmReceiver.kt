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
import java.math.BigDecimal

class RecurringReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
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
    }
}
