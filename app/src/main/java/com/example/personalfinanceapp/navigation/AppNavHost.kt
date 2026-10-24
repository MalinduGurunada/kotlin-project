package com.example.personalfinanceapp.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.personalfinanceapp.feature.auth.ui.AuthViewModel
import com.example.personalfinanceapp.feature.auth.ui.LoginScreen
import com.example.personalfinanceapp.feature.auth.ui.PasswordResetScreen
import com.example.personalfinanceapp.feature.auth.ui.RegisterScreen
import com.example.personalfinanceapp.feature.expense.ui.ExpenseListScreen
import com.example.personalfinanceapp.feature.expense.ui.ExpenseViewModel
import com.example.personalfinanceapp.feature.income.ui.IncomeListScreen
import com.example.personalfinanceapp.feature.income.ui.IncomeEditorScreen
import com.example.personalfinanceapp.feature.income.ui.IncomeViewModel
import com.example.personalfinanceapp.feature.income.ui.shiftIncomeMonth

@Composable
fun AppNavHost(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    val loginUiState by authViewModel.loginUiState.collectAsState()
    val registerUiState by authViewModel.registerUiState.collectAsState()
    val resetUiState by authViewModel.passwordResetUiState.collectAsState()

    LaunchedEffect(authState) {
        if (authState != null) {
            navController.navigate(Home) {
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

        composable<Home> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Personal Finance Dashboard",
                    style = MaterialTheme.typography.headlineMedium
                )

                if (authState != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Signed in as: $authState",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        navController.navigate(ExpenseList)
                    }
                ) {
                    Text("Manage Expenses")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        navController.navigate(IncomeList)
                    }
                ) {
                    Text("Manage Income")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        authViewModel.signOut()
                    }
                ) {
                    Text("Sign Out")
                }
            }
        }

        composable<ExpenseList> {
            val expenseViewModel: ExpenseViewModel = hiltViewModel()
            val expenseUiState by expenseViewModel.uiState.collectAsState()

            ExpenseListScreen(
                uiState = expenseUiState,
                onSaveExpense = expenseViewModel::saveExpense,
                onDeleteExpense = expenseViewModel::deleteExpense,
                onClearError = expenseViewModel::clearError
            )
        }

        composable<IncomeList> {
            val incomeViewModel: IncomeViewModel = hiltViewModel()
            val incomeUiState by incomeViewModel.uiState.collectAsState()
            val incomeFormState by incomeViewModel.formState.collectAsState()
            val incomeUserMessage by incomeViewModel.userMessage.collectAsState()
            val previousMonth = shiftIncomeMonth(incomeUiState.dateRange, -1L)
            val nextMonth = shiftIncomeMonth(incomeUiState.dateRange, 1L)

            val formState = incomeFormState
            if (formState != null) {
                IncomeEditorScreen(
                    uiState = formState,
                    onBack = incomeViewModel::closeIncomeEditor,
                    onSourceChanged = incomeViewModel::updateIncomeSource,
                    onAmountChanged = incomeViewModel::updateIncomeAmount,
                    onDateChanged = incomeViewModel::updateIncomeDate,
                    onProjectReferenceChanged = incomeViewModel::updateProjectReference,
                    onAssetSymbolChanged = incomeViewModel::updateAssetSymbol,
                    onPnlDirectionChanged = incomeViewModel::updatePnlDirection,
                    onNoteChanged = incomeViewModel::updateIncomeNote,
                    onSave = incomeViewModel::saveIncome
                )
            } else {
                IncomeListScreen(
                    uiState = incomeUiState,
                    onBack = { navController.popBackStack() },
                    onRetry = incomeViewModel::retryObservation,
                    onPreviousMonth = {
                        previousMonth?.let { range ->
                            incomeViewModel.setDateRange(
                                range.startDate,
                                range.endDateExclusive
                            )
                        }
                    },
                    onNextMonth = {
                        nextMonth?.let { range ->
                            incomeViewModel.setDateRange(
                                range.startDate,
                                range.endDateExclusive
                            )
                        }
                    },
                    onAddIncome = incomeViewModel::openNewIncome,
                    onEditIncome = incomeViewModel::openIncomeForEdit,
                    userMessage = incomeUserMessage,
                    onUserMessageShown = incomeViewModel::clearUserMessage,
                    canNavigateToPreviousMonth = previousMonth != null,
                    canNavigateToNextMonth = nextMonth != null
                )
            }
        }
    }
}
