package com.example.personalfinanceapp.feature.auth

import com.example.personalfinanceapp.core.model.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * Switches a data stream with the authenticated account and emits a clearing value before the
 * next account's cache is observed. The clearing emission is important because a StateFlow keeps
 * its previous value while a new Firestore listener waits for its first cache/server snapshot.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Flow<UserId?>.accountScopedData(
    clearedValue: T,
    observeOwner: (UserId) -> Flow<T>
): Flow<T> = distinctUntilChanged()
    .flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(clearedValue)
        } else {
            flow {
                emit(clearedValue)
                emitAll(observeOwner(userId))
            }
        }
    }
    .buffer(capacity = 0)
