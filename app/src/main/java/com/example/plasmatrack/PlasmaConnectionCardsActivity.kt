package com.example.plasmatrack

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.plasmatrack.ui.theme.PlasmaTrackTheme
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.util.Log

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private const val TAG = "PlasmaConnCards"


data class ConnectionCardItem2(
    val brand: String,
    val connectionSet: String,
    val plasmabioticsRef: String = "",
    val pentaxItem: String = "",
    val cycleCode: String = "",
    val connectionCard: String,
    val raw: String? = null
)

class PlasmaConnectionCardsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // masquer les barres système pour cette activité (maintient un comportement cohérent avec Aqua)
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller?.let {
                it.hide(WindowInsetsCompat.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } catch (_: Exception) {

        }
        enableEdgeToEdge()
        setContent {
            PlasmaTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PlasmaConnectionCardsScreen()
                }
            }
        }
    }
}

// REMARQUE : utilisez un nom de fichier distinct pour les cartes de connexion Plasma afin de ne pas écraser le fichier générique connection_cards.csv
private const val PLASMA_CONN_FILE = "plasma_connection_cards.csv"

private fun copyUriToInternalFile2(context: android.content.Context, uri: Uri, destName: String = PLASMA_CONN_FILE): Boolean {
    return try {
        val resolver = context.contentResolver
        resolver.openInputStream(uri).use { input ->
            val dest = File(context.filesDir, destName)
            FileOutputStream(dest).use { out ->
                input?.copyTo(out)
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// Chargeur modifié : accepte usePlus pour privilégier les colonnes "PlasmaTYPHOON+" lorsqu'il est vrai
fun loadConnectionCardsFromFile2(context: android.content.Context, usePlus: Boolean = false, fileName: String = PLASMA_CONN_FILE): List<ConnectionCardItem2> {
    val list = mutableListOf<ConnectionCardItem2>()
    try {
        val f = File(context.filesDir, fileName)
        if (!f.exists()) return list
        val lines = f.readLines()
        if (lines.size <= 1) return list

        for (i in 1 until lines.size) {
            val rawLine = lines[i].trim().trimStart('\uFEFF')
            if (rawLine.isBlank()) continue
            val parts = parseCsvLinePlasma(rawLine)

            if (!usePlus) {
                // Plasma a besoin d'au moins 6 colonnes, map 0,1,2,3,4,5
                if (parts.size >= 6) {
                    val brand = parts[0].trim()
                    if (brand.isBlank()) continue
                    val connectionSet = parts[1].trim()
                    val plasmRef = parts[2].trim()
                    val pentax = parts[3].trim()
                    val cycle = parts[4].trim()
                    val card = parts[5].trim()
                    list.add(ConnectionCardItem2(brand = brand, connectionSet = connectionSet, plasmabioticsRef = plasmRef, pentaxItem = pentax, cycleCode = cycle, connectionCard = card, raw = rawLine))
                }
            } else {
                // Plasma+ a besoin d'au moins 10 colonnes, map 0,1,6,7,8,9
                if (parts.size >= 10) {
                    val brand = parts[0].trim()
                    if (brand.isBlank()) continue
                    val connectionSet = parts[1].trim()
                    val plasmRef = parts[6].trim()
                    val pentax = parts[7].trim()
                    val cycle = parts[8].trim()
                    val card = parts[9].trim()
                    list.add(ConnectionCardItem2(brand = brand, connectionSet = connectionSet, plasmabioticsRef = plasmRef, pentaxItem = pentax, cycleCode = cycle, connectionCard = card, raw = rawLine))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

// très petit analyseur CSV pour fichier Plasma (local à ce fichier) qui gère des champs cités simples et à la fois des séparateurs virgule et point-virgule
private fun parseCsvLinePlasma(line: String): List<String> {
    val result = mutableListOf<String>()
    val s = line.trim().trimStart('\uFEFF')
    var cur = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < s.length) {
        val c = s[i]
        when {
            c == '"' -> {
                if (inQuotes && i + 1 < s.length && s[i + 1] == '"') {
                    cur.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            }
            !inQuotes && (c == ',' || c == ';') -> {
                result.add(cur.toString().trim())
                cur = StringBuilder()
            }
            else -> cur.append(c)
        }
        i++
    }
    result.add(cur.toString().trim())
    return result
}

// Détecter les indices de colonnes à partir de l'en-tête pour le débogage (réutilise une logique similaire à celle du chargeur)
private fun detectIndicesFromHeader(headerLine: String, usePlus: Boolean): Map<String, Int> {
    val headers = parseCsvLinePlasma(headerLine).map { it.lowercase().trim() }
    fun findIndexPrefer(headers: List<String>, keys: List<String>, preferPlus: Boolean): Int {
        for (i in headers.indices) {
            val h = headers[i]
            val hasPlus = h.contains('+') || h.contains("+") || h.contains("plus")
            if (preferPlus && !hasPlus) continue
            if (!preferPlus && hasPlus) continue
            for (k in keys) if (h.contains(k)) return i
        }
        for (i in headers.indices) {
            val h = headers[i]
            for (k in keys) if (h.contains(k)) return i
        }
        return -1
    }

    val result = mutableMapOf<String, Int>()
    if (!usePlus) {
        if (headers.size >= 6) {
            result["brand"] = 0
            result["connSet"] = 1
            result["plasma"] = 2
            result["pentax"] = 3
            result["cycle"] = 4
            result["card"] = 5
        } else {
            result["brand"] = findIndexPrefer(headers, listOf("brand", "marque", "name"), false)
            result["connSet"] = findIndexPrefer(headers, listOf("connection set", "connectionset", "connection set ref", "endoscope", "endoscope model", "endoscope_model"), false)
            result["plasma"] = findIndexPrefer(headers, listOf("plasmabiotics", "plasma", "plasmabiotics reference", "plasmabiotics ref", "plasma ref"), false)
            result["pentax"] = findIndexPrefer(headers, listOf("pentax", "pentax item", "item n", "item no", "item n°"), false)
            result["cycle"] = findIndexPrefer(headers, listOf("cycle code", "cycle", "code"), false)
            result["card"] = findIndexPrefer(headers, listOf("connection card", "card"), false)
        }
    } else {
        if (headers.size >= 10) {
            result["brand"] = 0
            result["connSet"] = 1
            result["plasma"] = 6
            result["pentax"] = 7
            result["cycle"] = 8
            result["card"] = 9
        } else {
            result["brand"] = findIndexPrefer(headers, listOf("brand", "marque", "name"), true)
            result["connSet"] = findIndexPrefer(headers, listOf("connection set", "connectionset", "connection set ref", "endoscope", "endoscope model", "endoscope_model"), true)
            result["plasma"] = findIndexPrefer(headers, listOf("plasmabiotics", "plasma", "plasmabiotics reference", "plasmabiotics ref", "plasma ref"), true)
            result["pentax"] = findIndexPrefer(headers, listOf("pentax", "pentax item", "item n", "item no", "item n°"), true)
            result["cycle"] = findIndexPrefer(headers, listOf("cycle code", "cycle", "code"), true)
            result["card"] = findIndexPrefer(headers, listOf("connection card", "card"), true)
        }
    }
    return result
}

@Composable
fun PlasmaConnectionCardsScreen() {
    val context = LocalContext.current
    var selectedTabPlasma by remember { mutableStateOf(0) } // 0 = PlasmaTYPHOON, 1 = PlasmaTYPHOON+
    var items by remember { mutableStateOf(loadConnectionCardsFromFile2(context, usePlus = selectedTabPlasma == 1)) }
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var showConfirmImport by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ConnectionCardItem2?>(null) }
    var showDetail by remember { mutableStateOf(false) }
    // favoris pour les cartes de connexion plasma (liste d'état mutable)
    val connFavoritesState = remember { mutableStateListOf<String>().apply { addAll(ProductStorage.loadConnFavorites(context)) } }

    // recharger lorsque l'onglet change
    LaunchedEffect(selectedTabPlasma) {
        items = loadConnectionCardsFromFile2(context, usePlus = selectedTabPlasma == 1)
    }

    fun isSuperadminNow(): Boolean {
        return SessionManager.isSuperadmin(context)
    }

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val ok = copyUriToInternalFile2(context, it)
            if (ok) {
                //  lire le fichier brut et rapporter l'en-tête / le nombre de lignes
                try {
                    val f = File(context.filesDir, PLASMA_CONN_FILE)
                    val allLines = f.readLines()
                    val totalLines = allLines.size
                    val headerLine = if (allLines.isNotEmpty()) allLines[0] else ""
                    val indices = detectIndicesFromHeader(headerLine, selectedTabPlasma == 1)

                    // recharger les éléments analysés
                    items = loadConnectionCardsFromFile2(context, usePlus = selectedTabPlasma == 1)

                    val debugMsg = "File lines: $totalLines, Parsed rows: ${items.size}, Header: ${headerLine.take(200)}"
                    Log.i(TAG, debugMsg)
                    Log.i(TAG, "Detected indices: $indices")
                    Toast.makeText(context, debugMsg, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading imported file", e)
                    Toast.makeText(context, "Imported but failed to read file", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 16.dp)) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() }) {
                    Icon(painter = painterResource(id = R.drawable.arrowleft), contentDescription = "Retour", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Connection Cards", style = MaterialTheme.typography.titleLarge)
            }
            if (isSuperadminNow()) {
                Button(onClick = { showConfirmImport = true }) {
                    Text(text = "Import CSV")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        //  New tabs pour PlasmaTYPHOON / PlasmaTYPHOON+
        TabRow(selectedTabIndex = selectedTabPlasma, modifier = Modifier.fillMaxWidth(), containerColor = Color.Transparent) {
            Tab(selected = selectedTabPlasma == 0, onClick = { selectedTabPlasma = 0 }) {
                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(text = "PlasmaTYPHOON")
                }
            }
            Tab(selected = selectedTabPlasma == 1, onClick = { selectedTabPlasma = 1 }) {
                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(text = "PlasmaTYPHOON+")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text(text = "Search") })

        Spacer(modifier = Modifier.height(12.dp))

        val filtered = remember(query, items) {
            val q = query.text.trim().lowercase()
            if (q.isEmpty()) items else {
                val tokens = q.split(Regex("\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
                items.filter { item ->
                    tokens.all { token ->
                        item.brand.lowercase().contains(token) ||
                                item.connectionSet.lowercase().contains(token) ||
                                item.plasmabioticsRef.lowercase().contains(token) ||
                                item.pentaxItem.lowercase().contains(token) ||
                                item.cycleCode.lowercase().contains(token) ||
                                item.connectionCard.lowercase().contains(token)
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { item ->
                val rawUid = (item.brand + "|" + item.connectionSet).trim()
                // prefix depending on selected tab: 0 -> PLASMA, 1 -> PLASMA_PLUS
                val prefUid = if (selectedTabPlasma == 1) "PLASMA_PLUS|$rawUid" else "PLASMA|$rawUid"
                // legacy (older saved entries) may store rawUid without prefix
                val legacyUid = rawUid
                // Consider favorite only when stored with the current tab prefix or as legacy rawUid
                val isFav = connFavoritesState.contains(prefUid) || connFavoritesState.contains(legacyUid)

                Card(modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedItem = item
                        showDetail = true
                    }) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.brand, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Endoscope model: ${item.connectionSet}")
                                Text(text = "Card: ${item.connectionCard}")
                            }
                            IconButton(onClick = {
                                if (connFavoritesState.contains(prefUid) || connFavoritesState.contains(legacyUid)) {
                                    connFavoritesState.remove(prefUid)
                                    connFavoritesState.remove(legacyUid)
                                } else {
                                    connFavoritesState.add(prefUid)
                                }
                                ProductStorage.saveConnFavorites(context, connFavoritesState.toSet())
                            }) {
                                Icon(painter = painterResource(id = if (isFav) R.drawable.rediconheartb else R.drawable.iconheartb), contentDescription = "favorite", tint = Color.Unspecified, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmImport) {
        AlertDialog(
            onDismissRequest = { showConfirmImport = false },
            title = { Text(text = "Import CSV") },
            text = { Text(text = "Importing a CSV will replace all existing connection cards. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmImport = false
                    csvPicker.launch(arrayOf("text/csv", "text/*"))
                }) { Text(text = "Yes") }
            },
            dismissButton = { TextButton(onClick = { showConfirmImport = false }) { Text(text = "Cancel") } }
        )
    }

    if (showDetail && selectedItem != null) {
        val it = selectedItem!!
        AlertDialog(
            onDismissRequest = { showDetail = false; selectedItem = null },
            confirmButton = {
                TextButton(onClick = { showDetail = false; selectedItem = null }) { Text(text = "Close") }
            },
            title = { Text(text = it.brand, style = MaterialTheme.typography.headlineLarge) },
            text = {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(4.dp)) {
                    Text(
                        text = buildAnnotatedString {
                            append("Endoscope Model : ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(it.connectionSet) }
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = buildAnnotatedString {
                            append("PlasmaTYPHOON Connection set Ref. PlasmaBiotics : ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(it.plasmabioticsRef) }
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = buildAnnotatedString {
                            append("PlasmaTYPHOON Connection set PENTAX Item N°: ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(it.pentaxItem) }
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = buildAnnotatedString {
                            append("PlasmaTYPHOON Cycle code : ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(it.cycleCode) }
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = buildAnnotatedString {
                            append("PlasmaTYPHOON Connection card : ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(it.connectionCard) }
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        )
    }
}
