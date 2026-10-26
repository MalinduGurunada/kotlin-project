package com.example.personalfinanceapp.feature.exchangerate

sealed class ExchangeRateException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    class NetworkFailure(cause: Throwable) : ExchangeRateException(
        message = "Unable to retrieve the exchange rate",
        cause = cause
    )

    class Timeout(cause: Throwable) : ExchangeRateException(
        message = "Exchange-rate request timed out",
        cause = cause
    )

    class HttpError(
        val statusCode: Int
    ) : ExchangeRateException(
        message = "Exchange-rate service returned HTTP $statusCode"
    )

    class RateLimited(
        val retryAfterSeconds: Long? = null
    ) : ExchangeRateException(
        message = "Exchange-rate service rate limit reached"
    )

    class ProviderError(
        val errorType: String
    ) : ExchangeRateException(
        message = "Exchange-rate provider error: $errorType"
    )

    class InvalidRate(
        message: String = "Exchange-rate response did not contain a valid USD/LKR rate"
    ) : ExchangeRateException(message)

    class MalformedResponse(cause: Throwable? = null) : ExchangeRateException(
        message = "Exchange-rate response was malformed",
        cause = cause
    )

    class StaleQuote(
        message: String = "Exchange-rate quote is stale"
    ) : ExchangeRateException(message)
}
