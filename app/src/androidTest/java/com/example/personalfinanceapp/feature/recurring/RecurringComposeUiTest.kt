package com.example.personalfinanceapp.feature.recurring

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.RecurrenceFrequency
import com.example.personalfinanceapp.core.model.RecurringExpense
import com.example.personalfinanceapp.core.model.RecurringOccurrence
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.recurring.ui.DueItemsContent
import com.example.personalfinanceapp.feature.recurring.ui.RecurringCommitmentsContent
import com.example.personalfinanceapp.feature.recurring.ui.RecurringDueItem
import com.example.personalfinanceapp.feature.recurring.ui.RecurringFilter
import com.example.personalfinanceapp.feature.recurring.ui.RecurringFormState
import com.example.personalfinanceapp.feature.recurring.ui.RecurringCommitmentEditorScreen
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
        composeRule.onNodeWithText("Reminder 2 day(s) before at 6:30 PM • BANK")
            .assertIsDisplayed()
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

    @Test
    fun nextMonthlyDateBeyondThirtyDaysRemainsOnCommitment() {
        val state = farFutureState()
        composeRule.setContent {
            PersonalFinanceAppTheme(dynamicColor = false) {
                RecurringCommitmentsContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onBack = {},
                    onAdd = {},
                    onOpenDueItems = {},
                    onFilterChanged = {},
                    onEdit = {},
                    onStatusChanged = { _, _ -> },
                    onRecordPayment = {}
                )
            }
        }

        composeRule.onNodeWithText("2026-11-30").assertIsDisplayed()
        composeRule.onNodeWithTag("record_payment_electricity_2026-11-30").assertIsDisplayed()
    }

    @Test
    fun dueItemsScreenHidesNextDateBeyondThirtyDayPeriod() {
        val state = farFutureState()
        composeRule.setContent {
            PersonalFinanceAppTheme(dynamicColor = false) {
                DueItemsContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onBack = {},
                    onRecordPayment = {},
                    onSkip = {}
                )
            }
        }

        composeRule.onNodeWithTag("due_item_electricity_2026-11-30").assertDoesNotExist()
        composeRule.onNodeWithText("No due items in this period.").assertIsDisplayed()
    }

    @Test
    fun editorOpensTwelveHourReminderClock() {
        composeRule.setContent {
            PersonalFinanceAppTheme(dynamicColor = false) {
                RecurringCommitmentEditorScreen(
                    state = RecurringFormState(
                        id = "new",
                        isEditing = false,
                        title = "Test commitment",
                        categoryId = "utilities",
                        amount = "100.00",
                        paymentChannel = PaymentChannel.BANK,
                        dueDate = "2026-11-05",
                        reminderEnabled = true,
                        reminderTimeMinutes = 18 * 60 + 30
                    ),
                    categories = listOf(
                        Category("utilities", "Utilities & Bills", SpendingClass.COMMITTED)
                    ),
                    onBack = {},
                    onTitleChanged = {},
                    onCategoryChanged = {},
                    onAmountChanged = {},
                    onCadenceChanged = { _, _ -> },
                    onPaymentChannelChanged = {},
                    onChannelLabelChanged = {},
                    onDueDateChanged = {},
                    onReminderEnabledChanged = {},
                    onReminderLeadDaysChanged = {},
                    onReminderTimeChanged = {},
                    onSave = {}
                )
            }
        }

        composeRule.onNodeWithTag("recurring_editor_list")
            .performScrollToNode(hasTestTag("commitment_reminder_time"))
        composeRule.onNodeWithTag("commitment_reminder_time")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag("commitment_reminder_time_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("AM").assertIsDisplayed()
        composeRule.onNodeWithText("PM").assertIsDisplayed()
    }

    private fun farFutureState(): RecurringUiState {
        val category = Category("bills", "Bills", SpendingClass.COMMITTED)
        val recurring = RecurringExpense(
            id = "electricity",
            title = "Electricity",
            categoryId = category.id,
            expectedAmountMinor = 10_000L,
            frequency = RecurrenceFrequency.MONTHLY,
            anchorDate = LocalDate.of(2026, 10, 31),
            startDate = LocalDate.of(2026, 10, 31)
        )
        val nextOccurrence = RecurringOccurrence(
            id = "electricity_2026-11-30",
            recurringExpenseId = recurring.id,
            dueDate = LocalDate.of(2026, 11, 30)
        )
        return RecurringUiState(
            isLoading = false,
            today = LocalDate.of(2026, 10, 30),
            recurringExpenses = listOf(recurring),
            categories = listOf(category),
            dueItems = listOf(RecurringDueItem(recurring, category, nextOccurrence))
        )
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
            reminderLeadDays = 2L,
            reminderTimeMinutes = 18 * 60 + 30
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
