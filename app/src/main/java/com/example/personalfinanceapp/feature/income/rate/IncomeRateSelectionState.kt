package com.example.personalfinanceapp.feature.income.rate

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateException
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateQuote
import java.time.LocalDate

data class IncomeRateSelectionState(
    val selectedCurrency: CurrencyCode = CurrencyCode.LKR,
    val selectedRateMode: IncomeRateSelectionMode? = null,
    val amountInput: String = "",
    val transactionDate: LocalDate,
    val manualRateInput: String = "",
    val manualRateSource: IncomeManualRateSource? = null,
    val manualRateDate: LocalDate,
    val automaticQuote: ExchangeRateQuote? = null,
    val isLoadingAutomaticRate: Boolean = false,
    val validationError: String? = null,
    val automaticRateError: ExchangeRateException? = null,
    val estimatedMoney: Money? = null
)
