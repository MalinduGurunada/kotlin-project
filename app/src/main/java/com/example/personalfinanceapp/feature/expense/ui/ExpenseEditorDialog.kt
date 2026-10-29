package com.example.personalfinanceapp.feature.expense.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.ui.theme.AccentLime
import com.example.personalfinanceapp.ui.theme.MutedCoral
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class ExpenseEditorInitialValues(
    val amountText: String,
    val currency: CurrencyCode,
    val categoryId: String,
    val paymentChannel: PaymentChannel,
    val spendingClass: SpendingClass,
    val noteText: String,
    val occurredDate: LocalDate
)

internal data class ExpenseEditorPresentation(
    val values: ExpenseEditorInitialValues,
    val errorMessage: String?
)

internal fun presentExpenseEditorError(
    values: ExpenseEditorInitialValues,
    errorMessage: String?
): ExpenseEditorPresentation = ExpenseEditorPresentation(values, errorMessage)

internal fun expenseEditorInitialValues(
    expense: Expense?,
    categories: List<Category>,
    today: LocalDate = LocalDate.now()
): ExpenseEditorInitialValues = ExpenseEditorInitialValues(
    amountText = expense?.money?.originalAmountDecimal.orEmpty(),
    currency = expense?.money?.originalCurrency ?: CurrencyCode.LKR,
    categoryId = expense?.categoryId ?: categories.firstOrNull { !it.archived }?.id.orEmpty(),
    paymentChannel = expense?.paymentChannel ?: PaymentChannel.CASH,
    spendingClass = expense?.spendingClass ?: SpendingClass.DISCRETIONARY,
    noteText = expense?.note.orEmpty(),
    occurredDate = expense?.occurredDate ?: today
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEditorDialog(
    expenseToEdit: Expense?,
    categories: List<Category>,
    errorMessage: String? = null,
    isSaving: Boolean = false,
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
    val initialValues = remember(expenseToEdit?.id) {
        expenseEditorInitialValues(expenseToEdit, categories)
    }
    var amountText by remember(expenseToEdit?.id) {
        mutableStateOf(initialValues.amountText)
    }
    var selectedCurrency by remember(expenseToEdit?.id) {
        mutableStateOf(initialValues.currency)
    }
    var selectedCategoryId by remember(expenseToEdit?.id) {
        mutableStateOf(initialValues.categoryId)
    }
    var selectedChannel by remember(expenseToEdit?.id) {
        mutableStateOf(initialValues.paymentChannel)
    }
    var selectedSpendingClass by remember(expenseToEdit?.id) {
        mutableStateOf(initialValues.spendingClass)
    }
    var noteText by remember(expenseToEdit?.id) {
        mutableStateOf(initialValues.noteText)
    }
    var occurredDate by remember(expenseToEdit?.id) {
        mutableStateOf(initialValues.occurredDate)
    }

    var categoryDropdownOpen by remember { mutableStateOf(false) }
    var datePickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(categories, expenseToEdit?.id) {
        if (selectedCategoryId.isBlank()) {
            selectedCategoryId = categories.firstOrNull { !it.archived }?.id.orEmpty()
        }
    }

    val selectedCategoryName = categories.find { it.id == selectedCategoryId }?.name
        ?: "Select category"
    val presentation = presentExpenseEditorError(
        values = ExpenseEditorInitialValues(
            amountText = amountText,
            currency = selectedCurrency,
            categoryId = selectedCategoryId,
            paymentChannel = selectedChannel,
            spendingClass = selectedSpendingClass,
            noteText = noteText,
            occurredDate = occurredDate
        ),
        errorMessage = errorMessage
    )

    if (datePickerOpen) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = occurredDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { datePickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            occurredDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        datePickerOpen = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerOpen = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF9FAFB)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (expenseToEdit == null) "Add Expense" else "Edit Expense",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = TextPrimaryDark
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // HERO CARD: Emerald Amount Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryDarkGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            // Top Row: EXPENSE AMOUNT + Currency Switcher Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EXPENSE AMOUNT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA5D6A7),
                                    letterSpacing = 1.sp
                                )

                                // Currency Toggle Pill (LKR / USD)
                                Row(
                                    modifier = Modifier
                                        .background(Color(0x33000000), RoundedCornerShape(16.dp))
                                        .padding(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (selectedCurrency == CurrencyCode.LKR) SurfaceWhite else Color.Transparent,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedCurrency = CurrencyCode.LKR }
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "LKR",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedCurrency == CurrencyCode.LKR) PrimaryDarkGreen else SurfaceWhite
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (selectedCurrency == CurrencyCode.USD) SurfaceWhite else Color.Transparent,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedCurrency = CurrencyCode.USD }
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "USD",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedCurrency == CurrencyCode.USD) PrimaryDarkGreen else SurfaceWhite
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Big Display Amount
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val symbol = if (selectedCurrency == CurrencyCode.LKR) "-Rs. " else "-$ "
                                Text(
                                    text = symbol,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF9A9A)
                                )

                                BasicTextField(
                                    value = amountText,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() || it == '.' }) {
                                            amountText = input
                                        }
                                    },
                                    textStyle = TextStyle(
                                        color = SurfaceWhite,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    cursorBrush = SolidColor(AccentLime),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Quick Add Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                QuickAddChip(label = "+ 500") {
                                    val cur = amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                    amountText = (cur + BigDecimal("500")).setScale(2, RoundingMode.HALF_UP).toPlainString()
                                }
                                QuickAddChip(label = "+ 1,000") {
                                    val cur = amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                    amountText = (cur + BigDecimal("1000")).setScale(2, RoundingMode.HALF_UP).toPlainString()
                                }
                                QuickAddChip(label = "+ 5,000") {
                                    val cur = amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                    amountText = (cur + BigDecimal("5000")).setScale(2, RoundingMode.HALF_UP).toPlainString()
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (amountText.isNotEmpty()) {
                                                amountText = amountText.dropLast(1)
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⌫", color = SurfaceWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 1. CATEGORY CARD
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryDropdownOpen = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(SecondaryPaleGreen, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🏷", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Category",
                                    fontSize = 11.sp,
                                    color = TextSecondaryMuted
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = selectedCategoryName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }
                            Text("▾", fontSize = 18.sp, color = TextSecondaryMuted)

                            DropdownMenu(
                                expanded = categoryDropdownOpen,
                                onDismissRequest = { categoryDropdownOpen = false }
                            ) {
                                categories.filter { !it.archived || it.id == selectedCategoryId }.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCategoryId = cat.id
                                            selectedSpendingClass = cat.defaultSpendingClass
                                            categoryDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. DATE CARD
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerOpen = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(SecondaryPaleGreen, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📅", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                text = "Date",
                                    fontSize = 11.sp,
                                    color = TextSecondaryMuted
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val dateStr = occurredDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.US))
                                Text(
                                    text = dateStr,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }
                            Text("🕒", fontSize = 18.sp)
                        }
                    }

                    // 3. PAYMENT METHOD SELECTOR
                    Text(
                        text = "PAYMENT METHOD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryMuted,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PaymentMethodCard(
                            label = "Cash",
                            icon = "💵",
                            isSelected = selectedChannel == PaymentChannel.CASH,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedChannel = PaymentChannel.CASH }
                        )
                        PaymentMethodCard(
                            label = "Card",
                            icon = "💳",
                            isSelected = selectedChannel == PaymentChannel.CARD,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedChannel = PaymentChannel.CARD }
                        )
                        PaymentMethodCard(
                            label = "Account",
                            icon = "🏦",
                            isSelected = selectedChannel == PaymentChannel.BANK,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedChannel = PaymentChannel.BANK }
                        )
                    }

                    // 4. CLASSIFICATION SELECTOR
                    Text(
                        text = "CLASSIFICATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryMuted,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClassificationCard(
                            title = "Committed",
                            subtitle = "Essential expense",
                            isSelected = selectedSpendingClass == SpendingClass.COMMITTED,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedSpendingClass = SpendingClass.COMMITTED }
                        )
                        ClassificationCard(
                            title = "Discretionary",
                            subtitle = "Lifestyle / Wants",
                            isSelected = selectedSpendingClass == SpendingClass.DISCRETIONARY,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedSpendingClass = SpendingClass.DISCRETIONARY }
                        )
                    }

                    // 5. MERCHANT & NOTE
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(SecondaryPaleGreen, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📝", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Merchant & Note",
                                    fontSize = 11.sp,
                                    color = TextSecondaryMuted
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                BasicTextField(
                                    value = noteText,
                                    onValueChange = { noteText = it },
                                    textStyle = TextStyle(
                                        color = TextPrimaryDark,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    presentation.errorMessage?.let { message ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            border = BorderStroke(1.dp, MutedCoral),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = message,
                                color = MutedCoral,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 7. SAVE BUTTON
                    Button(
                        onClick = {
                            onSave(
                                expenseToEdit?.id,
                                selectedCategoryId.ifBlank { categories.firstOrNull()?.id ?: "" },
                                amountText,
                                selectedCurrency,
                                selectedChannel,
                                selectedSpendingClass,
                                occurredDate,
                                noteText
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen),
                        enabled = !isSaving
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("✓", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SurfaceWhite)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSaving) "Saving…" else "Save Expense",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SurfaceWhite
                            )
                        }
                    }

                    // 8. CANCEL BUTTON
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Cancel",
                                fontSize = 15.sp,
                                color = TextSecondaryMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickAddChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = SurfaceWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PaymentMethodCard(
    label: String,
    icon: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SecondaryPaleGreen else SurfaceWhite
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) PrimaryDarkGreen else Color(0xFFE5E7EB)
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) PrimaryDarkGreen else TextPrimaryDark
            )
        }
    }
}

@Composable
private fun ClassificationCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SecondaryPaleGreen else SurfaceWhite
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) PrimaryDarkGreen else Color(0xFFE5E7EB)
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) PrimaryDarkGreen else TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondaryMuted
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(PrimaryDarkGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SurfaceWhite)
                }
            }
        }
    }
}
