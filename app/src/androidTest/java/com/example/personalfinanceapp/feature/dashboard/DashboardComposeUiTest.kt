package com.example.personalfinanceapp.feature.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.personalfinanceapp.feature.dashboard.ui.DashboardUiState
import com.example.personalfinanceapp.feature.dashboard.ui.NavDestination
import com.example.personalfinanceapp.feature.dashboard.ui.OverviewScreen
import com.example.personalfinanceapp.feature.savings.SavingsCoachGoalProgress
import com.example.personalfinanceapp.feature.savings.SavingsCoachResult
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.math.BigInteger
import java.time.LocalDate

class DashboardComposeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun overviewScreenDisplaysHeaderAndBalance() {
        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                OverviewScreen(
                    uiState = DashboardUiState(userName = "Himasha"),
                    currentNavDestination = NavDestination.OVERVIEW,
                    onNavDestinationSelected = {},
                    onNavigateToActivity = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Rupee Pulse").assertIsDisplayed()
        composeTestRule.onNodeWithText("Good morning, Himasha").assertIsDisplayed()
    }

    @Test
    fun overviewBottomNavigationExposesActivityGoalsAndMore() {
        val selectedDestinations = mutableListOf<NavDestination>()
        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                OverviewScreen(
                    uiState = DashboardUiState(userName = "Himasha"),
                    currentNavDestination = NavDestination.OVERVIEW,
                    onNavDestinationSelected = selectedDestinations::add,
                    onNavigateToActivity = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Activity").performClick()
        composeTestRule.onNodeWithText("Goals").performClick()
        composeTestRule.onNodeWithText("More").performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(
                    NavDestination.ACTIVITY,
                    NavDestination.GOALS,
                    NavDestination.MORE
                ),
                selectedDestinations
            )
        }
    }

    @Test
    fun smartSavingsContributionRequiresExplicitDialogConfirmation() {
        val confirmations = mutableListOf<Pair<String, Long>>()
        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                OverviewScreen(
                    uiState = DashboardUiState(
                        userName = "Himasha",
                        smartSavings = coachResult()
                    ),
                    currentNavDestination = NavDestination.OVERVIEW,
                    onNavDestinationSelected = {},
                    onNavigateToActivity = {},
                    onConfirmSavingsContribution = { goalId, amountLkr ->
                        confirmations += goalId to amountLkr
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("Review contribution").performScrollTo().performClick()
        composeTestRule.onNodeWithText(
            "This records a goal allocation only. It does not transfer money. Check your bank balance before confirming."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.runOnIdle { assertEquals(emptyList<Pair<String, Long>>(), confirmations) }

        composeTestRule.onNodeWithText("Review contribution").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Add Allocation").performClick()
        composeTestRule.runOnIdle {
            assertEquals(listOf("goal-1" to 1_000L), confirmations)
        }
    }

    private fun coachResult(): SavingsCoachResult {
        val zero = BigInteger.ZERO
        return SavingsCoachResult(
            goal = SavingsCoachGoalProgress(
                goalId = "goal-1",
                goalName = "Emergency fund",
                targetAmountMinor = BigInteger.valueOf(10_000_000L),
                savedAmountMinor = BigInteger.valueOf(2_000_000L),
                remainingAmountMinor = BigInteger.valueOf(8_000_000L),
                progressPercent = 20,
                targetDate = LocalDate.of(2027, 1, 31)
            ),
            qualifyingReceiptsMinor = BigInteger.valueOf(10_000_000L),
            recordedExpensesMinor = BigInteger.valueOf(5_000_000L),
            upcomingBillsMinor = zero,
            currentMonthGoalAllocationsMinor = zero,
            cryptoLossAdjustmentMinor = zero,
            estimatedCapacityBeforeReserveMinor = BigInteger.valueOf(5_000_000L),
            safetyReserveMinor = BigInteger.valueOf(1_000_000L),
            estimatedCapacityAfterReserveMinor = BigInteger.valueOf(4_000_000L),
            monthlyNeedMinor = BigInteger.valueOf(1_000_000L),
            suggestedContributionMinor = BigInteger.valueOf(100_000L),
            discretionarySpendingMinor = zero,
            discretionaryReductionTipMinor = null,
            unavailableReason = null
        )
    }
}
