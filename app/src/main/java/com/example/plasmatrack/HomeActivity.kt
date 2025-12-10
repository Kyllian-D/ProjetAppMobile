package com.example.plasmatrack

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import com.example.plasmatrack.ui.theme.PlasmaTrackTheme
import androidx.core.view.WindowCompat

// nouveaux imports pour gérer les insets
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.remember
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialiser la session en mémoire à partir des extras de l'intent si fournis (chemin de connexion)
        intent?.let { itnt ->
            val r = itnt.getStringExtra("session_role")
            val f = itnt.getStringExtra("session_first")
            if (!r.isNullOrBlank() || !f.isNullOrBlank()) {
                SessionManager.setSession(r, f)
            }
        }
        // Remettre la barre de navigation en blanc et demander des icônes sombres (même comportement que ClockActivity)
        try {
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.WHITE
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
        } catch (_: Exception) { /* ignore on older devices */ }
        setContent {
            PlasmaTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    // Récupérer le prénom/nom sauvegardé pour l'afficher
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    // Si une session en mémoire existe (par exemple superadmin), la privilégier par rapport aux préférences persistantes
    val sessionFirst = SessionManager.firstName
    val displayName = sessionFirst ?: prefs.getString("saved_first", null) ?: "utilisateur"

    // Détecter la hauteur de la barre de navigation (en pixels) et convertir en dp
    val view = LocalView.current
    val bottomPx = remember(view) {
        ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())
            ?.bottom ?: 0
    }
    val navBarBottomDp = with(LocalDensity.current) { bottomPx.toDp() }
    val bottomExtraPadding = if (navBarBottomDp > 0.dp) 8.dp else 0.dp

    // utilise fond blanc
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 60.dp, bottom = 8.dp)
        ) {
            // Ligne supérieure avatar + texte d'accueil
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar circulaire (image de profil)
                    Image(
                        painter = painterResource(id = R.drawable.pp1),
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.size(12.dp))

                    // Colonne contenant le texte "Hi, <prenom>" et le sous-texte
                    Column {
                        Text(text = "Hi, $displayName", color = Color.Black, fontWeight = FontWeight.Bold)
                        Text(text = "Explore the world", color = Color.Gray)
                    }
                }

                // Note zone de droite (top-right) retirée intentionnellement
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Barre de recherche / champ simulé (grand rectangle arrondi)
            // -> modifier .height(...) pour ajuster sa hauteur
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFF5F5F5))) {
            }

            // Icône panier (shopping bag) positionnée à droite sous la barre
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                // IconButton qui ouvre la page ShopActivity
                IconButton(onClick = { activity?.startActivity(Intent(activity, ShopActivity::class.java)) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.shoppingbagb),
                        contentDescription = "Shopping Bag",
                        modifier = Modifier.size(30.dp),
                        tint = Color.Unspecified
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // === Section produit 1 (Aqua) ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .height(180.dp)
                        .align(Alignment.Center)
                        .zIndex(0f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(12.dp)
                ) { }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)   // même largeur que la carte
                        .height(160.dp)        // même hauteur que la carte
                        .align(Alignment.Center)
                        .zIndex(1f)
                        .clickable { activity?.startActivity(Intent(activity, AquaTYPHOONActivity::class.java)) },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.aquamachine),
                        contentDescription = "AquaTYPHOON image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Petit logo / nom sous la photo du produit Aqua
            // -> modifier .height(...) et .fillMaxWidth(...) pour ajuster
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.aquanom),
                    contentDescription = "Aqua logo",
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(0.4f),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Section produit 2 (Plasma) 
            // Structure identique à la section Aqua : plaque blanche + conteneur image cliquable
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .height(180.dp)
                        .align(Alignment.Center)
                        .zIndex(0f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(12.dp)
                ) { }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)   // aligné avec la carte
                        .height(160.dp)
                        .align(Alignment.Center)
                        .zIndex(1f)
                        .clickable { activity?.startActivity(Intent(activity, PlasmaTYPHOONActivity::class.java)) }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.plasmamachine),
                        contentDescription = "PlasmaTYPHOON image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            // Petit logo / nom sous la photo du produit Plasma
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.plasmatyphoonnom),
                    contentDescription = "Plasma logo",
                    modifier = Modifier
                        .height(50.dp)
                        .fillMaxWidth(0.4f),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))


        }

        // Barre d'icônes fixée en bas du Box parent (copié depuis ClockActivity)
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
                    .padding(start = 16.dp, end = 16.dp, bottom = bottomExtraPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activity?.startActivity(Intent(activity, HomeActivity::class.java)) }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.blackiconhomeb), contentDescription = "home", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }

                IconButton(onClick = {
                    val intent = Intent(activity, ClockActivity::class.java)
                    try { activity?.startActivity(intent) } catch (_: Exception) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.iconclockb), contentDescription = "clock", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }

                IconButton(onClick = {
                    val intent = Intent(activity, FavoritesActivity::class.java)
                    try { activity?.startActivity(intent) } catch (_: Exception) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.iconheartb), contentDescription = "heart", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }

                IconButton(onClick = { activity?.startActivity(Intent(activity, ParametersActivity::class.java)) }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.iconuserb), contentDescription = "user", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    PlasmaTrackTheme {
        HomeScreen()
    }
}
