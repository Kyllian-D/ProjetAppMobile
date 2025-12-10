package com.example.plasmatrack

import android.content.Context

// Simple stockage de session en mémoire. Non conservé lors du redémarrage de l'application.
object SessionManager {
    var role: String? = null
    var firstName: String? = null

    fun clear() { role = null; firstName = null }

    fun setSession(r: String?, first: String?) {
        role = r
        firstName = first
    }

    fun isSuperadmin(context: Context): Boolean {
        // Préférer la session en mémoire
        if (role != null && role.equals("superadmin", ignoreCase = true)) return true
        // Se replier sur les préférences sauvegardées
        return try {
            val sp = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
            sp.getString("saved_role", "")?.equals("superadmin", ignoreCase = true) == true
        } catch (_: Exception) {
            false
        }
    }
}
