package com.example.personalfinanceapp.feature.category.ui

import com.example.personalfinanceapp.core.model.Category

sealed interface CategoryUiState {
    data object Loading : CategoryUiState
    data class Success(
        val activeCategories: List<Category>,
        val archivedCategories: List<Category>
    ) : CategoryUiState
    data class Error(val message: String) : CategoryUiState
}
