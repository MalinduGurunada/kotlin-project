package com.example.personalfinanceapp.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Goal
import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.example.personalfinanceapp.feature.category.CategoryRepository
import com.example.personalfinanceapp.feature.category.spendingClassesById
import com.example.personalfinanceapp.feature.category.withEffectiveSpendingClass
import com.example.personalfinanceapp.feature.dashboard.analytics.IncomeSpendingBreakdownCalculator
import com.example.personalfinanceapp.feature.dashboard.analytics.MonthlyAnalyticsBreakdown
import com.example.personalfinanceapp.feature.expense.ExpenseRepository
import com.example.personalfinanceapp.feature.goal.GoalRepository
import com.example.personalfinanceapp.feature.income.IncomeRepository
import com.example.personalfinanceapp.feature.income.CryptoResultCalculator
import com.example.personalfinanceapp.feature.income.CryptoResultTotals
import com.example.personalfinanceapp.feature.income.signedBaseAmountMinor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.math.BigInteger
import java.time.LocalDate
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class TimelineActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amountText: String,
    val isRedAmount: Boolean,
    val occurredDate: LocalDate,
    val isIncome: Boolean
)

data class ActiveSavingsGoalUiState(
    val id: String,
    val name: String,
    val targetAmountLkrMinor: Long,
    val currentSavedLkrMinor: Long?,
    val remainingLkrMinor: Long?,
    val progressPercent: Int?,
    val targetDateText: String
)

private data class DashboardInputs(
    val incomes: List<Income>,
    val yearToDateIncomes: List<Income>,
    val expenses: List<Expense>,
    val goals: List<Goal>,
    val activeGoalContributions: List<Contribution>?
)

data class DashboardUiState(
    val userName: String = "User",
    val monthlyNetCashFlowLkrMinor: Long = 0L,
    val totalIncomeLkrMinor: Long = 0L,
    val incomeCount: Int = 0,
    val monthlyCryptoResult: CryptoResultTotals = CryptoResultTotals(),
    val yearToDateCryptoResult: CryptoResultTotals = CryptoResultTotals(),
    val totalExpenseLkrMinor: Long = 0L,
    val expenseCount: Int = 0,
    val committedExpenseLkrMinor: Long = 0L,
    val committedPercent: Int = 0,
    val discretionaryExpenseLkrMinor: Long = 0L,
    val discretionaryPercent: Int = 0,
    val activeGoal: ActiveSavingsGoalUiState? = null,
    val recentActivity: List<TimelineActivityItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

enum class FinancialSummaryPeriod {
    MONTH,
    QUARTER
}

data class FinancialTrendPoint(
    val label: String,
    val incomeLkrMinor: Long,
    val expenseLkrMinor: Long,
    val netCashFlowLkrMinor: Long
)

data class FinancialSummaryUiState(
    val selectedPeriod: FinancialSummaryPeriod = FinancialSummaryPeriod.MONTH,
    val periodLabel: String = "",
    val comparisonLabel: String = "previous month",
    val totalIncomeLkrMinor: Long = 0L,
    val totalExpenseLkrMinor: Long = 0L,
    val netCashFlowLkrMinor: Long = 0L,
    val netCashFlowMarginPercent: BigInteger? = null,
    val analytics: MonthlyAnalyticsBreakdown = MonthlyAnalyticsBreakdown(),
    val trendPoints: List<FinancialTrendPoint> = emptyList(),
    val hasPreviousPeriodExpenses: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

internal data class FinancialSummaryBounds(
    val startDate: LocalDate,
    val endDateExclusive: LocalDate,
    val previousStartDate: LocalDate,
    val label: String,
    val comparisonLabel: String
)

internal fun financialSummaryLocalDate(
    clock: Clock,
    zoneId: ZoneId = ZoneId.systemDefault()
): LocalDate = LocalDate.now(clock.withZone(zoneId))

internal fun calculateNetCashFlowMarginPercent(
    totalIncomeMinor: Long,
    netCashFlowMinor: Long
): BigInteger? = if (totalIncomeMinor > 0L) {
    BigInteger.valueOf(netCashFlowMinor)
        .multiply(BigInteger.valueOf(100L))
        .divide(BigInteger.valueOf(totalIncomeMinor))
} else {
    null
}

internal fun financialSummaryBounds(
    today: LocalDate,
    period: FinancialSummaryPeriod
): FinancialSummaryBounds {
    val startDate = when (period) {
        FinancialSummaryPeriod.MONTH -> today.withDayOfMonth(1)
        FinancialSummaryPeriod.QUARTER -> {
            val firstMonth = ((today.monthValue - 1) / 3) * 3 + 1
            LocalDate.of(today.year, firstMonth, 1)
        }
    }
    val months = if (period == FinancialSummaryPeriod.MONTH) 1L else 3L
    val endDateExclusive = startDate.plusMonths(months)
    val label = when (period) {
        FinancialSummaryPeriod.MONTH -> startDate.format(
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
        )
        FinancialSummaryPeriod.QUARTER ->
            "Q${((startDate.monthValue - 1) / 3) + 1} ${startDate.year}"
    }
    return FinancialSummaryBounds(
        startDate = startDate,
        endDateExclusive = endDateExclusive,
        previousStartDate = startDate.minusMonths(months),
        label = label,
        comparisonLabel = if (period == FinancialSummaryPeriod.MONTH) {
            "previous month"
        } else {
            "previous quarter"
        }
    )
}

internal fun calculateFinancialTrend(
    period: FinancialSummaryPeriod,
    bounds: FinancialSummaryBounds,
    incomes: List<Income>,
    expenses: List<Expense>
): List<FinancialTrendPoint> {
    val windows = buildList {
        if (period == FinancialSummaryPeriod.MONTH) {
            var start = bounds.startDate
            while (start.isBefore(bounds.endDateExclusive)) {
                val end = minOf(start.plusDays(7), bounds.endDateExclusive)
                add(Triple(start, end, "${start.dayOfMonth}-${end.minusDays(1).dayOfMonth}"))
                start = end
            }
        } else {
            var start = bounds.startDate
            while (start.isBefore(bounds.endDateExclusive)) {
                val end = start.plusMonths(1)
                add(Triple(start, end, start.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }))
                start = end
            }
        }
    }

    return windows.map { (start, end, label) ->
        val incomeMinor = incomes.asSequence()
            .filter { !it.occurredDate.isBefore(start) && it.occurredDate.isBefore(end) }
            .fold(0L) { total, income -> Math.addExact(total, income.signedBaseAmountMinor()) }
        val expenseMinor = expenses.asSequence()
            .filter { !it.occurredDate.isBefore(start) && it.occurredDate.isBefore(end) }
            .fold(0L) { total, expense -> Math.addExact(total, expense.money.baseAmountMinor) }
        FinancialTrendPoint(
            label = label,
            incomeLkrMinor = incomeMinor,
            expenseLkrMinor = expenseMinor,
            netCashFlowLkrMinor = Math.subtractExact(incomeMinor, expenseMinor)
        )
    }
}

internal data class GoalProgressAmounts(
    val savedMinor: Long,
    val remainingMinor: Long,
    val progressPercent: Int
)

internal fun calculateGoalProgressAmounts(
    goal: Goal,
    contributions: List<Contribution>
): GoalProgressAmounts? = try {
    val savedMinor = contributions.fold(0L) { total, contribution ->
        Math.addExact(total, contribution.amountMinor)
    }
    val remainingMinor = Math.subtractExact(goal.targetAmountMinor, savedMinor)
        .coerceAtLeast(0L)
    val nonNegativeSaved = savedMinor.coerceAtLeast(0L)
    val percent = BigInteger.valueOf(nonNegativeSaved)
        .multiply(BigInteger.valueOf(100L))
        .divide(BigInteger.valueOf(goal.targetAmountMinor))
        .min(BigInteger.valueOf(100L))
        .toInt()
    GoalProgressAmounts(savedMinor, remainingMinor, percent)
} catch (_: ArithmeticException) {
    null
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val incomeRepository: IncomeRepository,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val goalRepository: GoalRepository,
    private val clock: Clock = Clock.systemUTC()
) : ViewModel() {

    private val now = financialSummaryLocalDate(clock)
    private val startDate = now.withDayOfMonth(1)
    private val endDateExclusive = now.withDayOfMonth(1).plusMonths(1)
    private val startOfYear = now.withDayOfYear(1)
    private val ytdEndDateExclusive = now.plusDays(1)
    private val financialSummaryToday = financialSummaryLocalDate(clock)
    private val financialSummaryPeriod = MutableStateFlow(FinancialSummaryPeriod.MONTH)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = authRepository.observeAuthState()
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(DashboardUiState(isLoading = false))
            } else {
                combine(
                    incomeRepository.observeIncomes(startDate, endDateExclusive),
                    incomeRepository.observeIncomes(startOfYear, ytdEndDateExclusive),
                    expenseRepository.observeExpenses(startDate, endDateExclusive),
                    goalRepository.observeGoals(),
                    categoryRepository.observeCategories()
                ) { incomes, yearToDateIncomes, expenses, goals, categories ->
                    val categorySpendingClasses = categories.spendingClassesById()
                    val effectiveExpenses = expenses.map { expense ->
                        expense.withEffectiveSpendingClass(categorySpendingClasses)
                    }
                    DashboardInputs(
                        incomes,
                        yearToDateIncomes,
                        effectiveExpenses,
                        goals,
                        emptyList()
                    )
                }.flatMapLatest { inputs ->
                    val goals = inputs.goals
                    val activeGoal = goals.firstOrNull { !it.archived }
                    if (activeGoal == null) {
                        flowOf(inputs)
                    } else {
                        goalRepository.observeContributions(activeGoal.id)
                            .map<List<Contribution>, List<Contribution>?> { it }
                            .catch { emit(null) }
                            .map { contributions ->
                                inputs.copy(activeGoalContributions = contributions)
                            }
                    }
                }.map { inputs ->
                    calculateDashboardState(
                        incomes = inputs.incomes,
                        yearToDateIncomes = inputs.yearToDateIncomes,
                        expenses = inputs.expenses,
                        goals = inputs.goals,
                        activeGoalContributions = inputs.activeGoalContributions
                    )
                }.onStart {
                    emit(DashboardUiState(isLoading = true))
                }.catch { exception ->
                    emit(
                        DashboardUiState(
                            isLoading = false,
                            errorMessage = if (exception is ArithmeticException) {
                                "Dashboard totals are too large to calculate safely"
                            } else {
                                exception.localizedMessage
                                    ?: "Dashboard data is currently unavailable"
                            }
                        )
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 0,
                replayExpirationMillis = 0
            ),
            initialValue = DashboardUiState(isLoading = true)
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val financialSummaryState: StateFlow<FinancialSummaryUiState> = combine(
        authRepository.observeAuthState(),
        financialSummaryPeriod
    ) { userId, period -> userId to period }
        .flatMapLatest { (userId, period) ->
            if (userId == null) {
                flowOf(FinancialSummaryUiState(selectedPeriod = period))
            } else {
                val bounds = financialSummaryBounds(financialSummaryToday, period)
                combine(
                    incomeRepository.observeIncomes(bounds.startDate, bounds.endDateExclusive),
                    expenseRepository.observeExpenses(bounds.startDate, bounds.endDateExclusive),
                    expenseRepository.observeExpenses(bounds.previousStartDate, bounds.startDate),
                    categoryRepository.observeCategories()
                ) { incomes, expenses, previousExpenses, categories ->
                    val categoryNames = categories.associate { it.id to it.name }
                    val categorySpendingClasses = categories.spendingClassesById()
                    val analytics = IncomeSpendingBreakdownCalculator.calculateBreakdown(
                        currentMonthIncomes = incomes,
                        currentMonthExpenses = expenses,
                        previousMonthExpenses = previousExpenses,
                        yearToDateCryptoIncomes = incomes.filter { it.source == IncomeSource.CRYPTO },
                        categoryNamesById = categoryNames,
                        categorySpendingClassesById = categorySpendingClasses
                    )
                    val totalIncomeMinor = incomes.fold(0L) { total, income ->
                        Math.addExact(total, income.signedBaseAmountMinor())
                    }
                    val totalExpenseMinor = expenses.fold(0L) { total, expense ->
                        Math.addExact(total, expense.money.baseAmountMinor)
                    }
                    val netCashFlowMinor = Math.subtractExact(totalIncomeMinor, totalExpenseMinor)
                    val netCashFlowMargin = calculateNetCashFlowMarginPercent(
                        totalIncomeMinor = totalIncomeMinor,
                        netCashFlowMinor = netCashFlowMinor
                    )
                    FinancialSummaryUiState(
                        selectedPeriod = period,
                        periodLabel = bounds.label,
                        comparisonLabel = bounds.comparisonLabel,
                        totalIncomeLkrMinor = totalIncomeMinor,
                        totalExpenseLkrMinor = totalExpenseMinor,
                        netCashFlowLkrMinor = netCashFlowMinor,
                        netCashFlowMarginPercent = netCashFlowMargin,
                        analytics = analytics,
                        trendPoints = calculateFinancialTrend(period, bounds, incomes, expenses),
                        hasPreviousPeriodExpenses = previousExpenses.isNotEmpty(),
                        isLoading = false
                    )
                }.onStart {
                    emit(
                        FinancialSummaryUiState(
                            selectedPeriod = period,
                            periodLabel = bounds.label,
                            comparisonLabel = bounds.comparisonLabel,
                            isLoading = true
                        )
                    )
                }.catch { exception ->
                    emit(
                        FinancialSummaryUiState(
                            selectedPeriod = period,
                            periodLabel = bounds.label,
                            comparisonLabel = bounds.comparisonLabel,
                            errorMessage = exception.localizedMessage
                                ?: "Financial summary data is currently unavailable"
                        )
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 0,
                replayExpirationMillis = 0
            ),
            initialValue = FinancialSummaryUiState(isLoading = true)
        )

    fun selectFinancialSummaryPeriod(period: FinancialSummaryPeriod) {
        financialSummaryPeriod.value = period
    }

    private fun calculateDashboardState(
        incomes: List<Income>,
        yearToDateIncomes: List<Income>,
        expenses: List<Expense>,
        goals: List<Goal>,
        activeGoalContributions: List<Contribution>?
    ): DashboardUiState {
        val netIncomeMinor = incomes.fold(0L) { total, income ->
            Math.addExact(total, income.signedBaseAmountMinor())
        }
        val monthlyCryptoResult = CryptoResultCalculator.calculate(
            incomes = incomes,
            startDateInclusive = startDate,
            endDateExclusive = endDateExclusive
        )
        val yearToDateCryptoResult = CryptoResultCalculator.calculate(
            incomes = yearToDateIncomes,
            startDateInclusive = startOfYear,
            endDateExclusive = ytdEndDateExclusive
        )

        val totalExpenseMinor = expenses.fold(0L) { total, expense ->
            Math.addExact(total, expense.money.baseAmountMinor)
        }
        val netCashFlowMinor = Math.subtractExact(netIncomeMinor, totalExpenseMinor)

        val committedMinor = expenses
            .filter { it.spendingClass == SpendingClass.COMMITTED }
            .fold(0L) { total, expense ->
                Math.addExact(total, expense.money.baseAmountMinor)
            }

        val discretionaryMinor = expenses
            .filter { it.spendingClass == SpendingClass.DISCRETIONARY }
            .fold(0L) { total, expense ->
                Math.addExact(total, expense.money.baseAmountMinor)
            }

        val totalOutflow = Math.addExact(committedMinor, discretionaryMinor)
        val committedPct = if (totalOutflow > 0L) {
            BigInteger.valueOf(committedMinor)
                .multiply(BigInteger.valueOf(100L))
                .divide(BigInteger.valueOf(totalOutflow))
                .toInt()
        } else 0
        val discretionaryPct = if (totalOutflow > 0) 100 - committedPct else 0

        val primaryGoal = goals.firstOrNull { !it.archived }?.let { goal ->
            val progress = activeGoalContributions?.let {
                calculateGoalProgressAmounts(goal, it)
            }

            ActiveSavingsGoalUiState(
                id = goal.id,
                name = goal.name,
                targetAmountLkrMinor = goal.targetAmountMinor,
                currentSavedLkrMinor = progress?.savedMinor,
                remainingLkrMinor = progress?.remainingMinor,
                progressPercent = progress?.progressPercent,
                targetDateText = "${goal.targetDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${goal.targetDate.year}"
            )
        }

        val timelineItems = mutableListOf<TimelineActivityItem>()

        expenses.forEach { exp ->
            val lkrDecimal = (exp.money.baseAmountMinor.toDouble() / 100.0)
            timelineItems.add(
                TimelineActivityItem(
                    id = exp.id,
                    title = exp.categoryNameSnapshot.ifBlank { "Expense" },
                    subtitle = "${exp.paymentChannel.name.lowercase().replaceFirstChar { it.uppercase() }} • ${exp.occurredDate}",
                    amountText = "-LKR ${String.format(Locale.US, "%,.2f", lkrDecimal)}",
                    isRedAmount = true,
                    occurredDate = exp.occurredDate,
                    isIncome = false
                )
            )
        }

        incomes.forEach { inc ->
            val isLoss = inc.entryKind == IncomeEntryKind.REALIZED_PNL && inc.pnlDirection == PnlDirection.LOSS
            val isBreakEven = inc.entryKind == IncomeEntryKind.REALIZED_PNL && inc.pnlDirection == PnlDirection.BREAK_EVEN
            val lkrDecimal = (inc.money.baseAmountMinor.toDouble() / 100.0)
            val title = if (inc.source == IncomeSource.CRYPTO) {
                when {
                    isLoss -> "Crypto Loss"
                    isBreakEven -> "Crypto Break Even"
                    else -> "Crypto Gain"
                }
            } else {
                "${inc.source.name.lowercase().replaceFirstChar { it.uppercase() }} Receipt"
            }
            timelineItems.add(
                TimelineActivityItem(
                    id = inc.id,
                    title = title,
                    subtitle = "${inc.occurredDate} • ${if (isLoss) "Loss" else "Inbound"}",
                    amountText = if (isLoss) {
                        "-LKR ${String.format(Locale.US, "%,.2f", lkrDecimal)}"
                    } else if (isBreakEven) {
                        "LKR 0.00"
                    } else {
                        "+LKR ${String.format(Locale.US, "%,.2f", lkrDecimal)}"
                    },
                    isRedAmount = isLoss,
                    occurredDate = inc.occurredDate,
                    isIncome = !isLoss && !isBreakEven
                )
            )
        }

        val sortedTimeline = timelineItems.sortedByDescending { it.occurredDate }

        val userSnapshot = authRepository.currentUserSnapshot()
        val displayName = userSnapshot?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User"

        return DashboardUiState(
            userName = displayName,
            monthlyNetCashFlowLkrMinor = netCashFlowMinor,
            totalIncomeLkrMinor = netIncomeMinor,
            incomeCount = incomes.size,
            monthlyCryptoResult = monthlyCryptoResult,
            yearToDateCryptoResult = yearToDateCryptoResult,
            totalExpenseLkrMinor = totalExpenseMinor,
            expenseCount = expenses.size,
            committedExpenseLkrMinor = committedMinor,
            committedPercent = committedPct,
            discretionaryExpenseLkrMinor = discretionaryMinor,
            discretionaryPercent = discretionaryPct,
            activeGoal = primaryGoal,
            recentActivity = sortedTimeline,
            isLoading = false
        )
    }
}
