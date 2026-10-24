package com.example.personalfinanceapp.feature.income.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.example.personalfinanceapp.feature.income.IncomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
    private val authRepository: AuthRepository,
    clock: Clock
) : ViewModel() {

    private val initialDateRange = currentMonthDateRange(clock)
    private val selectedDateRange = MutableStateFlow(initialDateRange)
    private val retryVersion = MutableStateFlow(0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<IncomeUiState> = combine(
        selectedDateRange,
        retryVersion
    ) { dateRange, retry ->
        ObservationRequest(dateRange = dateRange, retryVersion = retry)
    }.flatMapLatest { request ->
        observeRequest(request)
    }.buffer(capacity = 0)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = IncomeUiState.loading(initialDateRange)
        )

    fun setDateRange(startDate: LocalDate, endDateExclusive: LocalDate) {
        selectedDateRange.value = IncomeDateRange(startDate, endDateExclusive)
    }

    fun retryObservation() {
        if (!uiState.value.canRetry) return
        retryVersion.update { current ->
            if (current == Long.MAX_VALUE) 0L else current + 1L
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeRequest(request: ObservationRequest): Flow<IncomeUiState> =
        authRepository.observeAuthState()
            .distinctUntilChanged()
            .flatMapLatest { authenticatedUid ->
                if (authenticatedUid == null) {
                    flowOf(IncomeUiState.empty(request.dateRange))
                } else {
                    observeAuthenticatedRange(request.dateRange)
                }
            }
            .buffer(capacity = 0)
            .onStart {
                emit(IncomeUiState.loading(request.dateRange))
            }
            .catch { exception ->
                if (exception is CancellationException) throw exception
                emit(
                    IncomeUiState.error(
                        dateRange = request.dateRange,
                        message = OBSERVATION_ERROR_MESSAGE
                    )
                )
            }

    private fun observeAuthenticatedRange(
        dateRange: IncomeDateRange
    ): Flow<IncomeUiState> = incomeRepository
        .observeIncomes(dateRange.startDate, dateRange.endDateExclusive)
        .map { incomes -> loadedState(dateRange, incomes) }
        .onStart {
            emit(IncomeUiState.loading(dateRange))
        }
        .catch { exception ->
            if (exception is CancellationException) throw exception
            emit(
                IncomeUiState.error(
                    dateRange = dateRange,
                    message = OBSERVATION_ERROR_MESSAGE
                )
            )
        }

    private fun loadedState(
        dateRange: IncomeDateRange,
        incomes: List<Income>
    ): IncomeUiState {
        val immutableSnapshot = incomes.toList()
        if (immutableSnapshot.isEmpty()) {
            return IncomeUiState.empty(dateRange)
        }

        return try {
            IncomeUiState.success(
                dateRange = dateRange,
                incomes = immutableSnapshot,
                totalLkrMinor = calculateTotalLkrMinor(immutableSnapshot)
            )
        } catch (_: ArithmeticException) {
            IncomeUiState.error(
                dateRange = dateRange,
                message = TOTAL_OVERFLOW_ERROR_MESSAGE
            )
        }
    }

    /**
     * Stored money is already an LKR snapshot. Receipts and realised gains add that snapshot,
     * while a crypto realised loss subtracts its positive stored magnitude.
     */
    private fun calculateTotalLkrMinor(incomes: List<Income>): Long =
        incomes.fold(0L) { total, income ->
            val signedAmount = if (
                income.source == IncomeSource.CRYPTO &&
                income.entryKind == IncomeEntryKind.REALIZED_PNL &&
                income.pnlDirection == PnlDirection.LOSS
            ) {
                Math.negateExact(income.money.baseAmountMinor)
            } else {
                income.money.baseAmountMinor
            }
            Math.addExact(total, signedAmount)
        }

    private data class ObservationRequest(
        val dateRange: IncomeDateRange,
        val retryVersion: Long
    )

    private companion object {
        const val OBSERVATION_ERROR_MESSAGE =
            "Unable to load income records. Please try again."
        const val TOTAL_OVERFLOW_ERROR_MESSAGE =
            "Income total is too large to display."

        fun currentMonthDateRange(clock: Clock): IncomeDateRange {
            val currentMonthStart = LocalDate.now(clock).withDayOfMonth(1)
            return IncomeDateRange(
                startDate = currentMonthStart,
                endDateExclusive = currentMonthStart.plusMonths(1)
            )
        }
    }
}
