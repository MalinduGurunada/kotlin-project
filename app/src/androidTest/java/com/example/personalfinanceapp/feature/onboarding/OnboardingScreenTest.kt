package com.example.personalfinanceapp.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.feature.onboarding.ui.OnboardingGateState
import com.example.personalfinanceapp.feature.onboarding.ui.OnboardingScreen
import com.example.personalfinanceapp.feature.onboarding.ui.OnboardingUiState
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun incomeStepShowsEverySupportedSourceAndSalaryPayday() {
        composeRule.setContent {
            PersonalFinanceAppTheme(dynamicColor = false) {
                OnboardingScreen(
                    gateState = OnboardingGateState.Required,
                    uiState = OnboardingUiState(
                        step = 1,
                        selectedIncomeSources = setOf(IncomeSource.SALARY),
                        salaryPaydayInput = "25"
                    ),
                    onIncomeSourceToggled = {}, onSalaryPaydayChanged = {},
                    onExpectedMonthlyIncomeChanged = {}, onExpectedMonthlySpendingChanged = {},
                    onIncludeFirstGoalChanged = {}, onGoalNameChanged = {},
                    onGoalTargetChanged = {}, onGoalSavedChanged = {},
                    onGoalTargetDateChanged = {}, onBack = {}, onNext = {}, onFinish = {},
                    onRetryAccess = {}, onSignOut = {}
                )
            }
        }

        composeRule.onNodeWithText("How do you earn?").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_income_salary").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_income_freelance").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_income_adsense").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_income_crypto").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_salary_payday").assertIsDisplayed()
    }

    @Test
    fun enablingFirstGoalRevealsGoalFields() {
        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    OnboardingUiState(
                        step = 3,
                        selectedIncomeSources = setOf(IncomeSource.FREELANCE),
                        expectedMonthlyIncomeInput = "100000",
                        expectedMonthlySpendingInput = "60000"
                    )
                )
            }
            PersonalFinanceAppTheme(dynamicColor = false) {
                OnboardingScreen(
                    gateState = OnboardingGateState.Required,
                    uiState = state,
                    onIncomeSourceToggled = {}, onSalaryPaydayChanged = {},
                    onExpectedMonthlyIncomeChanged = {}, onExpectedMonthlySpendingChanged = {},
                    onIncludeFirstGoalChanged = { state = state.copy(includeFirstGoal = it) },
                    onGoalNameChanged = {}, onGoalTargetChanged = {}, onGoalSavedChanged = {},
                    onGoalTargetDateChanged = {}, onBack = {}, onNext = {}, onFinish = {},
                    onRetryAccess = {}, onSignOut = {}
                )
            }
        }

        composeRule.onNodeWithTag("onboarding_goal_toggle").performClick()
        composeRule.onNodeWithTag("onboarding_goal_name").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_goal_target").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_goal_saved").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_goal_date").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun goalDeadlineChooseButtonOpensDatePicker() {
        composeRule.setContent {
            PersonalFinanceAppTheme(dynamicColor = false) {
                OnboardingScreen(
                    gateState = OnboardingGateState.Required,
                    uiState = OnboardingUiState(
                        step = 3,
                        selectedIncomeSources = setOf(IncomeSource.FREELANCE),
                        expectedMonthlyIncomeInput = "100000",
                        expectedMonthlySpendingInput = "60000",
                        includeFirstGoal = true
                    ),
                    onIncomeSourceToggled = {}, onSalaryPaydayChanged = {},
                    onExpectedMonthlyIncomeChanged = {}, onExpectedMonthlySpendingChanged = {},
                    onIncludeFirstGoalChanged = {}, onGoalNameChanged = {},
                    onGoalTargetChanged = {}, onGoalSavedChanged = {},
                    onGoalTargetDateChanged = {}, onBack = {}, onNext = {}, onFinish = {},
                    onRetryAccess = {}, onSignOut = {}
                )
            }
        }

        composeRule.onNodeWithTag("onboarding_goal_date_picker")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Select").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
