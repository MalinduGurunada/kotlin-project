package com.example.personalfinanceapp.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmailVerificationScreen(
    uiState: EmailVerificationUiState,
    onResendClicked: () -> Unit,
    onCheckStatusClicked: () -> Unit,
    onSignOutClicked: () -> Unit
) {
    val busy = uiState.isResending || uiState.isChecking

    AuthBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                horizontal = AuthDimens.ScreenPadding,
                vertical = 24.dp
            ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    AuthBrand()

                    Text(
                        text = "@",
                        color = AuthColors.Lime,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .background(AuthColors.Lime.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, AuthColors.Lime.copy(alpha = 0.3f), CircleShape)
                            .padding(top = 13.dp)
                    )

                    Text(
                        text = "Verify your email",
                        color = AuthColors.White,
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Verification is required for",
                        color = AuthColors.TextMuted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = uiState.email.ifBlank { "your email address" },
                        color = AuthColors.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Open the link, then return here and check your status. Financial features stay locked until verification is complete.",
                        color = AuthColors.TextMuted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center
                    )

                    uiState.successMessage?.let {
                        AuthMessage(message = it, isError = false)
                    }
                    uiState.errorMessage?.let {
                        AuthMessage(message = it, isError = true)
                    }

                    AuthPrimaryButton(
                        text = "Check status",
                        onClick = onCheckStatusClicked,
                        isLoading = uiState.isChecking,
                        enabled = !busy,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    OutlinedButton(
                        onClick = onResendClicked,
                        enabled = !busy,
                        shape = RoundedCornerShape(AuthDimens.ButtonRadius),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AuthColors.Lime,
                            disabledContentColor = AuthColors.TextMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Resend verification email" }
                    ) {
                        if (uiState.isResending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = AuthColors.Lime,
                                strokeWidth = 2.25.dp
                            )
                        } else {
                            Text("Resend", fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = onSignOutClicked,
                        enabled = !busy,
                        modifier = Modifier.semantics { contentDescription = "Sign out" }
                    ) {
                        Text("Sign out", color = AuthColors.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
