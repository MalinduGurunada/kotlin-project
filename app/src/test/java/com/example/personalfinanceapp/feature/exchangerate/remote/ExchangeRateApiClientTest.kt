package com.example.personalfinanceapp.feature.exchangerate.remote

import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

class ExchangeRateApiClientTest {

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
    fun `successful GET returns body and uses only approved resource path`() = runBlocking {
        val responseBody = "{\"result\":\"success\"}"
        server.enqueue(MockResponse(body = responseBody))

        val result = testClient().fetchLatestUsdRates()

        assertEquals(responseBody, result)
        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(request)
        assertEquals("GET", request?.method)
        assertEquals("/v6/latest/USD", request?.target)
    }

    @Test
    fun `HTTP 400 and 500 return typed HTTP errors`() = runBlocking {
        listOf(400, 500).forEach { statusCode ->
            server.enqueue(MockResponse(code = statusCode, body = "failure"))

            val failure = fetchFailure(testClient())

            assertTrue(failure is ExchangeRateException.HttpError)
            assertEquals(statusCode, (failure as ExchangeRateException.HttpError).statusCode)
        }
    }

    @Test
    fun `HTTP 429 returns rate-limited error with numeric Retry-After`() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(429)
                .addHeader("Retry-After", "120")
                .body("limited")
                .build()
        )

        val failure = fetchFailure(testClient())

        assertTrue(failure is ExchangeRateException.RateLimited)
        assertEquals(120L, (failure as ExchangeRateException.RateLimited).retryAfterSeconds)
    }

    @Test
    fun `blank response body is rejected as malformed`() = runBlocking {
        server.enqueue(MockResponse(body = "  \n"))

        assertTrue(fetchFailure(testClient()) is ExchangeRateException.MalformedResponse)
    }

    @Test
    fun `response larger than safety limit is rejected as malformed`() = runBlocking {
        server.enqueue(MockResponse(body = "x".repeat(1_048_577)))

        assertTrue(fetchFailure(testClient()) is ExchangeRateException.MalformedResponse)
    }

    @Test
    fun `read timeout returns typed timeout failure`() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .body("delayed")
                .bodyDelay(1, TimeUnit.SECONDS)
                .build()
        )
        val shortTimeoutClient = OkHttpClient.Builder()
            .readTimeout(100, TimeUnit.MILLISECONDS)
            .callTimeout(300, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .build()

        val failure = fetchFailure(testClient(shortTimeoutClient))

        assertTrue(failure is ExchangeRateException.Timeout)
    }

    @Test
    fun `connection refusal returns typed network failure`() = runBlocking {
        val unusedPort = ServerSocket(0).use { it.localPort }
        val endpoint = "http://127.0.0.1:$unusedPort/v6/latest/USD".toHttpUrl()
        val client = ExchangeRateApiClient.createForTesting(
            OkHttpClient.Builder().retryOnConnectionFailure(false).build(),
            endpoint
        )

        assertTrue(fetchFailure(client) is ExchangeRateException.NetworkFailure)
    }

    @Test
    fun `cancelling coroutine cancels suspended request without returning a failure result`() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .body("delayed")
                .headersDelay(30, TimeUnit.SECONDS)
                .build()
        )
        val request = async(start = CoroutineStart.UNDISPATCHED) {
            testClient().fetchLatestUsdRates()
        }
        assertNotNull(server.takeRequest(2, TimeUnit.SECONDS))

        request.cancel()

        try {
            request.await()
            fail("Expected request coroutine to be cancelled")
        } catch (_: CancellationException) {
            assertTrue(request.isCancelled)
        }
    }

    private fun testClient(
        httpClient: OkHttpClient = OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .build()
    ): ExchangeRateApiClient = ExchangeRateApiClient.createForTesting(
        httpClient,
        server.url("/v6/latest/USD")
    )

    private suspend fun fetchFailure(client: ExchangeRateApiClient): Throwable = runCatching {
        client.fetchLatestUsdRates()
    }.exceptionOrNull() ?: error("Expected request to fail")
}
