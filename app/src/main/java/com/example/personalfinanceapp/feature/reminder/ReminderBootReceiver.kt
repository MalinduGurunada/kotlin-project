package com.example.personalfinanceapp.feature.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.personalfinanceapp.core.database.ReminderDao
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderBootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderDao: ReminderDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        val activeReminders = reminderDao.getRemindersListForUser(uid)
                        // Reminders persist in Room SQLite database across process restart and reboot
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
