package com.example.personalfinanceapp.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.personalfinanceapp.feature.auth.ui.LoginScreen
import com.example.personalfinanceapp.feature.auth.ui.LoginUiState
import com.example.personalfinanceapp.feature.auth.ui.PasswordResetScreen
import com.example.personalfinanceapp.feature.auth.ui.PasswordResetUiState
import com.example.personalfinanceapp.feature.auth.ui.RegisterScreen
import com.example.personalfinanceapp.feature.auth.ui.RegisterUiState
import com.example.personalfinanceapp.feature.dashboard.ui.MoreScreen
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthNavigationAndUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun registerBackReturnsToSingleLoginDestination() {
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = Login) {
                composable<Login> {
                    Text("Sign in destination", Modifier.testTag("login_destination"))
                }
                composable<Register> {
                    RegisterScreen(
                        uiState = RegisterUiState(),
                        onFullNameChanged = {},
                        onEmailChanged = {},
                        onPasswordChanged = {},
                        onConfirmPasswordChanged = {},
                        onRegisterClicked = {},
                        onNavigateToLogin = {
                            navController.popBackStack()
                        }
                    )
                }
            }
            LaunchedEffect(Unit) {
                navController.navigate(Register)
            }
        }

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithTag("login_destination").assertIsDisplayed()
        composeRule.runOnIdle { assertNull(navController.previousBackStackEntry) }
    }

    @Test
    fun systemBackFromResetReturnsToLogin() {
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = Login) {
                composable<Login> {
                    Text("Sign in destination", Modifier.testTag("login_destination"))
                }
                composable<PasswordReset> {
                    PasswordResetScreen(
                        uiState = PasswordResetUiState(),
                        onEmailChanged = {},
                        onResetClicked = {},
                        onNavigateToLogin = {
                            navController.popBackStack()
                        }
                    )
                }
            }
            LaunchedEffect(Unit) {
                navController.navigate(PasswordReset)
            }
        }

        composeRule.onNodeWithText("Reset your password").assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithTag("login_destination").assertIsDisplayed()
        composeRule.runOnIdle { assertNull(navController.previousBackStackEntry) }
    }

    @Test
    fun moreScreenExposesAllManagementDestinationsAndSignOut() {
        val destinations = mutableListOf<ManagementDestination>()
        var signOutClicked = false
        composeRule.setContent {
            MoreScreen(
                userName = "Raveen",
                onCategoriesClick = { destinations += ManagementDestination.CATEGORIES },
                onExpensesClick = { destinations += ManagementDestination.EXPENSES },
                onIncomeClick = { destinations += ManagementDestination.INCOME },
                onReceivablesClick = { destinations += ManagementDestination.RECEIVABLES },
                onSignOut = { signOutClicked = true },
                onBottomDestinationSelected = {}
            )
        }

        composeRule.onNodeWithText("More / Preferences").assertIsDisplayed()
        composeRule.onNodeWithTag("more_categories").performClick()
        composeRule.onNodeWithTag("more_list")
            .performScrollToNode(hasTestTag("more_expenses"))
        composeRule.onNodeWithTag("more_expenses").performClick()
        composeRule.onNodeWithTag("more_list")
            .performScrollToNode(hasTestTag("more_income"))
        composeRule.onNodeWithTag("more_income").performClick()
        composeRule.onNodeWithTag("more_list")
            .performScrollToNode(hasTestTag("more_receivables"))
        composeRule.onNodeWithTag("more_receivables").performClick()
        composeRule.onNodeWithTag("more_list")
            .performScrollToNode(hasTestTag("more_sign_out"))
        composeRule.onNodeWithTag("more_sign_out").performClick()
        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    ManagementDestination.CATEGORIES,
                    ManagementDestination.EXPENSES,
                    ManagementDestination.INCOME,
                    ManagementDestination.RECEIVABLES
                ),
                destinations
            )
            assertTrue(signOutClicked)
        }
    }

    @Test
    fun allDestinationTypesRegisterAndTransactionDetailReceivesItsId() {
        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = Login) {
                composable<Login> { Text("Login") }
                composable<Register> { Text("Register") }
                composable<PasswordReset> { Text("Password reset") }
                composable<Home> { Text("More") }
                composable<Overview> { Text("Overview") }
                composable<Activity> { Text("Activity") }
                composable<Goals> { Text("Goals") }
                composable<Category> { Text("Categories") }
                composable<ExpenseList> { Text("Expenses") }
                composable<IncomeList> { Text("Income") }
                composable<ReceivableList> { Text("Receivables") }
                composable<TransactionDetail> { backStackEntry ->
                    val route: TransactionDetail = backStackEntry.toRoute()
                    Text(route.transactionId, Modifier.testTag("transaction_id"))
                }
            }
            LaunchedEffect(Unit) {
                navController.navigate(Register)
                navController.navigate(PasswordReset)
                navController.navigate(Home)
                navController.navigate(Overview)
                navController.navigate(Activity)
                navController.navigate(Goals)
                navController.navigate(Category)
                navController.navigate(ExpenseList)
                navController.navigate(IncomeList)
                navController.navigate(ReceivableList)
                navController.navigate(TransactionDetail("transaction-42"))
            }
        }

        composeRule.onNodeWithTag("transaction_id").assertIsDisplayed()
        composeRule.onNodeWithText("transaction-42").assertIsDisplayed()
    }

    @Test
    fun passwordVisibilityAndLoadingStateAreAccessible() {
        composeRule.setContent {
            PersonalFinanceAppTheme(dynamicColor = false) {
                LoginScreen(
                    uiState = LoginUiState(
                        emailInput = "user@example.com",
                        passwordInput = "password123",
                        isLoading = true
                    ),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onLoginClicked = {},
                    onNavigateToRegister = {},
                    onNavigateToPasswordReset = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Email address").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Show password").performClick()
        composeRule.onNodeWithContentDescription("Hide password").assertIsDisplayed()
        composeRule.onNode(hasClickAction() and isNotEnabled()).assertIsDisplayed()
    }
}
