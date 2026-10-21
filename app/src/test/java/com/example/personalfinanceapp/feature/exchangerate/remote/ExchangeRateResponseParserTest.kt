package com.example.personalfinanceapp.feature.exchangerate.remote

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateException
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateProvider
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class ExchangeRateResponseParserTest {

    private val parser = ExchangeRateResponseParser(Json { ignoreUnknownKeys = true })

    @Test
    fun `valid response preserves pair provider timestamps and injected fetch time`() {
        val quote = parser.parse(validResponse(), FETCHED_AT)

        assertEquals(CurrencyCode.USD, quote.baseCurrency)
        assertEquals(CurrencyCode.LKR, quote.targetCurrency)
        assertEquals(BigDecimal("310.12"), quote.rate)
        assertEquals(ExchangeRateProvider.EXCHANGE_RATE_API_OPEN, quote.provider)
        assertEquals(LocalDate.of(2026, 10, 20), quote.applicableDate)
        assertEquals(UPDATED_AT, quote.providerUpdatedAt)
        assertEquals(NEXT_UPDATE_AT, quote.nextProviderUpdateAt)
        assertEquals(FETCHED_AT, quote.fetchedAt)
    }

    @Test
    fun `rate with exactly six decimals is unchanged`() {
        val quote = parser.parse(validResponse(rate = "310.123456"), FETCHED_AT)

        assertEquals("310.123456", quote.rate.toPlainString())
    }

    @Test
    fun `higher precision rate is normalized to six decimals with HALF_UP`() {
        val quote = parser.parse(validResponse(rate = "310.123456789"), FETCHED_AT)

        assertEquals("310.123457", quote.rate.toPlainString())
    }

    @Test
    fun `decimal rate is parsed without Double precision loss`() {
        val quote = parser.parse(validResponse(rate = "999999.123456499"), FETCHED_AT)

        assertEquals("999999.123456", quote.rate.toPlainString())
    }

    @Test
    fun `positive rate that normalizes to zero is rejected`() {
        val failure = parseFailure(validResponse(rate = "0.0000004"))

        assertTrue(failure is ExchangeRateException.InvalidRate)
    }

    @Test
    fun `empty and malformed JSON are rejected as malformed responses`() {
        listOf("", "{not-json", "[]").forEach { body ->
            assertTrue(parseFailure(body) is ExchangeRateException.MalformedResponse)
        }
    }

    @Test
    fun `missing rates object is rejected as malformed response`() {
        val response = validResponse().replace("\"rates\": { \"LKR\": 310.12 },", "")

        assertTrue(parseFailure(response) is ExchangeRateException.MalformedResponse)
    }

    @Test
    fun `missing LKR rate is rejected as invalid rate`() {
        val response = validResponse().replace("\"LKR\": 310.12", "\"EUR\": 0.92")

        assertTrue(parseFailure(response) is ExchangeRateException.InvalidRate)
    }

    @Test
    fun `wrong base currency is rejected as provider error`() {
        val failure = parseFailure(validResponse(baseCode = "EUR"))

        assertTrue(failure is ExchangeRateException.ProviderError)
        assertEquals("unexpected-base-code:EUR", (failure as ExchangeRateException.ProviderError).errorType)
    }

    @Test
    fun `provider failure result preserves provider error type`() {
        val response = """
            {
              "result": "error",
              "error-type": "unsupported-code"
            }
        """.trimIndent()

        val failure = parseFailure(response)

        assertTrue(failure is ExchangeRateException.ProviderError)
        assertEquals("unsupported-code", (failure as ExchangeRateException.ProviderError).errorType)
    }

    @Test
    fun `string zero and negative rates are rejected as invalid rates`() {
        listOf("\"310.12\"", "0", "-1.25").forEach { rate ->
            assertTrue(
                parseFailure(validResponse(rate = rate)) is ExchangeRateException.InvalidRate
            )
        }
    }

    @Test
    fun `missing string and fractional timestamps are rejected as malformed responses`() {
        val invalidResponses = listOf(
            validResponse(updatedAt = null),
            validResponse().replace(
                "\"time_next_update_unix\": ${NEXT_UPDATE_AT.epochSecond},",
                ""
            ),
            validResponse(updatedAt = "\"${UPDATED_AT.epochSecond}\""),
            validResponse(updatedAt = "${UPDATED_AT.epochSecond}.5")
        )

        invalidResponses.forEach { response ->
            assertTrue(parseFailure(response) is ExchangeRateException.MalformedResponse)
        }
    }

    @Test
    fun `quote at advertised next-update grace boundary remains valid`() {
        val boundary = NEXT_UPDATE_AT.plus(ExchangeRateResponseParser.NEXT_UPDATE_GRACE)

        val quote = parser.parse(validResponse(), boundary)

        assertEquals(boundary, quote.fetchedAt)
    }

    @Test
    fun `quote immediately beyond next-update grace boundary is stale`() {
        val beyondBoundary = NEXT_UPDATE_AT
            .plus(ExchangeRateResponseParser.NEXT_UPDATE_GRACE)
            .plusNanos(1)

        assertTrue(
            parseFailure(validResponse(), beyondBoundary) is ExchangeRateException.StaleQuote
        )
    }

    @Test
    fun `clearly outdated quote is stale`() {
        val clearlyStale = UPDATED_AT.plus(ExchangeRateResponseParser.MAX_QUOTE_AGE).plusSeconds(1)

        assertTrue(parseFailure(validResponse(), clearlyStale) is ExchangeRateException.StaleQuote)
    }

    @Test
    fun `contradictory provider timestamps are rejected`() {
        val response = validResponse(nextUpdateAt = UPDATED_AT.epochSecond.toString())

        assertTrue(parseFailure(response) is ExchangeRateException.MalformedResponse)
    }

    @Test
    fun `unreasonable provider update interval is rejected`() {
        val response = validResponse(
            nextUpdateAt = UPDATED_AT.plusSeconds(8 * 86_400L).epochSecond.toString()
        )

        assertTrue(parseFailure(response) is ExchangeRateException.MalformedResponse)
    }

    @Test
    fun `unreasonable numeric rate representation is rejected`() {
        val response = validResponse(rate = "123456789012345678901234567890123")

        assertTrue(parseFailure(response) is ExchangeRateException.InvalidRate)
    }

    @Test
    fun `provider update at future clock-skew boundary remains valid`() {
        val update = FETCHED_AT.plus(ExchangeRateResponseParser.FUTURE_CLOCK_SKEW)
        val response = validResponse(
            updatedAt = update.epochSecond.toString(),
            nextUpdateAt = update.plusSeconds(86_400).epochSecond.toString()
        )

        val quote = parser.parse(response, FETCHED_AT)

        assertEquals(update, quote.providerUpdatedAt)
    }

    @Test
    fun `provider update beyond future clock-skew boundary is rejected`() {
        val update = FETCHED_AT.plus(ExchangeRateResponseParser.FUTURE_CLOCK_SKEW).plusSeconds(1)
        val response = validResponse(
            updatedAt = update.epochSecond.toString(),
            nextUpdateAt = update.plusSeconds(86_400).epochSecond.toString()
        )

        assertTrue(parseFailure(response) is ExchangeRateException.MalformedResponse)
    }

    private fun parseFailure(
        response: String,
        fetchedAt: Instant = FETCHED_AT
    ): Throwable = runCatching {
        parser.parse(response, fetchedAt)
    }.exceptionOrNull() ?: error("Expected parsing to fail")

    private fun validResponse(
        rate: String = "310.12",
        baseCode: String = "USD",
        updatedAt: String? = UPDATED_AT.epochSecond.toString(),
        nextUpdateAt: String = NEXT_UPDATE_AT.epochSecond.toString()
    ): String {
        val updateField = updatedAt?.let { "\"time_last_update_unix\": $it," }.orEmpty()
        return """
            {
              "result": "success",
              "base_code": "$baseCode",
              $updateField
              "time_next_update_unix": $nextUpdateAt,
              "rates": { "LKR": $rate },
              "ignored": "value"
            }
        """.trimIndent()
    }

    companion object {
        private val UPDATED_AT: Instant = Instant.parse("2026-10-20T00:00:00Z")
        private val NEXT_UPDATE_AT: Instant = Instant.parse("2026-10-21T00:00:00Z")
        private val FETCHED_AT: Instant = Instant.parse("2026-10-20T03:30:00Z")
    }
}
