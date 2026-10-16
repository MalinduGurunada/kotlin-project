package com.example.personalfinanceapp.feature.auth

import com.example.personalfinanceapp.core.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeAuthRepository(
    initialUserId: UserId? = null
) : AuthRepository {

    private val currentUserId = MutableStateFlow<UserId?>(initialUserId)
    private val mutex = Mutex()

    var shouldFailLogin = false
    var shouldFailRegister = false
    var shouldFailPasswordReset = false

    override fun observeAuthState(): Flow<UserId?> {
        return currentUserId.asStateFlow()
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
        currentUserId.value = uid
        Result.success(uid)
    }

    override suspend fun register(
        email: String,
        password: String
    ): Result<UserId> = mutex.withLock {
        if (shouldFailRegister) {
            return@withLock Result.failure(
                IllegalArgumentException("Email already in use")
            )
        }
        val uid = "user_" + email.trim().replace("@", "_at_").replace(".", "_")
        currentUserId.value = uid
        Result.success(uid)
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
        currentUserId.value = null
        Result.success(Unit)
    }
}
