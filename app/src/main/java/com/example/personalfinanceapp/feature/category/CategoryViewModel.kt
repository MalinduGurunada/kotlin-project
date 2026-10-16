package com.example.personalfinanceapp.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.SpendingClass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryUiState>(CategoryUiState.Loading)
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CategoryEvent>()
    val events: SharedFlow<CategoryEvent> = _events.asSharedFlow()

    init {
        observeCategories()
    }

    private fun observeCategories() {
        viewModelScope.launch {
            repository.observeCategories()
                .catch { exception ->
                    _uiState.value = CategoryUiState.Error(
                        exception.message ?: "Failed to load categories"
                    )
                }
                .collect { categories ->
                    val active = categories.filter { !it.archived }
                    val archived = categories.filter { it.archived }

                    _uiState.value = CategoryUiState.Success(
                        activeCategories = active,
                        archivedCategories = archived
                    )
                }
        }
    }

    fun addCategory(
        name: String,
        spendingClass: SpendingClass
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val id = UUID.randomUUID().toString()

            val newCategory = Category(
                id = id,
                name = trimmedName,
                defaultSpendingClass = spendingClass,
                archived = false
            )

            val result = repository.addCategory(newCategory)

            result.fold(
                onSuccess = {
                    _events.emit(CategoryEvent.CategorySaved)
                },
                onFailure = { exception ->
                    _events.emit(
                        CategoryEvent.ValidationFailed(
                            exception.message ?: "Failed to add category"
                        )
                    )
                }
            )
        }
    }

    fun renameCategory(id: DocumentId, newName: String) {
        viewModelScope.launch {
            val result = repository.renameCategory(id, newName.trim())

            result.fold(
                onSuccess = {
                    _events.emit(CategoryEvent.CategorySaved)
                },
                onFailure = { exception ->
                    _events.emit(
                        CategoryEvent.ValidationFailed(
                            exception.message ?: "Failed to rename category"
                        )
                    )
                }
            )
        }
    }

    fun setCategoryArchived(id: DocumentId, archived: Boolean) {
        viewModelScope.launch {
            val result = repository.setCategoryArchived(id, archived)

            result.onFailure { exception ->
                _events.emit(
                    CategoryEvent.ValidationFailed(
                        exception.message ?: "Failed to update category state"
                    )
                )
            }
        }
    }
}
