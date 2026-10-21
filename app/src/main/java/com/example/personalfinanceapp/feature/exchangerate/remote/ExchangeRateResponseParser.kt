package com.example.personalfinanceapp.feature.exchangerate.remote

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateException
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateProvider
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateQuote
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject

class ExchangeRateResponseParser @Inject constructor(
    private val json: Json
) {

    fun parse(responseBody: String, fetchedAt: Instant): ExchangeRateQuote {
        val root = try {
            json.parseToJsonElement(responseBody) as? JsonObject
                ?: throw ExchangeRateException.MalformedResponse()
        } catch (exception: SerializationException) {
            throw ExchangeRateException.MalformedResponse(exception)
        } catch (exception: IllegalArgumentException) {
            throw ExchangeRateException.MalformedResponse(exception)
        }

        val result = root.requiredString(RESULT_FIELD)
        if (result != SUCCESS_RESULT) {
            val errorType = root.optionalString(ERROR_TYPE_FIELD) ?: result
            throw ExchangeRateException.ProviderError(errorType)
        }

        val baseCode = root.requiredString(BASE_CODE_FIELD)
        if (baseCode != CurrencyCode.USD.name) {
            throw ExchangeRateException.ProviderError("unexpected-base-code:$baseCode")
        }

        val rates = root[RATES_FIELD] as? JsonObject
            ?: throw ExchangeRateException.MalformedResponse()
        val rate = parseAndNormalizeRate(rates[LKR_FIELD])
        val providerUpdatedAt = root.requiredInstant(LAST_UPDATE_FIELD)
        val nextProviderUpdateAt = root.requiredInstant(NEXT_UPDATE_FIELD)

        validateTimestamps(providerUpdatedAt, nextProviderUpdateAt, fetchedAt)

        return ExchangeRateQuote(
            baseCurrency = CurrencyCode.USD,
            targetCurrency = CurrencyCode.LKR,
            rate = rate,
            provider = ExchangeRateProvider.EXCHANGE_RATE_API_OPEN,
            applicableDate = providerUpdatedAt.atZone(ZoneOffset.UTC).toLocalDate(),
            providerUpdatedAt = providerUpdatedAt,
            nextProviderUpdateAt = nextProviderUpdateAt,
            fetchedAt = fetchedAt
        )
    }

    private fun parseAndNormalizeRate(element: Any?): BigDecimal {
        val primitive = element as? JsonPrimitive
            ?: throw ExchangeRateException.InvalidRate()
        if (primitive.isString) {
            throw ExchangeRateException.InvalidRate("USD/LKR rate must be a JSON number")
        }

        val rawValue = primitive.content
        if (rawValue.length > MAX_RATE_TEXT_LENGTH) {
            throw ExchangeRateException.InvalidRate("USD/LKR rate is unreasonably large")
        }
        val rate = try {
            BigDecimal(rawValue)
        } catch (exception: NumberFormatException) {
            throw ExchangeRateException.InvalidRate()
        }
        if (rate.signum() <= 0) {
            throw ExchangeRateException.InvalidRate("USD/LKR rate must be positive")
        }
        if (rate.precision() > MAX_RATE_PRECISION || rate.scale() !in MIN_RATE_SCALE..MAX_RATE_SCALE) {
            throw ExchangeRateException.InvalidRate("USD/LKR rate has unreasonable precision")
        }

        val normalized = if (rate.scale() > MAX_FRACTION_DIGITS) {
            rate.setScale(MAX_FRACTION_DIGITS, RoundingMode.HALF_UP)
        } else {
            rate
        }
        if (normalized.signum() <= 0) {
            throw ExchangeRateException.InvalidRate(
                "USD/LKR rate becomes zero after six-decimal normalization"
            )
        }
        return normalized.stripTrailingZeros()
    }

    private fun validateTimestamps(
        providerUpdatedAt: Instant,
        nextProviderUpdateAt: Instant,
        fetchedAt: Instant
    ) {
        try {
            if (!nextProviderUpdateAt.isAfter(providerUpdatedAt)) {
                throw ExchangeRateException.MalformedResponse()
            }
            if (Duration.between(providerUpdatedAt, nextProviderUpdateAt) > MAX_PROVIDER_INTERVAL) {
                throw ExchangeRateException.MalformedResponse()
            }
            if (providerUpdatedAt.isAfter(fetchedAt.plus(FUTURE_CLOCK_SKEW))) {
                throw ExchangeRateException.MalformedResponse()
            }
            if (fetchedAt.isAfter(nextProviderUpdateAt.plus(NEXT_UPDATE_GRACE)) ||
                fetchedAt.isAfter(providerUpdatedAt.plus(MAX_QUOTE_AGE))
            ) {
                throw ExchangeRateException.StaleQuote()
            }
        } catch (exception: DateTimeException) {
            throw ExchangeRateException.MalformedResponse(exception)
        } catch (exception: ArithmeticException) {
            throw ExchangeRateException.MalformedResponse(exception)
        }
    }

    private fun JsonObject.requiredString(fieldName: String): String {
        val primitive = this[fieldName] as? JsonPrimitive
            ?: throw ExchangeRateException.MalformedResponse()
        if (!primitive.isString) {
            throw ExchangeRateException.MalformedResponse()
        }
        return primitive.content
    }

    private fun JsonObject.optionalString(fieldName: String): String? {
        val primitive = this[fieldName] as? JsonPrimitive ?: return null
        return primitive.content.takeIf { primitive.isString }
    }

    private fun JsonObject.requiredInstant(fieldName: String): Instant {
        val primitive = this[fieldName] as? JsonPrimitive
            ?: throw ExchangeRateException.MalformedResponse()
        if (primitive.isString) {
            throw ExchangeRateException.MalformedResponse()
        }
        val seconds = primitive.longOrNull
            ?: throw ExchangeRateException.MalformedResponse()
        return try {
            Instant.ofEpochSecond(seconds)
        } catch (exception: DateTimeException) {
            throw ExchangeRateException.MalformedResponse(exception)
        }
    }

    companion object {
        private const val RESULT_FIELD = "result"
        private const val SUCCESS_RESULT = "success"
        private const val ERROR_TYPE_FIELD = "error-type"
        private const val BASE_CODE_FIELD = "base_code"
        private const val RATES_FIELD = "rates"
        private const val LKR_FIELD = "LKR"
        private const val LAST_UPDATE_FIELD = "time_last_update_unix"
        private const val NEXT_UPDATE_FIELD = "time_next_update_unix"
        private const val MAX_FRACTION_DIGITS = 6
        private const val MAX_RATE_TEXT_LENGTH = 64
        private const val MAX_RATE_PRECISION = 32
        private const val MIN_RATE_SCALE = -32
        private const val MAX_RATE_SCALE = 32

        internal val FUTURE_CLOCK_SKEW: Duration = Duration.ofMinutes(5)
        internal val NEXT_UPDATE_GRACE: Duration = Duration.ofHours(6)
        internal val MAX_QUOTE_AGE: Duration = Duration.ofDays(7)
        private val MAX_PROVIDER_INTERVAL: Duration = Duration.ofDays(7)
    }
}
