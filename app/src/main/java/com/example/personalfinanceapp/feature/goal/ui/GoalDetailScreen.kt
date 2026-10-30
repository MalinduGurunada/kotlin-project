package com.example.personalfinanceapp.feature.goal.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.ContributionKind
import com.example.personalfinanceapp.ui.theme.AccentLime
import com.example.personalfinanceapp.ui.theme.MutedCoral
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goal: GoalItemUiState,
    contributions: List<Contribution>,
    onAddContributionClicked: () -> Unit,
    onReverseContributionClicked: (Contribution) -> Unit,
    onBackClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = goal.name,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Goal Progress Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryDarkGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SAVINGS GOAL PROGRESS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SurfaceWhite.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(AccentLime, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${goal.progressPercent}% Saved",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "LKR ${String.format(Locale.US, "%,d", goal.savedAmountLkr)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = SurfaceWhite
                        )

                        Text(
                            text = "Target: LKR ${String.format(Locale.US, "%,d", goal.targetAmountLkr)} • Deadline: ${goal.targetDate}",
                            fontSize = 12.sp,
                            color = SurfaceWhite.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LinearProgressIndicator(
                            progress = { goal.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = AccentLime,
                            trackColor = Color(0xFF0F6043)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "LKR ${String.format(Locale.US, "%,d", goal.remainingLkr)} remaining",
                                fontSize = 12.sp,
                                color = SurfaceWhite.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "Req: LKR ${String.format(Locale.US, "%,d", goal.requiredMonthlySavingsLkr)}/mo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentLime
                            )
                        }
                    }
                }
            }

            // Action Button
            item {
                Button(
                    onClick = onAddContributionClicked,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen, contentColor = SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("+ Add Savings Allocation", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // Contributions History Header
            item {
                Text(
                    text = "Contribution History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            if (contributions.isEmpty()) {
                item {
                    Text("No allocations or contributions recorded yet.", color = TextSecondaryMuted, fontSize = 13.sp)
                }
            } else {
                items(contributions, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            if (item.kind == ContributionKind.REVERSAL) Color(0xFFFFEBEE) else SecondaryPaleGreen,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (item.kind == ContributionKind.REVERSAL) "↩" else "💰",
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (item.kind == ContributionKind.REVERSAL) "Reversal Entry" else "Savings Allocation",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.occurredDate.toString(),
                                        fontSize = 12.sp,
                                        color = TextSecondaryMuted
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val amountLkr = item.amountMinor / 100
                                Text(
                                    text = "${if (amountLkr >= 0) "+" else ""}LKR ${String.format(Locale.US, "%,d", amountLkr)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (amountLkr < 0) MutedCoral else PrimaryDarkGreen
                                )
                                if (item.kind != ContributionKind.REVERSAL) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Reverse",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedCoral,
                                        modifier = Modifier.clickable { onReverseContributionClicked(item) }
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

@Preview(showBackground = true)
@Composable
fun GoalDetailScreenPreview() {
    PersonalFinanceAppTheme {
        GoalDetailScreen(
            goal = GoalItemUiState(
                id = "goal-m3",
                name = "MacBook Pro M3 Fund",
                targetAmountLkr = 550_000,
                savedAmountLkr = 385_000,
                remainingLkr = 165_000,
                progressPercent = 70,
                isCompleted = false,
                isOverdue = false,
                requiredMonthlySavingsLkr = 41_250,
                startDate = LocalDate.of(2025, 1, 1),
                targetDate = LocalDate.of(2025, 6, 30)
            ),
            contributions = emptyList(),
            onAddContributionClicked = {},
            onReverseContributionClicked = {},
            onBackClicked = {}
        )
    }
}
