package com.example.plasmatrack

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.core.view.WindowCompat
import androidx.core.content.edit
import com.example.plasmatrack.ui.theme.PlasmaTrackTheme

class ParametersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            window.navigationBarColor = android.graphics.Color.WHITE
            WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightNavigationBars = true
        } catch (_: Exception) { }
        setContent {
            PlasmaTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ParametersScreen()
                }
            }
        }
    }
}

@Composable
fun ParametersScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    val user = prefs.getString("saved_user", "") ?: ""
    val email = prefs.getString("saved_email", "") ?: ""
    val firstPref = prefs.getString("saved_first", "") ?: ""
    val last = prefs.getString("saved_last", "") ?: ""
    val rolePref = prefs.getString("saved_role", "") ?: ""

    // Prefer in-memory SessionManager values when present (e.g. recent login), otherwise fallback to persisted prefs
    val displayFirst = SessionManager.firstName ?: firstPref
    val displayRole = SessionManager.role ?: rolePref
    val displayName = if (displayFirst.isNotBlank()) displayFirst else (user.ifBlank { "user" })

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            // Header with back button and title
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { activity?.finish() }) {
                    Icon(painter = painterResource(id = R.drawable.arrowleft), contentDescription = "Back", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Settings", style = MaterialTheme.typography.titleLarge, color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profile row (picture + name)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.pp1),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(text = displayRole.ifBlank { "" }, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Info card with elevation (shadow)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {
                    ListRow(label = "First name", value = firstPref)
                    ListRow(label = "Last name", value = last)
                    ListRow(label = "Username", value = user)
                    ListRow(label = "Email", value = email)
                    ListRow(label = "Role", value = displayRole)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Logout button
            Button(
                onClick = {
                    // Supprimer uniquement les clés transitoires/de session (NE PAS supprimer les informations de compte enregistrées)
                    val editor = prefs.edit()
                    editor.remove("remember")
                    editor.remove("remember_user")
                    // Ne supprime pas saved_user/saved_pass/saved_first, etc. : conservez le compte
                    editor.apply()
                    SessionManager.clear()
                    Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)
                    activity?.finish()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Logout")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Debug: show stored preferences (auth)
            Button(onClick = {
                try {
                    val sb = StringBuilder()
                    sb.append("saved_user=${prefs.getString("saved_user", null)}\n")
                    sb.append("saved_email=${prefs.getString("saved_email", null)}\n")
                    sb.append("saved_first=${prefs.getString("saved_first", null)}\n")
                    sb.append("saved_last=${prefs.getString("saved_last", null)}\n")
                    sb.append("saved_role=${prefs.getString("saved_role", null)}\n")
                    sb.append("remember=${prefs.getBoolean("remember", false)}\n")
                    sb.append("remember_user=${prefs.getString("remember_user", null)}\n")
                    android.app.AlertDialog.Builder(context)
                        .setTitle("Auth prefs")
                        .setMessage(sb.toString())
                        .setPositiveButton("OK", null)
                        .show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to read prefs", Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(text = "Show prefs (debug)") }

            // Spacer to leave room for bottom bar
            Spacer(modifier = Modifier.height(96.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.draw2),
                    contentDescription = "Image draw",
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .heightIn(max = 120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            }
        }

        // Barre d'icônes en bas (fixe) — remplacée par Surface + navigationBarsPadding pour plus de cohérence
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
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
                IconButton(onClick = { activity?.startActivity(Intent(activity, HomeActivity::class.java)) }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.iconhomeb), contentDescription = "home", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }
                IconButton(onClick = {
                    val intent = Intent(activity, ClockActivity::class.java)
                    try { activity?.startActivity(intent) } catch (_: Exception) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.iconclockb), contentDescription = "clock", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }
                IconButton(onClick = {
                    val intent = Intent(activity, FavoritesActivity::class.java)
                    try {
                        activity?.startActivity(intent)
                    } catch (_: Exception) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.iconheartb), contentDescription = "heart", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }
                IconButton(onClick = { /* user icon - already here */ }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.blackiconuserb), contentDescription = "user", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }
            }
        }
    }
}

@Composable
private fun ListRow(label: String, value: String) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label:", modifier = Modifier.width(100.dp), color = Color.Gray)
        Text(text = value, modifier = Modifier.fillMaxWidth(), color = Color.Black)
    }
}

@Preview(showBackground = true)
@Composable
fun ParametersPreview() {
    PlasmaTrackTheme {
        ParametersScreen()
    }
}
