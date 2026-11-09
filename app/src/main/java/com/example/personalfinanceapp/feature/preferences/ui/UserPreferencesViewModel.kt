package com.example.personalfinanceapp.feature.preferences.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.feature.preferences.PreferencesRepository
import com.example.personalfinanceapp.feature.preferences.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserPreferencesViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    @OptIn(ExperimentalCoroutinesApi::class)
    val userPreferences: StateFlow<UserPreferences> =
        if (currentUserId == null) {
            flowOf(UserPreferences())
        } else {
            preferencesRepository.observeUserPreferences(currentUserId!!)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun setBiometricUnlockEnabled(enabled: Boolean) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            preferencesRepository.updateBiometricUnlockEnabled(uid, enabled)
        }
    }
}
