package com.example.personalfinanceapp.feature.exchangerate

import com.example.personalfinanceapp.core.model.CurrencyCode
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class ExchangeRateQuote(
    val baseCurrency: CurrencyCode,
    val targetCurrency: CurrencyCode,
    val rate: BigDecimal,
    val provider: ExchangeRateProvider,
    val applicableDate: LocalDate,
    val providerUpdatedAt: Instant?,
    val nextProviderUpdateAt: Instant?,
    val fetchedAt: Instant
) {
    init {
        require(rate > BigDecimal.ZERO) {
            "Exchange rate must be greater than zero"
        }
    }
}
