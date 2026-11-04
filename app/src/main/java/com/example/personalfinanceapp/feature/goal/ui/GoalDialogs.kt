package com.example.personalfinanceapp.feature.goal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.ui.theme.MutedCoral
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalDialog(
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, targetAmountLkr: Long, targetDate: LocalDate) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf(LocalDate.now().plusMonths(6)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = targetDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            targetDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = PrimaryDarkGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    val displayError = localError ?: errorMessage

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Savings Goal",
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        localError = null
                    },
                    label = { Text("Goal Name") },
                    placeholder = { Text("e.g. MacBook Pro M3 Fund") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_goal_dialog_name_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() }
                        localError = null
                    },
                    label = { Text("Target Amount (LKR)") },
                    placeholder = { Text("e.g. 550000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_goal_dialog_amount_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetDate.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Target Deadline") },
                    trailingIcon = {
                        Text(
                            text = "📅",
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clickable { showDatePicker = true }
                                .padding(end = 8.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .testTag("create_goal_dialog_date_field")
                )

                if (displayError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("create_goal_dialog_error")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isBlank()) {
                        localError = "Goal name cannot be blank"
                        return@Button
                    }
                    val amount = amountText.toLongOrNull()
                    if (amount == null || amount <= 0L) {
                        localError = "Target amount must be greater than zero"
                        return@Button
                    }
                    if (targetDate.isBefore(LocalDate.now())) {
                        localError = "Target date cannot be in the past"
                        return@Button
                    }
                    onConfirm(trimmedName, amount, targetDate)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryDarkGreen,
                    contentColor = SurfaceWhite
                ),
                modifier = Modifier.testTag("create_goal_dialog_confirm_button")
            ) {
                Text("Create Goal", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("create_goal_dialog_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddContributionDialog(
    goalName: String,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (amountLkr: Long, note: String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val displayError = localError ?: errorMessage

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Savings Allocation",
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Goal: $goalName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryDarkGreen,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() }
                        localError = null
                    },
                    label = { Text("Allocation Amount (LKR)") },
                    placeholder = { Text("e.g. 50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_contribution_dialog_amount_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (Optional)") },
                    placeholder = { Text("e.g. Monthly salary savings allocation") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_contribution_dialog_note_field")
                )

                if (displayError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("add_contribution_dialog_error")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull()
                    if (amount == null || amount <= 0L) {
                        localError = "Contribution amount must be positive"
                        return@Button
                    }
                    onConfirm(amount, noteText.trim().ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryDarkGreen,
                    contentColor = SurfaceWhite
                ),
                modifier = Modifier.testTag("add_contribution_dialog_confirm_button")
            ) {
                Text("Add Allocation", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("add_contribution_dialog_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReverseContributionConfirmationDialog(
    contribution: Contribution,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val amountLkr = Math.abs(contribution.amountMinor) / 100

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reverse Contribution",
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Are you sure you want to reverse this savings allocation?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Amount: LKR ${String.format(Locale.US, "%,d", amountLkr)}",
                    fontWeight = FontWeight.Bold,
                    color = MutedCoral
                )
                contribution.note?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Note: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This will append an exact ledger reversal entry and recalculate your goal progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MutedCoral,
                    contentColor = SurfaceWhite
                ),
                modifier = Modifier.testTag("reverse_contribution_dialog_confirm_button")
            ) {
                Text("Reverse Entry", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("reverse_contribution_dialog_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

