package com.example.personalfinanceapp.feature.expense

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.personalfinanceapp.feature.expense.ui.ExpenseListScreen
import com.example.personalfinanceapp.feature.expense.ui.ExpenseUiState
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import org.junit.Rule
import org.junit.Test

class ExpenseComposeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun expenseListScreenDisplaysTotalAndEmptyMessage() {
        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                ExpenseListScreen(
                    uiState = ExpenseUiState(
                        expenses = emptyList(),
                        totalLkrMinor = 0L
                    ),
                    onSaveExpense = { _, _, _, _, _, _, _, _ -> },
                    onDeleteExpense = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Expenses").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Monthly Expenses").assertIsDisplayed()
    }
}
