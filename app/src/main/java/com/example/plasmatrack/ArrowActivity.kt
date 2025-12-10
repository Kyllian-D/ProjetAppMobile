package com.example.plasmatrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.plasmatrack.ui.theme.PlasmaTrackTheme

class ArrowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlasmaTrackTheme {
                // Ecran d'accueil réutilisé ici
            }
        }
    }
}
