package com.example.personalfinanceapp

import android.app.Application
import com.example.personalfinanceapp.feature.notification.FcmTokenSyncCoordinator
import com.example.personalfinanceapp.feature.notification.FinanceNotificationHelper
import com.example.personalfinanceapp.feature.reminder.ReminderNotificationHelper
import com.example.personalfinanceapp.feature.reminder.daily.DailyCaptureReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FinanceApplication : Application() {
    @Inject lateinit var notificationHelper: FinanceNotificationHelper
    @Inject lateinit var tokenSyncCoordinator: FcmTokenSyncCoordinator
    @Inject lateinit var dailyCaptureReminderScheduler: DailyCaptureReminderScheduler

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannel()
        ReminderNotificationHelper.createNotificationChannel(this)
        tokenSyncCoordinator.start()
        dailyCaptureReminderScheduler.start()
    }
}
