package com.example.personalfinanceapp.feature.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.personalfinanceapp.feature.dashboard.ui.DashboardUiState
import com.example.personalfinanceapp.feature.dashboard.ui.NavDestination
import com.example.personalfinanceapp.feature.dashboard.ui.OverviewScreen
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import org.junit.Assert.assertEquals
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

    @Test
    fun overviewBottomNavigationExposesActivityGoalsAndMore() {
        val selectedDestinations = mutableListOf<NavDestination>()
        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                OverviewScreen(
                    uiState = DashboardUiState(userName = "Himasha"),
                    currentNavDestination = NavDestination.OVERVIEW,
                    onNavDestinationSelected = selectedDestinations::add,
                    onNavigateToActivity = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Activity").performClick()
        composeTestRule.onNodeWithText("Goals").performClick()
        composeTestRule.onNodeWithText("More").performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(
                    NavDestination.ACTIVITY,
                    NavDestination.GOALS,
                    NavDestination.MORE
                ),
                selectedDestinations
            )
        }
    }
}
