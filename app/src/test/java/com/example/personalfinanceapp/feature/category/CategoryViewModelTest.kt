package com.example.personalfinanceapp.feature.category

import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.category.ui.CategoryEvent
import com.example.personalfinanceapp.feature.category.ui.CategoryUiState
import com.example.personalfinanceapp.feature.category.ui.CategoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeCategoryRepository
    private lateinit var viewModel: CategoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeCategoryRepository()
        viewModel = CategoryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. Initial load: state becomes Success with sample categories, split into active and archived
    @Test
    fun initialLoad_loadsSampleCategories_splitsActiveAndArchivedCorrectly() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state is CategoryUiState.Success)
        val success = state as CategoryUiState.Success
        assertEquals(3, success.activeCategories.size)
        assertTrue(success.archivedCategories.isEmpty())
    }

    // 2. Add valid category (name with extra spaces): trimmed, appears in activeCategories, CategorySaved emitted
    @Test
    fun addCategory_validNameWithExtraSpaces_trimsNameAndEmitsSavedEvent() = runTest {
        val events = mutableListOf<CategoryEvent>()
        val job = backgroundScope.launch(testDispatcher) {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.addCategory("  Shopping  ", SpendingClass.DISCRETIONARY)

        assertEquals(1, events.size)
        assertEquals(CategoryEvent.CategorySaved, events.first())

        val state = viewModel.uiState.value as CategoryUiState.Success
        assertEquals(4, state.activeCategories.size)
        assertTrue(state.activeCategories.any { it.name == "Shopping" })

        job.cancel()
    }

    // 3. Add blank name: ValidationFailed event, list unchanged, state is still Success
    @Test
    fun addCategory_blankName_emitsValidationFailedAndKeepsSuccessState() = runTest {
        val events = mutableListOf<CategoryEvent>()
        val job = backgroundScope.launch(testDispatcher) {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.addCategory("   ", SpendingClass.DISCRETIONARY)

        assertEquals(1, events.size)
        assertTrue(events.first() is CategoryEvent.ValidationFailed)

        val state = viewModel.uiState.value
        assertTrue(state is CategoryUiState.Success)
        val success = state as CategoryUiState.Success
        assertEquals(3, success.activeCategories.size)

        job.cancel()
    }

    // 4. Add duplicate name (same text, different letter case, and with spaces): ValidationFailed, list unchanged
    @Test
    fun addCategory_duplicateNameDifferentCaseAndSpaces_emitsValidationFailed() = runTest {
        val events = mutableListOf<CategoryEvent>()
        val job = backgroundScope.launch(testDispatcher) {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.addCategory("  fOoD  ", SpendingClass.DISCRETIONARY)

        assertEquals(1, events.size)
        val event = events.first() as CategoryEvent.ValidationFailed
        assertEquals("Category name already exists", event.message)

        val state = viewModel.uiState.value as CategoryUiState.Success
        assertEquals(3, state.activeCategories.size)

        job.cancel()
    }

    // 5. The spending class chosen by the user (COMMITTED and DISCRETIONARY) is saved exactly as given
    @Test
    fun addCategory_userChosenSpendingClass_savedExactlyAsGiven() = runTest {
        viewModel.addCategory("Rent", SpendingClass.COMMITTED)

        val state = viewModel.uiState.value as CategoryUiState.Success
        val newCategory = state.activeCategories.first { it.name == "Rent" }
        assertEquals(SpendingClass.COMMITTED, newCategory.defaultSpendingClass)
    }

    // 6. Rename valid: name changes, id stays same, CategorySaved event
    @Test
    fun renameCategory_validNewName_updatesNameAndEmitsSavedEvent() = runTest {
        val events = mutableListOf<CategoryEvent>()
        val job = backgroundScope.launch(testDispatcher) {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.renameCategory("food", "Groceries")

        assertEquals(1, events.size)
        assertEquals(CategoryEvent.CategorySaved, events.first())

        val state = viewModel.uiState.value as CategoryUiState.Success
        val updated = state.activeCategories.first { it.id == "food" }
        assertEquals("Groceries", updated.name)

        job.cancel()
    }

    // 7. Rename to blank, and rename to another category's name: ValidationFailed, original name unchanged
    @Test
    fun renameCategory_blankOrDuplicateName_emitsValidationFailedAndKeepsOriginalName() = runTest {
        val events = mutableListOf<CategoryEvent>()
        val job = backgroundScope.launch(testDispatcher) {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.renameCategory("food", "   ")
        viewModel.renameCategory("food", "Transport")

        assertEquals(2, events.size)
        assertTrue(events[0] is CategoryEvent.ValidationFailed)
        assertTrue(events[1] is CategoryEvent.ValidationFailed)

        val state = viewModel.uiState.value as CategoryUiState.Success
        val category = state.activeCategories.first { it.id == "food" }
        assertEquals("Food", category.name)

        job.cancel()
    }

    // 8. Archive: category moves from active to archived, id is kept
    @Test
    fun setCategoryArchived_archiveCategory_movesToArchivedListWithSameId() = runTest {
        viewModel.setCategoryArchived("food", archived = true)

        val state = viewModel.uiState.value as CategoryUiState.Success
        assertEquals(2, state.activeCategories.size)
        assertEquals(1, state.archivedCategories.size)
        assertEquals("food", state.archivedCategories.first().id)
        assertTrue(state.archivedCategories.first().archived)
    }

    // 9. Restore (archived = false): category returns to active list
    @Test
    fun setCategoryArchived_restoreCategory_returnsToActiveList() = runTest {
        viewModel.setCategoryArchived("food", archived = true)
        viewModel.setCategoryArchived("food", archived = false)

        val state = viewModel.uiState.value as CategoryUiState.Success
        assertEquals(3, state.activeCategories.size)
        assertTrue(state.archivedCategories.isEmpty())
        assertFalse(state.activeCategories.first { it.id == "food" }.archived)
    }

    // 10. Archived categories are NOT in selectableCategories, but are still in archivedCategories with name
    @Test
    fun selectableCategories_excludesArchivedCategories_keepsNameInArchivedCategories() = runTest {
        viewModel.setCategoryArchived("food", archived = true)

        val state = viewModel.uiState.value as CategoryUiState.Success
        assertFalse(state.activeCategories.any { it.id == "food" })
        assertEquals(1, state.archivedCategories.size)
        assertEquals("Food", state.archivedCategories.first().name)
    }

    // 11. A failed action never replaces the list with CategoryUiState.Error
    @Test
    fun failedAction_neverReplacesListWithCategoryUiStateError() = runTest {
        viewModel.addCategory("", SpendingClass.DISCRETIONARY)
        viewModel.renameCategory("food", "")
        viewModel.setCategoryArchived("non-existent-id", true)

        val state = viewModel.uiState.value
        assertTrue("State must remain CategoryUiState.Success", state is CategoryUiState.Success)
    }
}
