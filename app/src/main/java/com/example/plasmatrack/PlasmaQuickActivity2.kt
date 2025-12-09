package com.example.plasmatrack

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.plasmatrack.ui.theme.PlasmaTrackTheme
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

class PlasmaQuickActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // hide system bars for this activity (keeps consistent behavior with Aqua)
        try {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } catch (_: Exception) {
            // ignore on older devices
        }

        enableEdgeToEdge()
        setContent {
            PlasmaTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PlasmaQuickScreen2()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // restore system bars when activity is destroyed
        try {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.show(WindowInsetsCompat.Type.systemBars())
        } catch (_: Exception) {
            // ignore
        }
    }
}

@Composable
fun PlasmaQuickScreen2() {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val sections = remember { mutableStateListOf<QuickGuideSection>().apply { addAll(PlasmaQuickGuidesStorage.loadAll(ctx)) } }
    var showAddSection by remember { mutableStateOf(false) }
    var showAddGuide by remember { mutableStateOf<String?>(null) }
    // states for add flow (choice between PDF and YouTube, PDF picker)
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

    // root column with global verticalScroll to make entire screen scrollable
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .background(MaterialTheme.colorScheme.background)
        .padding(16.dp)) {

        Row(modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { (activity as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() }) {
                Icon(painter = androidx.compose.ui.res.painterResource(id = R.drawable.arrowleft), contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Quick Guides", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // Add section reserved to superadmin
            if (SessionManager.isSuperadmin(ctx)) {
                Button(onClick = { showAddSection = true }) { Text("Add section") }
            } else {
                Spacer(modifier = Modifier.height(0.dp))
            }
            Button(onClick = { sections.clear(); sections.addAll(PlasmaQuickGuidesStorage.loadAll(ctx)) }) { Text("Refresh") }
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

        // --- affichage soit par sections (query vide) soit flat list de vidéos (query non vide)
        if (queryText.isEmpty()) {
            // affichage normal par sections en Column (scroll géré par parent)
            Column(modifier = Modifier.fillMaxWidth()) {
                sections.forEach { sec ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // keep the section name as the primary element
                                Column(modifier = Modifier.weight(1f)) { Text(text = sec.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                Spacer(modifier = Modifier.width(8.dp))
                                // ask user whether to add YouTube or PDF (only for superadmin)
                                if (SessionManager.isSuperadmin(ctx)) {
                                    TextButton(onClick = { showAddChoice = sec.name }) { Text("Add") }
                                    TextButton(onClick = { PlasmaQuickGuidesStorage.removeSection(ctx, sec.name); sections.clear(); sections.addAll(PlasmaQuickGuidesStorage.loadAll(ctx)) }) { Text("Delete") }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDDDDDD)))
                            Spacer(modifier = Modifier.height(8.dp))

                            sec.guides.forEach { g ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))) {
                                    Column(modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // open guide: if it's a PDF (file://, content:// or endsWith .pdf) open as PDF,
                                            // otherwise treat as YouTube/link as before
                                            try {
                                                val url = g.youtubeUrl
                                                val low = url.lowercase()
                                                val isPdf = low.endsWith(".pdf") || low.startsWith("file://") || low.startsWith("content://")
                                                if (isPdf) {
                                                    try {
                                                        // Create a safe Uri
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
                                                        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(g.youtubeUrl))
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
                                        }
                                        .padding(12.dp)) {
                                        Text(text = g.title, style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.Underline))
                                        if (g.description.isNotBlank()) { Spacer(modifier = Modifier.height(6.dp)); Text(text = g.description, style = MaterialTheme.typography.bodyMedium) }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            if (SessionManager.isSuperadmin(ctx)) {
                                                TextButton(onClick = { PlasmaQuickGuidesStorage.removeGuide(ctx, sec.name, g.id); sections.clear(); sections.addAll(PlasmaQuickGuidesStorage.loadAll(ctx)) }) { Text("Delete") }
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
                                    // Create a safe Uri
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

        // Dialog: user chooses whether to add PDF or YouTube
        if (showAddChoice != null) {
            val section = showAddChoice!!
            AlertDialog(onDismissRequest = { showAddChoice = null }, title = { Text("Add to \"$section\"") }, text = { Text("Choose type to add:") }, confirmButton = {
                TextButton(onClick = {
                    // choose YouTube -> open existing YouTube dialog
                    Toast.makeText(ctx, "YouTube chosen for $section", Toast.LENGTH_SHORT).show()
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
                        val destName = "quickguide_${section}_${System.currentTimeMillis()}.pdf"
                        val saved = try {
                            var ok = false
                            ctx.contentResolver.openInputStream(pickedPdfUri!!)?.use { input ->
                                val dest = File(ctx.filesDir, destName)
                                FileOutputStream(dest).use { out -> input.copyTo(out) }
                                ok = true
                            }
                            ok
                        } catch (e: Exception) { e.printStackTrace(); false }
                        if (saved) {
                            val fileUri = Uri.fromFile(File(ctx.filesDir, destName)).toString()
                            PlasmaQuickGuidesStorage.addGuide(ctx, section, title.trim(), fileUri, description.trim())
                            sections.clear(); sections.addAll(PlasmaQuickGuidesStorage.loadAll(ctx))
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

        // Dialog Add Guide (YouTube) - ADDED: same logic as AquaQuick but using PlasmaQuickGuidesStorage
        if (showAddGuide != null) {
            val section = showAddGuide!!
            var title by remember { mutableStateOf("") }
            var url by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = { showAddGuide = null }, confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && url.isNotBlank()) {
                        PlasmaQuickGuidesStorage.addGuide(ctx, section, title.trim(), url.trim(), description.trim())
                        sections.clear(); sections.addAll(PlasmaQuickGuidesStorage.loadAll(ctx))
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

        // Dialog Add Section (identique à QuickGuidesActivity2)
        if (showAddSection) {
            var input by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = { showAddSection = false }, confirmButton = {
                Button(onClick = {
                    if (input.isNotBlank()) {
                        PlasmaQuickGuidesStorage.addSection(ctx, input.trim())
                        sections.clear(); sections.addAll(PlasmaQuickGuidesStorage.loadAll(ctx))
                    }
                    showAddSection = false
                }) { Text("Add") }
            }, text = {
                Column { OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Section name") }) }
            })
        }
    }
}
