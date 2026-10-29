package com.example.personalfinanceapp.feature.exchangerate.remote

import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateException
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateQuote
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateRepository
import kotlinx.coroutines.CancellationException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteExchangeRateRepository @Inject constructor(
    private val apiClient: ExchangeRateApiClient,
    private val responseParser: ExchangeRateResponseParser,
    private val clock: Clock
) : ExchangeRateRepository {

    override suspend fun getLatestUsdToLkr(): Result<ExchangeRateQuote> = try {
        val responseBody = apiClient.fetchLatestUsdRates()
        Result.success(responseParser.parse(responseBody, clock.instant()))
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: ExchangeRateException) {
        Result.failure(exception)
    } catch (exception: Exception) {
        Result.failure(ExchangeRateException.MalformedResponse(exception))
    }
}
