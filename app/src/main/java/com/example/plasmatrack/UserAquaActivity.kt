package com.example.plasmatrack

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.plasmatrack.ui.theme.PlasmaTrackTheme
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.zIndex

class UserAquaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Remettre la navigation bar en blanc et demander des icônes sombres
        try {
            window.navigationBarColor = android.graphics.Color.WHITE
            WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightNavigationBars = true
        } catch (_: Exception) {}
        setContent {
            PlasmaTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    UserAquaScreen()
                }
            }
        }
    }
}

@Composable
fun UserAquaScreen() {
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
                IconButton(onClick = { (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() }) {
                    Icon(painter = painterResource(id = R.drawable.arrowleft), contentDescription = "Retour", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "User", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
            }
        }

        // Central images (4 clickable image buttons) - replaced by ButtonCard composable
        Box(modifier = Modifier.fillMaxSize().padding(top = 200.dp), contentAlignment = Alignment.TopCenter) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(30.dp)) {

                ButtonCard(
                    imageRes = R.drawable.btn_manual,
                    contentDesc = "Manual",
                    onClick = {
                        val intent = Intent().apply { setClassName(context.packageName, "com.example.plasmatrack.ManualActivity") }
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
                        val intent = Intent(context, QuickGuidesActivity::class.java)
                        try {
                            if (context is Activity) context.startActivity(intent) else { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // fallback: try launch AquaQuickActivity2 directly
                            try {
                                val i2 = Intent(context, AquaQuickActivity2::class.java)
                                if (context is Activity) context.startActivity(i2) else { i2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(i2) }
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                                Toast.makeText(context, "Unable to open Quick Guides", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                ButtonCard(
                    imageRes = R.drawable.btn_connectiongards,
                    contentDesc = "Connections Cards",
                    onClick = {
                        val intent = Intent(context, ConnectionCardsActivity::class.java)
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
                    onClick = {
                        val intent = Intent().apply { setClassName(context.packageName, "com.example.plasmatrack.TechManualActivity") }
                        try {
                            if (context is Activity) context.startActivity(intent) else { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent)
                        }
                    }
                )
            }
        }

        // Bottom nav (white background with four standard icons)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .align(Alignment.BottomCenter)
                .zIndex(1f),
            color = Color.White,
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

// Reusable card with shadow, rounded corners, and image offset to "remonter" l'image
@Composable
fun ButtonCard(imageRes: Int, contentDesc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .height(92.dp)
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = contentDesc,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .offset(y = (-8).dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun UserAquaPreview() {
    PlasmaTrackTheme {
        UserAquaScreen()
    }
}
