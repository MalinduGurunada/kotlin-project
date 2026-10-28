package com.example.personalfinanceapp.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.personalfinanceapp.feature.auth.ui.LoginScreen
import com.example.personalfinanceapp.feature.auth.ui.LoginUiState
import com.example.personalfinanceapp.feature.auth.ui.PasswordResetScreen
import com.example.personalfinanceapp.feature.auth.ui.PasswordResetUiState
import com.example.personalfinanceapp.feature.auth.ui.RegisterScreen
import com.example.personalfinanceapp.feature.auth.ui.RegisterUiState
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
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
                        onNavigateToLogin = navController::returnToSignIn
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
                        onNavigateToLogin = navController::returnToSignIn
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
    fun homeActionsExposeIncomeAndSignOut() {
        var incomeClicked = false
        var signOutClicked = false
        composeRule.setContent {
            HomeActionsScreen(
                onManageIncome = { incomeClicked = true },
                onSignOut = { signOutClicked = true }
            )
        }

        composeRule.onNodeWithText("Manage Income").performClick()
        composeRule.onNodeWithText("Sign Out").performClick()
        composeRule.runOnIdle {
            assertTrue(incomeClicked)
            assertTrue(signOutClicked)
        }
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
        composeRule.onNode(hasText("Sign in") and hasClickAction()).assertIsNotEnabled()
    }
}
