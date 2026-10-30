package com.example.personalfinanceapp.feature.category.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted
import kotlinx.coroutines.launch

sealed interface CategoryDialogState : java.io.Serializable {
    data object Add : CategoryDialogState
    data class Rename(val categoryId: String, val categoryName: String, val spendingClass: SpendingClass) : CategoryDialogState
}

@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var activeDialog by rememberSaveable { mutableStateOf<CategoryDialogState?>(null) }
    var dialogErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CategoryEvent.CategorySaved -> {
                    activeDialog = null
                    dialogErrorMessage = null
                }
                is CategoryEvent.ValidationFailed -> {
                    if (activeDialog != null) {
                        dialogErrorMessage = event.message
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(event.message)
                        }
                    }
                }
            }
        }
    }

    CategoryScreenContent(
        uiState = uiState,
        activeDialog = activeDialog,
        dialogErrorMessage = dialogErrorMessage,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAddClick = {
            dialogErrorMessage = null
            activeDialog = CategoryDialogState.Add
        },
        onRenameClick = { category ->
            dialogErrorMessage = null
            activeDialog = CategoryDialogState.Rename(
                categoryId = category.id,
                categoryName = category.name,
                spendingClass = category.defaultSpendingClass
            )
        },
        onDismissDialog = {
            activeDialog = null
            dialogErrorMessage = null
        },
        onClearDialogError = {
            dialogErrorMessage = null
        },
        onConfirmAdd = { name, spendingClass ->
            viewModel.addCategory(name, spendingClass)
        },
        onConfirmRename = { categoryId, newName ->
            viewModel.renameCategory(categoryId, newName)
        },
        onArchiveClick = { category ->
            viewModel.setCategoryArchived(category.id, archived = true)
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Archived ${category.name}",
                    actionLabel = "Undo"
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.setCategoryArchived(category.id, archived = false)
                }
            }
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
    modifier: Modifier = Modifier,
    activeDialog: CategoryDialogState? = null,
    dialogErrorMessage: String? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: (() -> Unit)? = null,
    onAddClick: () -> Unit = {},
    onRenameClick: (Category) -> Unit = {},
    onDismissDialog: () -> Unit = {},
    onClearDialogError: () -> Unit = {},
    onConfirmAdd: (String, SpendingClass) -> Unit = { _, _ -> },
    onConfirmRename: (String, String) -> Unit = { _, _ -> },
    onArchiveClick: (Category) -> Unit = {},
    onRestoreClick: (Category) -> Unit = {}
) {
    var isArchivedExpanded by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack
                        ) {
                            Icon(
                                imageVector = CategoryIcons.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite,
                    titleContentColor = TextPrimaryDark,
                    navigationIconContentColor = PrimaryDarkGreen
                )
            )
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

                LazyColumn(
                    modifier = contentModifier
                        .fillMaxSize()
                        .background(CategoryScreenBackground)
                        .padding(horizontal = 20.dp)
                        .testTag("category_list"),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = SecondaryPaleGreen
                                ) {
                                    Text(
                                        text = uiState.activeCategories.size.toString(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryDarkGreen
                                    )
                                }
                            }

                            Button(
                                onClick = onAddClick,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryDarkGreen,
                                    contentColor = SurfaceWhite
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("category_fab_add")
                            ) {
                                Icon(
                                    imageVector = CategoryIcons.Add,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                Text(
                                    text = "Add Category",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (hasNoCategories) {
                        item {
                            EmptyStateContent()
                        }
                    } else {
                        if (uiState.activeCategories.isNotEmpty()) {
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
                        } else {
                            item {
                                Text(
                                    text = "No active categories",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondaryMuted,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }

                        if (uiState.archivedCategories.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.padding(vertical = 8.dp))
                                SectionHeader(
                                    title = "Archived",
                                    count = uiState.archivedCategories.size,
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

        when (val dialog = activeDialog) {
            is CategoryDialogState.Add -> {
                AddEditCategoryDialog(
                    title = "Add Category",
                    errorMessage = dialogErrorMessage,
                    onNameChange = onClearDialogError,
                    onConfirm = { name, spendingClass ->
                        onConfirmAdd(name, spendingClass)
                    },
                    onDismiss = onDismissDialog
                )
            }

            is CategoryDialogState.Rename -> {
                AddEditCategoryDialog(
                    title = "Rename Category",
                    initialName = dialog.categoryName,
                    initialSpendingClass = dialog.spendingClass,
                    isSpendingClassEditable = false,
                    errorMessage = dialogErrorMessage,
                    onNameChange = onClearDialogError,
                    onConfirm = { newName, _ ->
                        onConfirmRename(dialog.categoryId, newName)
                    },
                    onDismiss = onDismissDialog
                )
            }

            null -> {}
        }
    }
}

// Previews
@Preview(showBackground = true)
@Composable
fun CategoryScreenLoadingPreview() {
    CategoryScreenContent(uiState = CategoryUiState.Loading)
}

@Preview(showBackground = true)
@Composable
fun CategoryScreenErrorPreview() {
    CategoryScreenContent(uiState = CategoryUiState.Error("Network error occurred"))
}

@Preview(showBackground = true)
@Composable
fun CategoryScreenEmptyPreview() {
    CategoryScreenContent(
        uiState = CategoryUiState.Success(
            activeCategories = emptyList(),
            archivedCategories = emptyList()
        )
    )
}

@Preview(showBackground = true)
@Composable
fun CategoryScreenSuccessPreview() {
    CategoryScreenContent(
        uiState = CategoryUiState.Success(
            activeCategories = listOf(
                Category("1", "Groceries", SpendingClass.DISCRETIONARY),
                Category("2", "Rent", SpendingClass.COMMITTED)
            ),
            archivedCategories = emptyList()
        )
    )
}

@Preview(showBackground = true)
@Composable
fun CategoryScreenWithArchivedPreview() {
    CategoryScreenContent(
        uiState = CategoryUiState.Success(
            activeCategories = listOf(
                Category("1", "Groceries", SpendingClass.DISCRETIONARY)
            ),
            archivedCategories = listOf(
                Category("2", "Old Gym", SpendingClass.DISCRETIONARY, archived = true)
            )
        )
    )
}

@Preview(showBackground = true)
@Composable
fun CategoryScreenDesignPreview() {
    CategoryScreenContent(
        uiState = CategoryUiState.Success(
            activeCategories = listOf(
                Category("1", "Groceries & Food", SpendingClass.COMMITTED),
                Category("2", "Utilities & Bills", SpendingClass.COMMITTED),
                Category("3", "Transport & Fuel", SpendingClass.COMMITTED),
                Category("4", "Electronics & Software", SpendingClass.DISCRETIONARY),
                Category("5", "Dining & Takeout", SpendingClass.DISCRETIONARY)
            ),
            archivedCategories = listOf(
                Category("6", "Old Gym Membership", SpendingClass.DISCRETIONARY, archived = true),
                Category("7", "Project Alpha Travel", SpendingClass.DISCRETIONARY, archived = true)
            )
        ),
        onBack = {}
    )
}
