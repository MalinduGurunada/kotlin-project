package com.example.personalfinanceapp.feature.onboarding.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun OnboardingScreen(
    gateState: OnboardingGateState,
    uiState: OnboardingUiState,
    onIncomeSourceToggled: (IncomeSource) -> Unit,
    onSalaryPaydayChanged: (String) -> Unit,
    onExpectedMonthlyIncomeChanged: (String) -> Unit,
    onExpectedMonthlySpendingChanged: (String) -> Unit,
    onIncludeFirstGoalChanged: (Boolean) -> Unit,
    onGoalNameChanged: (String) -> Unit,
    onGoalTargetChanged: (String) -> Unit,
    onGoalSavedChanged: (String) -> Unit,
    onGoalTargetDateChanged: (LocalDate) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onRetryAccess: () -> Unit,
    onSignOut: () -> Unit
) {
    when (gateState) {
        OnboardingGateState.Resolving -> OnboardingLoadingScreen()
        is OnboardingGateState.Error -> OnboardingErrorScreen(
            message = gateState.message,
            onRetry = onRetryAccess,
            onSignOut = onSignOut
        )
        OnboardingGateState.Complete -> OnboardingLoadingScreen()
        OnboardingGateState.Required -> OnboardingContent(
            uiState = uiState,
            onIncomeSourceToggled = onIncomeSourceToggled,
            onSalaryPaydayChanged = onSalaryPaydayChanged,
            onExpectedMonthlyIncomeChanged = onExpectedMonthlyIncomeChanged,
            onExpectedMonthlySpendingChanged = onExpectedMonthlySpendingChanged,
            onIncludeFirstGoalChanged = onIncludeFirstGoalChanged,
            onGoalNameChanged = onGoalNameChanged,
            onGoalTargetChanged = onGoalTargetChanged,
            onGoalSavedChanged = onGoalSavedChanged,
            onGoalTargetDateChanged = onGoalTargetDateChanged,
            onBack = onBack,
            onNext = onNext,
            onFinish = onFinish,
            onSignOut = onSignOut
        )
    }
}

@Composable
private fun OnboardingLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().testTag("onboarding_loading"),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PrimaryDarkGreen)
    }
}

@Composable
private fun OnboardingErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).testTag("onboarding_access_error"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Setup unavailable", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry, modifier = Modifier.testTag("onboarding_retry")) {
            Text("Try again")
        }
        TextButton(onClick = onSignOut) { Text("Sign out") }
    }
}

@Composable
private fun OnboardingContent(
    uiState: OnboardingUiState,
    onIncomeSourceToggled: (IncomeSource) -> Unit,
    onSalaryPaydayChanged: (String) -> Unit,
    onExpectedMonthlyIncomeChanged: (String) -> Unit,
    onExpectedMonthlySpendingChanged: (String) -> Unit,
    onIncludeFirstGoalChanged: (Boolean) -> Unit,
    onGoalNameChanged: (String) -> Unit,
    onGoalTargetChanged: (String) -> Unit,
    onGoalSavedChanged: (String) -> Unit,
    onGoalTargetDateChanged: (LocalDate) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onSignOut: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .testTag("onboarding_screen")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("FinClarity setup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = onSignOut, enabled = !uiState.isSaving) { Text("Sign out") }
            }
            Text(
                "Step ${uiState.step + 1} of ${LAST_ONBOARDING_STEP + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (uiState.step + 1f) / (LAST_ONBOARDING_STEP + 1f) },
                modifier = Modifier.fillMaxWidth().testTag("onboarding_progress"),
                color = PrimaryDarkGreen
            )
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 20.dp)
            ) {
                when (uiState.step) {
                    0 -> CurrencyStep()
                    1 -> IncomeSourcesStep(uiState, onIncomeSourceToggled, onSalaryPaydayChanged)
                    2 -> FinancialBaselineStep(
                        uiState,
                        onExpectedMonthlyIncomeChanged,
                        onExpectedMonthlySpendingChanged
                    )
                    else -> GoalAndReviewStep(
                        uiState,
                        onIncludeFirstGoalChanged,
                        onGoalNameChanged,
                        onGoalTargetChanged,
                        onGoalSavedChanged,
                        onGoalTargetDateChanged
                    )
                }
                uiState.errorMessage?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("onboarding_error")
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!uiState.isFirstStep) {
                    OutlinedButton(
                        onClick = onBack,
                        enabled = !uiState.isSaving && !uiState.submissionLocked,
                        modifier = Modifier.weight(1f).testTag("onboarding_back")
                    ) { Text("Back") }
                }
                Button(
                    onClick = if (uiState.isLastStep) onFinish else onNext,
                    enabled = !uiState.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen),
                    modifier = Modifier.weight(1f).testTag("onboarding_primary_action")
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (uiState.isLastStep) "Finish setup" else "Continue")
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeading(title: String, explanation: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun CurrencyStep() {
    StepHeading(
        "Choose your reporting currency",
        "FinClarity uses one base currency to calculate dashboard totals and comparisons."
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("onboarding_currency_lkr")
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("LKR — Sri Lankan Rupee", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Selected reporting currency. Foreign transactions retain their original currency and exchange-rate snapshot.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun IncomeSourcesStep(
    uiState: OnboardingUiState,
    onIncomeSourceToggled: (IncomeSource) -> Unit,
    onSalaryPaydayChanged: (String) -> Unit
) {
    StepHeading(
        "How do you earn?",
        "Select every income source you normally use. This configures your profile; it does not create transactions."
    )
    IncomeSource.entries.forEach { source ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !uiState.submissionLocked) { onIncomeSourceToggled(source) }
                .padding(vertical = 6.dp)
                .testTag("onboarding_income_${source.name.lowercase()}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = source in uiState.selectedIncomeSources,
                onCheckedChange = { onIncomeSourceToggled(source) },
                enabled = !uiState.submissionLocked
            )
            Text(source.displayLabel, fontWeight = FontWeight.Medium)
        }
    }
    if (IncomeSource.SALARY in uiState.selectedIncomeSources) {
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.salaryPaydayInput,
            onValueChange = onSalaryPaydayChanged,
            label = { Text("Usual salary payday") },
            supportingText = { Text("Enter a day from 1 to 31") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("onboarding_salary_payday")
        )
    }
}

@Composable
private fun FinancialBaselineStep(
    uiState: OnboardingUiState,
    onIncomeChanged: (String) -> Unit,
    onSpendingChanged: (String) -> Unit
) {
    StepHeading(
        "Set a monthly baseline",
        "These planning estimates are not transactions. They help compare your expectations with actual results."
    )
    MoneyField(
        value = uiState.expectedMonthlyIncomeInput,
        onValueChange = onIncomeChanged,
        label = "Expected monthly income (LKR)",
        tag = "onboarding_expected_income"
    )
    Spacer(Modifier.height(16.dp))
    MoneyField(
        value = uiState.expectedMonthlySpendingInput,
        onValueChange = onSpendingChanged,
        label = "Expected monthly spending (LKR)",
        tag = "onboarding_expected_spending"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalAndReviewStep(
    uiState: OnboardingUiState,
    onIncludeGoalChanged: (Boolean) -> Unit,
    onGoalNameChanged: (String) -> Unit,
    onGoalTargetChanged: (String) -> Unit,
    onGoalSavedChanged: (String) -> Unit,
    onGoalTargetDateChanged: (LocalDate) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.goalTargetDate
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onGoalTargetDateChanged(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text("Select") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }

    StepHeading(
        "Start with a goal",
        "Your first goal is optional. If you add one, any existing savings become an opening contribution."
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text("Create my first goal", fontWeight = FontWeight.SemiBold)
            Text("You can also create one later.", style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = uiState.includeFirstGoal,
            onCheckedChange = onIncludeGoalChanged,
            enabled = !uiState.submissionLocked,
            modifier = Modifier.testTag("onboarding_goal_toggle")
        )
    }
    if (uiState.includeFirstGoal) {
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.goalNameInput,
            onValueChange = onGoalNameChanged,
            label = { Text("Goal name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("onboarding_goal_name")
        )
        Spacer(Modifier.height(12.dp))
        MoneyField(uiState.goalTargetInput, onGoalTargetChanged, "Target amount (LKR)", "onboarding_goal_target")
        Spacer(Modifier.height(12.dp))
        MoneyField(uiState.goalSavedInput, onGoalSavedChanged, "Already saved (LKR)", "onboarding_goal_saved")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.goalTargetDate.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Target deadline") },
            trailingIcon = {
                TextButton(
                    onClick = { showDatePicker = true },
                    enabled = !uiState.submissionLocked,
                    modifier = Modifier.testTag("onboarding_goal_date_picker")
                ) {
                    Text("Choose")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !uiState.submissionLocked) { showDatePicker = true }
                .testTag("onboarding_goal_date")
        )
    }
    Spacer(Modifier.height(24.dp))
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().testTag("onboarding_review")
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Your setup", fontWeight = FontWeight.Bold)
            Text("Currency: LKR")
            Text("Income sources: ${uiState.selectedIncomeSources.sortedBy { it.name }.joinToString { it.displayLabel }}")
            Text("Expected income: LKR ${uiState.expectedMonthlyIncomeInput}")
            Text("Expected spending: LKR ${uiState.expectedMonthlySpendingInput}")
            Text(if (uiState.includeFirstGoal) "First goal: ${uiState.goalNameInput}" else "First goal: Add later")
        }
    }
}

@Composable
private fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag(tag)
    )
}

private val IncomeSource.displayLabel: String
    get() = when (this) {
        IncomeSource.SALARY -> "Salary"
        IncomeSource.FREELANCE -> "Freelance"
        IncomeSource.ADSENSE -> "AdSense"
        IncomeSource.CRYPTO -> "Cryptocurrency"
    }

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    PersonalFinanceAppTheme(dynamicColor = false) {
        OnboardingScreen(
            gateState = OnboardingGateState.Required,
            uiState = OnboardingUiState(
                step = 3,
                selectedIncomeSources = setOf(IncomeSource.SALARY, IncomeSource.FREELANCE),
                salaryPaydayInput = "25",
                expectedMonthlyIncomeInput = "150000",
                expectedMonthlySpendingInput = "90000"
            ),
            onIncomeSourceToggled = {}, onSalaryPaydayChanged = {},
            onExpectedMonthlyIncomeChanged = {}, onExpectedMonthlySpendingChanged = {},
            onIncludeFirstGoalChanged = {}, onGoalNameChanged = {}, onGoalTargetChanged = {},
            onGoalSavedChanged = {}, onGoalTargetDateChanged = {}, onBack = {}, onNext = {},
            onFinish = {}, onRetryAccess = {}, onSignOut = {}
        )
    }
}
