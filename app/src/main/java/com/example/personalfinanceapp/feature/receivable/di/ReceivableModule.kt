package com.example.personalfinanceapp.feature.receivable.di

import com.example.personalfinanceapp.feature.receivable.FirestoreReceivableRepository
import com.example.personalfinanceapp.feature.receivable.ReceivableRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReceivableModule {
    @Binds
    @Singleton
    abstract fun bindReceivableRepository(impl: FirestoreReceivableRepository): ReceivableRepository
}
