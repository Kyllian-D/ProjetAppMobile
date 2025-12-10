package com.example.plasmatrack

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity

// Activité wrapper simple : délègue à AquaQuickActivity2 qui contient l'UI Compose.
class QuickGuidesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            startActivity(Intent(this, AquaQuickActivity2::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "Échec de l'ouverture des guides rapides : ${e.message}", Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
