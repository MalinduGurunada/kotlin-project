package com.example.personalfinanceapp.feature.reminder.daily

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class DailyCaptureReminderViewModel @Inject constructor(
    auth: FirebaseAuth,
    private val store: DailyCaptureReminderStore,
    private val scheduler: DailyCaptureReminderScheduler
) : ViewModel() {
    private val userId = auth.currentUser?.uid
    private val _settings = MutableStateFlow(
        userId?.let(store::getSettings) ?: DailyCaptureReminderSettings()
    )
    val settings = _settings.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        val currentUserId = userId ?: return
        scheduler.setEnabled(currentUserId, enabled)
        _settings.value = store.getSettings(currentUserId)
    }

    fun setTimeMinutes(timeMinutes: Int) {
        val currentUserId = userId ?: return
        scheduler.setTimeMinutes(currentUserId, timeMinutes)
        _settings.value = store.getSettings(currentUserId)
    }
}
