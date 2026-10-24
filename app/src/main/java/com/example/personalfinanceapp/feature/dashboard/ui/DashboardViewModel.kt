package com.example.personalfinanceapp.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Goal
import com.example.personalfinanceapp.core.model.Income
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
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
    val targetAmountLkr: Long,
    val currentSavedLkr: Long,
    val remainingLkr: Long,
    val progressPercent: Int,
    val priorityText: String = "Priority 1",
    val targetDateText: String = "June 2025"
)

data class DashboardUiState(
    val userName: String = "Himasha",
    val totalAvailableBalanceLkrMinor: Long = 284_500_00L,
    val totalIncomeLkrMinor: Long = 410_000_00L,
    val incomeCount: Int = 3,
    val totalExpenseLkrMinor: Long = 125_500_00L,
    val expenseCount: Int = 24,
    val committedExpenseLkrMinor: Long = 84_200_00L,
    val committedPercent: Int = 67,
    val discretionaryExpenseLkrMinor: Long = 41_300_00L,
    val discretionaryPercent: Int = 33,
    val activeGoal: ActiveSavingsGoalUiState? = ActiveSavingsGoalUiState(
        id = "goal-m3",
        name = "MacBook Pro M3 Fund",
        targetAmountLkr = 550_000,
        currentSavedLkr = 385_000,
        remainingLkr = 165_000,
        progressPercent = 70
    ),
    val recentActivity: List<TimelineActivityItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

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
                    calculateDashboardState(incomes, expenses, goals)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState(isLoading = true)
        )

    private fun calculateDashboardState(
        incomes: List<Income>,
        expenses: List<Expense>,
        goals: List<Goal>
    ): DashboardUiState {
        val totalIncomeMinor = if (incomes.isNotEmpty()) incomes.sumOf { it.money.baseAmountMinor } else 410_000_00L
        val totalExpenseMinor = if (expenses.isNotEmpty()) expenses.sumOf { it.money.baseAmountMinor } else 125_500_00L
        val balanceMinor = totalIncomeMinor - totalExpenseMinor

        val committedMinor = expenses.filter { it.spendingClass == SpendingClass.COMMITTED }
            .sumOf { it.money.baseAmountMinor }
            .let { if (it > 0L) it else 84_200_00L }

        val discretionaryMinor = expenses.filter { it.spendingClass == SpendingClass.DISCRETIONARY }
            .sumOf { it.money.baseAmountMinor }
            .let { if (it > 0L) it else 41_300_00L }

        val totalOutflow = committedMinor + discretionaryMinor
        val committedPct = if (totalOutflow > 0) ((committedMinor.toDouble() / totalOutflow) * 100).toInt() else 67
        val discretionaryPct = 100 - committedPct

        val primaryGoal = goals.firstOrNull()?.let { goal ->
            val savedLkr = goal.targetAmountMinor / 100 / 2 // Default illustrative progress
            val targetLkr = goal.targetAmountMinor / 100
            val remainingLkr = targetLkr - savedLkr
            val pct = if (targetLkr > 0) ((savedLkr.toDouble() / targetLkr) * 100).toInt() else 70

            ActiveSavingsGoalUiState(
                id = goal.id,
                name = goal.name,
                targetAmountLkr = targetLkr,
                currentSavedLkr = savedLkr,
                remainingLkr = remainingLkr,
                progressPercent = pct
            )
        } ?: ActiveSavingsGoalUiState(
            id = "goal-m3",
            name = "MacBook Pro M3 Fund",
            targetAmountLkr = 550_000,
            currentSavedLkr = 385_000,
            remainingLkr = 165_000,
            progressPercent = 70
        )

        val timelineItems = mutableListOf<TimelineActivityItem>()

        expenses.forEach { exp ->
            val lkrDecimal = (exp.money.baseAmountMinor.toDouble() / 100.0)
            timelineItems.add(
                TimelineActivityItem(
                    id = exp.id,
                    title = exp.categoryNameSnapshot,
                    subtitle = "${exp.paymentChannel.name} • ${exp.occurredDate}",
                    amountText = "-LKR ${String.format("%.2f", lkrDecimal)}",
                    isRedAmount = true,
                    occurredDate = exp.occurredDate,
                    isIncome = false
                )
            )
        }

        incomes.forEach { inc ->
            val lkrDecimal = (inc.money.baseAmountMinor.toDouble() / 100.0)
            timelineItems.add(
                TimelineActivityItem(
                    id = inc.id,
                    title = "${inc.source.name} Receipt",
                    subtitle = "${inc.occurredDate} • Inbound",
                    amountText = "+LKR ${String.format("%.2f", lkrDecimal)}",
                    isRedAmount = false,
                    occurredDate = inc.occurredDate,
                    isIncome = true
                )
            )
        }

        val sortedTimeline = if (timelineItems.isNotEmpty()) {
            timelineItems.sortedByDescending { it.occurredDate }
        } else {
            listOf(
                TimelineActivityItem("1", "Freelance Upwork", "28 Mar • Inbound", "+LKR 145,000.00", false, LocalDate.of(2025, 3, 28), true),
                TimelineActivityItem("2", "Keells Supermarket", "27 Mar • Everyday", "-LKR 18,450.00", true, LocalDate.of(2025, 3, 27), false),
                TimelineActivityItem("3", "Dialog Axiata Home", "25 Mar • Utilities", "-LKR 6,200.00", true, LocalDate.of(2025, 3, 25), false)
            )
        }

        return DashboardUiState(
            userName = "Himasha",
            totalAvailableBalanceLkrMinor = balanceMinor,
            totalIncomeLkrMinor = totalIncomeMinor,
            incomeCount = if (incomes.isNotEmpty()) incomes.size else 3,
            totalExpenseLkrMinor = totalExpenseMinor,
            expenseCount = if (expenses.isNotEmpty()) expenses.size else 24,
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
