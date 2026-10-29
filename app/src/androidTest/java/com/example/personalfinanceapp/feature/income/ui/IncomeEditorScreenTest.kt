package com.example.personalfinanceapp.feature.income.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.money.MoneyConverter
import com.example.personalfinanceapp.feature.income.rate.IncomeManualRateSource
import com.example.personalfinanceapp.feature.income.rate.IncomeRateSelectionMode
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class IncomeEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addFormRendersMainFieldsAndAcceptsAmountInput() {
        setEditor()

        composeRule.onNodeWithText("Add Income").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Choose LKR or USD. USD values are stored with their exchange-rate snapshot."
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag("income-source").assertIsDisplayed()
        composeRule.onNodeWithTag("income-amount").performTextInput("1250.50")
        composeRule.onNodeWithTag("income-amount").assertTextContains("1250.50")
        composeRule.onNodeWithTag("income-date").assertIsDisplayed()
        composeRule.onNodeWithTag("income-note").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Save Income").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sourceSelectionShowsOnlyRelevantConditionalFields() {
        setEditor()

        composeRule.onNodeWithTag("income-source").performClick()
        composeRule.onNodeWithTag("income-source-option-FREELANCE").performClick()
        composeRule.onNodeWithTag("income-project-reference").assertIsDisplayed()
        composeRule.onAllNodesWithTag("income-asset-symbol").assertCountEquals(0)

        composeRule.onNodeWithTag("income-source").performClick()
        composeRule.onNodeWithTag("income-source-option-CRYPTO").performClick()
        composeRule.onAllNodesWithTag("income-project-reference").assertCountEquals(0)
        composeRule.onNodeWithTag("income-pnl-direction").assertIsDisplayed()
        composeRule.onNodeWithTag("income-asset-symbol").assertIsDisplayed()
    }

    @Test
    fun validationErrorsRenderBesideTheirFields() {
        setEditor(
            addState().copy(
                selectedSource = IncomeSource.CRYPTO,
                errors = IncomeFormErrors(
                    amount = "Amount cannot be empty",
                    pnlDirection = "Select gain or loss",
                    form = "Check the income details"
                )
            )
        )

        composeRule.onNodeWithText("Amount cannot be empty").assertIsDisplayed()
        composeRule.onNodeWithText("Select gain or loss").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Check the income details").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun savingStateShowsProgressAndDisablesRepeatedSubmission() {
        setEditor(addState().copy(isSaving = true))

        composeRule.onNodeWithTag("income-saving").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("income-save").assertIsNotEnabled()
        composeRule.onNodeWithText("Cancel").assertIsNotEnabled()
    }

    @Test
    fun editModeUsesEditLabelsAndOriginalValues() {
        val original = income("existing")
        setEditor(
            addState().copy(
                mode = IncomeEditorMode.EDIT,
                draftId = original.id,
                originalIncome = original,
                amountText = original.money.originalAmountDecimal,
                note = "Original note"
            )
        )

        composeRule.onNodeWithText("Edit Income").assertIsDisplayed()
        composeRule.onNodeWithTag("income-amount").assertTextContains("500")
        composeRule.onNodeWithTag("income-note").performScrollTo().assertTextContains("Original note")
        composeRule.onNodeWithText("Save Changes").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun cancelInvokesCallbackWithoutSaving() {
        var cancelled = false
        var saved = false
        setEditor(
            onBack = { cancelled = true },
            onSave = { saved = true }
        )

        composeRule.onNodeWithText("Cancel").performScrollTo().performClick()

        assertTrue(cancelled)
        assertEquals(false, saved)
    }

    @Test
    fun systemBackIsConsumedWhileSaving() {
        var backed = false
        setEditor(
            initialState = addState().copy(isSaving = true),
            onBack = { backed = true }
        )

        pressBack()
        composeRule.waitForIdle()

        assertFalse(backed)
        composeRule.onNodeWithText("Add Income").assertIsDisplayed()
    }

    @Test
    fun systemBackInvokesCallbackWhenNotSaving() {
        var backed = false
        setEditor(onBack = { backed = true })

        pressBack()
        composeRule.waitForIdle()

        assertTrue(backed)
    }

    @Test
    fun automaticUsdStateShowsReferenceDetailsAndExactPreview() {
        val money = usdMoney("125.50", "305.25", RateSource.EXCHANGE_RATE_API_REFERENCE)
        setEditor(
            addState().copy(
                selectedCurrency = CurrencyCode.USD,
                selectedRateMode = IncomeRateSelectionMode.AUTOMATIC,
                amountText = "125.50",
                estimatedMoney = money
            )
        )

        composeRule.onNodeWithTag("income-usd-rate-controls").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "Automatic rates are reference suggestions and may differ from your transfer rate."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Selected rate: 305.25 LKR/USD").assertIsDisplayed()
        composeRule.onNodeWithText("Converted amount: LKR 38308.88").assertIsDisplayed()
        composeRule.onNodeWithText("Rate source: Automatic API reference").assertIsDisplayed()
    }

    @Test
    fun automaticFailureOffersManualFallbackAndBlocksSave() {
        setEditor(
            addState().copy(
                selectedCurrency = CurrencyCode.USD,
                selectedRateMode = IncomeRateSelectionMode.AUTOMATIC,
                amountText = "10",
                automaticRateError = "Unable to load the USD/LKR reference rate.",
                estimatedMoney = null
            )
        )

        composeRule.onNodeWithTag("income-rate-network-error").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("income-rate-manual-fallback").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("income-manual-rate").assertIsDisplayed()
        composeRule.onNodeWithTag("income-save").assertIsNotEnabled()
    }

    @Test
    fun automaticUsdLoadingShowsProgressAndBlocksSave() {
        setEditor(
            addState().copy(
                selectedCurrency = CurrencyCode.USD,
                selectedRateMode = IncomeRateSelectionMode.AUTOMATIC,
                amountText = "10",
                isLoadingAutomaticRate = true,
                estimatedMoney = null
            )
        )

        composeRule.onNodeWithTag("income-rate-loading").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("income-save").assertIsNotEnabled()
    }

    @Test
    fun manualUsdModeShowsRateSourceDateAndValidation() {
        setEditor(
            addState().copy(
                selectedCurrency = CurrencyCode.USD,
                selectedRateMode = IncomeRateSelectionMode.MANUAL,
                amountText = "10",
                manualRateText = "0",
                manualRateSource = IncomeManualRateSource.REFERENCE,
                manualRateDate = DATE,
                rateValidationError = "Exchange rate must be greater than zero"
            )
        )

        composeRule.onNodeWithTag("income-manual-rate").performScrollTo()
            .assertTextContains("0")
        composeRule.onNodeWithText("Exchange rate must be greater than zero").assertIsDisplayed()
        composeRule.onNodeWithTag("income-manual-source-REFERENCE").assertIsDisplayed()
        composeRule.onNodeWithText("Rate date: $DATE").assertIsDisplayed()
        composeRule.onNodeWithTag("income-save").assertIsNotEnabled()
    }

    @Test
    fun transactionDatePickerPreservesDateAtPositiveTimezoneBoundary() {
        val originalTimeZone = TimeZone.getDefault()
        val boundaryDate = LocalDate.of(2026, 1, 1)
        var selectedDate: LocalDate? = null
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
            setEditor(
                initialState = addState().copy(transactionDate = boundaryDate),
                onTransactionDateChanged = { selectedDate = it }
            )

            composeRule.onNodeWithTag("income-date").performClick()
            composeRule.onNodeWithText("Select").performClick()
            composeRule.waitForIdle()

            assertEquals(boundaryDate, selectedDate)
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    @Test
    fun manualRateDatePickerPreservesDateAtNegativeTimezoneBoundary() {
        val originalTimeZone = TimeZone.getDefault()
        val boundaryDate = LocalDate.of(2026, 12, 31)
        var selectedDate: LocalDate? = null
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Adak"))
            setEditor(
                initialState = addState().copy(
                    selectedCurrency = CurrencyCode.USD,
                    selectedRateMode = IncomeRateSelectionMode.MANUAL,
                    manualRateDate = boundaryDate
                ),
                onManualRateDateChanged = { selectedDate = it }
            )

            composeRule.onNodeWithTag("income-manual-rate-date")
                .performScrollTo()
                .performClick()
            composeRule.onNodeWithText("Select").performClick()
            composeRule.waitForIdle()

            assertEquals(boundaryDate, selectedDate)
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    private fun setEditor(
        initialState: IncomeFormUiState = addState(),
        onBack: () -> Unit = {},
        onSave: () -> Unit = {},
        onTransactionDateChanged: (LocalDate) -> Unit = {},
        onManualRateDateChanged: (LocalDate) -> Unit = {}
    ) {
        composeRule.setContent {
            var state by remember { mutableStateOf(initialState) }
            PersonalFinanceAppTheme(dynamicColor = false) {
                IncomeEditorScreen(
                    uiState = state,
                    onBack = onBack,
                    onSourceChanged = { source ->
                        state = state.copy(
                            selectedSource = source,
                            projectReference = if (source == IncomeSource.FREELANCE) {
                                state.projectReference
                            } else {
                                ""
                            },
                            assetSymbol = if (source == IncomeSource.CRYPTO) {
                                state.assetSymbol
                            } else {
                                ""
                            },
                            pnlDirection = if (source == IncomeSource.CRYPTO) {
                                state.pnlDirection
                            } else {
                                null
                            }
                        )
                    },
                    onAmountChanged = { state = state.copy(amountText = it) },
                    onDateChanged = {
                        state = state.copy(transactionDate = it)
                        onTransactionDateChanged(it)
                    },
                    onCurrencyChanged = { state = state.copy(selectedCurrency = it) },
                    onRateModeChanged = { state = state.copy(selectedRateMode = it) },
                    onManualRateChanged = { state = state.copy(manualRateText = it) },
                    onManualRateSourceChanged = { state = state.copy(manualRateSource = it) },
                    onManualRateDateChanged = {
                        state = state.copy(manualRateDate = it)
                        onManualRateDateChanged(it)
                    },
                    onRefreshAutomaticRate = {},
                    onProjectReferenceChanged = { state = state.copy(projectReference = it) },
                    onReceivableChanged = { state = state.copy(selectedReceivableId = it) },
                    onAssetSymbolChanged = { state = state.copy(assetSymbol = it) },
                    onPnlDirectionChanged = { state = state.copy(pnlDirection = it) },
                    onNoteChanged = { state = state.copy(note = it) },
                    onSave = onSave
                )
            }
        }
    }

    private fun addState(): IncomeFormUiState = IncomeFormUiState(
        mode = IncomeEditorMode.ADD,
        draftId = "draft-id",
        ownerId = "user-a",
        originalIncome = null,
        selectedSource = IncomeSource.SALARY,
        amountText = "",
        transactionDate = DATE,
        projectReference = "",
        assetSymbol = "",
        pnlDirection = null,
        note = ""
    )

    private fun income(id: String): Income = Income(
        id = id,
        source = IncomeSource.SALARY,
        entryKind = IncomeEntryKind.RECEIPT,
        money = MoneyConverter.fromLkr("500", DATE).getOrThrow(),
        occurredDate = DATE
    )

    private fun usdMoney(
        amount: String,
        rate: String,
        source: RateSource
    ) = MoneyConverter.fromUsd(
        amountInput = amount,
        rateLkrPerUsdInput = rate,
        rateSource = source,
        rateDate = DATE
    ).getOrThrow()

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 10, 24)
    }
}
