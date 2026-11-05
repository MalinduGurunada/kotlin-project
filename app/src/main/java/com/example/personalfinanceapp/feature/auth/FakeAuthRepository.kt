package com.example.personalfinanceapp.feature.auth

import com.example.personalfinanceapp.core.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeAuthRepository(
    initialUserId: UserId? = null,
    initialEmail: String? = null,
    initialEmailVerified: Boolean = true,
    initialIsEmailPasswordUser: Boolean = initialEmail != null,
    initialDisplayName: String? = null
) : AuthRepository {

    private val currentUserId = MutableStateFlow<UserId?>(initialUserId)
    private var currentEmail: String? = initialEmail
    private var emailVerified = initialEmailVerified
    private var isEmailPasswordUser = initialIsEmailPasswordUser
    private val mutex = Mutex()

    var shouldFailLogin = false
    var shouldFailGoogleLogin = false
    var shouldFailRegister = false
    var shouldFailProfileUpdate = false
    var shouldFailPasswordReset = false
    var shouldFailSignOut = false
    var shouldFailVerificationEmail = false
    var shouldFailVerificationRefresh = false
    var loginEmailVerified = true
    var loginDisplayName: String? = null
    var registerCallCount = 0
        private set
    var googleLoginCallCount = 0
        private set
    var verificationEmailCallCount = 0
        private set
    var verificationRefreshCallCount = 0
        private set
    var displayName: String? = initialDisplayName
        private set

    override fun observeAuthState(): Flow<UserId?> {
        return currentUserId.asStateFlow()
    }

    override fun currentUserEmail(): String? = currentEmail

    override fun currentUserId(): UserId? = currentUserId.value

    override fun currentUserSnapshot(): AuthUserSnapshot? {
        val userId = currentUserId.value ?: return null
        return AuthUserSnapshot(
            userId = userId,
            email = currentEmail,
            isEmailVerified = emailVerified,
            isEmailPasswordUser = isEmailPasswordUser,
            displayName = displayName
        )
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
        displayName = loginDisplayName
        emailVerified = loginEmailVerified
        isEmailPasswordUser = true
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
        displayName = null
        emailVerified = false
        isEmailPasswordUser = true
        currentUserId.value = uid
        Result.success(uid)
    }

    override suspend fun loginWithGoogleIdToken(idToken: String): Result<UserId> = mutex.withLock {
        googleLoginCallCount += 1
        if (shouldFailGoogleLogin) {
            return@withLock Result.failure(IllegalArgumentException("Google authentication failed"))
        }
        if (idToken.isBlank()) {
            return@withLock Result.failure(IllegalArgumentException("Google ID token is blank"))
        }
        currentEmail = "google.user@example.com"
        displayName = "Google User"
        emailVerified = true
        isEmailPasswordUser = false
        currentUserId.value = "google-user"
        Result.success("google-user")
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

    override suspend fun sendEmailVerification(userId: UserId): Result<Unit> = mutex.withLock {
        verificationEmailCallCount += 1
        if (shouldFailVerificationEmail) {
            return@withLock Result.failure(IllegalStateException("Too many requests"))
        }
        if (currentUserId.value != userId) {
            return@withLock Result.failure(IllegalStateException("Wrong account"))
        }
        Result.success(Unit)
    }

    override suspend fun reloadAndRefreshUser(userId: UserId): Result<AuthUserSnapshot> =
        mutex.withLock {
            verificationRefreshCallCount += 1
            if (shouldFailVerificationRefresh) {
                return@withLock Result.failure(IllegalStateException("Network unavailable"))
            }
            if (currentUserId.value != userId) {
                return@withLock Result.failure(IllegalStateException("Wrong account"))
            }
            Result.success(currentUserSnapshot()!!)
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
        if (shouldFailSignOut) {
            return@withLock Result.failure(IllegalStateException("Firebase sign-out failed"))
        }
        currentEmail = null
        displayName = null
        emailVerified = true
        isEmailPasswordUser = false
        currentUserId.value = null
        Result.success(Unit)
    }

    fun simulateAccountSwitch(userId: UserId, email: String, displayName: String? = null) {
        currentEmail = email
        this.displayName = displayName
        emailVerified = true
        isEmailPasswordUser = true
        currentUserId.value = userId
    }

    fun markEmailVerified() {
        emailVerified = true
    }
}
