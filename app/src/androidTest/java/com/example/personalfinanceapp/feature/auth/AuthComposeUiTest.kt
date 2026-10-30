package com.example.personalfinanceapp.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.personalfinanceapp.feature.auth.ui.LoginScreen
import com.example.personalfinanceapp.feature.auth.ui.LoginUiState
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import org.junit.Rule
import org.junit.Test

class AuthComposeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreenDisplaysInputsAndTriggersCallbacks() {
        var emailEntered = ""
        var passwordEntered = ""
        var loginClicked = false
        var googleLoginClicked = false

        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                LoginScreen(
                    uiState = LoginUiState(
                        emailInput = "himasha@example.com",
                        passwordInput = "password123"
                    ),
                    onEmailChanged = { emailEntered = it },
                    onPasswordChanged = { passwordEntered = it },
                    onLoginClicked = { loginClicked = true },
                    onGoogleSignInClicked = { googleLoginClicked = true },
                    onNavigateToRegister = {},
                    onNavigateToPasswordReset = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Welcome back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in").performClick()
        composeTestRule.onNodeWithContentDescription("Continue with Google").performClick()
        assert(loginClicked)
        assert(googleLoginClicked)
    }
}
