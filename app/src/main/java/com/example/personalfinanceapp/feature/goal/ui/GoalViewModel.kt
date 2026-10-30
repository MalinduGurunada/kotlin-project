package com.example.personalfinanceapp.feature.goal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.ContributionKind
import com.example.personalfinanceapp.core.model.Goal
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.example.personalfinanceapp.feature.goal.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

data class GoalItemUiState(
    val id: String,
    val name: String,
    val targetAmountLkr: Long,
    val savedAmountLkr: Long,
    val remainingLkr: Long,
    val progressPercent: Int,
    val isCompleted: Boolean,
    val isOverdue: Boolean,
    val requiredMonthlySavingsLkr: Long,
    val startDate: LocalDate,
    val targetDate: LocalDate
)

data class GoalsUiState(
    val goals: List<GoalItemUiState> = emptyList(),
    val selectedGoalDetail: GoalItemUiState? = null,
    val contributions: List<Contribution> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _selectedGoalId = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GoalsUiState> = authRepository.observeAuthState()
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(GoalsUiState(isLoading = false))
            } else {
                goalRepository.observeGoals().flatMapLatest { goals ->
                    val activeId = _selectedGoalId.value ?: goals.firstOrNull()?.id
                    val contributionsFlow = if (activeId != null) {
                        goalRepository.observeContributions(activeId)
                    } else {
                        flowOf(emptyList())
                    }

                    combine(
                        contributionsFlow,
                        _selectedGoalId,
                        _isLoading,
                        _errorMessage
                    ) { contributions, selectedId, isLoading, error ->
                        val now = LocalDate.now()
                        val mappedGoals = goals.map { goal ->
                            val savedMinor = if (goal.id == activeId) {
                                contributions.sumOf { it.amountMinor }
                            } else 0L

                            val savedLkr = savedMinor / 100
                            val targetLkr = goal.targetAmountMinor / 100
                            val remainingLkr = (targetLkr - savedLkr).coerceAtLeast(0)
                            val pct = if (targetLkr > 0) ((savedLkr.toDouble() / targetLkr) * 100).toInt().coerceIn(0, 100) else 0

                            val monthsLeft = ChronoUnit.MONTHS.between(now, goal.targetDate).coerceAtLeast(1)
                            val requiredMonthly = if (remainingLkr > 0) remainingLkr / monthsLeft else 0L

                            GoalItemUiState(
                                id = goal.id,
                                name = goal.name,
                                targetAmountLkr = targetLkr,
                                savedAmountLkr = savedLkr,
                                remainingLkr = remainingLkr,
                                progressPercent = pct,
                                isCompleted = savedLkr >= targetLkr,
                                isOverdue = now.isAfter(goal.targetDate) && savedLkr < targetLkr,
                                requiredMonthlySavingsLkr = requiredMonthly,
                                startDate = goal.startDate,
                                targetDate = goal.targetDate
                            )
                        }

                        val selectedDetail = mappedGoals.find { it.id == (selectedId ?: activeId) } ?: mappedGoals.firstOrNull()

                        GoalsUiState(
                            goals = mappedGoals,
                            selectedGoalDetail = selectedDetail,
                            contributions = contributions,
                            isLoading = isLoading,
                            errorMessage = error
                        )
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GoalsUiState(isLoading = true)
        )

    fun selectGoal(goalId: String) {
        _selectedGoalId.value = goalId
    }

    fun saveGoal(
        id: String?,
        name: String,
        targetAmountLkr: Long,
        startDate: LocalDate,
        targetDate: LocalDate
    ) {
        if (name.isBlank()) {
            _errorMessage.value = "Goal name cannot be blank"
            return
        }
        if (targetAmountLkr <= 0) {
            _errorMessage.value = "Target amount must be greater than zero"
            return
        }

        val goalId = id?.ifBlank { null } ?: UUID.randomUUID().toString()
        val goal = Goal(
            id = goalId,
            name = name.trim(),
            targetAmountMinor = targetAmountLkr * 100,
            startDate = startDate,
            targetDate = targetDate
        )

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = goalRepository.saveGoal(goal)
            _isLoading.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save goal"
            } else {
                _selectedGoalId.value = goalId
            }
        }
    }

    fun addContribution(
        goalId: String,
        amountLkr: Long,
        note: String?
    ) {
        if (amountLkr <= 0) {
            _errorMessage.value = "Contribution amount must be positive"
            return
        }

        val contribution = Contribution(
            id = UUID.randomUUID().toString(),
            goalId = goalId,
            amountMinor = amountLkr * 100,
            kind = ContributionKind.ALLOCATION,
            occurredDate = LocalDate.now(),
            note = note?.trim()?.ifBlank { null }
        )

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = goalRepository.addContribution(contribution)
            _isLoading.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to add contribution"
            }
        }
    }

    fun reverseContribution(originalContribution: Contribution) {
        val reversal = Contribution(
            id = UUID.randomUUID().toString(),
            goalId = originalContribution.goalId,
            amountMinor = -originalContribution.amountMinor,
            kind = ContributionKind.REVERSAL,
            occurredDate = LocalDate.now(),
            reversesId = originalContribution.id,
            note = "Reversal of ${originalContribution.id}"
        )

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = goalRepository.addContribution(reversal)
            _isLoading.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to reverse contribution"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
