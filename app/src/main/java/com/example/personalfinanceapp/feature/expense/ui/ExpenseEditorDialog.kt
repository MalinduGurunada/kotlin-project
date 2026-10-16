package com.example.personalfinanceapp.feature.expense.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.SpendingClass
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEditorDialog(
    expenseToEdit: Expense?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        categoryId: String,
        amountDecimal: String,
        currency: CurrencyCode,
        paymentChannel: PaymentChannel,
        spendingClass: SpendingClass,
        occurredDate: LocalDate,
        note: String?
    ) -> Unit
) {
    var amountText by remember {
        mutableStateOf(expenseToEdit?.money?.originalAmountDecimal ?: "")
    }
    var selectedCurrency by remember {
        mutableStateOf(expenseToEdit?.money?.originalCurrency ?: CurrencyCode.LKR)
    }
    var selectedCategoryId by remember {
        mutableStateOf(expenseToEdit?.categoryId ?: categories.firstOrNull()?.id ?: "")
    }
    var selectedChannel by remember {
        mutableStateOf(expenseToEdit?.paymentChannel ?: PaymentChannel.CASH)
    }
    var selectedSpendingClass by remember {
        mutableStateOf(expenseToEdit?.spendingClass ?: SpendingClass.DISCRETIONARY)
    }
    var noteText by remember {
        mutableStateOf(expenseToEdit?.note ?: "")
    }
    var occurredDate by remember {
        mutableStateOf(expenseToEdit?.occurredDate ?: LocalDate.now())
    }

    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var channelMenuExpanded by remember { mutableStateOf(false) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (expenseToEdit == null) "Add Expense" else "Edit Expense")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Amount & Currency Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    ExposedDropdownMenuBox(
                        expanded = currencyMenuExpanded,
                        onExpandedChange = { currencyMenuExpanded = !currencyMenuExpanded },
                        modifier = Modifier
                            .weight(0.6f)
                            .padding(start = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedCurrency.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenuExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = currencyMenuExpanded,
                            onDismissRequest = { currencyMenuExpanded = false }
                        ) {
                            CurrencyCode.entries.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency.name) },
                                    onClick = {
                                        selectedCurrency = currency
                                        currencyMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentCategoryName = categories.find { it.id == selectedCategoryId }?.name ?: "Select Category"
                    OutlinedTextField(
                        value = currentCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        categories.filter { !it.archived || it.id == selectedCategoryId }.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Channel Dropdown
                ExposedDropdownMenuBox(
                    expanded = channelMenuExpanded,
                    onExpandedChange = { channelMenuExpanded = !channelMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedChannel.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Channel") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = channelMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = channelMenuExpanded,
                        onDismissRequest = { channelMenuExpanded = false }
                    ) {
                        PaymentChannel.entries.forEach { channel ->
                            DropdownMenuItem(
                                text = { Text(channel.name) },
                                onClick = {
                                    selectedChannel = channel
                                    channelMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Spending Class
                Text("Spending Class:", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedSpendingClass == SpendingClass.DISCRETIONARY,
                        onClick = { selectedSpendingClass = SpendingClass.DISCRETIONARY }
                    )
                    Text("Discretionary", style = MaterialTheme.typography.bodySmall)

                    RadioButton(
                        selected = selectedSpendingClass == SpendingClass.COMMITTED,
                        onClick = { selectedSpendingClass = SpendingClass.COMMITTED }
                    )
                    Text("Committed", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        expenseToEdit?.id,
                        selectedCategoryId,
                        amountText,
                        selectedCurrency,
                        selectedChannel,
                        selectedSpendingClass,
                        occurredDate,
                        noteText
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
