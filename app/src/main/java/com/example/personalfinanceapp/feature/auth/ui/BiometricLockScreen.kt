package com.example.personalfinanceapp.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite

@Composable
fun BiometricLockScreen(
    userName: String,
    onUnlockClicked: () -> Unit,
    onSignOutClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstName = userName.trim().split(" ").firstOrNull()?.ifBlank { "User" } ?: "User"

    AuthBackground {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(
                horizontal = AuthDimens.ScreenPadding,
                vertical = 24.dp
            ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProfileAvatar(
                        name = userName,
                        size = 72.dp,
                        containerColor = AuthColors.Lime,
                        contentColor = AuthColors.EmeraldDeep,
                        fontSize = 24
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "FinClarity",
                        color = AuthColors.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Welcome back, $firstName",
                        color = AuthColors.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your finances are protected.\nScan your fingerprint to unlock.",
                        color = AuthColors.TextMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(AuthColors.Lime.copy(alpha = 0.15f))
                            .clickable(onClick = onUnlockClicked),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👆", fontSize = 42.sp)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onUnlockClicked,
                        shape = RoundedCornerShape(AuthDimens.ButtonRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryDarkGreen,
                            contentColor = SurfaceWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AuthDimens.ButtonHeight)
                    ) {
                        Text(
                            text = "Unlock with Fingerprint",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Use Account Login Instead",
                        color = AuthColors.Lime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable(onClick = onSignOutClicked)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BiometricLockScreenPreview() {
    PersonalFinanceAppTheme {
        BiometricLockScreen(
            userName = "Himasha",
            onUnlockClicked = {},
            onSignOutClicked = {}
        )
    }
}
