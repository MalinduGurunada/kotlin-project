package com.example.personalfinanceapp.feature.notification

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.installations.FirebaseInstallations
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

interface PushTokenRepository {
    suspend fun syncCurrentToken(): Result<Unit>
    suspend fun onTokenRefresh(token: String): Result<Unit>
    suspend fun deleteCurrentToken(): Result<Unit>
}

object FcmTokenOwnershipPolicy {
    fun previousTokenToDelete(
        storedOwner: String?,
        storedToken: String?,
        currentOwner: String,
        refreshedToken: String
    ): String? = storedToken?.takeIf {
        storedOwner == currentOwner && it.isNotBlank() && it != refreshedToken
    }

    fun localTokenForCleanup(
        storedOwner: String?,
        storedToken: String?,
        currentOwner: String
    ): String? = storedToken?.takeIf {
        storedOwner == currentOwner && it.isNotBlank()
    }
}

@Singleton
class FirestorePushTokenRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val installations: FirebaseInstallations,
    @ApplicationContext context: Context
) : PushTokenRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun syncCurrentToken(): Result<Unit> {
        val expectedUid = auth.currentUser?.uid ?: return Result.success(Unit)
        val installationId = installations.id.awaitResult().getOrElse { return Result.failure(it) }
        if (auth.currentUser?.uid != expectedUid) {
            return Result.failure(IllegalStateException("Authenticated account changed during token sync"))
        }
        return register(expectedUid, installationId)
    }

    override suspend fun onTokenRefresh(token: String): Result<Unit> {
        if (token.isBlank()) return Result.failure(IllegalArgumentException("FCM token is blank"))
        val uid = auth.currentUser?.uid
        if (uid == null) {
            preferences.edit().putString(KEY_TOKEN, token).remove(KEY_OWNER).apply()
            return Result.success(Unit)
        }
        return register(uid, token)
    }

    override suspend fun deleteCurrentToken(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val storedOwner = preferences.getString(KEY_OWNER, null)
        val storedToken = preferences.getString(KEY_TOKEN, null)
        val token = FcmTokenOwnershipPolicy.localTokenForCleanup(
            storedOwner,
            storedToken,
            uid
        ) ?: run {
            installations.id.awaitResult().getOrElse { return Result.failure(it) }
        }
        if (auth.currentUser?.uid != uid) {
            return Result.failure(IllegalStateException("Authenticated account changed during token cleanup"))
        }
        val deletion = tokenDocument(uid, token).delete().awaitResult()
        if (deletion.isFailure) return Result.failure(deletion.exceptionOrNull()!!)
        if (preferences.getString(KEY_OWNER, null) == uid) {
            preferences.edit().remove(KEY_TOKEN).remove(KEY_OWNER).apply()
        }
        return Result.success(Unit)
    }

    private suspend fun register(uid: String, token: String): Result<Unit> {
        if (token.isBlank()) return Result.failure(IllegalArgumentException("FCM token is blank"))
        val previousOwner = preferences.getString(KEY_OWNER, null)
        val previousToken = preferences.getString(KEY_TOKEN, null)

        val previousTokenToDelete = FcmTokenOwnershipPolicy.previousTokenToDelete(
            previousOwner,
            previousToken,
            uid,
            token
        )
        if (previousTokenToDelete != null) {
            val deletion = tokenDocument(uid, previousTokenToDelete).delete().awaitResult()
            if (deletion.isFailure) return Result.failure(deletion.exceptionOrNull()!!)
        }
        if (auth.currentUser?.uid != uid) {
            return Result.failure(IllegalStateException("Authenticated account changed during token registration"))
        }

        val data = mapOf(
            "schemaVersion" to 1,
            "userId" to uid,
            "installationId" to token,
            "platform" to "android",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val write = tokenDocument(uid, token).set(data).awaitResult()
        if (write.isFailure) return Result.failure(write.exceptionOrNull()!!)
        if (auth.currentUser?.uid != uid) {
            return Result.failure(IllegalStateException("Authenticated account changed after token registration"))
        }
        preferences.edit().putString(KEY_OWNER, uid).putString(KEY_TOKEN, token).apply()
        return Result.success(Unit)
    }

    private fun tokenDocument(uid: String, token: String) = firestore
        .collection("users")
        .document(uid)
        .collection("fcmRegistrations")
        .document(token.sha256())

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val PREFERENCES_NAME = "fcm_token_registration"
        const val KEY_TOKEN = "token"
        const val KEY_OWNER = "owner"
    }
}

private suspend fun <T> Task<T>.awaitResult(): Result<T> = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener
        if (task.isSuccessful) {
            continuation.resume(Result.success(task.result))
        } else {
            continuation.resume(
                Result.failure(task.exception ?: IllegalStateException("Firebase task failed"))
            )
        }
    }
}
