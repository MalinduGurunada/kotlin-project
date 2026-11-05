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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigInteger
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
                    if (goals.isEmpty()) {
                        combine(
                            _selectedGoalId,
                            _isLoading,
                            _errorMessage
                        ) { _, isLoading, error ->
                            GoalsUiState(
                                goals = emptyList(),
                                selectedGoalDetail = null,
                                contributions = emptyList(),
                                isLoading = isLoading,
                                errorMessage = error
                            )
                        }
                    } else {
                        val flows: List<Flow<Pair<String, List<Contribution>>>> = goals.map { goal ->
                            goalRepository.observeContributions(goal.id).map { goal.id to it }
                        }
                        val contributionsMapFlow = combine(flows) { pairs ->
                            pairs.toMap()
                        }
                        combine(
                            contributionsMapFlow,
                            _selectedGoalId,
                            _isLoading,
                            _errorMessage
                        ) { contributionsByGoalId, selectedId, isLoading, error ->
                            val now = LocalDate.now()
                            val mappedGoals = goals.map { goal ->
                                val goalContributions = contributionsByGoalId[goal.id].orEmpty()
                                val savedMinorBig = goalContributions.fold(BigInteger.ZERO) { total, contribution ->
                                    total.add(BigInteger.valueOf(contribution.amountMinor))
                                }
                                val savedMinor = savedMinorBig
                                    .max(BigInteger.valueOf(Long.MIN_VALUE))
                                    .min(BigInteger.valueOf(Long.MAX_VALUE))
                                    .toLong()
                                
                                val targetBig = BigInteger.valueOf(goal.targetAmountMinor)
                                val remainingMinor = targetBig.subtract(savedMinorBig)
                                    .max(BigInteger.ZERO)
                                    .min(BigInteger.valueOf(Long.MAX_VALUE))
                                    .toLong()
                                val nonNegativeSaved = savedMinor.coerceAtLeast(0L)
                                val pct = if (goal.targetAmountMinor > 0L) {
                                    BigInteger.valueOf(nonNegativeSaved)
                                        .multiply(BigInteger.valueOf(100L))
                                        .divide(BigInteger.valueOf(goal.targetAmountMinor))
                                        .min(BigInteger.valueOf(100L))
                                        .toInt()
                                } else 0

                                val monthsLeft = ChronoUnit.MONTHS.between(now, goal.targetDate).coerceAtLeast(1)
                                val remainingLkr = remainingMinor / 100
                                val requiredMonthly = if (remainingLkr > 0) remainingLkr / monthsLeft else 0L

                                GoalItemUiState(
                                    id = goal.id,
                                    name = goal.name,
                                    targetAmountLkr = goal.targetAmountMinor / 100,
                                    savedAmountLkr = savedMinor / 100,
                                    remainingLkr = remainingLkr,
                                    progressPercent = pct,
                                    isCompleted = savedMinor >= goal.targetAmountMinor && goal.targetAmountMinor > 0L,
                                    isOverdue = now.isAfter(goal.targetDate) && savedMinor < goal.targetAmountMinor,
                                    requiredMonthlySavingsLkr = requiredMonthly,
                                    startDate = goal.startDate,
                                    targetDate = goal.targetDate
                                )
                            }

                            val activeId = selectedId ?: mappedGoals.firstOrNull()?.id
                            val selectedDetail = mappedGoals.find { it.id == activeId } ?: mappedGoals.firstOrNull()
                            val activeContributions = selectedDetail?.id?.let { contributionsByGoalId[it] }.orEmpty()

                            GoalsUiState(
                                goals = mappedGoals,
                                selectedGoalDetail = selectedDetail,
                                contributions = activeContributions,
                                isLoading = isLoading,
                                errorMessage = error
                            )
                        }
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
        if (targetDate.isBefore(startDate)) {
            _errorMessage.value = "Target date cannot be before start date"
            return
        }
        val targetAmountMinor = try {
            Math.multiplyExact(targetAmountLkr, 100L)
        } catch (_: ArithmeticException) {
            _errorMessage.value = "Goal target is too large"
            return
        }
        val ownerId = authRepository.currentUserId()
        if (ownerId.isNullOrBlank()) {
            _errorMessage.value = "User is not authenticated"
            return
        }

        val goalId = id?.ifBlank { null } ?: UUID.randomUUID().toString()
        val goal = Goal(
            id = goalId,
            name = name.trim(),
            targetAmountMinor = targetAmountMinor,
            startDate = startDate,
            targetDate = targetDate
        )

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = goalRepository.saveGoalForOwner(ownerId, goal)
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
        val amountMinor = try {
            Math.multiplyExact(amountLkr, 100L)
        } catch (_: ArithmeticException) {
            _errorMessage.value = "Contribution amount is too large"
            return
        }
        val ownerId = authRepository.currentUserId()
        if (ownerId.isNullOrBlank()) {
            _errorMessage.value = "User is not authenticated"
            return
        }

        val contribution = Contribution(
            id = UUID.randomUUID().toString(),
            goalId = goalId,
            amountMinor = amountMinor,
            kind = ContributionKind.ALLOCATION,
            occurredDate = LocalDate.now(),
            note = note?.trim()?.ifBlank { null }
        )

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = goalRepository.addContributionForOwner(ownerId, contribution)
            _isLoading.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to add contribution"
            }
        }
    }

    fun reverseContribution(originalContribution: Contribution) {
        val reversalAmount = try {
            Math.negateExact(originalContribution.amountMinor)
        } catch (_: ArithmeticException) {
            _errorMessage.value = "Contribution amount cannot be reversed safely"
            return
        }
        val ownerId = authRepository.currentUserId()
        if (ownerId.isNullOrBlank()) {
            _errorMessage.value = "User is not authenticated"
            return
        }
        val reversal = Contribution(
            id = UUID.randomUUID().toString(),
            goalId = originalContribution.goalId,
            amountMinor = reversalAmount,
            kind = ContributionKind.REVERSAL,
            occurredDate = LocalDate.now(),
            reversesId = originalContribution.id,
            note = "Reversal of ${originalContribution.id}"
        )

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = goalRepository.addContributionForOwner(ownerId, reversal)
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
