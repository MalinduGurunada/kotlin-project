
package com.example.personalfinanceapp.feature.category

import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.DocumentId
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

    suspend fun setCategoryArchived(
        id: DocumentId,
        archived: Boolean
    ): Result<Unit>
}
