package com.example.personalfinanceapp.feature.auth

import com.example.personalfinanceapp.core.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeAuthRepository(
    initialUserId: UserId? = null,
    initialEmail: String? = null
) : AuthRepository {

    private val currentUserId = MutableStateFlow<UserId?>(initialUserId)
    private var currentEmail: String? = initialEmail
    private val mutex = Mutex()

    var shouldFailLogin = false
    var shouldFailRegister = false
    var shouldFailProfileUpdate = false
    var shouldFailPasswordReset = false
    var registerCallCount = 0
        private set
    var displayName: String? = null
        private set

    override fun observeAuthState(): Flow<UserId?> {
        return currentUserId.asStateFlow()
    }

    override fun currentUserEmail(): String? = currentEmail

    override fun currentUserId(): UserId? = currentUserId.value

    override fun currentUserSnapshot(): AuthUserSnapshot? {
        val userId = currentUserId.value ?: return null
        return AuthUserSnapshot(userId, currentEmail)
    }

    override suspend fun login(
        email: String,
        password: String
    ): Result<UserId> = mutex.withLock {
        if (shouldFailLogin) {
            return@withLock Result.failure(
                IllegalArgumentException("Invalid credentials")
            )
        }
        val uid = "user_" + email.trim().replace("@", "_at_").replace(".", "_")
        currentEmail = email.trim()
        currentUserId.value = uid
        Result.success(uid)
    }

    override suspend fun register(
        email: String,
        password: String
    ): Result<UserId> = mutex.withLock {
        registerCallCount += 1
        if (shouldFailRegister) {
            return@withLock Result.failure(
                IllegalArgumentException("Email already in use")
            )
        }
        val uid = "user_" + email.trim().replace("@", "_at_").replace(".", "_")
        currentEmail = email.trim()
        currentUserId.value = uid
        Result.success(uid)
    }

    override suspend fun updateDisplayName(userId: UserId, fullName: String): Result<Unit> = mutex.withLock {
        if (shouldFailProfileUpdate) {
            return@withLock Result.failure(
                IllegalStateException("Profile update failed")
            )
        }
        if (currentUserId.value != userId) {
            return@withLock Result.failure(
                IllegalStateException("Authenticated user changed before profile update")
            )
        }
        displayName = fullName.trim()
        Result.success(Unit)
    }

    override suspend fun sendPasswordReset(
        email: String
    ): Result<Unit> = mutex.withLock {
        if (shouldFailPasswordReset) {
            return@withLock Result.failure(
                IllegalArgumentException("User not found")
            )
        }
        Result.success(Unit)
    }

    override suspend fun signOut(): Result<Unit> = mutex.withLock {
        currentEmail = null
        currentUserId.value = null
        Result.success(Unit)
    }
}
