package com.example.personalfinanceapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.personalfinanceapp.feature.notification.FinanceReviewPayload
import com.example.personalfinanceapp.feature.notification.NotificationPermissionPolicy
import com.example.personalfinanceapp.navigation.AppNavHost
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var notificationTarget by mutableStateOf<FinanceReviewPayload?>(null)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationTarget = FinanceReviewPayload.fromIntent(intent)

        enableEdgeToEdge()

        setContent {
            PersonalFinanceAppTheme {
                AppNavHost(
                    notificationTarget = notificationTarget,
                    onNotificationTargetConsumed = { notificationTarget = null },
                    onAuthenticated = ::requestNotificationPermissionIfNeeded
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationTarget = FinanceReviewPayload.fromIntent(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        val preferences = getSharedPreferences(PERMISSION_PREFERENCES, MODE_PRIVATE)
        val isGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val wasRequested = preferences.getBoolean(KEY_PERMISSION_REQUESTED, false)
        if (
            NotificationPermissionPolicy.shouldRequest(
                isGranted = isGranted,
                wasRequested = wasRequested,
                isAuthenticated = true
            )
        ) {
            preferences.edit().putBoolean(KEY_PERMISSION_REQUESTED, true).apply()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private companion object {
        const val PERMISSION_PREFERENCES = "notification_permission"
        const val KEY_PERMISSION_REQUESTED = "requested"
    }
}
