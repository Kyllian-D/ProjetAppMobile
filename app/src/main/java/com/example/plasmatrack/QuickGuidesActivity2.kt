package com.example.plasmatrack

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.plasmatrack.ui.theme.PlasmaTrackTheme
import java.util.regex.Pattern
import java.io.FileOutputStream
import java.io.File
import androidx.core.content.FileProvider

// imports added to control system bars
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

// imports pour scroll global
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

class AquaQuickActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // masquer les barres système pour cette activité
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller?.let {
                it.hide(WindowInsetsCompat.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } catch (_: Exception) {
            // ignorer sur les appareils plus anciens
        }

        enableEdgeToEdge()
        setContent {
            PlasmaTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AquaQuickScreen2()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // restaurer les barres système quand l'activité est détruite
        try {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller?.show(WindowInsetsCompat.Type.systemBars())
        } catch (_: Exception) {
            // ignorer
        }
    }
}

@Composable
fun AquaQuickScreen2() {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val sections = remember { mutableStateListOf<QuickGuideSection>().apply { addAll(QuickGuidesStorage.loadAll(ctx)) } }
    var showAddSection by remember { mutableStateOf(false) }
    var showAddGuide by remember { mutableStateOf<String?>(null) }
    // états utilisés pour le flux d'ajout (choix entre PDF et YouTube, sélection PDF)
    var showAddChoice by remember { mutableStateOf<String?>(null) }
    var showAddPdf by remember { mutableStateOf<String?>(null) }
    var pickedPdfUri by remember { mutableStateOf<Uri?>(null) }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> pickedPdfUri = uri }

    // recherche par nom de section -> modifié : recherche de vidéos quand query non vide
    var sectionQuery by remember { mutableStateOf(TextFieldValue("")) }
    val queryText = sectionQuery.text.trim().lowercase()

    // si query est vide -> on affiche les sections
    // si query non vide -> on crée une liste aplatie de Pair(sectionName, guide)
    val matchedGuides = remember(queryText, sections) {
        val q = queryText
        if (q.isEmpty()) emptyList<Pair<String, QuickGuide>>()
        else {
            val tokens = q.split(Regex("\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
            sections.flatMap { sec ->
                sec.guides.filter { g ->
                    tokens.all { token ->
                        (g.title.contains(token, ignoreCase = true)) ||
                        (g.description.contains(token, ignoreCase = true)) ||
                        (g.youtubeUrl.contains(token, ignoreCase = true))
                    }
                }.map { guide -> Pair(sec.name, guide) }
            }
        }
    }

    DisposableEffect(Unit) { onDispose { } }

    // colonne racine avec verticalScroll pour rendre l'écran entier scrollable
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .background(MaterialTheme.colorScheme.background)
        .padding(16.dp)) {

        Row(modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { (activity as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() }) {
                Icon(painter = androidx.compose.ui.res.painterResource(id = R.drawable.arrowleft), contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Quick Guides", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // Add section réservé au superadmin
            if (SessionManager.isSuperadmin(ctx)) {
                Button(onClick = { showAddSection = true }) { Text("Add section") }
            } else {
                Spacer(modifier = Modifier.height(0.dp))
            }
            Button(onClick = { sections.clear(); sections.addAll(QuickGuidesStorage.loadAll(ctx)) }) { Text("Refresh") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // champ de recherche des sections
        OutlinedTextField(
            value = sectionQuery,
            onValueChange = { sectionQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = "Search videos") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // affichage soit par sections (query vide) soit flat list de vidéos (query non vide)
        if (queryText.isEmpty()) {
            // affichage normal par sections en Column (scroll géré par parent)
            Column(modifier = Modifier.fillMaxWidth()) {
                sections.forEach { sec ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Suppression de la Box circulaire affichant la première lettre de la section
                                // garder le nom de la section comme élément principal
                                Column(modifier = Modifier.weight(1f)) { Text(text = sec.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                Spacer(modifier = Modifier.width(8.dp))
                                // quand Add est cliqué, demander si l'utilisateur veut ajouter un PDF ou une vidéo YouTube
                                // Add/Delete section seulement pour superadmin
                                if (SessionManager.isSuperadmin(ctx)) {
                                    TextButton(onClick = { showAddChoice = sec.name }) { Text("Add") }
                                    TextButton(onClick = { QuickGuidesStorage.removeSection(ctx, sec.name); sections.clear(); sections.addAll(QuickGuidesStorage.loadAll(ctx)) }) { Text("Delete") }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDDDDDD)))
                            Spacer(modifier = Modifier.height(8.dp))

                            sec.guides.forEach { g ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))) {
                                    Column(modifier = Modifier.fillMaxWidth().clickable {
                                        // ouvrir le guide : si c'est un PDF  ouvrir en PDF,
                                        // sinon traiter comme une vidéo YouTube / lien
                                        try {
                                            val url = g.youtubeUrl
                                            val low = url.lowercase()
                                            val isPdf = low.endsWith(".pdf") || low.startsWith("file://") || low.startsWith("content://")
                                            if (isPdf) {
                                                try {
                                                    // Créer un Uri sûr pour ouvrir le PDF
                                                    val pdfUri = when {
                                                        url.startsWith("file://") -> {
                                                            val path = Uri.parse(url).path ?: ""
                                                            val f = File(path)
                                                            FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", f)
                                                        }
                                                        url.startsWith("content://") -> Uri.parse(url)
                                                        url.startsWith("http://") || url.startsWith("https://") -> Uri.parse(url)
                                                        else -> Uri.parse(url)
                                                    }
                                                    val pdfIntent = Intent(Intent.ACTION_VIEW)
                                                    if (pdfUri.scheme == "http" || pdfUri.scheme == "https") {
                                                        // PDF distant : ouvrir dans le navigateur
                                                        pdfIntent.data = pdfUri
                                                    } else {
                                                        pdfIntent.setDataAndType(pdfUri, "application/pdf")
                                                        pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    if (ctx is Activity) ctx.startActivity(pdfIntent) else { pdfIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(pdfIntent) }
                                                } catch (e: ActivityNotFoundException) {
                                                    // sauvegarde  vue générique
                                                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    if (ctx is Activity) ctx.startActivity(fallback) else { fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(fallback) }
                                                }
                                            } else {
                                                val id = extractYoutubeId(url)
                                                if (!id.isNullOrBlank()) {
                                                    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id))
                                                    try { if (ctx is Activity) ctx.startActivity(appIntent) else { appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(appIntent) } }
                                                    catch (e: ActivityNotFoundException) { val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$id")); if (ctx is Activity) ctx.startActivity(web) else { web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(web) } }
                                                } else {
                                                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)); if (ctx is Activity) ctx.startActivity(i) else { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(i) }
                                                }
                                            }
                                        } catch (ex: Exception) { ex.printStackTrace() }
                                     }.padding(12.dp)) {
                                        Text(text = g.title, style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.Underline))
                                        if (g.description.isNotBlank()) { Spacer(modifier = Modifier.height(6.dp)); Text(text = g.description, style = MaterialTheme.typography.bodyMedium) }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            if (SessionManager.isSuperadmin(ctx)) {
                                                TextButton(onClick = { QuickGuidesStorage.removeGuide(ctx, sec.name, g.id); sections.clear(); sections.addAll(QuickGuidesStorage.loadAll(ctx)) }) { Text("Delete") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            // affichage des vidéos correspondant à la recherche (plat) en Column
            Column(modifier = Modifier.fillMaxWidth()) {
                matchedGuides.forEach { pair ->
                    Spacer(modifier = Modifier.height(8.dp))
                    val secName = pair.first
                    val g = pair.second
                    Card(modifier = Modifier.fillMaxWidth().clickable {
                        try {
                            val url = g.youtubeUrl
                            val low = url.lowercase()
                            val isPdf = low.endsWith(".pdf") || low.startsWith("file://") || low.startsWith("content://")
                            if (isPdf) {
                                try {
                                    // Créer un Uri sûr pour ouvrir le PDF
                                    val pdfUri = when {
                                        url.startsWith("file://") -> {
                                            val path = Uri.parse(url).path ?: ""
                                            val f = File(path)
                                            FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", f)
                                        }
                                        url.startsWith("content://") -> Uri.parse(url)
                                        url.startsWith("http://") || url.startsWith("https://") -> Uri.parse(url)
                                        else -> Uri.parse(url)
                                    }
                                    val pdfIntent = Intent(Intent.ACTION_VIEW)
                                    if (pdfUri.scheme == "http" || pdfUri.scheme == "https") {
                                        pdfIntent.data = pdfUri
                                    } else {
                                        pdfIntent.setDataAndType(pdfUri, "application/pdf")
                                        pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    if (ctx is Activity) ctx.startActivity(pdfIntent) else { pdfIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(pdfIntent) }
                                } catch (e: ActivityNotFoundException) {
                                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    if (ctx is Activity) ctx.startActivity(fallback) else { fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(fallback) }
                                }
                            } else {
                                val id = extractYoutubeId(url)
                                if (!id.isNullOrBlank()) {
                                    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id))
                                    try { if (ctx is Activity) ctx.startActivity(appIntent) else { appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(appIntent) } }
                                    catch (e: ActivityNotFoundException) { val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$id")); if (ctx is Activity) ctx.startActivity(web) else { web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(web) } }
                                } else {
                                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)); if (ctx is Activity) ctx.startActivity(i) else { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(i) }
                                }
                            }
                        } catch (ex: Exception) { ex.printStackTrace() }
                     }) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = g.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            if (g.description.isNotBlank()) Text(text = g.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Section: $secName", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Dialog Add Section
        if (showAddSection) {
            var input by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = { showAddSection = false }, confirmButton = {
                Button(onClick = {
                    if (input.isNotBlank()) {
                        QuickGuidesStorage.addSection(ctx, input.trim())
                        sections.clear(); sections.addAll(QuickGuidesStorage.loadAll(ctx))
                    }
                    showAddSection = false
                }) { Text("Add") }
            }, text = {
                Column { OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Section name") }) }
            })
        }

        // Dialog Add Guide (YouTube) - reused when user chooses YouTube
        if (showAddGuide != null) {
            val section = showAddGuide!!
            var title by remember { mutableStateOf("") }
            var url by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = { showAddGuide = null }, confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && url.isNotBlank()) {
                        QuickGuidesStorage.addGuide(ctx, section, title.trim(), url.trim(), description.trim())
                        sections.clear(); sections.addAll(QuickGuidesStorage.loadAll(ctx))
                    }
                    showAddGuide = null
                }) { Text("Add") }
            }, text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("YouTube URL") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                }
            })
        }

        // Dialog: user chooses whether to add PDF or YouTube
        if (showAddChoice != null) {
            val section = showAddChoice!!
            AlertDialog(onDismissRequest = { showAddChoice = null }, title = { Text("Add to \"$section\"") }, text = { Text("Choose type to add:") }, confirmButton = {
                TextButton(onClick = {
                    // choose YouTube -> open existing YouTube dialog
                    showAddGuide = section
                    showAddChoice = null
                }) { Text("YouTube") }
            }, dismissButton = {
                TextButton(onClick = {
                    // choose PDF -> open PDF add dialog
                    showAddPdf = section
                    showAddChoice = null
                }) { Text("PDF") }
            })
        }

        // Dialog Add PDF
        if (showAddPdf != null) {
             val section = showAddPdf!!
             var title by remember { mutableStateOf("") }
             var description by remember { mutableStateOf("") }
             AlertDialog(onDismissRequest = { showAddPdf = null; pickedPdfUri = null }, confirmButton = {
                 Button(onClick = {
                     if (title.isNotBlank() && pickedPdfUri != null) {
                         // copy uri to internal storage and add as a guide with a file:// uri in the url field
                         val destName = "quickguide_${section}_${System.currentTimeMillis()}.pdf"
                         val saved = try {
                             var ok = false
                             ctx.contentResolver.openInputStream(pickedPdfUri!!)?.use { input ->
                                 val dest = File(ctx.filesDir, destName)
                                 FileOutputStream(dest).use { out ->
                                     input.copyTo(out)
                                 }
                                 ok = true
                             }
                             ok
                         } catch (e: Exception) { e.printStackTrace(); false }
                         if (saved) {
                             val fileUri = Uri.fromFile(File(ctx.filesDir, destName)).toString()
                             QuickGuidesStorage.addGuide(ctx, section, title.trim(), fileUri, description.trim())
                             sections.clear(); sections.addAll(QuickGuidesStorage.loadAll(ctx))
                         }
                     }
                     showAddPdf = null; pickedPdfUri = null
                 }) { Text("Add PDF") }
             }, text = {
                 Column {
                     OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                     Spacer(modifier = Modifier.height(8.dp))
                     OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                     Spacer(modifier = Modifier.height(12.dp))
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         Button(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }) { Text("Pick PDF") }
                         Spacer(modifier = Modifier.width(8.dp))
                         Text(text = pickedPdfUri?.lastPathSegment ?: "No file selected", maxLines = 1, overflow = TextOverflow.Ellipsis)
                     }
                 }
             })
         }
    }
}
