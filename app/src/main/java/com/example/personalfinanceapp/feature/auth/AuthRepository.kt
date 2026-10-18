package com.example.personalfinanceapp.feature.auth

import com.example.personalfinanceapp.core.model.UserId
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    fun observeAuthState(): Flow<UserId?>

    suspend fun login(
        email: String,
        password: String
    ): Result<UserId>

    suspend fun register(
        email: String,
        password: String
    ): Result<UserId>

    suspend fun sendPasswordReset(
        email: String
    ): Result<Unit>

    suspend fun signOut(): Result<Unit>
}
