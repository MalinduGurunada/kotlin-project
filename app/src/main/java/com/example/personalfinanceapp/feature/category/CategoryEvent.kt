package com.example.personalfinanceapp.feature.category

sealed interface CategoryEvent {
    data object CategorySaved : CategoryEvent
    data class ValidationFailed(val message: String) : CategoryEvent
}
