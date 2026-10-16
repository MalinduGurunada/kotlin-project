package com.example.personalfinanceapp.feature.category

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.SpendingClass

@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel = hiltViewModel(),
    onAddClick: () -> Unit = {},
    onRenameClick: (Category) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CategoryScreenContent(
        uiState = uiState,
        onAddClick = onAddClick,
        onRenameClick = onRenameClick,
        onArchiveClick = { category ->
            viewModel.setCategoryArchived(category.id, archived = true)
        },
        onRestoreClick = { category ->
            viewModel.setCategoryArchived(category.id, archived = false)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreenContent(
    uiState: CategoryUiState,
    onAddClick: () -> Unit,
    onRenameClick: (Category) -> Unit,
    onArchiveClick: (Category) -> Unit,
    onRestoreClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    var isArchivedExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (uiState is CategoryUiState.Success) {
                FloatingActionButton(
                    onClick = onAddClick,
                    modifier = Modifier.testTag("category_fab_add")
                ) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    ) { paddingValues ->
        val contentModifier = Modifier.padding(paddingValues)

        when (uiState) {
            is CategoryUiState.Loading -> {
                LoadingStateContent(modifier = contentModifier)
            }

            is CategoryUiState.Error -> {
                ErrorStateContent(
                    message = uiState.message,
                    modifier = contentModifier
                )
            }

            is CategoryUiState.Success -> {
                val hasNoCategories =
                    uiState.activeCategories.isEmpty() && uiState.archivedCategories.isEmpty()

                if (hasNoCategories) {
                    EmptyStateContent(modifier = contentModifier)
                } else {
                    LazyColumn(
                        modifier = contentModifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .testTag("category_list"),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        if (uiState.activeCategories.isNotEmpty()) {
                            item {
                                SectionHeader(title = "ACTIVE CATEGORIES")
                            }
                            items(
                                items = uiState.activeCategories,
                                key = { it.id }
                            ) { category ->
                                ActiveCategoryRow(
                                    category = category,
                                    onRenameClick = { onRenameClick(category) },
                                    onArchiveClick = { onArchiveClick(category) }
                                )
                            }
                        }

                        if (uiState.archivedCategories.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "ARCHIVED CATEGORIES (${uiState.archivedCategories.size})",
                                    isExpanded = isArchivedExpanded,
                                    onToggleExpand = { isArchivedExpanded = !isArchivedExpanded },
                                    modifier = Modifier.testTag("category_archived_header")
                                )
                            }

                            if (isArchivedExpanded) {
                                items(
                                    items = uiState.archivedCategories,
                                    key = { it.id }
                                ) { category ->
                                    ArchivedCategoryRow(
                                        category = category,
                                        onRestoreClick = { onRestoreClick(category) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Previews
@Preview(showBackground = true)
@Composable
fun CategoryScreenLoadingPreview() {
    CategoryScreenContent(
        uiState = CategoryUiState.Loading,
        onAddClick = {},
        onRenameClick = {},
        onArchiveClick = {},
        onRestoreClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun CategoryScreenErrorPreview() {
    CategoryScreenContent(
        uiState = CategoryUiState.Error("Network error occurred"),
        onAddClick = {},
        onRenameClick = {},
        onArchiveClick = {},
        onRestoreClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun CategoryScreenEmptyPreview() {
    CategoryScreenContent(
        uiState = CategoryUiState.Success(
            activeCategories = emptyList(),
            archivedCategories = emptyList()
        ),
        onAddClick = {},
        onRenameClick = {},
        onArchiveClick = {},
        onRestoreClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun CategoryScreenSuccessPreview() {
    CategoryScreenContent(
        uiState = CategoryUiState.Success(
            activeCategories = listOf(
                Category("1", "Food", SpendingClass.DISCRETIONARY),
                Category("2", "Rent", SpendingClass.COMMITTED)
            ),
            archivedCategories = emptyList()
        ),
        onAddClick = {},
        onRenameClick = {},
        onArchiveClick = {},
        onRestoreClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun CategoryScreenWithArchivedPreview() {
    CategoryScreenContent(
        uiState = CategoryUiState.Success(
            activeCategories = listOf(
                Category("1", "Food", SpendingClass.DISCRETIONARY)
            ),
            archivedCategories = listOf(
                Category("2", "Gym", SpendingClass.DISCRETIONARY, archived = true)
            )
        ),
        onAddClick = {},
        onRenameClick = {},
        onArchiveClick = {},
        onRestoreClick = {}
    )
}
