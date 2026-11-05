package com.example.personalfinanceapp.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.personalfinanceapp.feature.auth.ui.LoginScreen
import com.example.personalfinanceapp.feature.auth.ui.LoginUiState
import com.example.personalfinanceapp.feature.auth.ui.EmailVerificationScreen
import com.example.personalfinanceapp.feature.auth.ui.EmailVerificationUiState
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

    @Test
    fun verificationScreenExposesRequiredActions() {
        var resendClicked = false
        var checkClicked = false
        var signOutClicked = false

        composeTestRule.setContent {
            PersonalFinanceAppTheme {
                EmailVerificationScreen(
                    uiState = EmailVerificationUiState(email = "pending@example.com"),
                    onResendClicked = { resendClicked = true },
                    onCheckStatusClicked = { checkClicked = true },
                    onSignOutClicked = { signOutClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Verify your email").assertIsDisplayed()
        composeTestRule.onNodeWithText("pending@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Resend verification email").performClick()
        composeTestRule.onNodeWithContentDescription("Check status").performClick()
        composeTestRule.onNodeWithContentDescription("Sign out").performClick()

        assert(resendClicked)
        assert(checkClicked)
        assert(signOutClicked)
    }
}
