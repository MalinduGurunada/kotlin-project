package com.example.personalfinanceapp.feature.auth

import android.content.Context
import com.example.personalfinanceapp.core.model.UserId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

data class PendingRegistration(
    val email: String,
    val fullName: String,
    val userId: UserId? = null,
    val phase: PendingRegistrationPhase = PendingRegistrationPhase.Active
)

enum class PendingRegistrationPhase {
    Active,
    Abandoned
}

sealed interface PendingRegistrationState {
    data object Absent : PendingRegistrationState
    data class Valid(val registration: PendingRegistration) : PendingRegistrationState
    data object Corrupt : PendingRegistrationState
}

interface PendingRegistrationStore {
    fun read(): PendingRegistrationState
    fun save(pendingRegistration: PendingRegistration): Result<Unit>
    fun clear(): Result<Unit>
}

class SharedPreferencesPendingRegistrationStore @Inject constructor(
    @ApplicationContext context: Context
) : PendingRegistrationStore {

    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    override fun read(): PendingRegistrationState {
        val active = runCatching { preferences.getBoolean(KEY_ACTIVE, false) }
            .getOrElse { return PendingRegistrationState.Corrupt }
        if (!active) return PendingRegistrationState.Absent

        val email = runCatching { preferences.getString(KEY_EMAIL, null) }
            .getOrElse { return PendingRegistrationState.Corrupt }
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.takeIf(String::isNotBlank)
            ?: return PendingRegistrationState.Corrupt
        val fullName = runCatching { preferences.getString(KEY_FULL_NAME, null) }
            .getOrElse { return PendingRegistrationState.Corrupt }
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: return PendingRegistrationState.Corrupt
        val userId = runCatching { preferences.getString(KEY_USER_ID, null) }
            .getOrElse { return PendingRegistrationState.Corrupt }
            ?.takeIf(String::isNotBlank)
        val phase = when (
            runCatching { preferences.getString(KEY_PHASE, null) }
                .getOrElse { return PendingRegistrationState.Corrupt }
        ) {
            null, PHASE_ACTIVE -> PendingRegistrationPhase.Active
            PHASE_ABANDONED -> PendingRegistrationPhase.Abandoned
            else -> return PendingRegistrationState.Corrupt
        }

        return PendingRegistrationState.Valid(
            PendingRegistration(
                email = email,
                fullName = fullName,
                userId = userId,
                phase = phase
            )
        )
    }

    override fun save(pendingRegistration: PendingRegistration): Result<Unit> {
        val committed = preferences.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_EMAIL, pendingRegistration.email.trim().lowercase(Locale.ROOT))
            .putString(KEY_FULL_NAME, pendingRegistration.fullName.trim())
            .putString(
                KEY_PHASE,
                if (pendingRegistration.phase == PendingRegistrationPhase.Abandoned) {
                    PHASE_ABANDONED
                } else {
                    PHASE_ACTIVE
                }
            )
            .apply {
                if (pendingRegistration.userId == null) {
                    remove(KEY_USER_ID)
                } else {
                    putString(KEY_USER_ID, pendingRegistration.userId)
                }
            }
            .commit()

        return if (committed) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Unable to persist registration progress"))
        }
    }

    override fun clear(): Result<Unit> {
        return if (preferences.edit().clear().commit()) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Unable to clear registration progress"))
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "pending_registration"
        const val KEY_ACTIVE = "active"
        const val KEY_EMAIL = "email"
        const val KEY_FULL_NAME = "full_name"
        const val KEY_USER_ID = "user_id"
        const val KEY_PHASE = "phase"
        const val PHASE_ACTIVE = "active"
        const val PHASE_ABANDONED = "abandoned"
    }
}
