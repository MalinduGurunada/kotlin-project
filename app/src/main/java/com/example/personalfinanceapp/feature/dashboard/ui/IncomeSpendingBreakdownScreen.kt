package com.example.personalfinanceapp.feature.dashboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.feature.dashboard.analytics.CategoryBreakdownItem
import com.example.personalfinanceapp.feature.dashboard.analytics.CryptoResultState
import com.example.personalfinanceapp.feature.dashboard.analytics.SourceBreakdownItem
import com.example.personalfinanceapp.ui.theme.AccentLime
import com.example.personalfinanceapp.ui.theme.MutedCoral
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted
import java.math.BigDecimal
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Locale

private val SummaryBackground = Color(0xFFFAF8F7)
private val SummaryCardTint = Color(0xFFF7F4F3)
private val OutflowTint = Color(0xFFFFE0DE)
private val SummaryRed = Color(0xFFB82025)
private val IncomePalette = listOf(
    PrimaryDarkGreen,
    Color(0xFF26745A),
    AccentLime,
    Color(0xFFCAD8D2)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeSpendingBreakdownScreen(
    uiState: FinancialSummaryUiState,
    onPeriodSelected: (FinancialSummaryPeriod) -> Unit,
    onBackClicked: () -> Unit
) {
    Scaffold(
        modifier = Modifier.testTag("financial_summary_screen"),
        containerColor = SummaryBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Financial Summaries",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDarkGreen,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Text(
                            text = "‹",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimaryDark
                        )
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("summary_content"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PeriodFilters(uiState, onPeriodSelected)

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.testTag("summary_loading"),
                            color = PrimaryDarkGreen
                        )
                    }
                }

                uiState.errorMessage != null -> {
                    MessageCard(
                        title = "Financial summaries are unavailable",
                        message = uiState.errorMessage
                    )
                }

                else -> {
                    NetCashFlowCard(uiState)
                    IncomeSourcesCard(uiState.analytics.incomeSourceBreakdown)
                    OutflowCard(uiState)
                    CryptoResultCard(uiState)
                    TrendCard(uiState)
                    MessageCard(
                        title = "About these figures",
                        message = "Calculated from income and expense transactions saved for the selected period. Currency conversions use each transaction's stored LKR value."
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PeriodFilters(
    uiState: FinancialSummaryUiState,
    onPeriodSelected: (FinancialSummaryPeriod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryFilterChip(
            label = if (uiState.selectedPeriod == FinancialSummaryPeriod.MONTH &&
                uiState.periodLabel.isNotBlank()
            ) {
                "This Month · ${uiState.periodLabel}"
            } else {
                "This Month"
            },
            selected = uiState.selectedPeriod == FinancialSummaryPeriod.MONTH,
            testTag = "summary_month_filter",
            onClick = { onPeriodSelected(FinancialSummaryPeriod.MONTH) }
        )
        SummaryFilterChip(
            label = if (uiState.selectedPeriod == FinancialSummaryPeriod.QUARTER &&
                uiState.periodLabel.isNotBlank()
            ) {
                uiState.periodLabel
            } else {
                "This Quarter"
            },
            selected = uiState.selectedPeriod == FinancialSummaryPeriod.QUARTER,
            testTag = "summary_quarter_filter",
            onClick = { onPeriodSelected(FinancialSummaryPeriod.QUARTER) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryFilterChip(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        modifier = Modifier.testTag(testTag),
        shape = RoundedCornerShape(24.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFDDEBE5),
            selectedLabelColor = PrimaryDarkGreen,
            containerColor = Color(0xFFECE9E8),
            labelColor = TextSecondaryMuted
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.Transparent,
            selectedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun NetCashFlowCard(uiState: FinancialSummaryUiState) {
    SummaryCard {
        Text(
            text = "NET CASH FLOW",
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            color = TextSecondaryMuted
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = formatLkr(uiState.netCashFlowLkrMinor),
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            color = if (uiState.netCashFlowLkrMinor >= 0L) TextPrimaryDark else SummaryRed
        )
        uiState.netCashFlowMarginPercent?.let { margin ->
            val isNonNegative = margin.signum() >= 0
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = if (isNonNegative) AccentLime else OutflowTint,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Net cash-flow margin $margin%",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isNonNegative) PrimaryDarkGreen else SummaryRed
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                title = "Total income",
                value = formatLkr(uiState.totalIncomeLkrMinor),
                containerColor = PrimaryDarkGreen,
                contentColor = SurfaceWhite,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                title = "Total outflow",
                value = formatLkr(uiState.totalExpenseLkrMinor),
                containerColor = OutflowTint,
                contentColor = SummaryRed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .padding(14.dp)
    ) {
        Text(title, fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            value,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
private fun IncomeSourcesCard(items: List<SourceBreakdownItem>) {
    SummaryCard {
        SectionHeading(
            title = "Income Sources",
            subtitle = "Net receipts and realised crypto results"
        )
        Spacer(modifier = Modifier.height(18.dp))
        if (items.isEmpty()) {
            EmptySection("No income transactions in this period.")
        } else {
            IncomeDonut(items)
            Spacer(modifier = Modifier.height(18.dp))
            items.forEachIndexed { index, item ->
                BreakdownRow(
                    title = prettySourceName(item.sourceName),
                    value = formatLkr(item.totalLkrMinor),
                    percentage = item.percentage,
                    color = IncomePalette[index % IncomePalette.size]
                )
                if (index != items.lastIndex) Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun IncomeDonut(items: List<SourceBreakdownItem>) {
    val leading = items.first()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(154.dp)) {
            val stroke = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Butt)
            var startAngle = -90f
            items.forEachIndexed { index, item ->
                val sweep = item.percentage.coerceAtLeast(0) * 3.6f
                drawArc(
                    color = IncomePalette[index % IncomePalette.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = stroke
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Largest source", fontSize = 11.sp, color = TextSecondaryMuted)
            Text(
                "${leading.percentage}%",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDarkGreen
            )
            Text(prettySourceName(leading.sourceName), fontSize = 11.sp, color = TextPrimaryDark)
        }
    }
}

@Composable
private fun BreakdownRow(
    title: String,
    value: String,
    percentage: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SummaryCardTint)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(13.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryDark
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            Text("$percentage%", fontSize = 11.sp, color = TextSecondaryMuted)
        }
    }
}

@Composable
private fun OutflowCard(uiState: FinancialSummaryUiState) {
    val analytics = uiState.analytics
    SummaryCard {
        SectionHeading(
            title = "Outflow Commitments",
            subtitle = "Spending mix by classification and category"
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Committed ${analytics.committedPercent}%",
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryDarkGreen
            )
            Text(
                "Discretionary ${analytics.discretionaryPercent}%",
                fontSize = 12.sp,
                color = TextSecondaryMuted
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        LinearProgressIndicator(
            progress = { analytics.committedPercent.coerceIn(0, 100) / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(10.dp)),
            color = PrimaryDarkGreen,
            trackColor = AccentLime
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AmountClassTile(
                label = "Committed",
                value = analytics.totalCommittedLkrMinor,
                modifier = Modifier.weight(1f)
            )
            AmountClassTile(
                label = "Discretionary",
                value = analytics.totalDiscretionaryLkrMinor,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        if (analytics.expenseCategoryBreakdown.isEmpty()) {
            EmptySection("No expense transactions in this period.")
        } else {
            analytics.expenseCategoryBreakdown.forEachIndexed { index, item ->
                CategoryRow(item)
                if (index != analytics.expenseCategoryBreakdown.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        val change = analytics.monthOverMonthOutflowChangePercent
        Text(
            text = if (uiState.hasPreviousPeriodExpenses) {
                "Outflow ${formatPercentChange(change)} vs ${uiState.comparisonLabel}"
            } else {
                "No ${uiState.comparisonLabel} outflow to compare"
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (change <= 0.0) PrimaryDarkGreen else SummaryRed
        )
    }
}

@Composable
private fun AmountClassTile(label: String, value: Long, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SummaryCardTint)
            .padding(12.dp)
    ) {
        Text(label, fontSize = 11.sp, color = TextSecondaryMuted)
        Spacer(modifier = Modifier.height(7.dp))
        Text(formatLkr(value), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryRow(item: CategoryBreakdownItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SummaryCardTint)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(PrimaryDarkGreen, CircleShape)
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            item.categoryName,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(formatLkr(item.totalLkrMinor), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${item.percentage}%", fontSize = 10.sp, color = TextSecondaryMuted)
        }
    }
}

@Composable
private fun CryptoResultCard(uiState: FinancialSummaryUiState) {
    val result = uiState.analytics.cryptoYtdResult
    SummaryCard(containerColor = PrimaryDarkGreen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "REALISED CRYPTO RESULT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = SurfaceWhite.copy(alpha = 0.75f)
            )
            Surface(color = AccentLime, shape = RoundedCornerShape(8.dp)) {
                Text(
                    result.pnlLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDarkGreen
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (result.state == CryptoResultState.NO_ACTIVITY) {
            Text(
                "No realised crypto activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SurfaceWhite
            )
        } else {
            Text(
                formatLkr(result.netPnlLkrMinor),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = SurfaceWhite
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                "Gains ${formatLkr(result.totalGainLkrMinor)}  ·  Losses ${formatLkr(result.totalLossLkrMinor)}",
                fontSize = 11.sp,
                color = SurfaceWhite.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun TrendCard(uiState: FinancialSummaryUiState) {
    SummaryCard {
        SectionHeading(
            title = "${uiState.periodLabel} Trend",
            subtitle = if (uiState.selectedPeriod == FinancialSummaryPeriod.MONTH) {
                "Weekly income and outflow"
            } else {
                "Monthly income and outflow"
            }
        )
        Spacer(modifier = Modifier.height(20.dp))
        TrendBars(uiState.trendPoints)
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            LegendDot(PrimaryDarkGreen, "Income")
            Spacer(modifier = Modifier.width(18.dp))
            LegendDot(SummaryRed, "Outflow")
        }
    }
}

@Composable
private fun TrendBars(points: List<FinancialTrendPoint>) {
    if (points.isEmpty() || points.all {
            it.incomeLkrMinor == 0L && it.expenseLkrMinor == 0L
        }
    ) {
        EmptySection("No income or expense transactions in this period.")
        return
    }
    val maxAmount = points.maxOf {
        maxOf(financialMagnitude(it.incomeLkrMinor), financialMagnitude(it.expenseLkrMinor))
    }.max(BigInteger.ONE)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val chartHeight = size.height - 8.dp.toPx()
        val groupWidth = size.width / points.size
        val barWidth = minOf(24.dp.toPx(), groupWidth * 0.28f)
        drawLine(
            color = Color(0xFFE2E4E2),
            start = Offset(0f, chartHeight),
            end = Offset(size.width, chartHeight),
            strokeWidth = 1.dp.toPx()
        )
        points.forEachIndexed { index, point ->
            val center = groupWidth * index + groupWidth / 2f
            val incomeHeight = chartHeight *
                (financialMagnitude(point.incomeLkrMinor).toDouble() / maxAmount.toDouble()).toFloat()
            val expenseHeight = chartHeight *
                (financialMagnitude(point.expenseLkrMinor).toDouble() / maxAmount.toDouble()).toFloat()
            drawRoundRect(
                color = if (point.incomeLkrMinor >= 0L) PrimaryDarkGreen else MutedCoral,
                topLeft = Offset(center - barWidth - 3.dp.toPx(), chartHeight - incomeHeight),
                size = Size(barWidth, incomeHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                color = SummaryRed,
                topLeft = Offset(center + 3.dp.toPx(), chartHeight - expenseHeight),
                size = Size(barWidth, expenseHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
        }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        points.forEach { point ->
            Text(
                text = point.label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                color = TextSecondaryMuted
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = TextSecondaryMuted)
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimaryDark
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(subtitle, fontSize = 11.sp, color = TextSecondaryMuted)
}

@Composable
private fun EmptySection(message: String) {
    Text(message, fontSize = 13.sp, color = TextSecondaryMuted)
}

@Composable
private fun MessageCard(title: String, message: String) {
    SummaryCard(containerColor = SummaryCardTint) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
        Spacer(modifier = Modifier.height(5.dp))
        Text(message, fontSize = 12.sp, lineHeight = 18.sp, color = TextSecondaryMuted)
    }
}

@Composable
private fun SummaryCard(
    containerColor: Color = SurfaceWhite,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

private fun formatLkr(amountMinor: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "LKR ${formatter.format(BigDecimal.valueOf(amountMinor, 2))}"
}

private fun formatPercentChange(value: Double): String =
    String.format(Locale.US, "%+.1f%%", value)

internal fun financialMagnitude(value: Long): BigInteger = BigInteger.valueOf(value).abs()

private fun prettySourceName(name: String): String = name
    .lowercase(Locale.US)
    .replaceFirstChar { it.titlecase(Locale.US) }

@Preview(showBackground = true)
@Composable
private fun IncomeSpendingBreakdownScreenPreview() {
    PersonalFinanceAppTheme {
        IncomeSpendingBreakdownScreen(
            uiState = FinancialSummaryUiState(periodLabel = "Current month"),
            onPeriodSelected = {},
            onBackClicked = {}
        )
    }
}
