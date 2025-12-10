package com.example.plasmatrack

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.example.plasmatrack.ui.theme.PlasmaTrackTheme
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.compose.foundation.layout.size
import androidx.core.content.edit
import androidx.core.view.WindowCompat

// la classe de données stocke maintenant le chemin de l'image dans le stockage interne sous forme de chaîne pour la persistance
data class Product(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val description: String,
    val section: String,
    val imagePath: String?
)

class ShopActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            window.navigationBarColor = android.graphics.Color.WHITE
            WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightNavigationBars = true
        } catch (_: Exception) { /* ignore on older devices */ }
        setContent {
            PlasmaTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ShopScreen()
                }
            }
        }
    }
}

private fun copyUriToInternalFile(context: android.content.Context, uri: Uri): String? {
    return try {
        val resolver = context.contentResolver
        val input: InputStream? = resolver.openInputStream(uri)
        val ext = when (resolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/jpeg" -> "jpg"
            else -> "jpg"
        }
        val destFile = File(context.filesDir, "shop_${System.currentTimeMillis()}.$ext")
        FileOutputStream(destFile).use { out ->
            input?.use { inp ->
                val buffer = ByteArray(8 * 1024)
                var read: Int
                while (inp.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                out.flush()
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Utilisez SessionManager pour vérifier le statut de superadmin (en mémoire de préférence).
private fun isSuperadminNow(context: android.content.Context): Boolean {
    return SessionManager.isSuperadmin(context)
}

// Assistant de persistance des sections
private const val PREF_SECTIONS = "product_sections"
private fun loadSections(context: android.content.Context): MutableList<String> {
    val sp = context.getSharedPreferences("shop_prefs", android.content.Context.MODE_PRIVATE)
    val joined = sp.getString(PREF_SECTIONS, null)
    return if (joined.isNullOrEmpty()) {
        mutableListOf("ENDOSCOPE")
    } else {
        joined.split("||").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    }
}

private fun saveSections(context: android.content.Context, sections: List<String>) {
    val sp = context.getSharedPreferences("shop_prefs", android.content.Context.MODE_PRIVATE)
    sp.edit { putString(PREF_SECTIONS, sections.joinToString("||")) }
}

@Composable
fun ShopScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val products = remember { mutableStateListOf<Product>().apply { addAll(ProductStorage.loadProducts(context)) } }
    var dialogProduct by remember { mutableStateOf<Product?>(null) } // null -> add, non-null -> edit
    var showDialog by remember { mutableStateOf(false) }

    // Sections dynamiques (persistées)
    val sections = remember { mutableStateListOf<String>().apply { addAll(loadSections(context)) } }

    // État des favoris (persisté) via ProductStorage
    val favoritesSet = remember { mutableStateListOf<Long>().apply { addAll(ProductStorage.loadFavorites(context)) } }
    fun toggleFavorite(id: Long) {
        if (favoritesSet.contains(id)) favoritesSet.remove(id) else favoritesSet.add(id)
        ProductStorage.saveFavorites(context, favoritesSet.toSet())
    }

    // Search state
    var query by remember { mutableStateOf("") }

    // Lancer pour l'activité de détail du produit -> rafraîchir la liste lorsque le résultat est OK
    val detailLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // reload products
            products.clear(); products.addAll(ProductStorage.loadProducts(context))
        }
    }

    // Utiliser un gestionnaire de session centralisé afin que la session superadmin en mémoire soit respectée
    val isSuperadmin = SessionManager.isSuperadmin(context)

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 12.dp)) {
            // Ligne d'en-tête avec bouton de retour et petite image de profil
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { (activity as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() }) {
                        Icon(painter = painterResource(id = R.drawable.arrowleft), contentDescription = "Retour", tint = Color.Black)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Column {
                        Text(text = "Products", style = MaterialTheme.typography.titleLarge, color = Color.Black)
                        Text(text = "Here are all our products", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barre de recherche + petite icône de filtre
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(text = "Search products") },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Bouton Ajouter une section visible pour superadmin (texte compact)
                if (isSuperadmin && isSuperadminNow(context)) {
                    Button(onClick = {
                        val input = android.widget.EditText(context)
                        val alert = android.app.AlertDialog.Builder(context)
                            .setTitle("New section name")
                            .setView(input)
                            .setPositiveButton("Add") { d, _ ->
                                val name = input.text.toString().trim()
                                if (name.isNotEmpty() && !sections.contains(name)) {
                                    sections.add(name)
                                    saveSections(context, sections)
                                } else {
                                    Toast.makeText(context, "Invalid or existing section", Toast.LENGTH_SHORT).show()
                                }
                                d.dismiss()
                            }
                            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                            .create()
                        alert.show()
                    }, modifier = Modifier.height(40.dp)) {
                        Text(text = "Add section")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Liste principale des produits regroupés par section, filtrée par requête
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                // Pour chaque section, afficher l'en-tête et ses produits
                sections.forEach { sec ->
                    val sectionProducts = products.filter { it.section.equals(sec, ignoreCase = true) && (query.isBlank() || it.name.contains(query, ignoreCase = true)) }

                    item {
                        // Header de section avec séparateur et actions pour superadmin
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = sec.uppercase(), color = Color.Black, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            // ligne noire de 365 x 1 dp
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier
                                    .padding(start = 0.dp)
                                    .size(width = 365.dp, height = 1.dp)
                                    .background(Color.Black))
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Actions Edit / Delete de la section (visible uniquement aux superadmin)
                            if (isSuperadmin && isSuperadminNow(context)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = {
                                        // Edit section name
                                        val input = android.widget.EditText(context)
                                        input.setText(sec)
                                        val alert = android.app.AlertDialog.Builder(context)
                                            .setTitle("Rename section")
                                            .setView(input)
                                            .setPositiveButton("Save") { d, _ ->
                                                val newName = input.text.toString().trim()
                                                if (newName.isNotEmpty() && !sections.contains(newName)) {
                                                    val idx = sections.indexOfFirst { it == sec }
                                                    if (idx >= 0) {
                                                        sections[idx] = newName
                                                        // mettre à jour les produits appartenant à cette section
                                                        val updated = products.map { p ->
                                                            if (p.section.equals(sec, ignoreCase = true)) p.copy(section = newName) else p
                                                        }
                                                        products.clear(); products.addAll(updated)
                                                        saveSections(context, sections)
                                                        ProductStorage.saveProducts(context, products)
                                                        Toast.makeText(context, "Section renamed", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Invalid or existing name", Toast.LENGTH_SHORT).show()
                                                }
                                                d.dismiss()
                                            }
                                            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                                            .create()
                                        alert.show()
                                    }) { Text(text = "Edit section") }

                                    TextButton(onClick = {
                                        // Supprimer la section avec confirmation
                                        val confirm = android.app.AlertDialog.Builder(context)
                                            .setTitle("Delete section")
                                            .setMessage("Delete section '$sec' and all its products? This cannot be undone.")
                                            .setPositiveButton("Delete") { d, _ ->
                                                // supprimer la section et ses produits
                                                sections.remove(sec)
                                                val removed = products.filter { it.section.equals(sec, ignoreCase = true) }
                                                if (removed.isNotEmpty()) {
                                                    products.removeAll { it.section.equals(sec, ignoreCase = true) }
                                                }
                                                saveSections(context, sections)
                                                ProductStorage.saveProducts(context, products)
                                                Toast.makeText(context, "Section deleted", Toast.LENGTH_SHORT).show()
                                                d.dismiss()
                                            }
                                            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                                            .create()
                                        confirm.show()
                                    }) { Text(text = "Delete section", color = Color.Red) }
                                }
                            }
                        }
                    }

                    if (sectionProducts.isEmpty()) {
                        // Empty placeholder
                        item {
                            androidx.compose.material3.Card(modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "No products in this section", color = Color.Gray)
                                    if (isSuperadmin && isSuperadminNow(context)) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(onClick = { dialogProduct = null; showDialog = true }) { Text(text = "Add product") }
                                    }
                                }
                            }
                        }
                    } else {
                        items(sectionProducts) { p ->
                            // rendre la card cliquable pour tous (utilisateur ou superadmin) : ouvre ProductDetailActivity (édition si existant)
                            val cardModifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)

                                .clickable {
                                    // ouvrir l'activité de détail correcte selon le rôle
                                    // procéder à l'ouverture du détail en fonction du rôle
                                    if (isSuperadminNow(context)) {
                                        // superadmin ouvrez l'éditeur via le lanceur pour obtenir le résultat et actualiser
                                        val intent = Intent(activity, ProductDetailActivity::class.java).putExtra("product_id", p.id)
                                        try {
                                            detailLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            activity?.startActivity(intent)
                                        }
                                    } else {
                                        // utilisateur normal : préférer le contexte de l'Activité lorsque disponible
                                        val intent = Intent(context, ProductDetailActivityUser::class.java).putExtra("product_id", p.id)
                                        try {
                                            if (activity != null) {
                                                // ouverture du produit (vue utilisateur) via activité
                                                activity.startActivity(intent)
                                            } else {
                                                // solution de repli : utiliser le contexte de l'application avec NEW_TASK
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            // final fallback
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }
                                    }
                                }

                            androidx.compose.material3.Card(
                                modifier = cardModifier,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Ligne du haut : nom + espaceur + section + icône de cœur
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = p.name, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(text = p.section, color = Color.Gray, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = {
                                            toggleFavorite(p.id)
                                        }) {
                                            val fav = favoritesSet.contains(p.id)
                                            Icon(painter = painterResource(id = if (fav) R.drawable.rediconheartb else R.drawable.iconheartb), contentDescription = "favorite", tint = Color.Unspecified, modifier = Modifier.size(24.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = p.description, maxLines = 3, overflow = TextOverflow.Ellipsis)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (isSuperadmin && isSuperadminNow(context)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            // Bouton Modifier supprimé sur demande ; garder uniquement Supprimer
                                            TextButton(onClick = {
                                                if (!isSuperadminNow(context)) {
                                                    Toast.makeText(context, "Accès refusé", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    products.remove(p)
                                                    p.imagePath?.let { oldPath -> try { File(oldPath).delete() } catch (_: Exception) {} }
                                                    ProductStorage.saveProducts(context, products)
                                                    Toast.makeText(context, "Produit supprimé", Toast.LENGTH_SHORT).show()
                                                }

                                            }) { Text(text = "Delete", color = Color.Red) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FloatingActionButton and dialog logic: remonter le FAB (au-dessus de la barre) et légèrement agrandir
        if (isSuperadmin && isSuperadminNow(context)) {
            FloatingActionButton(onClick = {
                // Ouvrir ProductDetailActivity en mode création (product_id = -1) pour le résultat
                val intent = Intent(activity, ProductDetailActivity::class.java).putExtra("product_id", -1L).putExtra("is_superadmin", true)
                detailLauncher.launch(intent)
            }, modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 96.dp)
                .size(56.dp)) {
                Text(text = "+")
            }
        }

        if (showDialog) {
            if (!isSuperadminNow(context) || !isSuperadmin) {
                Toast.makeText(context, "Accès refusé", Toast.LENGTH_SHORT).show()
                showDialog = false
                dialogProduct = null
            } else {
                ProductDialog(initialProduct = dialogProduct, sections = sections.toList(), onCancel = { showDialog = false; dialogProduct = null }, onConfirm = { prod ->
                    if (dialogProduct == null) {
                        products.add(0, prod)
                    } else {
                        val idx = products.indexOfFirst { it.id == prod.id }
                        if (idx >= 0) products[idx] = prod
                    }
                    ProductStorage.saveProducts(context, products)
                    Toast.makeText(context, "Produit enregistré", Toast.LENGTH_SHORT).show()
                    showDialog = false
                    dialogProduct = null
                })
            }
        }


        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
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
                    Icon(painter = painterResource(id = R.drawable.iconheartb), contentDescription = "favorites", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }

                IconButton(onClick = { activity?.startActivity(Intent(activity, ParametersActivity::class.java)) }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.iconuserb), contentDescription = "user", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }
            }
        }
    }
}

@Composable
fun ProductDialog(initialProduct: Product?, sections: List<String>, onCancel: () -> Unit, onConfirm: (Product) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var description by remember { mutableStateOf(initialProduct?.description ?: "") }
    var section by remember { mutableStateOf(initialProduct?.section ?: (sections.firstOrNull() ?: "General")) }
    val sectionsList = sections

    var expanded by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImagePath by remember { mutableStateOf(initialProduct?.imagePath) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        imageUri = uri
        uri?.let { u ->
            try {
                context.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = if (initialProduct == null) "Nouveau produit" else "Modifier le produit") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
                Spacer(modifier = Modifier.height(8.dp))

                // Section de menu déroulant simple utilisant des boutons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Section: $section", modifier = Modifier.align(Alignment.CenterVertically))
                    Button(onClick = { expanded = !expanded }) { Text(text = "Choose") }
                }
                if (expanded) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        sectionsList.forEach { s ->
                            Button(onClick = { section = s; expanded = false }, modifier = Modifier.fillMaxWidth()) { Text(text = s) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text(text = "Choose photo") }

                // aperçu de l'image existante ou sélectionnée
                val previewPath = imageUri?.toString() ?: existingImagePath
                previewPath?.let { pth ->
                    val uriToShow = if (imageUri != null) imageUri else existingImagePath?.let { Uri.fromFile(File(it)) }
                    uriToShow?.let { u ->
                        Spacer(modifier = Modifier.height(8.dp))
                        AndroidView(factory = { ctx ->
                            ImageView(ctx).apply {
                                adjustViewBounds = true
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                setImageURI(u)
                            }
                        }, update = { view -> view.setImageURI(u) }, modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!isSuperadminNow(context)) {
                    Toast.makeText(context, "Accès refusé", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                if (name.isBlank()) {
                    Toast.makeText(context, "Nom requis", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }

                // Si une nouvelle image a été choisie, copiez-la dans le stockage interne et supprimez l'ancienne image
                var finalImagePath: String? = existingImagePath
                if (imageUri != null) {
                    val copied = copyUriToInternalFile(context, imageUri!!)
                    if (copied != null) {
                        existingImagePath?.let { old -> if (old != copied) try { File(old).delete() } catch (_: Exception) {} }
                        finalImagePath = copied
                    }
                }

                val id = initialProduct?.id ?: System.currentTimeMillis()
                val prod = Product(id = id, name = name, description = description, section = section, imagePath = finalImagePath)
                onConfirm(prod)
            }) { Text(text = if (initialProduct == null) "Ajouter" else "Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(text = "Annuler") }
        }
    )
}
