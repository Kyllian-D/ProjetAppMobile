package com.example.plasmatrack

import java.util.regex.Pattern

/**
 * Utilitaire central pour extraire l'ID YouTube d'une URL.
 * Gardons le nom `extractYoutubeId` pour compatibilité avec le code existant.
 */
fun extractYoutubeId(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return try {
        val patterns = listOf(
            Pattern.compile("v=([A-Za-z0-9_-]{11})"),
            Pattern.compile("v/([A-Za-z0-9_-]{11})"),
            Pattern.compile("youtu\\.be/([A-Za-z0-9_-]{11})"),
            Pattern.compile("embed/([A-Za-z0-9_-]{11})"),
            Pattern.compile("shorts/([A-Za-z0-9_-]{11})"),
            Pattern.compile("watch\\?.*v=([A-Za-z0-9_-]{11})")
        )
        for (p in patterns) {
            val m = p.matcher(url)
            if (m.find()) return m.group(1)
        }
        // repli : renvoie le premier token de 11 caractères s'il est présent
        val fallback = Regex("([A-Za-z0-9_-]{11})").find(url)
        fallback?.value
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
