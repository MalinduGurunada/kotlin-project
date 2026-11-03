package com.example.personalfinanceapp.feature.notification

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FinanceMessagingService : FirebaseMessagingService() {
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var notificationHelper: FinanceNotificationHelper
    @Inject lateinit var notificationPreferenceStore: NotificationPreferenceStore
    @Inject lateinit var tokenSyncCoordinator: FcmTokenSyncCoordinator

    override fun onRegistered(installationId: String) {
        tokenSyncCoordinator.onTokenRefresh(installationId)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = FinanceReviewPayload.parse(message.data) ?: return
        if (!NotificationDeliveryPolicy.shouldDisplay(
                authenticatedUserId = auth.currentUser?.uid,
                payloadUserId = payload.userId,
                notificationsEnabled = notificationPreferenceStore.isEnabled(payload.userId)
            )
        ) return
        notificationHelper.show(payload)
    }
}
