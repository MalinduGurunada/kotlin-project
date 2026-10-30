package com.example.personalfinanceapp.feature.auth

import android.content.Context
import com.example.personalfinanceapp.feature.notification.PushTokenRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface AuthSessionCleanup {
    suspend fun beforeSignOut(): Result<Unit>
}

object NoOpAuthSessionCleanup : AuthSessionCleanup {
    override suspend fun beforeSignOut(): Result<Unit> = Result.success(Unit)
}

class FirebaseAuthSessionCleanup @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val pushTokenRepository: PushTokenRepository
) : AuthSessionCleanup {
    override suspend fun beforeSignOut(): Result<Unit> {
        val tokenCleanup = pushTokenRepository.deleteCurrentToken()
        if (tokenCleanup.isFailure) return tokenCleanup
        // Firebase sign-out must still complete if the platform credential provider
        // cannot clear its optional UI state. Auto-select is disabled on the next login.
        GoogleCredentialSignIn.clearCredentialState(context)
        return Result.success(Unit)
    }
}
