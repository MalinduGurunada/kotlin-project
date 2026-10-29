package com.example.personalfinanceapp.feature.category

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.category.ui.CategoryScreenContent
import com.example.personalfinanceapp.feature.category.ui.CategoryUiState
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CategoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingState_whenLoading_showsLoadingIndicator() {
        // Tests that loading state displays the circular progress indicator
        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                CategoryScreenContent(
                    uiState = CategoryUiState.Loading
                )
            }
        }

        composeTestRule.onNodeWithTag("category_loading").assertIsDisplayed()
    }

    @Test
    fun errorState_whenError_showsErrorMessageAndTitle() {
        // Tests that error state displays error title and message
        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                CategoryScreenContent(
                    uiState = CategoryUiState.Error("Network failure")
                )
            }
        }

        composeTestRule.onNodeWithTag("category_error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network failure").assertIsDisplayed()
    }

    @Test
    fun emptyState_whenNoCategories_showsEmptyMessage() {
        // Tests that empty state displays no categories message
        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                CategoryScreenContent(
                    uiState = CategoryUiState.Success(
                        activeCategories = emptyList(),
                        archivedCategories = emptyList()
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag("category_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Categories Yet").assertIsDisplayed()
    }

    @Test
    fun activeCategories_whenSuccess_showsNamesAndSpendingClassLabels() {
        // Tests that active categories display names and spending class labels
        val activeList = listOf(
            Category("1", "Rent", SpendingClass.COMMITTED),
            Category("2", "Food", SpendingClass.DISCRETIONARY)
        )

        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                CategoryScreenContent(
                    uiState = CategoryUiState.Success(
                        activeCategories = activeList,
                        archivedCategories = emptyList()
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Rent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Locked in").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your choice").assertIsDisplayed()
    }

    @Test
    fun renameAndArchiveButtons_whenClicked_callsCallbacksWithCategory() {
        // Tests that clicking rename or archive triggers callback with correct category
        val category = Category("1", "Food", SpendingClass.DISCRETIONARY)
        var renamedCategory: Category? = null
        var archivedCategory: Category? = null

        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                CategoryScreenContent(
                    uiState = CategoryUiState.Success(
                        activeCategories = listOf(category),
                        archivedCategories = emptyList()
                    ),
                    onRenameClick = { renamedCategory = it },
                    onArchiveClick = { archivedCategory = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Rename").performClick()
        assertEquals(category, renamedCategory)

        composeTestRule.onNodeWithText("Archive").performClick()
        assertEquals(category, archivedCategory)
    }

    @Test
    fun archivedSection_whenHeaderTapped_expandsAndShowsArchivedCategoriesWithRestore() {
        // Tests that archived section expands on header tap to reveal archived categories
        val archivedCategory = Category("2", "Gym", SpendingClass.DISCRETIONARY, archived = true)

        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                CategoryScreenContent(
                    uiState = CategoryUiState.Success(
                        activeCategories = emptyList(),
                        archivedCategories = listOf(archivedCategory)
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag("category_archived_header").assertIsDisplayed()
        composeTestRule.onNodeWithText("ARCHIVED CATEGORIES (1)", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Gym").assertDoesNotExist()

        composeTestRule.onNodeWithTag("category_archived_header").performClick()

        composeTestRule.onNodeWithText("Gym").assertIsDisplayed()
        composeTestRule.onNodeWithText("Restore").assertIsDisplayed()
    }

    @Test
    fun restoreButton_whenClicked_callsRestoreCallback() {
        // Tests that clicking restore calls restore callback with correct category
        val archivedCategory = Category("2", "Gym", SpendingClass.DISCRETIONARY, archived = true)
        var restoredCategory: Category? = null

        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                CategoryScreenContent(
                    uiState = CategoryUiState.Success(
                        activeCategories = emptyList(),
                        archivedCategories = listOf(archivedCategory)
                    ),
                    onRestoreClick = { restoredCategory = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("category_archived_header").performClick()
        composeTestRule.onNodeWithText("Restore").performClick()

        assertEquals(archivedCategory, restoredCategory)
    }

    @Test
    fun deleteOption_always_isNotPresentOnScreen() {
        // Tests that delete options are not present anywhere on screen
        val activeCategory = Category("1", "Food", SpendingClass.DISCRETIONARY)
        val archivedCategory = Category("2", "Gym", SpendingClass.DISCRETIONARY, archived = true)

        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                CategoryScreenContent(
                    uiState = CategoryUiState.Success(
                        activeCategories = listOf(activeCategory),
                        archivedCategories = listOf(archivedCategory)
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag("category_archived_header").performClick()
        composeTestRule.onNodeWithText("Delete").assertDoesNotExist()
    }
}
