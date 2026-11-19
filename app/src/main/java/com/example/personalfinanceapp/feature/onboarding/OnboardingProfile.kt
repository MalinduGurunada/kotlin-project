package com.example.personalfinanceapp.feature.onboarding

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.IncomeSource

data class OnboardingProfile(
    val userId: String,
    val baseCurrency: CurrencyCode,
    val incomeSources: Set<IncomeSource>,
    val salaryPayday: Int?,
    val expectedMonthlyIncomeMinor: Long,
    val expectedMonthlySpendingMinor: Long,
    val onboardingVersion: Int = CURRENT_ONBOARDING_VERSION,
    val onboardingCompleted: Boolean = true
)

const val CURRENT_ONBOARDING_VERSION = 1

sealed interface OnboardingAccess {
    data object Required : OnboardingAccess
    data class Complete(val profile: OnboardingProfile) : OnboardingAccess
}
