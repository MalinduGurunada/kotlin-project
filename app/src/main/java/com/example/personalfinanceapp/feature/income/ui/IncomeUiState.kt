package com.example.personalfinanceapp.feature.income.ui

import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.UserId
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
    val ownerId: UserId?,
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
        require(ownerId == null || ownerId.isNotBlank()) {
            "Income observation owner cannot be blank"
        }
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

            IncomeObservationStatus.SUCCESS -> {
                require(incomes.isNotEmpty()) {
                    "SUCCESS state requires at least one income record"
                }
                require(ownerId != null) {
                    "SUCCESS state requires an observation owner"
                }
            }
        }

        require((observationStatus == IncomeObservationStatus.ERROR) == (errorMessage != null)) {
            "Only ERROR state can expose an error message"
        }
    }

    companion object {
        internal fun loading(
            dateRange: IncomeDateRange,
            ownerId: UserId? = null
        ): IncomeUiState = IncomeUiState(
            dateRange = dateRange,
            ownerId = ownerId,
            incomes = emptyList(),
            observationStatus = IncomeObservationStatus.LOADING,
            totalLkrMinor = 0L,
            errorMessage = null
        )

        internal fun empty(
            dateRange: IncomeDateRange,
            ownerId: UserId? = null
        ): IncomeUiState = IncomeUiState(
            dateRange = dateRange,
            ownerId = ownerId,
            incomes = emptyList(),
            observationStatus = IncomeObservationStatus.EMPTY,
            totalLkrMinor = 0L,
            errorMessage = null
        )

        internal fun success(
            dateRange: IncomeDateRange,
            ownerId: UserId,
            incomes: List<Income>,
            totalLkrMinor: Long
        ): IncomeUiState = IncomeUiState(
            dateRange = dateRange,
            ownerId = ownerId,
            incomes = incomes,
            observationStatus = IncomeObservationStatus.SUCCESS,
            totalLkrMinor = totalLkrMinor,
            errorMessage = null
        )

        internal fun error(
            dateRange: IncomeDateRange,
            ownerId: UserId? = null,
            message: String
        ): IncomeUiState = IncomeUiState(
            dateRange = dateRange,
            ownerId = ownerId,
            incomes = emptyList(),
            observationStatus = IncomeObservationStatus.ERROR,
            totalLkrMinor = 0L,
            errorMessage = message
        )
    }
}

data class IncomeDeleteUiState(
    val selectedIncome: Income? = null,
    val ownerId: UserId? = null,
    val deletingIncomeId: String? = null,
    val errorMessage: String? = null
) {
    val isDeleting: Boolean
        get() = deletingIncomeId != null

    init {
        require((selectedIncome == null) == (ownerId == null)) {
            "A pending income deletion requires its captured owner"
        }
        require(deletingIncomeId == null || deletingIncomeId == selectedIncome?.id) {
            "Deleting income must match the selected record"
        }
        require(errorMessage == null || selectedIncome != null) {
            "A delete error requires a selected income"
        }
    }
}
