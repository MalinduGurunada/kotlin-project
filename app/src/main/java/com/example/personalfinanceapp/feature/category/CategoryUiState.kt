package com.example.personalfinanceapp.feature.category

import com.example.personalfinanceapp.core.model.Category

sealed interface CategoryUiState {
    data object Loading : CategoryUiState

    data class Success(
        val activeCategories: List<Category> = emptyList(),
        val archivedCategories: List<Category> = emptyList()
    ) : CategoryUiState {
        val selectableCategories: List<Category>
            get() = activeCategories
    }

    data class Error(val message: String) : CategoryUiState
}
