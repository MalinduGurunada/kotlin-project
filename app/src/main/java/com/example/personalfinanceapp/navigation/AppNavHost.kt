package com.example.personalfinanceapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.personalfinanceapp.feature.auth.ui.AuthViewModel
import com.example.personalfinanceapp.feature.auth.ui.LoginScreen
import com.example.personalfinanceapp.feature.auth.ui.PasswordResetScreen
import com.example.personalfinanceapp.feature.auth.ui.RegisterScreen
import com.example.personalfinanceapp.feature.dashboard.ui.ActivityScreen
import com.example.personalfinanceapp.feature.dashboard.ui.DashboardViewModel
import com.example.personalfinanceapp.feature.dashboard.ui.NavDestination
import com.example.personalfinanceapp.feature.dashboard.ui.OverviewScreen
import com.example.personalfinanceapp.feature.dashboard.ui.TransactionDetailScreen
import com.example.personalfinanceapp.feature.expense.ui.ExpenseEditorDialog
import com.example.personalfinanceapp.feature.expense.ui.ExpenseViewModel

@Composable
fun AppNavHost(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    val loginUiState by authViewModel.loginUiState.collectAsState()
    val registerUiState by authViewModel.registerUiState.collectAsState()
    val resetUiState by authViewModel.passwordResetUiState.collectAsState()

    var showRecordExpenseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState != null) {
            navController.navigate(Overview) {
                popUpTo(Login) { inclusive = true }
            }
        } else {
            navController.navigate(Login) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Login
    ) {
        composable<Login> {
            LoginScreen(
                uiState = loginUiState,
                onEmailChanged = authViewModel::onLoginEmailChanged,
                onPasswordChanged = authViewModel::onLoginPasswordChanged,
                onLoginClicked = authViewModel::login,
                onNavigateToRegister = { navController.navigate(Register) },
                onNavigateToPasswordReset = { navController.navigate(PasswordReset) }
            )
        }

        composable<Register> {
            RegisterScreen(
                uiState = registerUiState,
                onEmailChanged = authViewModel::onRegisterEmailChanged,
                onPasswordChanged = authViewModel::onRegisterPasswordChanged,
                onConfirmPasswordChanged = authViewModel::onRegisterConfirmPasswordChanged,
                onRegisterClicked = authViewModel::register,
                onNavigateToLogin = { navController.navigate(Login) }
            )
        }

        composable<PasswordReset> {
            PasswordResetScreen(
                uiState = resetUiState,
                onEmailChanged = authViewModel::onResetEmailChanged,
                onResetClicked = authViewModel::sendPasswordReset,
                onNavigateToLogin = { navController.navigate(Login) }
            )
        }

        composable<Overview> {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val uiState by dashboardViewModel.uiState.collectAsState()

            OverviewScreen(
                uiState = uiState,
                currentNavDestination = NavDestination.OVERVIEW,
                onNavDestinationSelected = { dest ->
                    when (dest) {
                        NavDestination.OVERVIEW -> {}
                        NavDestination.ACTIVITY -> navController.navigate(Activity)
                        NavDestination.GOALS -> {}
                        NavDestination.MORE -> {}
                    }
                },
                onNavigateToActivity = { navController.navigate(Activity) }
            )
        }

        composable<Activity> {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val expenseViewModel: ExpenseViewModel = hiltViewModel()
            val uiState by dashboardViewModel.uiState.collectAsState()
            val expenseUiState by expenseViewModel.uiState.collectAsState()

            if (showRecordExpenseDialog) {
                ExpenseEditorDialog(
                    expenseToEdit = null,
                    categories = expenseUiState.categories,
                    onDismiss = { showRecordExpenseDialog = false },
                    onSave = { id, categoryId, amountDecimal, currency, channel, spendingClass, date, note ->
                        expenseViewModel.saveExpense(id, categoryId, amountDecimal, currency, channel, spendingClass, date, note)
                        showRecordExpenseDialog = false
                    }
                )
            }

            ActivityScreen(
                uiState = uiState,
                currentNavDestination = NavDestination.ACTIVITY,
                onNavDestinationSelected = { dest ->
                    when (dest) {
                        NavDestination.OVERVIEW -> navController.navigate(Overview)
                        NavDestination.ACTIVITY -> {}
                        NavDestination.GOALS -> {}
                        NavDestination.MORE -> {}
                    }
                },
                onSelectTransaction = { id ->
                    navController.navigate(TransactionDetail(id))
                },
                onRecordExpenseClicked = {
                    showRecordExpenseDialog = true
                }
            )
        }

        composable<TransactionDetail> { backStackEntry ->
            val detailRoute: TransactionDetail = backStackEntry.toRoute()

            TransactionDetailScreen(
                transactionId = detailRoute.transactionId,
                onBackClicked = { navController.popBackStack() },
                onEditClicked = { },
                onDeleteClicked = { navController.popBackStack() }
            )
        }
    }
}
