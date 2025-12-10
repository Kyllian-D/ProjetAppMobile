package com.example.plasmatrack

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.plasmatrack.ui.theme.PlasmaTrackTheme
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.zIndex

class UserPlasmaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Remettre la navigation bar en blanc et demander des icônes sombres (comme dans ClockActivity)
        try {
            window.navigationBarColor = android.graphics.Color.WHITE
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightNavigationBars = true
        } catch (_: Exception) {
            // ignore on older devices
        }
        setContent {
            PlasmaTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    UserPlasmaScreen()
                }
            }
        }
    }
}

@Composable
fun UserPlasmaScreen() {
    val context = LocalContext.current

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Transparent)
    ) {
        // Header : abaisser le titre en le déplaçant vers le bas (120.dp)
        Box(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 55.dp, bottom = 0.dp)) {
            Row(modifier = Modifier
                .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { (context as? androidx.activity.ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() }) {
                    Icon(painter = painterResource(id = R.drawable.arrowleft), contentDescription = "Retour", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "User", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
            }
        }

        // Images centrales (4 boutons image cliquables) - utiliser ButtonCard comme Aqua
        Box(modifier = Modifier.fillMaxSize().padding(top = 200.dp), contentAlignment = Alignment.TopCenter) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {

                ButtonCard(
                    imageRes = R.drawable.btn_manual,
                    contentDesc = "Manual",
                    onClick = {
                        val intent = Intent().apply { setClassName(context.packageName, "com.example.plasmatrack.PlasmaManualActivity") }
                        try {
                            if (context is Activity) context.startActivity(intent) else { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent)
                        }
                    }
                )

                ButtonCard(
                    imageRes = R.drawable.btn_quickguides,
                    contentDesc = "Quick Guides",
                    onClick = {
                        val intent = Intent(context, PlasmaQuickActivity2::class.java)
                        try {
                            if (context is Activity) context.startActivity(intent) else { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    }
                )

                ButtonCard(
                    imageRes = R.drawable.btn_connectiongards,
                    contentDesc = "Connections Cards",
                    onClick = {
                        val intent = Intent(context, PlasmaConnectionCardsActivity::class.java)
                        try {
                            if (context is Activity) context.startActivity(intent) else {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace(); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent)
                        }
                    }
                )

                ButtonCard(
                    imageRes = R.drawable.btn_techmanual,
                    contentDesc = "Tech Manual",
                    onClick = { Toast.makeText(context, "Tech Manual clicked", Toast.LENGTH_SHORT).show() }
                )
            }
        }


        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .align(Alignment.BottomCenter)
                .zIndex(1f),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val intent = Intent(context, HomeActivity::class.java)
                    try {
                        if (context is Activity) context.startActivity(intent) else {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent)
                        }
                    } catch (_: Exception) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent)
                    }
                }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.iconhomeb), contentDescription = "home", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }

                IconButton(onClick = {
                    val intent = Intent(context, ClockActivity::class.java)
                    try {
                        if (context is Activity) context.startActivity(intent) else {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    } catch (_: Exception) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.iconclockb),
                        contentDescription = "clock",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                }

                IconButton(onClick = {
                    val intent = Intent(context, FavoritesActivity::class.java)
                    try {
                        if (context is Activity) context.startActivity(intent) else {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent)
                        }
                    } catch (_: Exception) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent)
                    }
                }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.iconheartb), contentDescription = "heart", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }

                IconButton(onClick = {
                    val intent = Intent(context, ParametersActivity::class.java)
                    try {
                        if (context is Activity) context.startActivity(intent) else {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent)
                        }
                    } catch (_: Exception) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent)
                    }
                }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.iconuserb), contentDescription = "user", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun UserPlasmaPreview() {
    PlasmaTrackTheme {
        UserPlasmaScreen()
    }
}
