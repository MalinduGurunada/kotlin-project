
package com.example.personalfinanceapp.feature.category

import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.SpendingClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeCategoryRepository : CategoryRepository {

    private val categories = MutableStateFlow(
        listOf(
            Category(
                id = "food",
                name = "Food",
                defaultSpendingClass = SpendingClass.DISCRETIONARY
            ),
            Category(
                id = "transport",
                name = "Transport",
                defaultSpendingClass = SpendingClass.DISCRETIONARY
            ),
            Category(
                id = "bills",
                name = "Bills",
                defaultSpendingClass = SpendingClass.COMMITTED
            )
        )
    )

    private val mutex = Mutex()

    override fun observeCategories(): Flow<List<Category>> {
        return categories.asStateFlow()
    }

    override suspend fun addCategory(
        category: Category
    ): Result<Unit> = mutex.withLock {

        val name = category.name.trim()
        val current = categories.value

        if (category.id.isBlank() || name.isBlank()) {
            return@withLock Result.failure(
                IllegalArgumentException("Category ID and name cannot be blank")
            )
        }

        if (current.any { it.id == category.id }) {
            return@withLock Result.failure(
                IllegalArgumentException("Category ID already exists")
            )
        }

        if (current.any { it.name.equals(name, ignoreCase = true) }) {
            return@withLock Result.failure(
                IllegalArgumentException("Category name already exists")
            )
        }

        categories.value = current + category.copy(name = name)

        Result.success(Unit)
    }

    override suspend fun renameCategory(
        id: DocumentId,
        newName: String
    ): Result<Unit> = mutex.withLock {

        val name = newName.trim()
        val current = categories.value

        if (name.isBlank()) {
            return@withLock Result.failure(
                IllegalArgumentException("Category name cannot be blank")
            )
        }

        if (current.none { it.id == id }) {
            return@withLock Result.failure(
                NoSuchElementException("Category not found")
            )
        }

        if (current.any {
                it.id != id && it.name.equals(name, ignoreCase = true)
            }) {
            return@withLock Result.failure(
                IllegalArgumentException("Category name already exists")
            )
        }

        categories.value = current.map {
            if (it.id == id) it.copy(name = name) else it
        }

        Result.success(Unit)
    }

    override suspend fun setCategoryArchived(
        id: DocumentId,
        archived: Boolean
    ): Result<Unit> = mutex.withLock {

        val current = categories.value

        if (current.none { it.id == id }) {
            return@withLock Result.failure(
                NoSuchElementException("Category not found")
            )
        }

        categories.value = current.map {
            if (it.id == id) it.copy(archived = archived) else it
        }

        Result.success(Unit)
    }
}
