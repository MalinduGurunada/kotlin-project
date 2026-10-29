package com.example.personalfinanceapp.feature.auth.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme

@Composable
fun RegisterScreen(
    uiState: RegisterUiState,
    onFullNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onRegisterClicked: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    BackHandler(onBack = onNavigateToLogin)

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = AuthDimens.ScreenPadding, vertical = 18.dp)
        ) {
            AuthTopBar(onBack = onNavigateToLogin)

            Text(
                text = "Create your account",
                color = AuthColors.White,
                fontSize = 31.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.65).sp,
                modifier = Modifier.padding(top = 30.dp)
            )
            Text(
                text = "Start building a clearer financial future.",
                color = AuthColors.TextMuted,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 7.dp, bottom = 22.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AuthTextField(
                    value = uiState.fullNameInput,
                    onValueChange = onFullNameChanged,
                    label = "Full name",
                    placeholder = "Enter your full name",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = AuthFieldIcon.Person,
                    enabled = !uiState.isLoading
                )
                AuthTextField(
                    value = uiState.emailInput,
                    onValueChange = onEmailChanged,
                    label = "Email address",
                    placeholder = "name@example.com",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = AuthFieldIcon.Email,
                    enabled = !uiState.isLoading && !uiState.isAccountCreated
                )
                AuthTextField(
                    value = uiState.passwordInput,
                    onValueChange = onPasswordChanged,
                    label = "Password",
                    placeholder = "Create a password",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = AuthFieldIcon.Lock,
                    enabled = !uiState.isLoading && !uiState.isAccountCreated,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        PasswordVisibilityButton(passwordVisible) {
                            passwordVisible = !passwordVisible
                        }
                    }
                )
                AuthTextField(
                    value = uiState.confirmPasswordInput,
                    onValueChange = onConfirmPasswordChanged,
                    label = "Confirm password",
                    placeholder = "Re-enter your password",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    leadingIcon = AuthFieldIcon.Lock,
                    enabled = !uiState.isLoading && !uiState.isAccountCreated,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        PasswordVisibilityButton(confirmPasswordVisible) {
                            confirmPasswordVisible = !confirmPasswordVisible
                        }
                    }
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PasswordRequirementChip(
                    text = "6+ characters",
                    satisfied = uiState.passwordInput.length >= 6
                )
                PasswordRequirementChip(
                    text = "Passwords match",
                    satisfied = uiState.passwordInput.isNotEmpty() &&
                        uiState.passwordInput == uiState.confirmPasswordInput
                )
            }

            uiState.errorMessage?.let {
                AuthMessage(it, isError = true, modifier = Modifier.padding(top = 13.dp))
            }

            AuthPrimaryButton(
                text = when {
                    uiState.requiresRecoveryAbandonment -> "Setup recovery required"
                    uiState.isAccountCreated -> "Save name"
                    else -> "Create account"
                },
                onClick = onRegisterClicked,
                isLoading = uiState.isLoading,
                modifier = Modifier.padding(top = 21.dp),
                enabled = !uiState.requiresRecoveryAbandonment
            )

            AuthInlineLink(
                prompt = if (uiState.isAccountCreated) "" else "Already have an account?  ",
                link = if (uiState.isAccountCreated) {
                    "Abandon setup and sign out"
                } else {
                    "Sign in"
                },
                onClick = onNavigateToLogin,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
        }
    }
}
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RegisterScreenPreview() {
    PersonalFinanceAppTheme(dynamicColor = false) {
        RegisterScreen(
            uiState = RegisterUiState(
                fullNameInput = "Malindu Perera",
                emailInput = "malindu@example.com",
                passwordInput = "password123",
                confirmPasswordInput = "password123"
            ),
            onFullNameChanged = {},
            onEmailChanged = {},
            onPasswordChanged = {},
            onConfirmPasswordChanged = {},
            onRegisterClicked = {},
            onNavigateToLogin = {}
        )
    }
}
