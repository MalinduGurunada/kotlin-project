package com.example.personalfinanceapp.feature.onboarding

interface OnboardingRepository {
    suspend fun resolveAccess(expectedUserId: String): Result<OnboardingAccess>

    suspend fun completeOnboarding(
        expectedUserId: String,
        profile: OnboardingProfile
    ): Result<Unit>
}
