package com.example.personalfinanceapp.feature.auth

import com.example.personalfinanceapp.core.model.UserId

fun interface AuthSessionCleanup {

    suspend fun clearForUser(userId: UserId): Result<Unit>

    companion object {
        val NoOp = AuthSessionCleanup { Result.success(Unit) }
    }
}
