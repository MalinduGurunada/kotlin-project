package com.example.personalfinanceapp.feature.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseAuthSignOutPolicyTest {

    @Test
    fun cleanupFailureDoesNotPreventFirebaseSignOut() = runTest {
        var firebaseSignOutCalls = 0

        val result = performFirebaseSignOut(
            cleanup = { Result.failure(IllegalStateException("permission denied")) },
            firebaseSignOut = { firebaseSignOutCalls += 1 }
        )

        assertTrue(result.isSuccess)
        assertEquals(1, firebaseSignOutCalls)
    }

    @Test
    fun offlineCleanupExceptionDoesNotPreventFirebaseSignOut() = runTest {
        var firebaseSignOutCalls = 0

        val result = performFirebaseSignOut(
            cleanup = { throw IllegalStateException("offline") },
            firebaseSignOut = { firebaseSignOutCalls += 1 }
        )

        assertTrue(result.isSuccess)
        assertEquals(1, firebaseSignOutCalls)
    }

    @Test
    fun actualFirebaseSignOutFailureIsReturned() = runTest {
        val failure = IllegalStateException("Firebase sign-out failed")

        val result = performFirebaseSignOut(
            cleanup = { Result.success(Unit) },
            firebaseSignOut = { throw failure }
        )

        assertTrue(result.isFailure)
        assertEquals(failure, result.exceptionOrNull())
    }
}
