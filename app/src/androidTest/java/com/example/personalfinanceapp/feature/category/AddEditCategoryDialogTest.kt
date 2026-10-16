package com.example.personalfinanceapp.feature.category

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddEditCategoryDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun saveButton_whenNameIsBlankAndFilled_updatesEnabledState() {
        // Tests that save button is disabled for blank name and enabled when name is typed
        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                AddEditCategoryDialog(
                    title = "Add Category",
                    initialName = "",
                    onNameChange = {},
                    onConfirm = { _, _ -> },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("category_dialog_save").assertIsNotEnabled()

        composeTestRule.onNodeWithTag("category_dialog_name_field").performTextInput("Groceries")

        composeTestRule.onNodeWithTag("category_dialog_save").assertIsEnabled()
    }

    @Test
    fun typingAndConfirm_whenInteracted_callsOnNameChangeAndConfirmWithData() {
        // Tests that typing triggers onNameChange and clicking save calls onConfirm
        var nameChangedCalled = false
        var confirmedName: String? = null
        var confirmedClass: SpendingClass? = null

        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                AddEditCategoryDialog(
                    title = "Add Category",
                    initialName = "",
                    initialSpendingClass = SpendingClass.DISCRETIONARY,
                    onNameChange = { nameChangedCalled = true },
                    onConfirm = { name, spendingClass ->
                        confirmedName = name
                        confirmedClass = spendingClass
                    },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("category_dialog_name_field").performTextInput("Food")
        assertTrue(nameChangedCalled)

        composeTestRule.onNodeWithTag("category_dialog_option_committed").performClick()
        composeTestRule.onNodeWithTag("category_dialog_save").performClick()

        assertEquals("Food", confirmedName)
        assertEquals(SpendingClass.COMMITTED, confirmedClass)
    }

    @Test
    fun errorMessage_whenProvided_isDisplayedInlineWithDialog() {
        // Tests that error message displays inline while dialog remains visible
        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                AddEditCategoryDialog(
                    title = "Add Category",
                    initialName = "Food",
                    errorMessage = "Category name already exists",
                    onNameChange = {},
                    onConfirm = { _, _ -> },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("category_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_dialog_error", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Category name already exists").assertIsDisplayed()
    }

    @Test
    fun spendingClassOptions_whenNotEditable_areDisabled() {
        // Tests that spending class choices are disabled in rename mode
        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                AddEditCategoryDialog(
                    title = "Rename Category",
                    initialName = "Food",
                    isSpendingClassEditable = false,
                    onNameChange = {},
                    onConfirm = { _, _ -> },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("category_dialog_option_committed").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("category_dialog_option_discretionary").assertIsNotEnabled()
    }

    @Test
    fun cancelButton_whenClicked_callsOnDismiss() {
        // Tests that clicking cancel calls onDismiss callback
        var dismissCalled = false

        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                AddEditCategoryDialog(
                    title = "Add Category",
                    onNameChange = {},
                    onConfirm = { _, _ -> },
                    onDismiss = { dismissCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("category_dialog_cancel").performClick()
        assertTrue(dismissCalled)
    }
}
