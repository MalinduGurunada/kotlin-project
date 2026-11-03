package com.example.personalfinanceapp.feature.notification

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class NotificationPreferencesViewModel @Inject constructor(
    auth: FirebaseAuth,
    private val preferenceStore: NotificationPreferenceStore,
    private val tokenSyncCoordinator: FcmTokenSyncCoordinator
) : ViewModel() {
    private val userId = auth.currentUser?.uid
    private val _notificationsEnabled = MutableStateFlow(
        userId?.let(preferenceStore::isEnabled) ?: false
    )
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        val currentUserId = userId ?: return
        preferenceStore.setEnabled(currentUserId, enabled)
        _notificationsEnabled.value = enabled
        tokenSyncCoordinator.onPreferenceChanged(currentUserId, enabled)
    }
}
