package com.example.personalfinanceapp.feature.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountScopedDataTest {

    @Test
    fun accountSwitchClearsPreviousValuesBeforeObservingReplacementOwner() = runTest {
        val authState = MutableStateFlow<String?>("user-a")
        val ownerStreams = mutableMapOf<String, MutableSharedFlow<List<String>>>()
        val emissions = mutableListOf<List<String>>()

        val collection = launch(UnconfinedTestDispatcher(testScheduler)) {
            authState.accountScopedData(emptyList()) { ownerId ->
                ownerStreams.getOrPut(ownerId) { MutableSharedFlow(extraBufferCapacity = 1) }
            }.collect(emissions::add)
        }

        assertEquals(listOf(emptyList<String>()), emissions)
        ownerStreams.getValue("user-a").emit(listOf("account-a-record"))
        runCurrent()

        authState.value = "user-b"
        runCurrent()
        ownerStreams.getValue("user-a").emit(listOf("late-account-a-record"))
        ownerStreams.getValue("user-b").emit(listOf("account-b-record"))
        runCurrent()

        assertEquals(
            listOf(
                emptyList(),
                listOf("account-a-record"),
                emptyList(),
                listOf("account-b-record")
            ),
            emissions
        )
        collection.cancel()
    }

    @Test
    fun signOutClearsCurrentOwnersValues() = runTest {
        val authState = MutableStateFlow<String?>("user-a")
        val ownerData = MutableSharedFlow<List<String>>(extraBufferCapacity = 1)
        val emissions = mutableListOf<List<String>>()

        val collection = launch(UnconfinedTestDispatcher(testScheduler)) {
            authState.accountScopedData(emptyList()) { ownerData }.collect(emissions::add)
        }
        ownerData.emit(listOf("private-record"))
        authState.value = null
        runCurrent()

        assertEquals(
            listOf(emptyList(), listOf("private-record"), emptyList()),
            emissions
        )
        collection.cancel()
    }
}
