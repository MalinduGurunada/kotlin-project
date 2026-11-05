package com.example.personalfinanceapp.feature.auth

import android.content.Context
import com.example.personalfinanceapp.feature.notification.PushTokenRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

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
        // Firestore can be offline or reject a stale token document. Authentication
        // sign-out must still proceed; the owner-scoped cleanup is advisory.
        try {
            pushTokenRepository.deleteCurrentToken()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Continue to credential cleanup; Firebase sign-out is handled by the caller.
        }
        // Firebase sign-out must still complete if the platform credential provider
        // cannot clear its optional UI state. Auto-select is disabled on the next login.
        GoogleCredentialSignIn.clearCredentialState(context)
        return Result.success(Unit)
    }
}
