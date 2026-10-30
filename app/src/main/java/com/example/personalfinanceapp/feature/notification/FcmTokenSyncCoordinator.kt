package com.example.personalfinanceapp.feature.notification

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class FcmTokenSyncCoordinator @Inject constructor(
    private val auth: FirebaseAuth,
    private val messaging: FirebaseMessaging,
    private val tokenRepository: PushTokenRepository,
    private val preferenceStore: NotificationPreferenceStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        started = true
        auth.addAuthStateListener { currentAuth ->
            val userId = currentAuth.currentUser?.uid ?: return@addAuthStateListener
            if (preferenceStore.isEnabled(userId)) {
                register(userId)
            } else {
                unregister(userId)
            }
        }
    }

    fun onTokenRefresh(installationId: String) {
        val userId = auth.currentUser?.uid
        if (userId != null && !preferenceStore.isEnabled(userId)) {
            unregister(userId)
            return
        }
        scope.launch { tokenRepository.onTokenRefresh(installationId) }
    }

    fun onPreferenceChanged(userId: String, enabled: Boolean) {
        if (auth.currentUser?.uid != userId) return
        if (enabled) register(userId) else unregister(userId)
    }

    private fun register(expectedUserId: String) {
        messaging.isAutoInitEnabled = true
        messaging.register().addOnCompleteListener { task ->
            if (
                task.isSuccessful &&
                auth.currentUser?.uid == expectedUserId &&
                preferenceStore.isEnabled(expectedUserId)
            ) {
                scope.launch { tokenRepository.syncCurrentToken() }
            }
        }
    }

    private fun unregister(expectedUserId: String) {
        scope.launch {
            val cleanup = tokenRepository.deleteCurrentToken()
            if (
                cleanup.isSuccess &&
                auth.currentUser?.uid == expectedUserId &&
                !preferenceStore.isEnabled(expectedUserId)
            ) {
                messaging.isAutoInitEnabled = false
                messaging.unregister()
            }
        }
    }
}
