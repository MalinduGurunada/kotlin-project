package com.example.personalfinanceapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.feature.auth.ui.AuthNavigationState
import com.example.personalfinanceapp.feature.auth.ui.AuthViewModel
import com.example.personalfinanceapp.feature.auth.ui.LoginScreen
import com.example.personalfinanceapp.feature.auth.ui.PasswordResetScreen
import com.example.personalfinanceapp.feature.auth.ui.RegisterScreen
import com.example.personalfinanceapp.feature.category.ui.CategoryScreen
import com.example.personalfinanceapp.feature.dashboard.ui.ActivityScreen
import com.example.personalfinanceapp.feature.dashboard.ui.DashboardViewModel
import com.example.personalfinanceapp.feature.dashboard.ui.GoalsScreen
import com.example.personalfinanceapp.feature.dashboard.ui.MoreScreen
import com.example.personalfinanceapp.feature.dashboard.ui.NavDestination
import com.example.personalfinanceapp.feature.dashboard.ui.OverviewScreen
import com.example.personalfinanceapp.feature.dashboard.ui.TransactionDetailScreen
import com.example.personalfinanceapp.feature.expense.ui.ExpenseEditorDialog
import com.example.personalfinanceapp.feature.expense.ui.ExpenseListScreen
import com.example.personalfinanceapp.feature.expense.ui.ExpenseViewModel
import com.example.personalfinanceapp.feature.income.ui.IncomeListScreen
import com.example.personalfinanceapp.feature.income.ui.IncomeEditorScreen
import com.example.personalfinanceapp.feature.income.ui.IncomeViewModel
import com.example.personalfinanceapp.feature.income.ui.shiftIncomeMonth
import com.example.personalfinanceapp.feature.receivable.ui.ReceivableScreen
import com.example.personalfinanceapp.feature.receivable.ui.ReceivableViewModel
import com.example.personalfinanceapp.feature.recurring.ui.DueItemsScreen
import com.example.personalfinanceapp.feature.recurring.ui.RecurringCommitmentsScreen

internal fun NavHostController.returnToSignIn() {
    if (!popBackStack()) {
        navigate(Login) { launchSingleTop = true }
    }
}

internal enum class AppNavigationTarget {
    LOGIN,
    REGISTRATION_SETUP,
    OVERVIEW,
    ACTIVITY,
    GOALS,
    MORE,
    EXPENSES,
    CATEGORIES,
    INCOME,
    RECEIVABLES,
    RECURRING,
    DUE_ITEMS
}

internal enum class ManagementDestination {
    EXPENSES,
    CATEGORIES,
    INCOME,
    RECEIVABLES,
    RECURRING,
    DUE_ITEMS
}

internal fun startupTarget(state: AuthNavigationState): AppNavigationTarget? = when (state) {
    AuthNavigationState.Resolving -> null
    AuthNavigationState.Unauthenticated -> AppNavigationTarget.LOGIN
    AuthNavigationState.RegistrationSetup -> AppNavigationTarget.REGISTRATION_SETUP
    is AuthNavigationState.Authenticated -> AppNavigationTarget.OVERVIEW
}

internal fun dashboardTarget(destination: NavDestination): AppNavigationTarget = when (destination) {
    NavDestination.OVERVIEW -> AppNavigationTarget.OVERVIEW
    NavDestination.ACTIVITY -> AppNavigationTarget.ACTIVITY
    NavDestination.GOALS -> AppNavigationTarget.GOALS
    NavDestination.MORE -> AppNavigationTarget.MORE
}

internal fun managementTarget(destination: ManagementDestination): AppNavigationTarget =
    when (destination) {
        ManagementDestination.EXPENSES -> AppNavigationTarget.EXPENSES
        ManagementDestination.CATEGORIES -> AppNavigationTarget.CATEGORIES
        ManagementDestination.INCOME -> AppNavigationTarget.INCOME
        ManagementDestination.RECEIVABLES -> AppNavigationTarget.RECEIVABLES
        ManagementDestination.RECURRING -> AppNavigationTarget.RECURRING
        ManagementDestination.DUE_ITEMS -> AppNavigationTarget.DUE_ITEMS
    }

private fun NavOptionsBuilder.replaceBackStackIfNeeded(replaceBackStack: Boolean) {
    if (replaceBackStack) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateTo(
    target: AppNavigationTarget,
    replaceBackStack: Boolean = false
) {
    when (target) {
        AppNavigationTarget.LOGIN -> navigate(Login) {
            replaceBackStackIfNeeded(replaceBackStack)
        }
        AppNavigationTarget.REGISTRATION_SETUP -> navigate(Register) {
            replaceBackStackIfNeeded(replaceBackStack)
        }
        AppNavigationTarget.OVERVIEW -> navigate(Overview) {
            replaceBackStackIfNeeded(replaceBackStack)
        }
        AppNavigationTarget.ACTIVITY -> navigate(Activity) {
            replaceBackStackIfNeeded(replaceBackStack)
        }
        AppNavigationTarget.GOALS -> navigate(Goals) {
            replaceBackStackIfNeeded(replaceBackStack)
        }
        AppNavigationTarget.MORE -> navigate(Home) {
            replaceBackStackIfNeeded(replaceBackStack)
        }
        AppNavigationTarget.EXPENSES -> navigate(ExpenseList) {
            replaceBackStackIfNeeded(replaceBackStack)
        }
        AppNavigationTarget.CATEGORIES -> navigate(Category) {
            replaceBackStackIfNeeded(replaceBackStack)
        }
        AppNavigationTarget.INCOME -> navigate(IncomeList) {
            replaceBackStackIfNeeded(replaceBackStack)
        }
        AppNavigationTarget.RECEIVABLES -> navigate(ReceivableList) {
            replaceBackStackIfNeeded(replaceBackStack)
        }
        AppNavigationTarget.RECURRING -> navigate(RecurringCommitments) {
            replaceBackStackIfNeeded(replaceBackStack)
        }
        AppNavigationTarget.DUE_ITEMS -> navigate(DueItems) {
            replaceBackStackIfNeeded(replaceBackStack)
        }
    }
}

@Composable
fun AppNavHost(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navigationState by authViewModel.navigationState.collectAsState()
    val loginUiState by authViewModel.loginUiState.collectAsState()
    val registerUiState by authViewModel.registerUiState.collectAsState()
    val resetUiState by authViewModel.passwordResetUiState.collectAsState()

    var showRecordExpenseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(navigationState) {
        startupTarget(navigationState)?.let { target ->
            navController.navigateTo(target, replaceBackStack = true)
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
                onNavigateToLogin = { navController.returnToSignIn() }
            )
        }

        composable<Home> {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val uiState by dashboardViewModel.uiState.collectAsState()

            MoreScreen(
                userName = uiState.userName,
                onCategoriesClick = {
                    navController.navigateTo(
                        managementTarget(ManagementDestination.CATEGORIES)
                    )
                },
                onExpensesClick = {
                    navController.navigateTo(
                        managementTarget(ManagementDestination.EXPENSES)
                    )
                },
                onIncomeClick = {
                    navController.navigateTo(
                        managementTarget(ManagementDestination.INCOME)
                    )
                },
                onReceivablesClick = {
                    navController.navigateTo(
                        managementTarget(ManagementDestination.RECEIVABLES)
                    )
                },
                onRecurringClick = {
                    navController.navigateTo(
                        managementTarget(ManagementDestination.RECURRING)
                    )
                },
                onDueItemsClick = {
                    navController.navigateTo(
                        managementTarget(ManagementDestination.DUE_ITEMS)
                    )
                },
                onSignOut = authViewModel::signOut,
                onBottomDestinationSelected = { destination ->
                    if (destination != NavDestination.MORE) {
                        navController.navigateTo(dashboardTarget(destination))
                    }
                }
            )
        }

        composable<Overview> {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val uiState by dashboardViewModel.uiState.collectAsState()

            OverviewScreen(
                uiState = uiState,
                currentNavDestination = NavDestination.OVERVIEW,
                onNavDestinationSelected = { destination ->
                    if (destination != NavDestination.OVERVIEW) {
                        navController.navigateTo(dashboardTarget(destination))
                    }
                },
                onNavigateToActivity = {
                    navController.navigateTo(AppNavigationTarget.ACTIVITY)
                },
                onNavigateToIncome = {
                    navController.navigateTo(AppNavigationTarget.INCOME)
                }
            )
        }

        composable<Activity> {
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
                    onSave = {
                            id,
                            categoryId,
                            amountDecimal,
                            currency,
                            channel,
                            spendingClass,
                            date,
                            note ->
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
                onNavDestinationSelected = { destination ->
                    if (destination != NavDestination.ACTIVITY) {
                        navController.navigateTo(dashboardTarget(destination))
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
                    navController.navigateTo(AppNavigationTarget.INCOME)
                }
            )
        }

        composable<Goals> {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val uiState by dashboardViewModel.uiState.collectAsState()

            GoalsScreen(
                uiState = uiState,
                onNavDestinationSelected = { destination ->
                    if (destination != NavDestination.GOALS) {
                        navController.navigateTo(dashboardTarget(destination))
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

        composable<Category> {
            CategoryScreen(
                onBack = { navController.popBackStack() }
            )
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

        composable<RecurringCommitments> {
            RecurringCommitmentsScreen(
                onBack = { navController.popBackStack() },
                onOpenDueItems = { navController.navigate(DueItems) }
            )
        }

        composable<DueItems> {
            DueItemsScreen(onBack = { navController.popBackStack() })
        }
    }
}
