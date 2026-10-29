package com.example.personalfinanceapp.feature.dashboard.ui

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.ui.theme.MutedCoral
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted

private val MoreBackground = Color(0xFFF8F9FA)
private val DividerColor = Color(0xFFE9ECEA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    userName: String,
    onCategoriesClick: () -> Unit,
    onExpensesClick: () -> Unit,
    onIncomeClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onSignOut: () -> Unit,
    onBottomDestinationSelected: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = userName.ifBlank { "User" }
    val profileInitial = displayName.first().uppercase()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("more_screen"),
        containerColor = MoreBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MoreIconBadge(
                            icon = MoreIcons.Apps,
                            contentDescription = null,
                            size = 38.dp
                        )
                        Text(
                            text = "More / Preferences",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(40.dp),
                        shape = CircleShape,
                        color = PrimaryDarkGreen
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = profileInitial,
                                color = SurfaceWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        bottomBar = {
            AppBottomNavigation(
                currentDestination = NavDestination.MORE,
                onDestinationSelected = onBottomDestinationSelected
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .testTag("more_list"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                ProfileSummaryCard(displayName = displayName, profileInitial = profileInitial)
            }

            item {
                MoreSection(
                    title = "Core app management",
                    items = listOf(
                        MoreItem(
                            title = "Categories",
                            subtitle = "Manage active and archived spending categories",
                            icon = MoreIcons.Categories,
                            testTag = "more_categories",
                            onClick = onCategoriesClick
                        ),
                        MoreItem(
                            title = "Financial Summaries",
                            subtitle = "Monthly cash-flow reports and breakdowns",
                            icon = MoreIcons.Summary,
                            status = "Coming soon"
                        ),
                        MoreItem(
                            title = "Recurring Commitments",
                            subtitle = "Bills, utilities, loans and periodic obligations",
                            icon = MoreIcons.Recurring,
                            status = "Coming soon"
                        ),
                        MoreItem(
                            title = "Due Items & Reminders",
                            subtitle = "Upcoming payments and reminder controls",
                            icon = MoreIcons.Reminder,
                            status = "Coming soon"
                        )
                    )
                )
            }

            item {
                MoreSection(
                    title = "Display & regional preferences",
                    items = listOf(
                        MoreItem(
                            title = "Primary Accounting Currency",
                            subtitle = "Secondary indicator for converted amounts",
                            icon = MoreIcons.Currency,
                            status = "LKR"
                        ),
                        MoreItem(
                            title = "Fiscal Year Cycle",
                            subtitle = "Sri Lanka IRD assessment period",
                            icon = MoreIcons.Calendar,
                            status = "01 Apr – 31 Mar"
                        ),
                        MoreItem(
                            title = "Theme & Appearance",
                            subtitle = "Follow the device light or dark theme",
                            icon = MoreIcons.Theme,
                            status = "System"
                        ),
                        MoreItem(
                            title = "Number Formatting",
                            subtitle = "Display: LKR 284,500.00",
                            icon = MoreIcons.Number,
                            status = "Standard"
                        ),
                        MoreItem(
                            title = "Start of Week",
                            subtitle = "Used for weekly ledger grouping",
                            icon = MoreIcons.Calendar,
                            status = "Monday"
                        )
                    )
                )
            }

            item {
                MoreSection(
                    title = "Finance tools",
                    items = listOf(
                        MoreItem(
                            title = "Expenses",
                            subtitle = "Review and manage recorded expenses",
                            icon = MoreIcons.Expenses,
                            testTag = "more_expenses",
                            onClick = onExpensesClick
                        ),
                        MoreItem(
                            title = "Income",
                            subtitle = "Review income and historical currency values",
                            icon = MoreIcons.Income,
                            testTag = "more_income",
                            onClick = onIncomeClick
                        ),
                        MoreItem(
                            title = "Receivables",
                            subtitle = "Track outstanding and partially received payments",
                            icon = MoreIcons.Receivables,
                            testTag = "more_receivables",
                            onClick = onReceivablesClick
                        )
                    )
                )
            }

            item {
                MoreSection(
                    title = "Data, privacy & vault",
                    items = listOf(
                        MoreItem(
                            title = "Protected local data",
                            subtitle = "Financial data remains scoped to the signed-in user",
                            icon = MoreIcons.Shield,
                            status = "Protected"
                        )
                    )
                )
            }

            item {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("more_sign_out")
                ) {
                    Icon(
                        imageVector = MoreIcons.SignOut,
                        contentDescription = null,
                        tint = MutedCoral
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Out",
                        color = MutedCoral,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProfileSummaryCard(
    displayName: String,
    profileInitial: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryDarkGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = SecondaryPaleGreen
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = profileInitial,
                            style = MaterialTheme.typography.titleLarge,
                            color = PrimaryDarkGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = SurfaceWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Personal finance profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = SurfaceWhite.copy(alpha = 0.78f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = SecondaryPaleGreen.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "PERSONAL",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = SurfaceWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = MoreIcons.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = SecondaryPaleGreen
                    )
                    Column {
                        Text(
                            text = "Local data protected",
                            style = MaterialTheme.typography.labelLarge,
                            color = SurfaceWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Signed-in user scope",
                            style = MaterialTheme.typography.labelSmall,
                            color = SurfaceWhite.copy(alpha = 0.72f)
                        )
                    }
                }
            }
        }
    }
}

private data class MoreItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val status: String? = null,
    val testTag: String? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
private fun MoreSection(
    title: String,
    items: List<MoreItem>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondaryMuted,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            items.forEachIndexed { index, item ->
                MoreMenuRow(item = item)
                if (index != items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 76.dp),
                        color = DividerColor
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreMenuRow(item: MoreItem) {
    val interactionModifier = if (item.onClick != null) {
        Modifier.clickable(
            role = Role.Button,
            onClick = item.onClick
        )
    } else {
        Modifier.semantics { disabled() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(interactionModifier)
            .then(if (item.testTag != null) Modifier.testTag(item.testTag) else Modifier)
            .heightIn(min = 76.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MoreIconBadge(
            icon = item.icon,
            contentDescription = null
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimaryDark,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryMuted
            )
        }

        if (item.status != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (item.status == "Coming soon") {
                    MoreBackground
                } else {
                    SecondaryPaleGreen
                }
            ) {
                Text(
                    text = item.status,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.status == "Coming soon") {
                        TextSecondaryMuted
                    } else {
                        PrimaryDarkGreen
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = TextSecondaryMuted
            )
        }
    }
}

@Composable
private fun MoreIconBadge(
    icon: ImageVector,
    contentDescription: String?,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(SecondaryPaleGreen, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            tint = PrimaryDarkGreen
        )
    }
}

private object MoreIcons {
    val Apps = moreIcon("Apps") {
        moveTo(4f, 4f); lineTo(10f, 4f); lineTo(10f, 10f); lineTo(4f, 10f); close()
        moveTo(14f, 4f); lineTo(20f, 4f); lineTo(20f, 10f); lineTo(14f, 10f); close()
        moveTo(4f, 14f); lineTo(10f, 14f); lineTo(10f, 20f); lineTo(4f, 20f); close()
        moveTo(14f, 14f); lineTo(20f, 14f); lineTo(20f, 20f); lineTo(14f, 20f); close()
    }

    val Categories = moreIcon("Categories") {
        moveTo(3f, 5f); lineTo(10f, 5f); lineTo(10f, 12f); lineTo(3f, 12f); close()
        moveTo(14f, 5f); lineTo(21f, 5f); lineTo(21f, 12f); lineTo(14f, 12f); close()
        moveTo(3f, 16f); lineTo(10f, 16f); lineTo(10f, 21f); lineTo(3f, 21f); close()
        moveTo(14f, 16f); lineTo(21f, 16f); lineTo(21f, 21f); lineTo(14f, 21f); close()
    }

    val Summary = moreIcon("Summary") {
        moveTo(4f, 19f); lineTo(4f, 11f); lineTo(8f, 11f); lineTo(8f, 19f); close()
        moveTo(10f, 19f); lineTo(10f, 5f); lineTo(14f, 5f); lineTo(14f, 19f); close()
        moveTo(16f, 19f); lineTo(16f, 8f); lineTo(20f, 8f); lineTo(20f, 19f); close()
    }

    val Recurring = moreIcon("Recurring") {
        moveTo(18.7f, 7.3f); curveTo(17.2f, 5.8f, 15.1f, 5f, 13f, 5f)
        curveTo(8.6f, 5f, 5f, 8.6f, 5f, 13f); lineTo(2f, 13f); lineTo(6f, 17f)
        lineTo(10f, 13f); lineTo(7f, 13f); curveTo(7f, 9.7f, 9.7f, 7f, 13f, 7f)
        curveTo(14.6f, 7f, 16.1f, 7.6f, 17.2f, 8.8f); close()
        moveTo(14f, 11f); lineTo(14f, 6f); lineTo(12f, 6f); lineTo(12f, 13f)
        lineTo(17f, 16f); lineTo(18f, 14.3f); close()
    }

    val Reminder = moreIcon("Reminder") {
        moveTo(12f, 3f); curveTo(8.7f, 3f, 6f, 5.7f, 6f, 9f); lineTo(6f, 14f)
        lineTo(4f, 17f); lineTo(20f, 17f); lineTo(18f, 14f); lineTo(18f, 9f)
        curveTo(18f, 5.7f, 15.3f, 3f, 12f, 3f); close()
        moveTo(9f, 19f); curveTo(9.4f, 20.2f, 10.5f, 21f, 12f, 21f)
        curveTo(13.5f, 21f, 14.6f, 20.2f, 15f, 19f); close()
    }

    val Currency = moreIcon("Currency") {
        moveTo(3f, 6f); lineTo(21f, 6f); lineTo(21f, 18f); lineTo(3f, 18f); close()
        moveTo(6f, 9f); lineTo(13f, 9f); lineTo(13f, 11f); lineTo(6f, 11f); close()
        moveTo(6f, 13f); lineTo(10f, 13f); lineTo(10f, 15f); lineTo(6f, 15f); close()
    }

    val Calendar = moreIcon("Calendar") {
        moveTo(5f, 4f); lineTo(7f, 4f); lineTo(7f, 2f); lineTo(9f, 2f); lineTo(9f, 4f)
        lineTo(15f, 4f); lineTo(15f, 2f); lineTo(17f, 2f); lineTo(17f, 4f)
        lineTo(19f, 4f); lineTo(19f, 21f); lineTo(5f, 21f); close()
        moveTo(7f, 9f); lineTo(17f, 9f); lineTo(17f, 19f); lineTo(7f, 19f); close()
    }

    val Theme = moreIcon("Theme") {
        moveTo(12f, 3f); curveTo(7f, 3f, 3f, 6.6f, 3f, 11f)
        curveTo(3f, 15.4f, 7f, 19f, 12f, 19f); lineTo(13.5f, 19f)
        curveTo(14.9f, 19f, 16f, 17.9f, 16f, 16.5f)
        curveTo(16f, 15.1f, 14.9f, 14f, 13.5f, 14f); lineTo(12f, 14f)
        curveTo(10.9f, 14f, 10f, 13.1f, 10f, 12f)
        curveTo(10f, 10.9f, 10.9f, 10f, 12f, 10f); lineTo(16f, 10f)
        curveTo(18.8f, 10f, 21f, 8.5f, 21f, 6.5f); curveTo(21f, 4.6f, 17f, 3f, 12f, 3f); close()
    }

    val Number = moreIcon("Number") {
        moveTo(5f, 5f); lineTo(19f, 5f); lineTo(19f, 8f); lineTo(5f, 8f); close()
        moveTo(5f, 10.5f); lineTo(19f, 10.5f); lineTo(19f, 13.5f); lineTo(5f, 13.5f); close()
        moveTo(5f, 16f); lineTo(19f, 16f); lineTo(19f, 19f); lineTo(5f, 19f); close()
    }

    val Expenses = moreIcon("Expenses") {
        moveTo(5f, 3f); lineTo(19f, 3f); lineTo(19f, 21f); lineTo(16f, 19f)
        lineTo(13f, 21f); lineTo(10f, 19f); lineTo(7f, 21f); lineTo(5f, 19f); close()
        moveTo(8f, 7f); lineTo(16f, 7f); lineTo(16f, 9f); lineTo(8f, 9f); close()
        moveTo(8f, 12f); lineTo(16f, 12f); lineTo(16f, 14f); lineTo(8f, 14f); close()
    }

    val Income = moreIcon("Income") {
        moveTo(4f, 19f); lineTo(4f, 15f); lineTo(8f, 15f); lineTo(8f, 11f)
        lineTo(12f, 11f); lineTo(12f, 7f); lineTo(9f, 7f); lineTo(14f, 2f)
        lineTo(19f, 7f); lineTo(16f, 7f); lineTo(16f, 15f); lineTo(20f, 15f)
        lineTo(20f, 19f); close()
    }

    val Receivables = moreIcon("Receivables") {
        moveTo(3f, 6f); lineTo(21f, 6f); lineTo(21f, 19f); lineTo(3f, 19f); close()
        moveTo(5f, 8f); lineTo(19f, 8f); lineTo(19f, 10f); lineTo(5f, 10f); close()
        moveTo(15f, 13f); lineTo(19f, 13f); lineTo(19f, 17f); lineTo(15f, 17f); close()
    }

    val Shield = moreIcon("Shield") {
        moveTo(12f, 2f); lineTo(20f, 5f); lineTo(20f, 11f)
        curveTo(20f, 16f, 16.6f, 20.2f, 12f, 22f)
        curveTo(7.4f, 20.2f, 4f, 16f, 4f, 11f); lineTo(4f, 5f); close()
        moveTo(8f, 11f); lineTo(11f, 14f); lineTo(16f, 8f); lineTo(17.5f, 9.3f)
        lineTo(11.2f, 17f); lineTo(6.5f, 12.3f); close()
    }

    val SignOut = moreIcon("SignOut") {
        moveTo(4f, 3f); lineTo(13f, 3f); lineTo(13f, 5f); lineTo(6f, 5f)
        lineTo(6f, 19f); lineTo(13f, 19f); lineTo(13f, 21f); lineTo(4f, 21f); close()
        moveTo(13f, 8f); lineTo(17f, 11f); lineTo(9f, 11f); lineTo(9f, 13f)
        lineTo(17f, 13f); lineTo(13f, 16f); lineTo(14.4f, 17.4f); lineTo(21f, 12f)
        lineTo(14.4f, 6.6f); close()
    }
}

private fun moreIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black), pathBuilder = block)
    }.build()

@Preview(showBackground = true)
@Composable
private fun MoreScreenPreview() {
    PersonalFinanceAppTheme(dynamicColor = false) {
        MoreScreen(
            userName = "Himasha Perera",
            onCategoriesClick = {},
            onExpensesClick = {},
            onIncomeClick = {},
            onReceivablesClick = {},
            onSignOut = {},
            onBottomDestinationSelected = {}
        )
    }
}
