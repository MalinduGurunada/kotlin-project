package com.example.personalfinanceapp.feature.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.core.model.SpendingClass

@Composable
fun AddEditCategoryDialog(
    title: String,
    initialName: String = "",
    initialSpendingClass: SpendingClass = SpendingClass.DISCRETIONARY,
    isSpendingClassEditable: Boolean = true,
    errorMessage: String? = null,
    onNameChange: () -> Unit,
    onConfirm: (String, SpendingClass) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var spendingClass by rememberSaveable { mutableStateOf(initialSpendingClass) }

    val content = @Composable {
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = name,
                onValueChange = { newText ->
                    name = newText
                    onNameChange()
                },
                label = { Text("Category Name") },
                singleLine = true,
                isError = errorMessage != null,
                supportingText = {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("category_dialog_error")
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_dialog_name_field")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Spending Type",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.selectableGroup()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (spendingClass == SpendingClass.COMMITTED),
                            onClick = {
                                if (isSpendingClassEditable) {
                                    spendingClass = SpendingClass.COMMITTED
                                }
                            },
                            role = Role.RadioButton,
                            enabled = isSpendingClassEditable
                        )
                        .padding(vertical = 8.dp)
                        .testTag("category_dialog_option_committed"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (spendingClass == SpendingClass.COMMITTED),
                        onClick = null,
                        enabled = isSpendingClassEditable
                    )
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    Text(
                        text = "Locked in (rent, bills)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (spendingClass == SpendingClass.DISCRETIONARY),
                            onClick = {
                                if (isSpendingClassEditable) {
                                    spendingClass = SpendingClass.DISCRETIONARY
                                }
                            },
                            role = Role.RadioButton,
                            enabled = isSpendingClassEditable
                        )
                        .padding(vertical = 8.dp)
                        .testTag("category_dialog_option_discretionary"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (spendingClass == SpendingClass.DISCRETIONARY),
                        onClick = null,
                        enabled = isSpendingClassEditable
                    )
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    Text(
                        text = "Your choice (dining, fun)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // In Compose Preview / Inspection mode (LocalInspectionMode.current == true),
    // AlertDialog's internal Dialog window creates a separate AndroidComposeView
    // which triggers an IndexOutOfBoundsException during SnapshotStateObserver.start() in Layoutlib.
    // Rendering dialog content directly within a Surface in inspection mode circumvents
    // creating a secondary window in preview mode while keeping normal runtime behavior.
    if (LocalInspectionMode.current) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.testTag("category_dialog")
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                content()
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.align(Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("category_dialog_cancel")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    TextButton(
                        onClick = { onConfirm(name, spendingClass) },
                        enabled = name.trim().isNotBlank(),
                        modifier = Modifier.testTag("category_dialog_save")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.testTag("category_dialog"),
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = content,
            confirmButton = {
                TextButton(
                    onClick = { onConfirm(name, spendingClass) },
                    enabled = name.trim().isNotBlank(),
                    modifier = Modifier.testTag("category_dialog_save")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("category_dialog_cancel")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview
@Composable
fun AddCategoryDialogPreview() {
    AddEditCategoryDialog(
        title = "Add Category",
        onNameChange = {},
        onConfirm = { _, _ -> },
        onDismiss = {}
    )
}

@Preview
@Composable
fun RenameCategoryDialogPreview() {
    AddEditCategoryDialog(
        title = "Rename Category",
        initialName = "Food",
        isSpendingClassEditable = false,
        onNameChange = {},
        onConfirm = { _, _ -> },
        onDismiss = {}
    )
}

@Preview
@Composable
fun AddCategoryDialogErrorPreview() {
    AddEditCategoryDialog(
        title = "Add Category",
        initialName = "Food",
        errorMessage = "Category name already exists",
        onNameChange = {},
        onConfirm = { _, _ -> },
        onDismiss = {}
    )
}
