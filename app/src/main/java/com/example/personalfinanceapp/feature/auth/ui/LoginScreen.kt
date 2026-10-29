package com.example.personalfinanceapp.feature.auth.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToPasswordReset: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    AuthBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(
                horizontal = AuthDimens.ScreenPadding,
                vertical = 20.dp
            ),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AuthBrand(Modifier.align(Alignment.Start))
                    PaymentCardArtwork(Modifier.padding(top = 7.dp, bottom = 2.dp))

                    Text(
                        text = "Welcome back",
                        color = AuthColors.White,
                        fontSize = 32.sp,
                        lineHeight = 37.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.7).sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Sign in to take control of your money.",
                        color = AuthColors.TextMuted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 7.dp, bottom = 22.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                        AuthTextField(
                            value = uiState.emailInput,
                            onValueChange = onEmailChanged,
                            label = "Email address",
                            placeholder = "name@example.com",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            leadingIcon = AuthFieldIcon.Email
                        )
                        AuthTextField(
                            value = uiState.passwordInput,
                            onValueChange = onPasswordChanged,
                            label = "Password",
                            placeholder = "Enter your password",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            leadingIcon = AuthFieldIcon.Lock,
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                PasswordVisibilityButton(passwordVisible) {
                                    passwordVisible = !passwordVisible
                                }
                            }
                        )
                    }

                    Text(
                        text = "Forgot password?",
                        color = AuthColors.Lime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable(onClick = onNavigateToPasswordReset)
                            .defaultMinSize(minHeight = 48.dp)
                            .padding(top = 11.dp, bottom = 4.dp, start = 12.dp)
                    )

                    uiState.errorMessage?.let {
                        AuthMessage(
                            message = it,
                            isError = true,
                            modifier = Modifier.padding(top = 11.dp)
                        )
                    }

                    AuthPrimaryButton(
                        text = "Sign in",
                        onClick = onLoginClicked,
                        isLoading = uiState.isLoading,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                }
            }
            item {
                AuthInlineLink(
                    prompt = "New to FinClarity?  ",
                    link = "Create account",
                    onClick = onNavigateToRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginScreenPreview() {
    PersonalFinanceAppTheme(dynamicColor = false) {
        LoginScreen(
            uiState = LoginUiState(
                emailInput = "malindu@example.com",
                passwordInput = "password123"
            ),
            onEmailChanged = {},
            onPasswordChanged = {},
            onLoginClicked = {},
            onNavigateToRegister = {},
            onNavigateToPasswordReset = {}
        )
    }
}
