package com.example.personalfinanceapp

import android.app.Application
import com.example.personalfinanceapp.feature.notification.FcmTokenSyncCoordinator
import com.example.personalfinanceapp.feature.notification.FinanceNotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FinanceApplication : Application() {
    @Inject lateinit var notificationHelper: FinanceNotificationHelper
    @Inject lateinit var tokenSyncCoordinator: FcmTokenSyncCoordinator

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannel()
        tokenSyncCoordinator.start()
    }
}
