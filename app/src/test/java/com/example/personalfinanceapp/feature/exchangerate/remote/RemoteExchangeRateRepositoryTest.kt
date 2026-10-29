package com.example.personalfinanceapp.feature.exchangerate.remote

import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateException
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class RemoteExchangeRateRepositoryTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `repository contract returns successful quote using injected Clock`() = runBlocking {
        server.enqueue(MockResponse(body = validResponse()))
        val repository: ExchangeRateRepository = repository()

        val result = repository.getLatestUsdToLkr()

        assertTrue(result.isSuccess)
        val quote = result.getOrThrow()
        assertEquals("310.123457", quote.rate.toPlainString())
        assertEquals(FETCHED_AT, quote.fetchedAt)
    }

    @Test
    fun `repository returns typed parser failure in Result`() = runBlocking {
        server.enqueue(MockResponse(body = validResponse().replace("310.1234567", "0")))

        val result = repository().getLatestUsdToLkr()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ExchangeRateException.InvalidRate)
    }

    @Test
    fun `repository preserves typed HTTP failure in Result`() = runBlocking {
        server.enqueue(MockResponse(code = 500, body = "failure"))

        val result = repository().getLatestUsdToLkr()

        assertTrue(result.isFailure)
        val failure = result.exceptionOrNull()
        assertTrue(failure is ExchangeRateException.HttpError)
        assertEquals(500, (failure as ExchangeRateException.HttpError).statusCode)
    }

    @Test
    fun `repository rethrows cancellation instead of wrapping it in Result`() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .body("delayed")
                .headersDelay(30, TimeUnit.SECONDS)
                .build()
        )
        val deferred = async(start = CoroutineStart.UNDISPATCHED) {
            repository().getLatestUsdToLkr()
        }
        assertNotNull(server.takeRequest(2, TimeUnit.SECONDS))

        deferred.cancel()

        try {
            deferred.await()
            fail("Expected repository request to be cancelled")
        } catch (_: CancellationException) {
            assertTrue(deferred.isCancelled)
        }
    }

    private fun repository(): RemoteExchangeRateRepository {
        val client = ExchangeRateApiClient.createForTesting(
            OkHttpClient.Builder().retryOnConnectionFailure(false).build(),
            server.url("/v6/latest/USD")
        )
        return RemoteExchangeRateRepository(
            apiClient = client,
            responseParser = ExchangeRateResponseParser(Json { ignoreUnknownKeys = true }),
            clock = Clock.fixed(FETCHED_AT, ZoneOffset.UTC)
        )
    }

    private fun validResponse(): String = """
        {
          "result": "success",
          "base_code": "USD",
          "time_last_update_unix": 1792454400,
          "time_next_update_unix": 1792540800,
          "rates": { "LKR": 310.1234567 }
        }
    """.trimIndent()

    companion object {
        private val FETCHED_AT: Instant = Instant.parse("2026-10-20T03:30:00Z")
    }
}
