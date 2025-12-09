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

class AquaTYPHOONActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Remettre la navigation bar en blanc et demander des icônes sombres
        try {
            window.navigationBarColor = android.graphics.Color.WHITE
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightNavigationBars = true
        } catch (_: Exception) {}
        setContent {
            PlasmaTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AquaTYPHOONScreen()
                }
            }
        }
    }
}

@Composable
fun AquaTYPHOONScreen() {
    val context = LocalContext.current

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Transparent) // plus de fond blanc — transparent pour laisser la thème s'appliquer
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
                Text(text = "AquaTYPHOON", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
            }
        }

        // On descend la zone centrale pour qu'elle soit plus bas sur l'écran (décalée pour ne pas chevaucher le header)
        Box(modifier = Modifier.fillMaxSize().padding(top = 200.dp), contentAlignment = Alignment.TopCenter) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Image 1 : sales
                Box(modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(92.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .clickable { Toast.makeText(context, "Sales clicked", Toast.LENGTH_SHORT).show() },
                    contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.btn_sales),
                        contentDescription = "Sales",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Image 2 : technical
                Box(modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(92.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .clickable { Toast.makeText(context, "Technical clicked", Toast.LENGTH_SHORT).show() },
                    contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.btn_technical),
                        contentDescription = "Technical",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Image 3 : user
                Box(modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(92.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .clickable {
                        val intent = Intent(context, UserAquaActivity::class.java)
                        try {
                            if (context is Activity) context.startActivity(intent) else {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent)
                            }
                        } catch (_: Exception) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent)
                        }
                    },
                     contentAlignment = Alignment.Center) {
                     Image(
                         painter = painterResource(id = R.drawable.btn_user),
                         contentDescription = "User",
                         modifier = Modifier.fillMaxSize(),
                         contentScale = ContentScale.Crop,
                         alignment = Alignment.Center
                     )
                 }
            }
        }

        // Bottom nav (same style as ClockActivity)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .align(Alignment.BottomCenter),
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
                // Home -> ouvre HomeActivity
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

                IconButton(onClick = { /* open clock */ }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.iconclockb), contentDescription = "clock", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
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
                    // Open Settings (ParametersActivity) when tapping the user icon
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
private fun AquaTYPHOONPreview() {
    PlasmaTrackTheme {
        AquaTYPHOONScreen()
    }
}
