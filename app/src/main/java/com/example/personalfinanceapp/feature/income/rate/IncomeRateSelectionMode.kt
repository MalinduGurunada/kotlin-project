package com.example.personalfinanceapp.feature.income.rate

import com.example.personalfinanceapp.core.model.RateSource

enum class IncomeRateSelectionMode {
    AUTOMATIC,
    MANUAL
}

enum class IncomeManualRateSource(
    internal val moneyRateSource: RateSource
) {
    ACTUAL_TRANSFER(RateSource.MANUAL_TRANSFER),
    REFERENCE(RateSource.MANUAL_REFERENCE)
}
