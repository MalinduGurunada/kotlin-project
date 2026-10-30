package com.example.personalfinanceapp.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
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
import com.example.personalfinanceapp.feature.auth.FakeAuthRepository
import com.example.personalfinanceapp.feature.auth.PendingRegistration
import com.example.personalfinanceapp.feature.auth.PendingRegistrationState
import com.example.personalfinanceapp.feature.auth.PendingRegistrationStore
import com.example.personalfinanceapp.feature.auth.ui.AuthViewModel
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
    fun completeAppNavHostCreatesEveryTypedDestinationWithoutSerializationFailure() {
        val authViewModel = AuthViewModel(
            FakeAuthRepository(),
            EmptyPendingRegistrationStore
        )

        composeRule.setContent {
            PersonalFinanceAppTheme(dynamicColor = false) {
                AppNavHost(authViewModel = authViewModel)
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Welcome back").assertIsDisplayed()
    }

    @Test
    fun primaryTypedRoutesNavigateWithoutSerializationFailure() {
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = Login) {
                composable<Login> { Text("Login route") }
                composable<Register> { Text("Register route") }
                composable<Home> { Text("More route") }
                composable<Overview> { Text("Overview route") }
                composable<ActivityRoute> { Text("Activity route") }
                composable<Goals> { Text("Goals route") }
                composable<IncomeList> { Text("Income route") }
                composable<ReceivableList> { Text("Receivables route") }
            }
        }

        composeRule.onNodeWithText("Login route").assertIsDisplayed()
        composeRule.runOnIdle { navController.navigate(Overview) }
        composeRule.onNodeWithText("Overview route").assertIsDisplayed()
        composeRule.runOnIdle { navController.navigate(ActivityRoute) }
        composeRule.onNodeWithText("Activity route").assertIsDisplayed()
        composeRule.runOnIdle { navController.navigate(Goals) }
        composeRule.onNodeWithText("Goals route").assertIsDisplayed()
        composeRule.runOnIdle { navController.navigate(Home) }
        composeRule.onNodeWithText("More route").assertIsDisplayed()
        composeRule.runOnIdle { navController.navigate(IncomeList) }
        composeRule.onNodeWithText("Income route").assertIsDisplayed()
        composeRule.runOnIdle { navController.navigate(ReceivableList) }
        composeRule.onNodeWithText("Receivables route").assertIsDisplayed()
        composeRule.runOnIdle { navController.navigate(Login) }
        composeRule.onNodeWithText("Login route").assertIsDisplayed()
        composeRule.runOnIdle { navController.navigate(Register) }
        composeRule.onNodeWithText("Register route").assertIsDisplayed()
    }

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
            PersonalFinanceAppTheme(dynamicColor = false) {
                MoreScreen(
                    userName = "Raveen",
                    onCategoriesClick = { destinations += ManagementDestination.CATEGORIES },
                    onExpensesClick = { destinations += ManagementDestination.EXPENSES },
                    onIncomeClick = { destinations += ManagementDestination.INCOME },
                    onReceivablesClick = { destinations += ManagementDestination.RECEIVABLES },
                    onRecurringClick = { destinations += ManagementDestination.RECURRING },
                    onDueItemsClick = { destinations += ManagementDestination.DUE_ITEMS },
                    onSignOut = { signOutClicked = true },
                    onBottomDestinationSelected = {}
                )
            }
        }

        composeRule.onNodeWithText("More / Preferences").assertIsDisplayed()
        composeRule.onNodeWithTag("more_categories").performClick()
        composeRule.onNodeWithTag("more_recurring").performClick()
        composeRule.onNodeWithTag("more_due_items").performClick()
        listOf("more_expenses", "more_income", "more_receivables", "more_sign_out").forEach { tag ->
            composeRule.onNodeWithTag("more_list").performScrollToNode(hasTestTag(tag))
            composeRule.onNodeWithTag(tag).performClick()
        }
        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    ManagementDestination.CATEGORIES,
                    ManagementDestination.RECURRING,
                    ManagementDestination.DUE_ITEMS,
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
                composable<ActivityRoute> { Text("Activity") }
                composable<Goals> { Text("Goals") }
                composable<Category> { Text("Categories") }
                composable<ExpenseList> { Text("Expenses") }
                composable<IncomeList> { Text("Income") }
                composable<ReceivableList> { Text("Receivables") }
                composable<RecurringCommitments> { Text("Recurring") }
                composable<DueItems> { Text("Due items") }
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
                navController.navigate(ActivityRoute)
                navController.navigate(Goals)
                navController.navigate(Category)
                navController.navigate(ExpenseList)
                navController.navigate(IncomeList)
                navController.navigate(ReceivableList)
                navController.navigate(RecurringCommitments)
                navController.navigate(DueItems)
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
                    onGoogleSignInClicked = {},
                    onNavigateToRegister = {},
                    onNavigateToPasswordReset = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Email address").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Show password").performClick()
        composeRule.onNodeWithContentDescription("Hide password").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sign in").assertIsNotEnabled()
    }

    private object EmptyPendingRegistrationStore : PendingRegistrationStore {
        override fun read(): PendingRegistrationState = PendingRegistrationState.Absent
        override fun save(pendingRegistration: PendingRegistration): Result<Unit> =
            Result.success(Unit)
        override fun clear(): Result<Unit> = Result.success(Unit)
    }
}
