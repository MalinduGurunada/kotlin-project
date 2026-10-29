package com.example.personalfinanceapp.feature.auth

import com.example.personalfinanceapp.core.model.UserId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
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
        return AuthUserSnapshot(user.uid, user.email)
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
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
