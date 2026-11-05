
package com.example.personalfinanceapp.feature.category

import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.SpendingClass
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun observeCategories(): Flow<List<Category>>

    suspend fun addCategory(
        category: Category
    ): Result<Unit>

    suspend fun renameCategory(
        id: DocumentId,
        newName: String
    ): Result<Unit>

    suspend fun updateCategory(
        id: DocumentId,
        newName: String,
        spendingClass: SpendingClass
    ): Result<Unit> = renameCategory(id, newName)

    suspend fun setCategoryArchived(
        id: DocumentId,
        archived: Boolean
    ): Result<Unit>
}
