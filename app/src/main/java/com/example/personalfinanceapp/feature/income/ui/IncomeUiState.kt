package com.example.personalfinanceapp.feature.income.ui

import com.example.personalfinanceapp.core.model.Income
import java.time.LocalDate

data class IncomeDateRange(
    val startDate: LocalDate,
    val endDateExclusive: LocalDate
) {
    init {
        require(startDate.isBefore(endDateExclusive)) {
            "Start date must be before end date"
        }
    }
}

enum class IncomeObservationStatus {
    LOADING,
    EMPTY,
    SUCCESS,
    ERROR
}

data class IncomeUiState(
    val dateRange: IncomeDateRange,
    val incomes: List<Income>,
    val observationStatus: IncomeObservationStatus,
    val totalLkrMinor: Long,
    val errorMessage: String?
) {
    val isLoading: Boolean
        get() = observationStatus == IncomeObservationStatus.LOADING

    val isEmpty: Boolean
        get() = observationStatus == IncomeObservationStatus.EMPTY

    val canRetry: Boolean
        get() = observationStatus == IncomeObservationStatus.ERROR

    init {
        when (observationStatus) {
            IncomeObservationStatus.LOADING,
            IncomeObservationStatus.EMPTY,
            IncomeObservationStatus.ERROR -> {
                require(incomes.isEmpty()) {
                    "$observationStatus state cannot expose income records"
                }
                require(totalLkrMinor == 0L) {
                    "$observationStatus state cannot expose an income total"
                }
            }

            IncomeObservationStatus.SUCCESS -> require(incomes.isNotEmpty()) {
                "SUCCESS state requires at least one income record"
            }
        }

        require((observationStatus == IncomeObservationStatus.ERROR) == (errorMessage != null)) {
            "Only ERROR state can expose an error message"
        }
    }

    companion object {
        internal fun loading(dateRange: IncomeDateRange): IncomeUiState = IncomeUiState(
            dateRange = dateRange,
            incomes = emptyList(),
            observationStatus = IncomeObservationStatus.LOADING,
            totalLkrMinor = 0L,
            errorMessage = null
        )

        internal fun empty(dateRange: IncomeDateRange): IncomeUiState = IncomeUiState(
            dateRange = dateRange,
            incomes = emptyList(),
            observationStatus = IncomeObservationStatus.EMPTY,
            totalLkrMinor = 0L,
            errorMessage = null
        )

        internal fun success(
            dateRange: IncomeDateRange,
            incomes: List<Income>,
            totalLkrMinor: Long
        ): IncomeUiState = IncomeUiState(
            dateRange = dateRange,
            incomes = incomes,
            observationStatus = IncomeObservationStatus.SUCCESS,
            totalLkrMinor = totalLkrMinor,
            errorMessage = null
        )

        internal fun error(
            dateRange: IncomeDateRange,
            message: String
        ): IncomeUiState = IncomeUiState(
            dateRange = dateRange,
            incomes = emptyList(),
            observationStatus = IncomeObservationStatus.ERROR,
            totalLkrMinor = 0L,
            errorMessage = message
        )
    }
}
