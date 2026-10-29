package com.example.personalfinanceapp.feature.exchangerate

import com.example.personalfinanceapp.core.model.CurrencyCode
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

object ExchangeRateQuoteValidator {

    fun validateUsdToLkr(
        quote: ExchangeRateQuote,
        currentTime: Instant
    ): Result<Unit> = try {
        if (quote.baseCurrency != CurrencyCode.USD || quote.targetCurrency != CurrencyCode.LKR) {
            throw ExchangeRateException.ProviderError("unexpected-currency-pair")
        }
        if (quote.provider != ExchangeRateProvider.EXCHANGE_RATE_API_OPEN) {
            throw ExchangeRateException.ProviderError("unexpected-provider")
        }
        if (quote.rate <= BigDecimal.ZERO || quote.rate.scale() > MAX_RATE_FRACTION_DIGITS) {
            throw ExchangeRateException.InvalidRate()
        }

        val providerUpdatedAt = quote.providerUpdatedAt
            ?: throw ExchangeRateException.MalformedResponse()
        val nextProviderUpdateAt = quote.nextProviderUpdateAt
            ?: throw ExchangeRateException.MalformedResponse()
        if (quote.applicableDate != providerUpdatedAt.atZone(ZoneOffset.UTC).toLocalDate()) {
            throw ExchangeRateException.MalformedResponse()
        }

        requireFreshTimestamps(
            providerUpdatedAt = providerUpdatedAt,
            nextProviderUpdateAt = nextProviderUpdateAt,
            observedAt = currentTime
        )
        Result.success(Unit)
    } catch (exception: ExchangeRateException) {
        Result.failure(exception)
    } catch (exception: Exception) {
        Result.failure(ExchangeRateException.MalformedResponse(exception))
    }

    internal fun requireFreshTimestamps(
        providerUpdatedAt: Instant,
        nextProviderUpdateAt: Instant,
        observedAt: Instant
    ) {
        try {
            if (!nextProviderUpdateAt.isAfter(providerUpdatedAt)) {
                throw ExchangeRateException.MalformedResponse()
            }
            if (Duration.between(providerUpdatedAt, nextProviderUpdateAt) > MAX_PROVIDER_INTERVAL) {
                throw ExchangeRateException.MalformedResponse()
            }
            if (providerUpdatedAt.isAfter(observedAt.plus(FUTURE_CLOCK_SKEW))) {
                throw ExchangeRateException.MalformedResponse()
            }
            if (observedAt.isAfter(nextProviderUpdateAt.plus(NEXT_UPDATE_GRACE)) ||
                observedAt.isAfter(providerUpdatedAt.plus(MAX_QUOTE_AGE))
            ) {
                throw ExchangeRateException.StaleQuote()
            }
        } catch (exception: DateTimeException) {
            throw ExchangeRateException.MalformedResponse(exception)
        } catch (exception: ArithmeticException) {
            throw ExchangeRateException.MalformedResponse(exception)
        }
    }

    internal const val MAX_RATE_FRACTION_DIGITS = 6
    internal val FUTURE_CLOCK_SKEW: Duration = Duration.ofMinutes(5)
    internal val NEXT_UPDATE_GRACE: Duration = Duration.ofHours(6)
    internal val MAX_QUOTE_AGE: Duration = Duration.ofDays(7)
    private val MAX_PROVIDER_INTERVAL: Duration = Duration.ofDays(7)
}
