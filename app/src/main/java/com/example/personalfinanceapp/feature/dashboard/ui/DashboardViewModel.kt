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
import com.example.personalfinanceapp.feature.expense.ExpenseRepository
import com.example.personalfinanceapp.feature.goal.GoalRepository
import com.example.personalfinanceapp.feature.income.IncomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
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
    val expenses: List<Expense>,
    val goals: List<Goal>,
    val activeGoalContributions: List<Contribution>?
)

data class DashboardUiState(
    val userName: String = "User",
    val totalAvailableBalanceLkrMinor: Long = 0L,
    val totalIncomeLkrMinor: Long = 0L,
    val incomeCount: Int = 0,
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
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val now = LocalDate.now()
    private val startDate = now.withDayOfMonth(1)
    private val endDateExclusive = now.withDayOfMonth(1).plusMonths(1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = authRepository.observeAuthState()
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(DashboardUiState(isLoading = false))
            } else {
                combine(
                    incomeRepository.observeIncomes(startDate, endDateExclusive),
                    expenseRepository.observeExpenses(startDate, endDateExclusive),
                    goalRepository.observeGoals()
                ) { incomes, expenses, goals ->
                    Triple(incomes, expenses, goals)
                }.flatMapLatest { (incomes, expenses, goals) ->
                    val activeGoal = goals.firstOrNull { !it.archived }
                    if (activeGoal == null) {
                        flowOf(DashboardInputs(incomes, expenses, goals, emptyList()))
                    } else {
                        goalRepository.observeContributions(activeGoal.id)
                            .map<List<Contribution>, List<Contribution>?> { it }
                            .catch { emit(null) }
                            .map { contributions ->
                                DashboardInputs(incomes, expenses, goals, contributions)
                            }
                    }
                }.map { inputs ->
                    calculateDashboardState(
                        incomes = inputs.incomes,
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
                            errorMessage = exception.localizedMessage
                                ?: "Goal data is currently unavailable"
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

    private fun calculateDashboardState(
        incomes: List<Income>,
        expenses: List<Expense>,
        goals: List<Goal>,
        activeGoalContributions: List<Contribution>?
    ): DashboardUiState {
        var netIncomeMinor = 0L
        incomes.forEach { income ->
            val isLoss = income.entryKind == IncomeEntryKind.REALIZED_PNL && income.pnlDirection == PnlDirection.LOSS
            if (isLoss) {
                netIncomeMinor -= income.money.baseAmountMinor
            } else {
                netIncomeMinor += income.money.baseAmountMinor
            }
        }

        val totalExpenseMinor = expenses.sumOf { it.money.baseAmountMinor }
        val balanceMinor = netIncomeMinor - totalExpenseMinor

        val committedMinor = expenses
            .filter { it.spendingClass == SpendingClass.COMMITTED }
            .sumOf { it.money.baseAmountMinor }

        val discretionaryMinor = expenses
            .filter { it.spendingClass == SpendingClass.DISCRETIONARY }
            .sumOf { it.money.baseAmountMinor }

        val totalOutflow = committedMinor + discretionaryMinor
        val committedPct = if (totalOutflow > 0) ((committedMinor.toDouble() / totalOutflow) * 100).toInt() else 0
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
            val lkrDecimal = (inc.money.baseAmountMinor.toDouble() / 100.0)
            val title = if (inc.source == IncomeSource.CRYPTO) {
                if (isLoss) "Crypto Loss" else "Crypto Gain"
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
                    } else {
                        "+LKR ${String.format(Locale.US, "%,.2f", lkrDecimal)}"
                    },
                    isRedAmount = isLoss,
                    occurredDate = inc.occurredDate,
                    isIncome = !isLoss
                )
            )
        }

        val sortedTimeline = timelineItems.sortedByDescending { it.occurredDate }

        val userSnapshot = authRepository.currentUserSnapshot()
        val displayName = userSnapshot?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User"

        return DashboardUiState(
            userName = displayName,
            totalAvailableBalanceLkrMinor = balanceMinor,
            totalIncomeLkrMinor = netIncomeMinor,
            incomeCount = incomes.size,
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
