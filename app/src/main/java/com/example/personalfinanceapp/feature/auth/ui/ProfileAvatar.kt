package com.example.personalfinanceapp.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite

fun calculateUserInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return "U"
    val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
        parts.size == 1 && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "U"
    }
}

@Composable
fun ProfileAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    containerColor: Color = PrimaryDarkGreen,
    contentColor: Color = SurfaceWhite,
    fontSize: Int = 14,
    onClick: (() -> Unit)? = null
) {
    val initials = calculateUserInitials(name)
    val clickableModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier

    Box(
        modifier = clickableModifier
            .size(size)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = contentColor,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
