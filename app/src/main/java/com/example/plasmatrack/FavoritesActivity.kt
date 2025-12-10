package com.example.plasmatrack

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width

import com.example.plasmatrack.ui.theme.PlasmaTrackTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.core.view.WindowCompat

class FavoritesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.WHITE
            // getInsetsController retourne non-null → appeler directement pour éviter le safe-call inutile
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
        } catch (_: Exception) { }
        setContent {
            PlasmaTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FavoritesScreen()
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val products = remember { ProductStorage.loadProducts(context) }
    val favoritesList = remember { androidx.compose.runtime.mutableStateListOf<Long>().apply { addAll(ProductStorage.loadFavorites(context)) } }
    val connFavsList = remember { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(ProductStorage.loadConnFavorites(context)) } }

    val prefs = context.getSharedPreferences("shop_prefs", Context.MODE_PRIVATE)
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "favorites" -> {
                    val new = ProductStorage.loadFavorites(context)
                    favoritesList.clear(); favoritesList.addAll(new)
                }
                "conn_favorites" -> {
                    val new = ProductStorage.loadConnFavorites(context)
                    connFavsList.clear(); connFavsList.addAll(new)
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val aquaItems = remember { loadConnectionCardsFromFile(context) }
    val plasmaItems = remember { loadConnectionCardsFromFile2(context, usePlus = false) }
    val plasmaPlusItems = remember { loadConnectionCardsFromFile2(context, usePlus = true) }

    val gap = with(LocalDensity.current) { 2f.toDp() }

    var selectedAqua by remember { mutableStateOf<ConnectionCardItem?>(null) }
    var selectedPlasma by remember { mutableStateOf<ConnectionCardItem2?>(null) }
    var selectedPlasmaPlus by remember { mutableStateOf<ConnectionCardItem2?>(null) }

    // déterminer si l'utilisateur est superadmin via le gestionnaire de session centralisé (mémoire d'abord)
    val isSuperadmin = SessionManager.isSuperadmin(context)

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 72.dp, bottom = 16.dp)) {

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val intent = Intent(activity, HomeActivity::class.java)
                    try { activity?.startActivity(intent) } catch (_: Exception) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                }) {
                    Icon(painter = painterResource(id = R.drawable.arrowleft), contentDescription = "Back", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Favorites", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Favorites des produits
            val favProducts = products.filter { favoritesList.contains(it.id) }

            // Utiliser spacedBy=0 et gérer l'écart par carte pour garantir une séparation exacte
            // style pour les titres de sections (plus grand + souligné)
            val sectionTitleStyle = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline
            )

            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                item { Text(text = "Products", style = sectionTitleStyle) }
                items(favProducts) { p ->
                    // Carte cliquable : ouvre le détail du produit (même comportement que dans ShopScreen)
                    val cardModifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = if (isSuperadmin) {
                                Intent(activity, ProductDetailActivity::class.java).putExtra("product_id", p.id)
                            } else {
                                Intent(activity ?: context, ProductDetailActivityUser::class.java).putExtra("product_id", p.id)
                            }
                            try {
                                activity?.startActivity(intent)
                            } catch (_: Exception) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        }

                    // Carte produit : fond gris clair (au lieu de blanc) et uniquement bouton de suppression
                    Card(modifier = cardModifier.padding(vertical = gap), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                         Column(modifier = Modifier.padding(12.dp)) {
                             Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                 Text(text = p.name, style = MaterialTheme.typography.titleMedium)
                                 Spacer(modifier = Modifier.weight(1f))
                                 Text(text = p.section, color = Color.Gray)
                                 Spacer(modifier = Modifier.size(8.dp))
                                 // bouton supprimer (gris) — seul bouton d'action sur cette page
                                 IconButton(onClick = {
                                    favoritesList.remove(p.id)
                                    ProductStorage.saveFavorites(context, favoritesList.toSet())
                                }) {
                                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(24.dp))
                                }
                             }
                             Spacer(modifier = Modifier.height(6.dp))
                             Text(text = p.description)
                         }
                     }
                 }

                // Sections Aqua, Plasma et Plasma+ favorites conservées comme avant
                item { Spacer(modifier = Modifier.height(12.dp)); Text(text = "AquaTYPHOON Connection Cards", style = sectionTitleStyle) }

                val aquaFavItems = aquaItems.filter { item ->
                    val rawUid = (item.brand + "|" + item.connectionSet).trim()
                    val prefUid = "AQUA|$rawUid"
                    connFavsList.contains(prefUid) || connFavsList.contains(rawUid)
                }

                items(aquaFavItems) { item ->
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = gap)
                        .clickable { selectedAqua = item }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.brand, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Endoscope model: ${item.connectionSet}")
                                }
                                IconButton(modifier = Modifier.size(48.dp), onClick = {
                                    val rawUid = (item.brand + "|" + item.connectionSet).trim()
                                    val prefUid = "AQUA|$rawUid"
                                    connFavsList.remove(prefUid); connFavsList.remove(rawUid)
                                    ProductStorage.saveConnFavorites(context, connFavsList.toSet())
                                }) {
                                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(28.dp))
                                }
                             }
                         }
                     }
                 }

                item { Spacer(modifier = Modifier.height(12.dp)); Text(text = "PlasmaTYPHOON Connection Cards", style = sectionTitleStyle) }

                val plasmaFavItems = plasmaItems.filter { item ->
                    val rawUid = (item.brand + "|" + item.connectionSet).trim()
                    val prefUid = "PLASMA|$rawUid"
                    connFavsList.contains(prefUid) || connFavsList.contains(rawUid)
                }

                items(plasmaFavItems) { item ->
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = gap)
                        .clickable { selectedPlasma = item }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.brand, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Endoscope model: ${item.connectionSet}")
                                }
                                IconButton(modifier = Modifier.size(48.dp), onClick = {
                                    val rawUid = (item.brand + "|" + item.connectionSet).trim()
                                    val prefUid = "PLASMA|$rawUid"
                                    connFavsList.remove(prefUid); connFavsList.remove(rawUid); connFavsList.remove("PLASMA_PLUS|$rawUid")
                                    ProductStorage.saveConnFavorites(context, connFavsList.toSet())
                                }) {
                                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(28.dp))
                                }
                             }
                         }
                     }
                 }

                item { Spacer(modifier = Modifier.height(12.dp)); Text(text = "PlasmaTYPHOON+ Connection Cards", style = sectionTitleStyle) }

                val plasmaPlusFavItems = plasmaPlusItems.filter { item ->
                    val rawUid = (item.brand + "|" + item.connectionSet).trim()
                    val prefUid = "PLASMA_PLUS|$rawUid"
                    connFavsList.contains(prefUid) || connFavsList.contains(rawUid)
                }

                items(plasmaPlusFavItems) { item ->
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = gap)
                        .clickable { selectedPlasmaPlus = item }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.brand, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Endoscope model: ${item.connectionSet}")
                                }
                                IconButton(modifier = Modifier.size(48.dp), onClick = {
                                    val rawUid = (item.brand + "|" + item.connectionSet).trim()
                                    val prefUid = "PLASMA_PLUS|$rawUid"
                                    connFavsList.remove(prefUid); connFavsList.remove(rawUid); connFavsList.remove("PLASMA|$rawUid")
                                    ProductStorage.saveConnFavorites(context, connFavsList.toSet())
                                }) {
                                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(28.dp))
                                }
                             }
                         }
                     }
                 }

            }
        }

        // Barre d'icônes  fond blanc + légère élévation pour ne pas survoler le contenu
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
            ,
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
                    val intent = Intent(activity, HomeActivity::class.java)
                    try { activity?.startActivity(intent) } catch (_: Exception) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                }, modifier = Modifier.size(48.dp)) {
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
                    try { activity?.startActivity(intent) } catch (_: Exception) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.blackiconheartb), contentDescription = "heart", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }

                IconButton(onClick = { activity?.startActivity(Intent(activity, ParametersActivity::class.java)) }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.iconuserb), contentDescription = "user", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }
            }
        }

        // Dialogs de détail pour les items de connexion (inchangés)
        if (selectedAqua != null) {
            val it = selectedAqua!!
            AlertDialog(onDismissRequest = { selectedAqua = null }, confirmButton = { TextButton(onClick = { selectedAqua = null }) { Text("Close") } }, title = { Text(it.brand) }, text = {
                Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Text(text = "Endoscope Model : ${it.connectionSet}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "AquaTYPHOON Connection set Ref. PlasmaBiotics : ${it.plasmabioticsRef}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "AquaTYPHOON Connection set PENTAX Item N°: ${it.pentaxItem}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "AquaTYPHOON Cycle code : ${it.cycleCode}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "AquaTYPHOON Connection card : ${it.connectionCard}", style = MaterialTheme.typography.titleLarge)
                }
            })

        }

        if (selectedPlasma != null) {
            val it = selectedPlasma!!
            AlertDialog(onDismissRequest = { selectedPlasma = null }, confirmButton = { TextButton(onClick = { selectedPlasma = null }) { Text("Close") } }, title = { Text(it.brand) }, text = {
                Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Text(text = "Endoscope Model : ${it.connectionSet}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "PlasmaTYPHOON Connection set Ref. PlasmaBiotics : ${it.plasmabioticsRef}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "PlasmaTYPHOON Connection set PENTAX Item N°: ${it.pentaxItem}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "PlasmaTYPHOON Cycle code : ${it.cycleCode}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "PlasmaTYPHOON Connection card : ${it.connectionCard}", style = MaterialTheme.typography.titleLarge)
                }
            })

        }

        if (selectedPlasmaPlus != null) {
            val it = selectedPlasmaPlus!!
            AlertDialog(onDismissRequest = { selectedPlasmaPlus = null }, confirmButton = { TextButton(onClick = { selectedPlasmaPlus = null }) { Text("Close") } }, title = { Text(it.brand) }, text = {
                Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Text(text = "Endoscope Model : ${it.connectionSet}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "PlasmaTYPHOON+ Connection set Ref. PlasmaBiotics : ${it.plasmabioticsRef}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "PlasmaTYPHOON+ Connection set PENTAX Item N°: ${it.pentaxItem}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "PlasmaTYPHOON+ Cycle code : ${it.cycleCode}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "PlasmaTYPHOON+ Connection card : ${it.connectionCard}", style = MaterialTheme.typography.titleLarge)
                }
            })

        }

    }
}
