package com.example.personalfinanceapp.feature.auth

import com.example.personalfinanceapp.core.model.UserId
import kotlinx.coroutines.flow.Flow

data class AuthUserSnapshot(
    val userId: UserId,
    val email: String?
)

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

    suspend fun register(
        email: String,
        password: String
    ): Result<UserId>

    /** Updates the authenticated Firebase user's display name after account creation. */
    suspend fun updateDisplayName(userId: UserId, fullName: String): Result<Unit> = Result.failure(
        UnsupportedOperationException("User profile updates are not supported")
    )

    suspend fun sendPasswordReset(
        email: String
    ): Result<Unit>

    suspend fun signOut(): Result<Unit>
}
