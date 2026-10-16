package com.example.personalfinanceapp.core.model

import java.time.LocalDate

enum class CurrencyCode {
    LKR,
    USD
}

enum class RateSource{
    LKR_FIXED,
    MANUAL_TRANSFER,
    MANUAL_REFERENCE
}

data class Money (
    val originalAmountDecimal: String,
    val originalCurrency: CurrencyCode,
    val rateLkrPerUnitDecimal: String,
    val baseAmountMinor: Long,
    val rateSource: RateSource,
    val rateDate: LocalDate
)