package com.example.personalfinanceapp.feature.auth

import com.example.personalfinanceapp.core.model.UserId

fun interface AuthPostSignOutCleanup {

    suspend fun clearForUser(userId: UserId): Result<Unit>

    companion object {
        val NoOp = AuthPostSignOutCleanup { Result.success(Unit) }
    }
}
