package com.example.plasmatrack

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Démarrer immédiatement LoginActivity et terminer MainActivity afin que la connexion soit affichée en premier
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}