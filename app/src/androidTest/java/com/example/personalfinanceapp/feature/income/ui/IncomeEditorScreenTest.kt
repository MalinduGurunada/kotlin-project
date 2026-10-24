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
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.money.MoneyConverter
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class IncomeEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addFormRendersMainFieldsAndAcceptsAmountInput() {
        setEditor()

        composeRule.onNodeWithText("Add Income").assertIsDisplayed()
        composeRule.onNodeWithText("All amounts in this form are recorded in LKR.")
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

    private fun setEditor(
        initialState: IncomeFormUiState = addState(),
        onBack: () -> Unit = {},
        onSave: () -> Unit = {}
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
                    onDateChanged = { state = state.copy(transactionDate = it) },
                    onProjectReferenceChanged = { state = state.copy(projectReference = it) },
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

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 10, 24)
    }
}
