package com.example.personalfinanceapp.feature.onboarding

class FakeOnboardingRepository(
    initialAccess: OnboardingAccess = OnboardingAccess.Required
) : OnboardingRepository {
    var access: OnboardingAccess = initialAccess
        private set
    var completionFailure: Throwable? = null
    var resolveFailure: Throwable? = null

    override suspend fun resolveAccess(expectedUserId: String): Result<OnboardingAccess> {
        resolveFailure?.let { return Result.failure(it) }
        return Result.success(access)
    }

    override suspend fun completeOnboarding(
        expectedUserId: String,
        profile: OnboardingProfile
    ): Result<Unit> {
        completionFailure?.let { return Result.failure(it) }
        if (expectedUserId.isBlank() || profile.userId != expectedUserId) {
            return Result.failure(IllegalArgumentException("Invalid onboarding owner"))
        }
        access = OnboardingAccess.Complete(profile)
        return Result.success(Unit)
    }
}
