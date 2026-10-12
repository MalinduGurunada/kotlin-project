
package com.example.personalfinanceapp.feature.category

import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.SpendingClass
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeCategoryRepositoryTest {

    @Test
    fun initialCategoriesAreAvailable() = runTest {
        val repository = FakeCategoryRepository()

        val categories = repository.observeCategories().first()

        assertEquals(3, categories.size)
    }

    @Test
    fun addCategorySuccessfully() = runTest {
        val repository = FakeCategoryRepository()

        val newCategory = Category(
            id = "health",
            name = "Health",
            defaultSpendingClass = SpendingClass.COMMITTED
        )

        val result = repository.addCategory(newCategory)

        assertTrue(result.isSuccess)

        val categories = repository.observeCategories().first()

        assertEquals(4, categories.size)
        assertTrue(categories.any { it.name == "Health" })
    }

    @Test
    fun rejectBlankCategoryName() = runTest {
        val repository = FakeCategoryRepository()

        val category = Category(
            id = "invalid",
            name = "   ",
            defaultSpendingClass = SpendingClass.DISCRETIONARY
        )

        val result = repository.addCategory(category)

        assertTrue(result.isFailure)
        assertEquals(3, repository.observeCategories().first().size)
    }

    @Test
    fun rejectDuplicateCategoryName() = runTest {
        val repository = FakeCategoryRepository()

        val category = Category(
            id = "food2",
            name = "food",
            defaultSpendingClass = SpendingClass.DISCRETIONARY
        )

        val result = repository.addCategory(category)

        assertTrue(result.isFailure)
    }

    @Test
    fun renameCategorySuccessfully() = runTest {
        val repository = FakeCategoryRepository()

        val result = repository.renameCategory(
            id = "food",
            newName = "Meals"
        )

        assertTrue(result.isSuccess)

        val categories = repository.observeCategories().first()
        val updatedCategory = categories.first { it.id == "food" }

        assertEquals("Meals", updatedCategory.name)
    }

    @Test
    fun archiveCategorySuccessfully() = runTest {
        val repository = FakeCategoryRepository()

        val result = repository.setCategoryArchived(
            id = "food",
            archived = true
        )

        assertTrue(result.isSuccess)

        val categories = repository.observeCategories().first()
        val category = categories.first { it.id == "food" }

        assertTrue(category.archived)
        assertFalse(categories.none { it.id == "food" })
    }
}
