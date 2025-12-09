package com.example.plasmatrack

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start LoginActivity immediately and finish MainActivity so Login is shown first
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}