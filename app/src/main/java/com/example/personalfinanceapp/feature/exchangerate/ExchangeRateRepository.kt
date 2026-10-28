package com.example.personalfinanceapp.feature.exchangerate

interface ExchangeRateRepository {
    suspend fun getLatestUsdToLkr(): Result<ExchangeRateQuote>
}
