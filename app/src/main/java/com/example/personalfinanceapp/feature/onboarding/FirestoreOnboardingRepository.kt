package com.example.personalfinanceapp.feature.onboarding

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.android.gms.tasks.Task
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine

class FirestoreOnboardingRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : OnboardingRepository {

    override suspend fun resolveAccess(expectedUserId: String): Result<OnboardingAccess> =
        repositoryResult {
            requireCurrentOwner(expectedUserId)
            val snapshot = userDocument(expectedUserId).get().awaitResult()
            requireCurrentOwner(expectedUserId)
            val data = snapshot.data
            val profile = data?.toOnboardingProfile(expectedUserId)
            if (profile?.onboardingCompleted == true &&
                profile.onboardingVersion >= CURRENT_ONBOARDING_VERSION
            ) {
                OnboardingAccess.Complete(profile)
            } else {
                OnboardingAccess.Required
            }
        }

    override suspend fun completeOnboarding(
        expectedUserId: String,
        profile: OnboardingProfile
    ): Result<Unit> = repositoryResult {
        require(profile.userId == expectedUserId) {
            "Onboarding profile owner does not match the authenticated user"
        }
        validateProfile(profile)
        requireCurrentOwner(expectedUserId)
        userDocument(expectedUserId)
            .set(profile.toFirestoreMap(), SetOptions.merge())
            .awaitResult<Void>()
        requireCurrentOwner(expectedUserId)
    }

    private fun requireCurrentOwner(expectedUserId: String) {
        require(expectedUserId.isNotBlank()) { "Onboarding owner cannot be blank" }
        check(authRepository.currentUserId() == expectedUserId) {
            "Authenticated user changed during onboarding"
        }
    }

    private fun userDocument(userId: String) =
        firestore.collection(USERS_COLLECTION).document(userId)
}

private fun validateProfile(profile: OnboardingProfile) {
    require(profile.userId.isNotBlank()) { "User ID cannot be blank" }
    require(profile.baseCurrency == CurrencyCode.LKR) {
        "LKR is the supported reporting currency"
    }
    require(profile.incomeSources.isNotEmpty()) { "Select at least one income source" }
    require(profile.expectedMonthlyIncomeMinor >= 0L) {
        "Expected monthly income cannot be negative"
    }
    require(profile.expectedMonthlySpendingMinor >= 0L) {
        "Expected monthly spending cannot be negative"
    }
    require(profile.salaryPayday == null || profile.salaryPayday in 1..31) {
        "Salary payday must be between 1 and 31"
    }
    require(
        IncomeSource.SALARY in profile.incomeSources || profile.salaryPayday == null
    ) { "Salary payday requires Salary as an income source" }
    require(profile.onboardingVersion == CURRENT_ONBOARDING_VERSION) {
        "Unsupported onboarding version"
    }
    require(profile.onboardingCompleted) { "Onboarding profile must be complete" }
}

private fun OnboardingProfile.toFirestoreMap(): Map<String, Any?> = mapOf(
    "onboardingSchemaVersion" to 1L,
    "userId" to userId,
    "baseCurrency" to baseCurrency.name,
    "incomeSources" to incomeSources.map(IncomeSource::name).sorted(),
    "salaryPayday" to salaryPayday?.toLong(),
    "expectedMonthlyIncomeMinor" to expectedMonthlyIncomeMinor,
    "expectedMonthlySpendingMinor" to expectedMonthlySpendingMinor,
    "onboardingVersion" to onboardingVersion.toLong(),
    "onboardingCompleted" to onboardingCompleted,
    "onboardingUpdatedAt" to FieldValue.serverTimestamp()
)

private fun Map<String, Any>.toOnboardingProfile(userId: String): OnboardingProfile? {
    val storedUserId = this["userId"] as? String ?: return null
    if (storedUserId != userId) return null
    val currency = (this["baseCurrency"] as? String)
        ?.let { runCatching { CurrencyCode.valueOf(it) }.getOrNull() }
        ?: return null
    val incomeSources = (this["incomeSources"] as? List<*>)
        ?.mapNotNull { raw ->
            (raw as? String)?.let { runCatching { IncomeSource.valueOf(it) }.getOrNull() }
        }
        ?.toSet()
        ?: return null
    return OnboardingProfile(
        userId = storedUserId,
        baseCurrency = currency,
        incomeSources = incomeSources,
        salaryPayday = (this["salaryPayday"] as? Number)?.toInt(),
        expectedMonthlyIncomeMinor =
            (this["expectedMonthlyIncomeMinor"] as? Number)?.toLong() ?: return null,
        expectedMonthlySpendingMinor =
            (this["expectedMonthlySpendingMinor"] as? Number)?.toLong() ?: return null,
        onboardingVersion = (this["onboardingVersion"] as? Number)?.toInt() ?: return null,
        onboardingCompleted = this["onboardingCompleted"] as? Boolean ?: false
    )
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { completed ->
        when {
            completed.isCanceled -> continuation.cancel(
                CancellationException("Firebase task was cancelled")
            )
            completed.isSuccessful -> continuation.resume(completed.result) { _, _, _ -> }
            else -> continuation.resumeWith(
                Result.failure(
                    completed.exception
                        ?: IllegalStateException("Firebase task failed without an exception")
                )
            )
        }
    }
}

private suspend inline fun <T> repositoryResult(
    crossinline operation: suspend () -> T
): Result<T> = try {
    Result.success(operation())
} catch (exception: CancellationException) {
    throw exception
} catch (exception: Exception) {
    Result.failure(exception)
}

private const val USERS_COLLECTION = "users"
