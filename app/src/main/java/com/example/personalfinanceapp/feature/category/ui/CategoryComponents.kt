package com.example.personalfinanceapp.feature.category.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted

internal val CategoryScreenBackground = Color(0xFFF8F9FA)
private val ArchivedCardBackground = Color(0xFFFDF2F2)
private val ArchivedCardBorder = Color(0xFFEAD8D8)
private val ArchivedIconBackground = Color(0xFFE5D5D5)
private val ArchivedContent = Color(0xFF777777)

@Composable
fun LoadingStateContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("category_loading"),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorStateContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("category_error"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyStateContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("category_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = CategoryIcons.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = PrimaryDarkGreen
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Categories Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap the '+ Add Category' button above to add your first spending category.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActiveCategoryRow(
    category: Category,
    onRenameClick: () -> Unit,
    onArchiveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spendingClassLabel = when (category.defaultSpendingClass) {
        SpendingClass.COMMITTED -> "Locked in"
        SpendingClass.DISCRETIONARY -> "Your choice"
    }

    val categoryIcon = categoryIconFor(category)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("category_row_active_${category.id}"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceWhite
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecondaryPaleGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = PrimaryDarkGreen
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = spendingClassLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryMuted
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onRenameClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = CategoryIcons.Edit,
                        contentDescription = "Edit ${category.name}",
                        tint = PrimaryDarkGreen
                    )
                }
                IconButton(
                    onClick = onArchiveClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = CategoryIcons.Archive,
                        contentDescription = "Archive ${category.name}",
                        tint = PrimaryDarkGreen
                    )
                }
            }
        }
    }
}

@Composable
fun ArchivedCategoryRow(
    category: Category,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryIcon = categoryIconFor(category)

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("category_row_archived_${category.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ArchivedCardBorder),
        colors = CardDefaults.outlinedCardColors(
            containerColor = ArchivedCardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ArchivedIconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = ArchivedContent
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ArchivedContent
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Archived",
                        style = MaterialTheme.typography.bodySmall,
                        color = ArchivedContent
                    )
                }
            }

            TextButton(
                onClick = onRestoreClick,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .widthIn(min = 104.dp)
                    .semantics { contentDescription = "Restore ${category.name}" }
            ) {
                Icon(
                    imageVector = CategoryIcons.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = PrimaryDarkGreen
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Restore",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDarkGreen
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    isExpanded: Boolean? = null,
    onToggleExpand: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val headerModifier = if (onToggleExpand != null) {
        modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = if (isExpanded == true) "Collapse archived categories" else "Expand archived categories",
                onClick = onToggleExpand
            )
            .heightIn(min = 48.dp)
            .padding(vertical = 10.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    }

    Row(
        modifier = headerModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            if (count != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SecondaryPaleGreen
                ) {
                    Text(
                        text = count.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDarkGreen
                    )
                }
            }
        }

        if (isExpanded != null && onToggleExpand != null) {
            Icon(
                imageVector = if (isExpanded) CategoryIcons.ExpandLess else CategoryIcons.ExpandMore,
                contentDescription = null,
                tint = TextSecondaryMuted
            )
        }
    }
}
