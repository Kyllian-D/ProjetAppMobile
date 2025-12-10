package com.example.plasmatrack

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.plasmatrack.ui.theme.PlasmaTrackTheme
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private fun ensureDefaultPlasmaInstructions(ctx: Context) {
    try {
        // Assurez-vous toujours que PlasmaManualStorage contient exactement une section nommée "instructions"
        // avec le PDF inclus intitulé "Instructions d'utilisation de PlasmaTYPHOON".
        val resName = "instructionsplasmatyphoon"
        val resId = ctx.resources.getIdentifier(resName, "raw", ctx.packageName)
        var input: InputStream? = null
        if (resId != 0) {
            input = ctx.resources.openRawResource(resId)
        } else {
            try { input = ctx.assets.open("$resName.pdf") } catch (_: Exception) { input = null }
        }
        // Si le PDF inclus existe, copiez-le dans filesDir et construisez un fileUri ; sinon, utilisez une chaîne vide
        var fileUri = ""
        if (input != null) {
            val destName = "$resName.pdf"
            val destFile = File(ctx.filesDir, destName)
            try {
                FileOutputStream(destFile).use { out -> input.copyTo(out) }
                fileUri = Uri.fromFile(destFile).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { input.close() } catch (_: Exception) {}
            }
        }

        // Écraser le stockage Plasma pour ne contenir que la section des instructions avec le PDF unique
        val item = PlasmaManualItem(id = System.currentTimeMillis(), title = "Instructions use PlasmaTYPHOON", fileUrl = fileUri, description = "")
        val sec = PlasmaManualSection(name = "instructions", items = mutableListOf(item))
        PlasmaManualStorage.saveAll(ctx, listOf(sec))
    } catch (e: Exception) { e.printStackTrace() }
}

class PlasmaManualActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Assurez-vous que les instructions plasma par défaut existent
        ensureDefaultPlasmaInstructions(this)

        super.onCreate(savedInstanceState)
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller?.let {
                it.hide(WindowInsetsCompat.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } catch (_: Exception) { }

        enableEdgeToEdge()
        setContent {
            PlasmaTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PlasmaManualScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { val controller = WindowCompat.getInsetsController(window, window.decorView); controller?.show(WindowInsetsCompat.Type.systemBars()) } catch (_: Exception) { }
    }
}

@Composable
fun PlasmaManualScreen() {
    val ctx = LocalContext.current
    val sections = remember { mutableStateListOf<PlasmaManualSection>().apply { addAll(PlasmaManualStorage.loadAll(ctx)) } }
    var showAddSection by remember { mutableStateOf(false) }
    var showAddItem by remember { mutableStateOf<String?>(null) }
    var showPickPdfForSection by remember { mutableStateOf<String?>(null) }
    var pickedPdfUri by remember { mutableStateOf<Uri?>(null) }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> pickedPdfUri = uri }

    var sectionQuery by remember { mutableStateOf(TextFieldValue("")) }
    val queryText = sectionQuery.text.trim().lowercase()

    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .background(MaterialTheme.colorScheme.background)
        .padding(16.dp)) {

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { (ctx as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() }) {
                Icon(painter = androidx.compose.ui.res.painterResource(id = R.drawable.arrowleft), contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Manuals", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (SessionManager.isSuperadmin(ctx)) {
                Button(onClick = { showAddSection = true }) { Text("Add section") }
            }
            Button(onClick = { sections.clear(); sections.addAll(PlasmaManualStorage.loadAll(ctx)) }) { Text("Refresh") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = sectionQuery,
            onValueChange = { sectionQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = "Search manuals") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (queryText.isEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                sections.forEach { sec ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) { Text(text = sec.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (SessionManager.isSuperadmin(ctx)) { TextButton(onClick = { showAddItem = sec.name }) { Text("Add") } ; TextButton(onClick = { PlasmaManualStorage.removeSection(ctx, sec.name); sections.clear(); sections.addAll(PlasmaManualStorage.loadAll(ctx)) }) { Text("Delete") } }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDDDDDD)))
                            Spacer(modifier = Modifier.height(8.dp))

                            sec.items.forEach { it ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))) {
                                    Column(modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            try {
                                                val url = it.fileUrl
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
                                            } catch (ex: Exception) { ex.printStackTrace() }
                                        }
                                        .padding(12.dp)) {
                                        Text(text = it.title, style = MaterialTheme.typography.titleMedium)
                                        if (it.description.isNotBlank()) { Spacer(modifier = Modifier.height(6.dp)); Text(text = it.description, style = MaterialTheme.typography.bodyMedium) }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            if (SessionManager.isSuperadmin(ctx)) {
                                                TextButton(onClick = { PlasmaManualStorage.removeItem(ctx, sec.name, it.id); sections.clear(); sections.addAll(PlasmaManualStorage.loadAll(ctx)) }) { Text("Delete") }
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
            Column(modifier = Modifier.fillMaxWidth()) {
                val tokens = queryText.split(Regex("\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
                val matched = sections.flatMap { sec: PlasmaManualSection ->
                    sec.items.filter { item -> tokens.all { token -> item.title.contains(token, ignoreCase = true) || item.description.contains(token, ignoreCase = true) } }
                        .map { item -> Pair(sec.name, item) }
                }
                matched.forEach { pair ->
                    Spacer(modifier = Modifier.height(8.dp))
                    val secName = pair.first
                    val itItem = pair.second
                    Card(modifier = Modifier.fillMaxWidth().clickable {
                        try {
                            val url = itItem.fileUrl
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
                        } catch (ex: Exception) { ex.printStackTrace() }
                    }) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = itItem.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            if (itItem.description.isNotBlank()) Text(text = itItem.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Section: $secName", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Dialogues
        if (showAddSection) {
            var input by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = { showAddSection = false }, confirmButton = {
                Button(onClick = {
                    if (input.isNotBlank()) {
                        PlasmaManualStorage.addSection(ctx, input.trim())
                        sections.clear(); sections.addAll(PlasmaManualStorage.loadAll(ctx))
                    }
                    showAddSection = false
                }) { Text("Add") }
            }, text = {
                Column { OutlinedTextField(value = input, onValueChange = { v -> input = v }, label = { Text("Section name") }) }
            })
        }

        if (showAddItem != null) {
            val section = showAddItem!!
            var title by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = { showAddItem = null }, confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        // open file picker
                        showPickPdfForSection = section
                        showAddItem = null
                    }
                }) { Text("Next") }
            }, text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { v -> title = v }, label = { Text("Title") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = description, onValueChange = { v -> description = v }, label = { Text("Description") })
                }
            })
        }

        if (showPickPdfForSection != null) {
            val section = showPickPdfForSection!!
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { pdfPicker.launch(arrayOf("application/pdf")); }) { Text("Pick PDF") }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = pickedPdfUri?.lastPathSegment ?: "No file selected")
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(onClick = {
                        if (pickedPdfUri != null) {
                            // copy to internal and add
                            val destName = "manual_${section}_${System.currentTimeMillis()}.pdf"
                            val saved = try {
                                var ok = false
                                val uri = pickedPdfUri!!
                                ctx.contentResolver.openInputStream(uri)?.use { input ->
                                    val dest = File(ctx.filesDir, destName)
                                    FileOutputStream(dest).use { out -> input.copyTo(out) }
                                    ok = true
                                }
                                ok
                            } catch (e: Exception) { e.printStackTrace(); false }
                            if (saved) {
                                val fileUri = Uri.fromFile(File(ctx.filesDir, destName)).toString()
                                PlasmaManualStorage.addItem(ctx, section, "New manual", fileUri, "")
                                sections.clear(); sections.addAll(PlasmaManualStorage.loadAll(ctx))
                                showPickPdfForSection = null
                                pickedPdfUri = null
                            }
                        }
                    }) { Text("Save") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { showPickPdfForSection = null; pickedPdfUri = null }) { Text("Cancel") }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
