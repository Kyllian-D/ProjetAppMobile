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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private const val CONN_FILE = "connection_cards.csv"

// Simple data model for a connection card row (columns A..F)
data class ConnectionCardItem(
    val brand: String,
    val connectionSet: String,
    val plasmabioticsRef: String = "",
    val pentaxItem: String = "",
    val cycleCode: String = "",
    val connectionCard: String,
    val raw: String? = null // raw CSV line for debug/full display
)

class ConnectionCardsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // hide system bars for this activity (keeps consistent behavior with Aqua)
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller?.let {
                it.hide(WindowInsetsCompat.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } catch (_: Exception) {
            // ignore on older devices
        }
        enableEdgeToEdge()
        setContent {
            PlasmaTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ConnectionCardsScreen()
                }
            }
        }
    }
}

private fun copyUriToInternalFile(context: android.content.Context, uri: Uri, destName: String = CONN_FILE): Boolean {
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

fun loadConnectionCardsFromFile(context: android.content.Context, fileName: String = CONN_FILE): List<ConnectionCardItem> {
    val list = mutableListOf<ConnectionCardItem>()
    try {
        val f = File(context.filesDir, fileName)
        if (!f.exists()) return list
        f.bufferedReader().useLines { lines ->
            val iter = lines.iterator()
            if (!iter.hasNext()) return list
            val headerLine = iter.next().trim().trimStart('\uFEFF')
            val headers = parseCsvLine2(headerLine).map { it.lowercase().trim() }

            // find index helper
            fun findIndex(vararg keys: String): Int {
                val lowered = headers
                for (k in keys) {
                    val key = k.lowercase()
                    val idx = lowered.indexOfFirst { it.contains(key) }
                    if (idx >= 0) return idx
                }
                for (i in lowered.indices) {
                    val h = lowered[i]
                    for (k in keys) if (h.contains(k.lowercase())) return i
                }
                return -1
            }

            // prefer fixed positions A..F when header has >=6 columns
            val brandIdx: Int
            val connSetIdx: Int
            val plasmaIdx: Int
            val pentaxIdx: Int
            val cycleIdx: Int
            val cardIdx: Int

            if (headers.size >= 6) {
                brandIdx = 0
                connSetIdx = 1
                plasmaIdx = 2
                pentaxIdx = 3
                cycleIdx = 4
                cardIdx = 5
            } else {
                brandIdx = findIndex("brand", "marque", "name")
                connSetIdx = findIndex("connection set", "endoscope", "endoscope model", "connectionset")
                plasmaIdx = findIndex("plasmabiotics", "plasma")
                pentaxIdx = findIndex("pentax", "pentax item")
                cycleIdx = findIndex("cycle code", "cycle")
                cardIdx = findIndex("connection card", "card", "carte")
            }

            while (iter.hasNext()) {
                val rawLine = iter.next().trim().trimStart('\uFEFF')
                if (rawLine.isBlank()) continue
                val parts = parseCsvLine2(rawLine)
                fun v(idx: Int, posFallback: Int): String {
                    return when {
                        idx >= 0 && idx < parts.size -> parts[idx].trim()
                        posFallback >= 0 && posFallback < parts.size -> parts[posFallback].trim()
                        else -> ""
                    }
                }

                val brandVal = v(brandIdx, 0)
                if (brandVal.isBlank()) continue
                val connectionSetVal = v(connSetIdx, 1)
                val plasmabioticsVal = v(plasmaIdx, 2)
                val pentaxVal = v(pentaxIdx, 3)
                val cycleVal = v(cycleIdx, 4)
                val cardVal = v(cardIdx, 5)

                list.add(
                    ConnectionCardItem(
                        brand = brandVal,
                        connectionSet = connectionSetVal,
                        plasmabioticsRef = plasmabioticsVal,
                        pentaxItem = pentaxVal,
                        cycleCode = cycleVal,
                        connectionCard = cardVal,
                        raw = rawLine
                    )
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

// very small CSV parser that handles simple quoted fields and both comma and semicolon separators
private fun parseCsvLine2(line: String): List<String> {
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

@Composable
fun ConnectionCardsScreen() {
    val context = LocalContext.current
    var items by remember { mutableStateOf(loadConnectionCardsFromFile(context)) }
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var showConfirmImport by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ConnectionCardItem?>(null) }
    var showDetail by remember { mutableStateOf(false) }
    // favorites for connection cards: use an immutable Set<String> stored in MutableState
    val connFavoritesState = remember { mutableStateOf(ProductStorage.loadConnFavorites(context).toSet()) }

    fun isSuperadminNow(): Boolean {
        return SessionManager.isSuperadmin(context)
    }

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val ok = copyUriToInternalFile(context, it)
            if (ok) {
                items = loadConnectionCardsFromFile(context)
                Toast.makeText(context, "CSV imported (${items.size} rows)", Toast.LENGTH_SHORT).show()
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
                // build a unique id for the connection item
                val rawUid = (item.brand + "|" + item.connectionSet).trim()
                val prefUid = "AQUA|$rawUid"
                // consider old entries (no prefix) as AQUA for backward compatibility
                val isFav = connFavoritesState.value.contains(prefUid) || connFavoritesState.value.contains(rawUid)

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
                            // heart icon (22.dp) aligned to the right
                            IconButton(onClick = {
                                val current = connFavoritesState.value.toMutableSet()
                                if (current.contains(prefUid) || current.contains(rawUid)) {
                                    // remove both forms if present
                                    current.remove(prefUid)
                                    current.remove(rawUid)
                                } else {
                                    current.add(prefUid)
                                }
                                connFavoritesState.value = current
                                ProductStorage.saveConnFavorites(context, current)
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
                            append("AquaTYPHOON Connection set Ref. PlasmaBiotics : ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(it.plasmabioticsRef) }
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = buildAnnotatedString {
                            append("AquaTYPHOON Connection set PENTAX Item N°: ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(it.pentaxItem) }
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = buildAnnotatedString {
                            append("AquaTYPHOON Cycle code : ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(it.cycleCode) }
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = buildAnnotatedString {
                            append("AquaTYPHOON Connection card : ")
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
