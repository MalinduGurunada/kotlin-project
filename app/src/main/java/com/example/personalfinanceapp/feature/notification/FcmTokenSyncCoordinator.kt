package com.example.personalfinanceapp.feature.notification

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class FcmTokenSyncCoordinator @Inject constructor(
    private val auth: FirebaseAuth,
    private val tokenRepository: PushTokenRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        started = true
        auth.addAuthStateListener { currentAuth ->
            if (currentAuth.currentUser != null) {
                scope.launch { tokenRepository.syncCurrentToken() }
            }
        }
    }

    fun onTokenRefresh(token: String) {
        scope.launch { tokenRepository.onTokenRefresh(token) }
    }
}
