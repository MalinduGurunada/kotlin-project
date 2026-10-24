package com.example.personalfinanceapp.feature.income.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class IncomeListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingStateRendersAccessibleProgressMessage() {
        setContent(state(IncomeObservationStatus.LOADING))
        composeRule.onNodeWithText("Loading income records").assertIsDisplayed()
    }

    @Test
    fun emptyStateRendersZeroSummaryAndMessage() {
        setContent(state(IncomeObservationStatus.EMPTY))
        composeRule.onNodeWithText("Net income").assertIsDisplayed()
        composeRule.onNodeWithText("LKR 0.00").assertIsDisplayed()
        composeRule.onNodeWithText("No income records for this period.").assertIsDisplayed()
    }

    @Test
    fun successRendersNetSummaryOriginalAmountsLabelsDatesAndStableItems() {
        val incomes = listOf(
            income(
                id = "salary",
                source = IncomeSource.SALARY,
                amount = "125000.00",
                currency = CurrencyCode.LKR
            ),
            income(
                id = "gain",
                source = IncomeSource.CRYPTO,
                amount = "450.00",
                currency = CurrencyCode.USD,
                direction = PnlDirection.GAIN
            ),
            income(
                id = "loss",
                source = IncomeSource.CRYPTO,
                amount = "25.50",
                currency = CurrencyCode.USD,
                direction = PnlDirection.LOSS,
                occurredDate = LocalDate.of(2026, 10, 22)
            )
        )
        setContent(
            state(
                status = IncomeObservationStatus.SUCCESS,
                incomes = incomes,
                totalLkrMinor = -123_45L
            )
        )

        composeRule.onNodeWithText("LKR -123.45").assertIsDisplayed()
        composeRule.onNodeWithText("Salary").assertIsDisplayed()
        composeRule.onNodeWithText("LKR 125,000.00").assertIsDisplayed()
        composeRule.onNodeWithText("Crypto Gain").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("USD 450.00").assertIsDisplayed()
        composeRule.onNodeWithText("Crypto Loss").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Oct 22, 2026").assertIsDisplayed()
        composeRule.onNodeWithTag("income-item-loss").assertIsDisplayed()
    }

    @Test
    fun errorRendersSafeMessageAndInvokesRetry() {
        var retried = false
        setContent(
            state(
                status = IncomeObservationStatus.ERROR,
                errorMessage = "Unable to load income records. Please try again."
            ),
            onRetry = { retried = true }
        )

        composeRule.onNodeWithText("Unable to load income records. Please try again.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertTrue(retried)
    }

    @Test
    fun backAndMonthControlsInvokeCallbacks() {
        var backed = false
        var previous = false
        var next = false
        setContent(
            state(IncomeObservationStatus.EMPTY),
            onBack = { backed = true },
            onPreviousMonth = { previous = true },
            onNextMonth = { next = true }
        )

        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Previous").performClick()
        composeRule.onNodeWithText("Next").performClick()

        assertTrue(backed)
        assertTrue(previous)
        assertTrue(next)
    }

    @Test
    fun addAndLkrEditActionsWorkWhileUsdEditHasClearExplanation() {
        var added = false
        var editedId: String? = null
        val incomes = listOf(
            income(
                id = "lkr",
                source = IncomeSource.SALARY,
                amount = "500.00",
                currency = CurrencyCode.LKR
            ),
            income(
                id = "usd",
                source = IncomeSource.ADSENSE,
                amount = "25.00",
                currency = CurrencyCode.USD
            )
        )
        setContent(
            uiState = state(
                status = IncomeObservationStatus.SUCCESS,
                incomes = incomes,
                totalLkrMinor = 500_00L
            ),
            onAddIncome = { added = true },
            onEditIncome = { editedId = it.id }
        )

        composeRule.onNodeWithTag("add-income").performClick()
        composeRule.onNodeWithTag("edit-income-lkr", useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText(
            "USD editing is not available yet.",
            useUnmergedTree = true
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edit-income-usd", useUnmergedTree = true)
            .assertIsNotEnabled()

        assertTrue(added)
        assertTrue(editedId == "lkr")
    }

    private fun setContent(
        uiState: IncomeUiState,
        onBack: () -> Unit = {},
        onRetry: () -> Unit = {},
        onPreviousMonth: () -> Unit = {},
        onNextMonth: () -> Unit = {},
        onAddIncome: () -> Unit = {},
        onEditIncome: (Income) -> Unit = {}
    ) {
        composeRule.setContent {
            PersonalFinanceAppTheme(dynamicColor = false) {
                IncomeListScreen(
                    uiState = uiState,
                    onBack = onBack,
                    onRetry = onRetry,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onAddIncome = onAddIncome,
                    onEditIncome = onEditIncome
                )
            }
        }
    }

    private fun state(
        status: IncomeObservationStatus,
        incomes: List<Income> = emptyList(),
        totalLkrMinor: Long = 0L,
        errorMessage: String? = null
    ): IncomeUiState = IncomeUiState(
        dateRange = IncomeDateRange(
            startDate = LocalDate.of(2026, 10, 1),
            endDateExclusive = LocalDate.of(2026, 11, 1)
        ),
        incomes = incomes,
        observationStatus = status,
        totalLkrMinor = totalLkrMinor,
        errorMessage = errorMessage
    )

    private fun income(
        id: String,
        source: IncomeSource,
        amount: String,
        currency: CurrencyCode,
        direction: PnlDirection? = null,
        occurredDate: LocalDate = LocalDate.of(2026, 10, 24)
    ): Income = Income(
        id = id,
        source = source,
        entryKind = if (source == IncomeSource.CRYPTO) {
            IncomeEntryKind.REALIZED_PNL
        } else {
            IncomeEntryKind.RECEIPT
        },
        money = Money(
            originalAmountDecimal = amount,
            originalCurrency = currency,
            rateLkrPerUnitDecimal = "1",
            baseAmountMinor = 1L,
            rateSource = RateSource.LKR_FIXED,
            rateDate = occurredDate
        ),
        occurredDate = occurredDate,
        pnlDirection = direction
    )
}
