package com.example.personalfinanceapp.feature.auth

import com.example.personalfinanceapp.core.model.UserId
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val sessionCleanup: AuthSessionCleanup = NoOpAuthSessionCleanup
) : AuthRepository {

    override fun observeAuthState(): Flow<UserId?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }.distinctUntilChanged()

    override fun currentUserEmail(): String? = firebaseAuth.currentUser?.email

    override fun currentUserId(): UserId? = firebaseAuth.currentUser?.uid

    override fun currentUserSnapshot(): AuthUserSnapshot? {
        val user = firebaseAuth.currentUser ?: return null
        return user.toAuthUserSnapshot()
    }

    override suspend fun login(
        email: String,
        password: String
    ): Result<UserId> = suspendCancellableCoroutine { continuation ->
        firebaseAuth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    continuation.resume(Result.success(uid))
                } else {
                    continuation.resume(
                        Result.failure(IllegalStateException("User ID is null after login"))
                    )
                }
            }
            .addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
    }

    override suspend fun register(
        email: String,
        password: String
    ): Result<UserId> = suspendCancellableCoroutine { continuation ->
        firebaseAuth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    continuation.resume(Result.success(uid))
                } else {
                    continuation.resume(
                        Result.failure(IllegalStateException("User ID is null after registration"))
                    )
                }
            }
            .addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
    }

    override suspend fun loginWithGoogleIdToken(idToken: String): Result<UserId> =
        suspendCancellableCoroutine { continuation ->
            if (idToken.isBlank()) {
                continuation.resume(Result.failure(IllegalArgumentException("Google ID token is blank")))
                return@suspendCancellableCoroutine
            }
            if (firebaseAuth.currentUser != null) {
                continuation.resume(
                    Result.failure(IllegalStateException("Sign out before changing accounts"))
                )
                return@suspendCancellableCoroutine
            }

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid
                    if (uid != null) {
                        continuation.resume(Result.success(uid))
                    } else {
                        continuation.resume(
                            Result.failure(IllegalStateException("User ID is null after Google sign-in"))
                        )
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
        }

    override suspend fun updateDisplayName(userId: UserId, fullName: String): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            val user = firebaseAuth.currentUser
            if (user == null || user.uid != userId) {
                continuation.resume(
                    Result.failure(IllegalStateException("Authenticated user changed before profile update"))
                )
                return@suspendCancellableCoroutine
            }

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName.trim())
                .build()

            user.updateProfile(profileUpdates)
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
        }

    override suspend fun sendEmailVerification(userId: UserId): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            val user = firebaseAuth.currentUser
            if (user == null || user.uid != userId) {
                continuation.resume(
                    Result.failure(IllegalStateException("Authenticated user changed before verification email"))
                )
                return@suspendCancellableCoroutine
            }
            if (!user.isEmailPasswordUser()) {
                continuation.resume(Result.success(Unit))
                return@suspendCancellableCoroutine
            }
            if (user.isEmailVerified) {
                continuation.resume(Result.success(Unit))
                return@suspendCancellableCoroutine
            }

            user.sendEmailVerification()
                .addOnSuccessListener { continuation.resume(Result.success(Unit)) }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
        }

    override suspend fun reloadAndRefreshUser(userId: UserId): Result<AuthUserSnapshot> =
        suspendCancellableCoroutine { continuation ->
            val initialUser = firebaseAuth.currentUser
            if (initialUser == null || initialUser.uid != userId) {
                continuation.resume(
                    Result.failure(IllegalStateException("Authenticated user changed before verification check"))
                )
                return@suspendCancellableCoroutine
            }

            initialUser.reload()
                .addOnSuccessListener {
                    val refreshedUser = firebaseAuth.currentUser
                    if (refreshedUser == null || refreshedUser.uid != userId) {
                        continuation.resume(
                            Result.failure(IllegalStateException("Authenticated user changed during verification check"))
                        )
                    } else if (!refreshedUser.isEmailVerified) {
                        continuation.resume(Result.success(refreshedUser.toAuthUserSnapshot()))
                    } else {
                        refreshedUser.getIdToken(true)
                            .addOnSuccessListener {
                                val currentUser = firebaseAuth.currentUser
                                if (currentUser == null || currentUser.uid != userId) {
                                    continuation.resume(
                                        Result.failure(
                                            IllegalStateException(
                                                "Authenticated user changed during token refresh"
                                            )
                                        )
                                    )
                                } else {
                                    continuation.resume(Result.success(currentUser.toAuthUserSnapshot()))
                                }
                            }
                            .addOnFailureListener { exception ->
                                continuation.resume(Result.failure(exception))
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
        }

    override suspend fun sendPasswordReset(
        email: String
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        firebaseAuth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener {
                continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
    }

    override suspend fun signOut(): Result<Unit> {
        return performFirebaseSignOut(
            cleanup = sessionCleanup::beforeSignOut,
            firebaseSignOut = firebaseAuth::signOut
        )
    }

    private fun FirebaseUser.isEmailPasswordUser(): Boolean =
        providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }

    private fun FirebaseUser.toAuthUserSnapshot(): AuthUserSnapshot = AuthUserSnapshot(
        userId = uid,
        email = email,
        isEmailVerified = isEmailVerified,
        isEmailPasswordUser = isEmailPasswordUser(),
        displayName = displayName
    )
}
