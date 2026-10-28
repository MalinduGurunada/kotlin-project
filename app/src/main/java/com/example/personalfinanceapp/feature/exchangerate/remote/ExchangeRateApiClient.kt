package com.example.personalfinanceapp.feature.exchangerate.remote

import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class ExchangeRateApiClient private constructor(
    private val httpClient: OkHttpClient,
    private val endpoint: HttpUrl
) {

    @Inject
    constructor(httpClient: OkHttpClient) : this(httpClient, PRODUCTION_ENDPOINT)

    suspend fun fetchLatestUsdRates(): String = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url(endpoint)
            .get()
            .build()
        val call = httpClient.newCall(request)

        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWith(
                    Result.failure(e.toExchangeRateException())
                )
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!continuation.isActive) return

                    val outcome = runCatching { responseBodyOrThrow(it) }
                    outcome.fold(
                        onSuccess = { body ->
                            continuation.resumeWith(Result.success(body))
                        },
                        onFailure = { error ->
                            val mappedError = when (error) {
                                is ExchangeRateException -> error
                                is IOException -> error.toExchangeRateException()
                                else -> ExchangeRateException.MalformedResponse(error)
                            }
                            continuation.resumeWith(Result.failure(mappedError))
                        }
                    )
                }
            }
        })
    }

    private fun responseBodyOrThrow(response: Response): String {
        if (response.code == HTTP_TOO_MANY_REQUESTS) {
            val retryAfterSeconds = response.header(RETRY_AFTER_HEADER)
                ?.toLongOrNull()
                ?.takeIf { it >= 0L }
            throw ExchangeRateException.RateLimited(retryAfterSeconds)
        }
        if (!response.isSuccessful) {
            throw ExchangeRateException.HttpError(response.code)
        }

        val responseBody = response.body
        val declaredLength = responseBody.contentLength()
        if (declaredLength > MAX_RESPONSE_BYTES) {
            throw ExchangeRateException.MalformedResponse()
        }

        val source = responseBody.source()
        source.request(MAX_RESPONSE_BYTES + 1L)
        if (source.buffer.size > MAX_RESPONSE_BYTES) {
            throw ExchangeRateException.MalformedResponse()
        }

        val charset = responseBody.contentType()?.charset(StandardCharsets.UTF_8)
            ?: StandardCharsets.UTF_8
        val body = source.buffer.clone().readString(charset)
        if (body.isBlank()) {
            throw ExchangeRateException.MalformedResponse()
        }
        return body
    }

    private fun IOException.toExchangeRateException(): ExchangeRateException =
        if (this is SocketTimeoutException || this is InterruptedIOException) {
            ExchangeRateException.Timeout(this)
        } else {
            ExchangeRateException.NetworkFailure(this)
        }

    companion object {
        private const val ENDPOINT = "https://open.er-api.com/v6/latest/USD"
        private val PRODUCTION_ENDPOINT = ENDPOINT.toHttpUrl()
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val RETRY_AFTER_HEADER = "Retry-After"
        private const val MAX_RESPONSE_BYTES = 1_048_576L

        internal fun createForTesting(
            httpClient: OkHttpClient,
            endpoint: HttpUrl
        ): ExchangeRateApiClient = ExchangeRateApiClient(httpClient, endpoint)
    }
}
