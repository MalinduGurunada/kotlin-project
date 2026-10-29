package com.example.personalfinanceapp.feature.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.personalfinanceapp.feature.dashboard.ui.DashboardUiState
import com.example.personalfinanceapp.feature.dashboard.ui.NavDestination
import com.example.personalfinanceapp.feature.dashboard.ui.OverviewScreen
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import org.junit.Rule
import org.junit.Test

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
}
