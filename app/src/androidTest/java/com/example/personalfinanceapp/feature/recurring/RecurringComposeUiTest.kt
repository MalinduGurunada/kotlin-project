package com.example.personalfinanceapp.feature.recurring

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.RecurrenceFrequency
import com.example.personalfinanceapp.core.model.RecurringExpense
import com.example.personalfinanceapp.core.model.RecurringOccurrence
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.recurring.ui.DueItemsContent
import com.example.personalfinanceapp.feature.recurring.ui.RecurringCommitmentsContent
import com.example.personalfinanceapp.feature.recurring.ui.RecurringDueItem
import com.example.personalfinanceapp.feature.recurring.ui.RecurringFilter
import com.example.personalfinanceapp.feature.recurring.ui.RecurringUiState
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class RecurringComposeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun commitmentsScreenShowsSummaryAndOverdueAction() {
        val state = state()
        var selectedFilter: RecurringFilter? = null
        var recordedId: String? = null

        composeRule.setContent {
            PersonalFinanceAppTheme(dynamicColor = false) {
                RecurringCommitmentsContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onBack = {},
                    onAdd = {},
                    onOpenDueItems = {},
                    onFilterChanged = { selectedFilter = it },
                    onEdit = {},
                    onStatusChanged = { _, _ -> },
                    onRecordPayment = { recordedId = it.occurrence.id }
                )
            }
        }

        composeRule.onNodeWithText("TOTAL MONTHLY COMMITTED").assertIsDisplayed()
        composeRule.onNodeWithText("CEB Electricity Bill").assertIsDisplayed()
        composeRule.onNodeWithTag("recurring_filter_overdue").performClick()
        assertEquals(RecurringFilter.OVERDUE, selectedFilter)
        composeRule.onNodeWithTag("record_payment_ceb_2026-10-26").performClick()
        assertEquals("ceb_2026-10-26", recordedId)
    }

    @Test
    fun dueItemsScreenProvidesRecordAndSkipActions() {
        val state = state()
        var recorded = false
        var skipped = false

        composeRule.setContent {
            PersonalFinanceAppTheme(dynamicColor = false) {
                DueItemsContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onBack = {},
                    onRecordPayment = { recorded = true },
                    onSkip = { skipped = true }
                )
            }
        }

        composeRule.onNodeWithText("Overdue occurrences").assertIsDisplayed()
        composeRule.onNodeWithTag("due_record_ceb_2026-10-26").performClick()
        composeRule.onNodeWithTag("due_skip_ceb_2026-10-26").performClick()
        assertEquals(true, recorded)
        assertEquals(true, skipped)
    }

    private fun state(): RecurringUiState {
        val category = Category("utilities", "Utilities & Bills", SpendingClass.COMMITTED)
        val recurring = RecurringExpense(
            id = "ceb",
            title = "CEB Electricity Bill",
            categoryId = category.id,
            expectedAmountMinor = 12_500_00L,
            frequency = RecurrenceFrequency.MONTHLY,
            anchorDate = LocalDate.of(2026, 10, 26),
            startDate = LocalDate.of(2026, 10, 26),
            reminderEnabled = true,
            reminderLeadDays = 2L
        )
        val occurrence = RecurringOccurrence(
            id = "ceb_2026-10-26",
            recurringExpenseId = recurring.id,
            dueDate = LocalDate.of(2026, 10, 26)
        )
        return RecurringUiState(
            isLoading = false,
            today = LocalDate.of(2026, 10, 30),
            recurringExpenses = listOf(recurring),
            categories = listOf(category),
            dueItems = listOf(RecurringDueItem(recurring, category, occurrence))
        )
    }
}
