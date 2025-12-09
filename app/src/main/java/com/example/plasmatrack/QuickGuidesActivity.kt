package com.example.plasmatrack

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity

// Simple wrapper activity: delegate to AquaQuickActivity2 which contains the Compose UI.
class QuickGuidesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            startActivity(Intent(this, AquaQuickActivity2::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to open Quick Guides: ${e.message}", Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
