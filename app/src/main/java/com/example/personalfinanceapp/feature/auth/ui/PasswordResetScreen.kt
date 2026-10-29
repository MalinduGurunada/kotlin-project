package com.example.personalfinanceapp.feature.auth.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme

@Composable
fun PasswordResetScreen(
    uiState: PasswordResetUiState,
    onEmailChanged: (String) -> Unit,
    onResetClicked: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    BackHandler(onBack = onNavigateToLogin)

    AuthBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(
                horizontal = AuthDimens.ScreenPadding,
                vertical = 18.dp
            ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            item {
                Column {
                    AuthTopBar(onBack = onNavigateToLogin)

                    Text(
                        text = "Reset your password",
                        color = AuthColors.White,
                        fontSize = 31.sp,
                        lineHeight = 37.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.65).sp,
                        modifier = Modifier.padding(top = 62.dp)
                    )
                    Text(
                        text = "Enter the email linked to your account and we’ll send you a secure reset link.",
                        color = AuthColors.TextMuted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 10.dp, bottom = 32.dp)
                    )

                    AuthTextField(
                        value = uiState.emailInput,
                        onValueChange = onEmailChanged,
                        label = "Email address",
                        placeholder = "name@example.com",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        leadingIcon = AuthFieldIcon.Email
                    )

                    uiState.errorMessage?.let {
                        AuthMessage(
                            message = it,
                            isError = true,
                            modifier = Modifier.padding(top = 13.dp)
                        )
                    }
                    if (uiState.isSuccess) {
                        AuthMessage(
                            message = "Reset link sent. Check your inbox to continue.",
                            isError = false,
                            modifier = Modifier.padding(top = 13.dp)
                        )
                    }

                    AuthPrimaryButton(
                        text = "Send reset link",
                        onClick = onResetClicked,
                        isLoading = uiState.isLoading,
                        modifier = Modifier.padding(top = 24.dp)
                    )

                    AuthInlineLink(
                        prompt = "Remember your password?  ",
                        link = "Back to sign in",
                        onClick = onNavigateToLogin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 15.dp)
                    )
                }
            }
            item {
                SupportCard(Modifier.padding(top = 38.dp, bottom = 8.dp))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PasswordResetScreenPreview() {
    PersonalFinanceAppTheme(dynamicColor = false) {
        PasswordResetScreen(
            uiState = PasswordResetUiState(emailInput = "malindu@example.com"),
            onEmailChanged = {},
            onResetClicked = {},
            onNavigateToLogin = {}
        )
    }
}
