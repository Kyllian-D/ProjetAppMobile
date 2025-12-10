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


// badge de compte à rebours (indicateur visuel uniquement)
@Composable
fun CountdownBadge(targetMillis: Long, size: Dp = 32.dp, modifier: Modifier = Modifier) {
    // mise à jour toutes les secondes
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(targetMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val remaining = (targetMillis - now).coerceAtLeast(0L)
    val remainingDays = remaining / (1000L * 60L * 60L * 24L)

    // déterminer la couleur en fonction du temps restant
    val targetColor = when {
        remaining == 0L -> Color(0xFFB00020)
        remainingDays >= 2L && remainingDays <= 7L -> Color(0xFF4CAF50)
        remainingDays >= 1L && remainingDays < 2L -> Color(0xFFFFA000)
        remainingDays < 1L -> Color(0xFFB00020)
        else -> Color(0xFF4CAF50)
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
        // Aucun texte à l'intérieur du badge — indicateur visuel pur (couleur uniquement)
    }
}

// Prévisualisations (utilisées uniquement dans l'aperçu Android Studio)
@Composable
fun CountdownBadgePreviewGreen() {
    // 3 jours à partir de maintenant
    val threeDays = System.currentTimeMillis() + 3L * 24L * 60L * 60L * 1000L
    CountdownBadge(targetMillis = threeDays)
}

@Composable
fun CountdownBadgePreviewRed() {
    // 5 heures à partir de maintenant
    val fiveHours = System.currentTimeMillis() + 5L * 60L * 60L * 1000L
    CountdownBadge(targetMillis = fiveHours, size = 24.dp)
}
