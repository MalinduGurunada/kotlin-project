package com.example.personalfinanceapp.feature.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.example.personalfinanceapp.feature.dashboard.ui.FinancialSummaryUiState
import com.example.personalfinanceapp.feature.dashboard.ui.IncomeSpendingBreakdownScreen
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigInteger

class FinancialSummaryComposeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingStateShowsProgress() {
        setSummaryContent(FinancialSummaryUiState(isLoading = true))

        composeRule.onNodeWithTag("summary_loading").assertIsDisplayed()
    }

    @Test
    fun errorStateShowsMessage() {
        setSummaryContent(FinancialSummaryUiState(errorMessage = "Unable to load summary"))

        composeRule.onNodeWithText("Financial summaries are unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Unable to load summary").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsAccurateMessages() {
        setSummaryContent(FinancialSummaryUiState(periodLabel = "November 2026"))

        assertTextAfterScroll("No income transactions in this period.")
        assertTextAfterScroll("No expense transactions in this period.")
        assertTextAfterScroll("NO ACTIVITY")
        assertTextAfterScroll("No realised crypto activity")
        assertTextAfterScroll("No income or expense transactions in this period.")
    }

    @Test
    fun marginUsesCashFlowWording() {
        setSummaryContent(
            FinancialSummaryUiState(
                periodLabel = "November 2026",
                netCashFlowMarginPercent = BigInteger.valueOf(25L)
            )
        )

        composeRule.onNodeWithText("Net cash-flow margin 25%").assertIsDisplayed()
    }

    private fun setSummaryContent(state: FinancialSummaryUiState) {
        composeRule.setContent {
            PersonalFinanceAppTheme(dynamicColor = false) {
                IncomeSpendingBreakdownScreen(
                    uiState = state,
                    onPeriodSelected = {},
                    onBackClicked = {}
                )
            }
        }
    }

    private fun assertTextAfterScroll(text: String) {
        composeRule.onNodeWithTag("summary_content").performScrollToNode(hasText(text))
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
