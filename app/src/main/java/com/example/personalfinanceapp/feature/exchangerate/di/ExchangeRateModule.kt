package com.example.personalfinanceapp.feature.exchangerate.di

import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateRepository
import com.example.personalfinanceapp.feature.exchangerate.remote.RemoteExchangeRateRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExchangeRateModule {

    @Binds
    @Singleton
    abstract fun bindExchangeRateRepository(
        implementation: RemoteExchangeRateRepository
    ): ExchangeRateRepository

    companion object {
        @Provides
        @Singleton
        fun provideExchangeRateHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        @Provides
        @Singleton
        fun provideExchangeRateJson(): Json = Json {
            ignoreUnknownKeys = true
            isLenient = false
            allowSpecialFloatingPointValues = false
        }

        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemUTC()

        private const val CONNECT_TIMEOUT_SECONDS = 5L
        private const val READ_TIMEOUT_SECONDS = 10L
        private const val WRITE_TIMEOUT_SECONDS = 10L
        private const val CALL_TIMEOUT_SECONDS = 15L
    }
}
