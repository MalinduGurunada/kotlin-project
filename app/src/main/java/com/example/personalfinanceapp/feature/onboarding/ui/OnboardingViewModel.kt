package com.example.personalfinanceapp.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.ContributionKind
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Goal
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.example.personalfinanceapp.feature.goal.GoalRepository
import com.example.personalfinanceapp.feature.onboarding.OnboardingAccess
import com.example.personalfinanceapp.feature.onboarding.OnboardingProfile
import com.example.personalfinanceapp.feature.onboarding.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface OnboardingGateState {
    data object Resolving : OnboardingGateState
    data object Required : OnboardingGateState
    data object Complete : OnboardingGateState
    data class Error(val message: String) : OnboardingGateState
}

data class OnboardingUiState(
    val step: Int = 0,
    val baseCurrency: CurrencyCode = CurrencyCode.LKR,
    val selectedIncomeSources: Set<IncomeSource> = emptySet(),
    val salaryPaydayInput: String = "",
    val expectedMonthlyIncomeInput: String = "",
    val expectedMonthlySpendingInput: String = "",
    val includeFirstGoal: Boolean = false,
    val goalNameInput: String = "",
    val goalTargetInput: String = "",
    val goalSavedInput: String = "",
    val goalTargetDate: LocalDate = LocalDate.now().plusYears(1),
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val submissionLocked: Boolean = false
) {
    val isFirstStep: Boolean get() = step == 0
    val isLastStep: Boolean get() = step == LAST_ONBOARDING_STEP
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val onboardingRepository: OnboardingRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _gateState = MutableStateFlow<OnboardingGateState>(OnboardingGateState.Resolving)
    val gateState: StateFlow<OnboardingGateState> = _gateState.asStateFlow()

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var resolvedUserId: String? = null

    init {
        viewModelScope.launch {
            authRepository.observeAuthState().collectLatest { userId ->
                resolvedUserId = userId
                _uiState.value = OnboardingUiState()
                if (userId.isNullOrBlank()) {
                    _gateState.value = OnboardingGateState.Resolving
                } else {
                    resolveAccess(userId)
                }
            }
        }
    }

    fun retryAccess() {
        val userId = resolvedUserId ?: return
        viewModelScope.launch { resolveAccess(userId) }
    }

    fun toggleIncomeSource(source: IncomeSource) = updateEditableState { state ->
        val selected = state.selectedIncomeSources.toMutableSet()
        if (!selected.add(source)) selected.remove(source)
        state.copy(
            selectedIncomeSources = selected,
            salaryPaydayInput = if (source == IncomeSource.SALARY && source !in selected) {
                ""
            } else {
                state.salaryPaydayInput
            }
        )
    }

    fun onSalaryPaydayChanged(value: String) = updateEditableState {
        it.copy(salaryPaydayInput = value.filter(Char::isDigit).take(2))
    }

    fun onExpectedMonthlyIncomeChanged(value: String) = updateEditableState {
        it.copy(expectedMonthlyIncomeInput = filterMoneyInput(value))
    }

    fun onExpectedMonthlySpendingChanged(value: String) = updateEditableState {
        it.copy(expectedMonthlySpendingInput = filterMoneyInput(value))
    }

    fun onIncludeFirstGoalChanged(include: Boolean) = updateEditableState {
        it.copy(includeFirstGoal = include)
    }

    fun onGoalNameChanged(value: String) = updateEditableState {
        it.copy(goalNameInput = value.take(MAX_GOAL_NAME_LENGTH))
    }

    fun onGoalTargetChanged(value: String) = updateEditableState {
        it.copy(goalTargetInput = filterMoneyInput(value))
    }

    fun onGoalSavedChanged(value: String) = updateEditableState {
        it.copy(goalSavedInput = filterMoneyInput(value))
    }

    fun onGoalTargetDateChanged(date: LocalDate) = updateEditableState {
        it.copy(goalTargetDate = date)
    }

    fun nextStep() {
        val state = _uiState.value
        if (state.isSaving || state.submissionLocked || state.isLastStep) return
        val error = validateStep(state)
        if (error != null) {
            _uiState.value = state.copy(errorMessage = error)
            return
        }
        _uiState.value = state.copy(step = state.step + 1, errorMessage = null)
    }

    fun previousStep() {
        val state = _uiState.value
        if (state.isSaving || state.submissionLocked || state.isFirstStep) return
        _uiState.value = state.copy(step = state.step - 1, errorMessage = null)
    }

    fun finishOnboarding() {
        val state = _uiState.value
        val userId = resolvedUserId ?: return
        if (!state.isLastStep || state.isSaving) return

        val error = validateAll(state)
        if (error != null) {
            _uiState.value = state.copy(errorMessage = error)
            return
        }

        val incomeMinor = state.expectedMonthlyIncomeInput.toMinorUnitsOrNull()!!
        val spendingMinor = state.expectedMonthlySpendingInput.toMinorUnitsOrNull()!!
        val profile = OnboardingProfile(
            userId = userId,
            baseCurrency = state.baseCurrency,
            incomeSources = state.selectedIncomeSources,
            salaryPayday = state.salaryPaydayInput.toIntOrNull(),
            expectedMonthlyIncomeMinor = incomeMinor,
            expectedMonthlySpendingMinor = spendingMinor
        )

        _uiState.value = state.copy(
            isSaving = true,
            submissionLocked = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = completeSetup(userId, state, profile)
            if (resolvedUserId != userId) return@launch
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = null)
                _gateState.value = OnboardingGateState.Complete
            } else {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage
                        ?: "Setup could not be saved. Check your connection and retry."
                )
            }
        }
    }

    private suspend fun resolveAccess(userId: String) {
        _gateState.value = OnboardingGateState.Resolving
        val result = onboardingRepository.resolveAccess(userId)
        if (resolvedUserId != userId) return
        _gateState.value = result.fold(
            onSuccess = { access ->
                when (access) {
                    OnboardingAccess.Required -> OnboardingGateState.Required
                    is OnboardingAccess.Complete -> OnboardingGateState.Complete
                }
            },
            onFailure = {
                OnboardingGateState.Error(
                    "We couldn't load your setup. Check your connection and try again."
                )
            }
        )
    }

    private suspend fun completeSetup(
        userId: String,
        state: OnboardingUiState,
        profile: OnboardingProfile
    ): Result<Unit> = operationResult {
        if (state.includeFirstGoal) {
            val goal = Goal(
                id = ONBOARDING_GOAL_ID,
                name = state.goalNameInput.trim(),
                targetAmountMinor = state.goalTargetInput.toMinorUnitsOrNull()!!,
                startDate = LocalDate.now(),
                targetDate = state.goalTargetDate
            )
            goalRepository.saveGoalForOwner(userId, goal).getOrThrow()

            val openingAmount = state.goalSavedInput.toMinorUnitsOrNull()!!
            if (openingAmount > 0L) {
                goalRepository.addContributionForOwner(
                    userId,
                    Contribution(
                        id = ONBOARDING_OPENING_CONTRIBUTION_ID,
                        goalId = goal.id,
                        amountMinor = openingAmount,
                        kind = ContributionKind.OPENING,
                        occurredDate = goal.startDate,
                        note = "Opening balance from initial setup"
                    )
                ).getOrThrow()
            }
        }

        // This write is deliberately last: the navigation gate opens only after every
        // requested setup record has been stored successfully.
        onboardingRepository.completeOnboarding(userId, profile).getOrThrow()
    }

    private fun updateEditableState(transform: (OnboardingUiState) -> OnboardingUiState) {
        val state = _uiState.value
        if (state.isSaving || state.submissionLocked) return
        _uiState.value = transform(state).copy(errorMessage = null)
    }
}

internal fun validateStep(state: OnboardingUiState): String? = when (state.step) {
    0 -> if (state.baseCurrency != CurrencyCode.LKR) {
        "LKR is currently the supported reporting currency"
    } else null
    1 -> when {
        state.selectedIncomeSources.isEmpty() -> "Select at least one income source"
        IncomeSource.SALARY in state.selectedIncomeSources -> {
            val payday = state.salaryPaydayInput.toIntOrNull()
            if (payday == null || payday !in 1..31) {
                "Enter a salary payday from 1 to 31"
            } else {
                null
            }
        }
        else -> null
    }
    2 -> when {
        state.expectedMonthlyIncomeInput.toMinorUnitsOrNull() == null ->
            "Enter a valid expected monthly income"
        state.expectedMonthlySpendingInput.toMinorUnitsOrNull() == null ->
            "Enter a valid expected monthly spending amount"
        else -> null
    }
    3 -> validateGoal(state)
    else -> "Invalid setup step"
}

internal fun validateAll(state: OnboardingUiState): String? {
    for (step in 0..LAST_ONBOARDING_STEP) {
        validateStep(state.copy(step = step))?.let { return it }
    }
    return null
}

private fun validateGoal(state: OnboardingUiState): String? {
    if (!state.includeFirstGoal) return null
    val target = state.goalTargetInput.toMinorUnitsOrNull()
    val saved = state.goalSavedInput.toMinorUnitsOrNull()
    return when {
        state.goalNameInput.trim().isBlank() -> "Enter a name for your first goal"
        target == null || target <= 0L -> "Enter a valid goal target greater than zero"
        saved == null -> "Enter the amount already saved, or enter 0"
        saved > target -> "Already saved amount cannot exceed the goal target"
        !state.goalTargetDate.isAfter(LocalDate.now()) -> "Goal deadline must be in the future"
        else -> null
    }
}

internal fun String.toMinorUnitsOrNull(): Long? {
    return try {
        val normalized = trim()
        if (normalized.isBlank()) return null
        val amount = BigDecimal(normalized)
        if (amount.signum() < 0 || amount.scale() > 2) return null
        amount.movePointRight(2).longValueExact()
    } catch (_: ArithmeticException) {
        null
    } catch (_: NumberFormatException) {
        null
    }
}

private fun filterMoneyInput(value: String): String {
    val filtered = value.filter { it.isDigit() || it == '.' }
    val decimalIndex = filtered.indexOf('.')
    return if (decimalIndex < 0) {
        filtered.take(MAX_MONEY_INPUT_LENGTH)
    } else {
        val whole = filtered.substring(0, decimalIndex).take(MAX_MONEY_INPUT_LENGTH - 3)
        val fraction = filtered.substring(decimalIndex + 1).filter(Char::isDigit).take(2)
        "$whole.$fraction"
    }
}

private suspend inline fun operationResult(crossinline operation: suspend () -> Unit): Result<Unit> =
    try {
        operation()
        Result.success(Unit)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }

const val LAST_ONBOARDING_STEP = 3
private const val ONBOARDING_GOAL_ID = "onboarding-goal-v1"
private const val ONBOARDING_OPENING_CONTRIBUTION_ID = "onboarding-opening-v1"
private const val MAX_GOAL_NAME_LENGTH = 80
private const val MAX_MONEY_INPUT_LENGTH = 18
