package com.example.plasmatrack

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay


@Composable
fun CountdownBadge(targetMillis: Long, size: Dp = 32.dp, modifier: Modifier = Modifier) {
    // tick every second
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(targetMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val remaining = (targetMillis - now).coerceAtLeast(0L)
    val remainingDays = remaining / (1000L * 60L * 60L * 24L)

    // decide color based on remaining time
    val targetColor = when {
        remaining == 0L -> Color(0xFFB00020) // past due -> red (Material error)
        remainingDays >= 2L && remainingDays <= 7L -> Color(0xFF4CAF50) // green
        remainingDays >= 1L && remainingDays < 2L -> Color(0xFFFFA000) // orange
        remainingDays < 1L -> Color(0xFFB00020) // red for hours
        else -> Color(0xFF4CAF50) // default green
    }

    val animatedColor by animateColorAsState(targetColor)

    // pas de texte — simple indicateur visuel

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(animatedColor),
        contentAlignment = Alignment.Center
    ) {
        // No text inside the badge — pure visual indicator (color only)
    }
}

// Preview helpers (only used in Android Studio preview)
@Composable
fun CountdownBadgePreviewGreen() {
    // 3 days from now
    val threeDays = System.currentTimeMillis() + 3L * 24L * 60L * 60L * 1000L
    CountdownBadge(targetMillis = threeDays)
}

@Composable
fun CountdownBadgePreviewRed() {
    // 5 hours from now
    val fiveHours = System.currentTimeMillis() + 5L * 60L * 60L * 1000L
    CountdownBadge(targetMillis = fiveHours, size = 24.dp)
}
