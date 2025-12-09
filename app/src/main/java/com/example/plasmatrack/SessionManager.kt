package com.example.plasmatrack

import android.content.Context

// Simple in-memory session holder. Not persisted across app restarts.
object SessionManager {
    var role: String? = null
    var firstName: String? = null

    fun clear() { role = null; firstName = null }

    fun setSession(r: String?, first: String?) {
        role = r
        firstName = first
    }

    fun isSuperadmin(context: Context): Boolean {
        // Prefer in-memory session
        if (role != null && role.equals("superadmin", ignoreCase = true)) return true
        // Fallback to persisted prefs
        return try {
            val sp = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
            sp.getString("saved_role", "")?.equals("superadmin", ignoreCase = true) == true
        } catch (_: Exception) {
            false
        }
    }
}
