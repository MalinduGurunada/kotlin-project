package com.example.personalfinanceapp.feature.auth

import com.example.personalfinanceapp.core.model.UserId
import kotlinx.coroutines.flow.Flow

data class AuthUserSnapshot(
    val userId: UserId,
    val email: String?,
    val isEmailVerified: Boolean = true,
    val isEmailPasswordUser: Boolean = false,
    val displayName: String? = null
) {
    val requiresEmailVerification: Boolean
        get() = isEmailPasswordUser && !isEmailVerified
}

interface AuthRepository {

    fun observeAuthState(): Flow<UserId?>

    /** Email for the currently authenticated account, used only to bind a pre-creation marker. */
    fun currentUserEmail(): String? = null

    /** Synchronous identity check used to bind profile updates to one Firebase session. */
    fun currentUserId(): UserId? = null

    fun currentUserSnapshot(): AuthUserSnapshot? {
        val userId = currentUserId() ?: return null
        return AuthUserSnapshot(userId, currentUserEmail())
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<UserId>

    suspend fun loginWithGoogleIdToken(idToken: String): Result<UserId> = Result.failure(
        UnsupportedOperationException("Google sign-in is not supported")
    )

    suspend fun register(
        email: String,
        password: String
    ): Result<UserId>

    /** Updates the authenticated Firebase user's display name after account creation. */
    suspend fun updateDisplayName(userId: UserId, fullName: String): Result<Unit> = Result.failure(
        UnsupportedOperationException("User profile updates are not supported")
    )

    /** Sends Firebase Auth's built-in verification email to the current password user. */
    suspend fun sendEmailVerification(userId: UserId): Result<Unit> = Result.failure(
        UnsupportedOperationException("Email verification is not supported")
    )

    /** Reloads the current Firebase user and force-refreshes its token once verified. */
    suspend fun reloadAndRefreshUser(userId: UserId): Result<AuthUserSnapshot> = Result.failure(
        UnsupportedOperationException("Email verification refresh is not supported")
    )

    suspend fun sendPasswordReset(
        email: String
    ): Result<Unit>

    suspend fun signOut(): Result<Unit>
}

fun interface AuthPostVerificationSync {
    fun sync(userId: UserId)

    companion object {
        val NoOp = AuthPostVerificationSync { }
    }
}
