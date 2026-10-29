package com.example.personalfinanceapp.feature.exchangerate

import com.example.personalfinanceapp.core.model.CurrencyCode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class ExchangeRateContractsTest {

    private val quote = ExchangeRateQuote(
        baseCurrency = CurrencyCode.USD,
        targetCurrency = CurrencyCode.LKR,
        rate = BigDecimal("305.123456"),
        provider = ExchangeRateProvider.EXCHANGE_RATE_API_OPEN,
        applicableDate = LocalDate.of(2026, 10, 20),
        providerUpdatedAt = Instant.parse("2026-10-20T00:00:00Z"),
        nextProviderUpdateAt = Instant.parse("2026-10-21T00:00:00Z"),
        fetchedAt = Instant.parse("2026-10-20T03:30:00Z")
    )

    @Test
    fun representsQuoteMetadataWithoutFloatingPointConversion() {
        assertEquals(CurrencyCode.USD, quote.baseCurrency)
        assertEquals(CurrencyCode.LKR, quote.targetCurrency)
        assertEquals("305.123456", quote.rate.toPlainString())
        assertEquals(ExchangeRateProvider.EXCHANGE_RATE_API_OPEN, quote.provider)
        assertEquals(LocalDate.of(2026, 10, 20), quote.applicableDate)
        assertEquals(
            Instant.parse("2026-10-20T00:00:00Z"),
            quote.providerUpdatedAt
        )
        assertEquals(
            Instant.parse("2026-10-21T00:00:00Z"),
            quote.nextProviderUpdateAt
        )
        assertEquals(Instant.parse("2026-10-20T03:30:00Z"), quote.fetchedAt)
    }

    @Test
    fun rejectsNonPositiveRates() {
        listOf(BigDecimal.ZERO, BigDecimal("-0.01")).forEach { invalidRate ->
            val result = runCatching {
                quote.copy(rate = invalidRate)
            }

            assertTrue(result.isFailure)
            assertEquals(
                "Exchange rate must be greater than zero",
                result.exceptionOrNull()?.message
            )
        }
    }

    @Test
    fun repositoryContractReturnsQuoteInResult() = runTest {
        val repository = object : ExchangeRateRepository {
            override suspend fun getLatestUsdToLkr(): Result<ExchangeRateQuote> =
                Result.success(quote)
        }

        val result = repository.getLatestUsdToLkr()

        assertTrue(result.isSuccess)
        assertSame(quote, result.getOrThrow())
    }
}
