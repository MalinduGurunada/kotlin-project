package com.example.personalfinanceapp.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.personalfinanceapp.feature.auth.ui.AuthViewModel
import com.example.personalfinanceapp.feature.auth.GoogleCredentialResult
import com.example.personalfinanceapp.feature.auth.GoogleCredentialSignIn
import com.example.personalfinanceapp.feature.auth.ui.AuthNavigationState
import com.example.personalfinanceapp.feature.auth.ui.LoginScreen
import com.example.personalfinanceapp.feature.auth.ui.PasswordResetScreen
import com.example.personalfinanceapp.feature.auth.ui.RegisterScreen
import com.example.personalfinanceapp.feature.dashboard.ui.ActivityScreen
import com.example.personalfinanceapp.feature.dashboard.ui.DashboardViewModel
import com.example.personalfinanceapp.feature.dashboard.ui.NavDestination
import com.example.personalfinanceapp.feature.dashboard.ui.OverviewScreen
import com.example.personalfinanceapp.feature.dashboard.ui.GoalsScreen
import com.example.personalfinanceapp.feature.dashboard.ui.TransactionDetailScreen
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.feature.expense.ui.ExpenseEditorDialog
import com.example.personalfinanceapp.feature.expense.ui.ExpenseViewModel
import com.example.personalfinanceapp.feature.income.ui.IncomeListScreen
import com.example.personalfinanceapp.feature.income.ui.IncomeEditorScreen
import com.example.personalfinanceapp.feature.income.ui.IncomeViewModel
import com.example.personalfinanceapp.feature.income.ui.shiftIncomeMonth
import com.example.personalfinanceapp.feature.receivable.ui.ReceivableScreen
import com.example.personalfinanceapp.feature.receivable.ui.ReceivableViewModel
import com.example.personalfinanceapp.feature.notification.FinanceReviewNavigationDecision
import com.example.personalfinanceapp.feature.notification.FinanceReviewNavigationResolver
import com.example.personalfinanceapp.feature.notification.FinanceReviewPayload
import kotlinx.coroutines.launch

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun NavHostController.returnToSignIn() {
    if (!popBackStack()) {
        navigate(Login) { launchSingleTop = true }
    }
}

@Composable
internal fun HomeActionsScreen(
    onManageIncome: () -> Unit,
    onManageReceivables: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = onManageIncome) {
            Text("Manage Income")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onManageReceivables) {
            Text("Manage Receivables")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onSignOut) {
            Text("Sign Out")
        }
    }
}

@Composable
fun AppNavHost(
    authViewModel: AuthViewModel = hiltViewModel(),
    notificationTarget: FinanceReviewPayload? = null,
    onNotificationTargetConsumed: () -> Unit = {},
    onAuthenticated: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navigationState by authViewModel.navigationState.collectAsState()
    val loginUiState by authViewModel.loginUiState.collectAsState()
    val registerUiState by authViewModel.registerUiState.collectAsState()
    val resetUiState by authViewModel.passwordResetUiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showRecordExpenseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(navigationState, notificationTarget) {
        when (navigationState) {
            AuthNavigationState.Resolving -> Unit
            AuthNavigationState.Unauthenticated -> {
                navController.navigate(Login) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
            AuthNavigationState.RegistrationSetup -> {
                navController.navigate(Register) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
            is AuthNavigationState.Authenticated -> {
                onAuthenticated()
                val authenticatedUserId =
                    (navigationState as AuthNavigationState.Authenticated).userId
                val navigateToFinanceActivity = notificationTarget?.let { target ->
                    when (
                        FinanceReviewNavigationResolver.resolve(target, authenticatedUserId)
                    ) {
                        FinanceReviewNavigationDecision.NAVIGATE_TO_ACTIVITY -> true
                        FinanceReviewNavigationDecision.IGNORE -> false
                        FinanceReviewNavigationDecision.WAIT_FOR_AUTH -> false
                    }
                } ?: false
                if (notificationTarget != null) onNotificationTargetConsumed()
                if (navigateToFinanceActivity) {
                    navController.navigate(ActivityRoute) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate(Overview) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
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
                onGoogleSignInClicked = {
                    val requestId = authViewModel.beginGoogleSignIn()
                        ?: return@LoginScreen
                    val activity = context.findActivity()
                    if (activity == null) {
                        authViewModel.failGoogleSignIn(
                            requestId,
                            "Google sign-in is unavailable in this context."
                        )
                    } else {
                        coroutineScope.launch {
                            when (val result = GoogleCredentialSignIn.requestIdToken(activity)) {
                                is GoogleCredentialResult.Success ->
                                    authViewModel.completeGoogleSignIn(requestId, result.idToken)
                                GoogleCredentialResult.Cancelled ->
                                    authViewModel.cancelGoogleSignIn(requestId)
                                is GoogleCredentialResult.Failure ->
                                    authViewModel.failGoogleSignIn(requestId, result.message)
                            }
                        }
                    }
                },
                onNavigateToRegister = { navController.navigate(Register) },
                onNavigateToPasswordReset = { navController.navigate(PasswordReset) }
            )
        }

        composable<Register> {
            RegisterScreen(
                uiState = registerUiState,
                onFullNameChanged = authViewModel::onRegisterFullNameChanged,
                onEmailChanged = authViewModel::onRegisterEmailChanged,
                onPasswordChanged = authViewModel::onRegisterPasswordChanged,
                onConfirmPasswordChanged = authViewModel::onRegisterConfirmPasswordChanged,
                onRegisterClicked = authViewModel::register,
                onNavigateToLogin = {
                    if (registerUiState.isProfileUpdatePending) {
                        authViewModel.abandonPendingRegistration()
                    }
                    navController.returnToSignIn()
                }
            )
        }

        composable<PasswordReset> {
            PasswordResetScreen(
                uiState = resetUiState,
                onEmailChanged = authViewModel::onResetEmailChanged,
                onResetClicked = authViewModel::sendPasswordReset,
                onNavigateToLogin = {
                    navController.returnToSignIn()
                }
            )
        }

        composable<Home> {
            HomeActionsScreen(
                onManageIncome = { navController.navigate(IncomeList) },
                onManageReceivables = { navController.navigate(ReceivableList) },
                onSignOut = authViewModel::signOut
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
                        NavDestination.ACTIVITY -> navController.navigate(ActivityRoute)
                        NavDestination.GOALS -> navController.navigate(Goals)
                        NavDestination.MORE -> navController.navigate(Home)
                    }
                },
                onNavigateToActivity = { navController.navigate(ActivityRoute) },
                onNavigateToIncome = { navController.navigate(IncomeList) }
            )
        }

        composable<ActivityRoute> {
            val expenseViewModel: ExpenseViewModel = hiltViewModel()
            val expenseUiState by expenseViewModel.uiState.collectAsState()
            val expenseSaveUiState by expenseViewModel.saveUiState.collectAsState()
            var editingExpense by remember { mutableStateOf<Expense?>(null) }

            LaunchedEffect(expenseSaveUiState.savedExpenseId) {
                if (expenseSaveUiState.savedExpenseId != null) {
                    showRecordExpenseDialog = false
                    editingExpense = null
                    expenseViewModel.consumeSaveResult()
                }
            }

            if (showRecordExpenseDialog) {
                ExpenseEditorDialog(
                    expenseToEdit = editingExpense,
                    categories = expenseUiState.categories,
                    errorMessage = expenseSaveUiState.errorMessage,
                    isSaving = expenseSaveUiState.isSaving,
                    onDismiss = {
                        showRecordExpenseDialog = false
                        editingExpense = null
                        expenseViewModel.consumeSaveResult()
                        expenseViewModel.clearError()
                    },
                    onSave = { id, categoryId, amountDecimal, currency, channel, spendingClass, date, note ->
                        expenseViewModel.saveExpense(
                            id = id,
                            categoryId = categoryId,
                            amountDecimal = amountDecimal,
                            currency = currency,
                            paymentChannel = channel,
                            spendingClass = spendingClass,
                            occurredDate = date,
                            note = note,
                            existingExpense = editingExpense
                        )
                    }
                )
            }

            ActivityScreen(
                expenseUiState = expenseUiState,
                currentNavDestination = NavDestination.ACTIVITY,
                onNavDestinationSelected = { dest ->
                    when (dest) {
                        NavDestination.OVERVIEW -> navController.navigate(Overview)
                        NavDestination.ACTIVITY -> {}
                        NavDestination.GOALS -> navController.navigate(Goals)
                        NavDestination.MORE -> navController.navigate(Home)
                    }
                },
                onRecordExpenseClicked = {
                    expenseViewModel.consumeSaveResult()
                    editingExpense = null
                    showRecordExpenseDialog = true
                },
                onExpenseClicked = { expense ->
                    expenseViewModel.consumeSaveResult()
                    editingExpense = expense
                    showRecordExpenseDialog = true
                },
                onNavigateToIncome = {
                    navController.navigate(IncomeList)
                }
            )
        }

        composable<Goals> {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val uiState by dashboardViewModel.uiState.collectAsState()

            GoalsScreen(
                uiState = uiState,
                onNavDestinationSelected = { destination ->
                    when (destination) {
                        NavDestination.OVERVIEW -> navController.navigate(Overview)
                        NavDestination.ACTIVITY -> navController.navigate(ActivityRoute)
                        NavDestination.GOALS -> Unit
                        NavDestination.MORE -> navController.navigate(Home)
                    }
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

        composable<IncomeList> {
            val incomeViewModel: IncomeViewModel = hiltViewModel()
            val incomeUiState by incomeViewModel.uiState.collectAsState()
            val incomeFormState by incomeViewModel.formState.collectAsState()
            val incomeUserMessage by incomeViewModel.userMessage.collectAsState()
            val incomeDeleteState by incomeViewModel.deleteState.collectAsState()
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
                    onCurrencyChanged = incomeViewModel::updateIncomeCurrency,
                    onRateModeChanged = incomeViewModel::updateRateMode,
                    onManualRateChanged = incomeViewModel::updateManualRate,
                    onManualRateSourceChanged = incomeViewModel::updateManualRateSource,
                    onManualRateDateChanged = incomeViewModel::updateManualRateDate,
                    onRefreshAutomaticRate = incomeViewModel::refreshAutomaticRate,
                    onProjectReferenceChanged = incomeViewModel::updateProjectReference,
                    onReceivableChanged = incomeViewModel::updateReceivableId,
                    onAssetSymbolChanged = incomeViewModel::updateAssetSymbol,
                    onPnlDirectionChanged = incomeViewModel::updatePnlDirection,
                    onNoteChanged = incomeViewModel::updateIncomeNote,
                    onSave = incomeViewModel::saveIncome
                )
            } else {
                IncomeListScreen(
                    uiState = incomeUiState,
                    deleteUiState = incomeDeleteState,
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
                    onDeleteIncome = incomeViewModel::requestIncomeDeletion,
                    onCancelDelete = incomeViewModel::cancelIncomeDeletion,
                    onConfirmDelete = incomeViewModel::confirmIncomeDeletion,
                    userMessage = incomeUserMessage,
                    onUserMessageShown = incomeViewModel::clearUserMessage,
                    canNavigateToPreviousMonth = previousMonth != null,
                    canNavigateToNextMonth = nextMonth != null
                )
            }
        }

        composable<ReceivableList> {
            val viewModel: ReceivableViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val formState by viewModel.formState.collectAsState()
            ReceivableScreen(
                uiState = uiState,
                formState = formState,
                onBack = { navController.popBackStack() },
                onRetry = viewModel::retry,
                onCreate = viewModel::openCreate,
                onEdit = viewModel::openEdit,
                onArchive = viewModel::setArchived,
                onCloseForm = viewModel::closeForm,
                onTitleChanged = viewModel::updateTitle,
                onProjectChanged = viewModel::updateProject,
                onAmountChanged = viewModel::updateAmount,
                onCurrencyChanged = viewModel::updateCurrency,
                onDateChanged = viewModel::updateDate,
                onSave = viewModel::save
            )
        }
    }
}
