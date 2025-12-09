package com.example.plasmatrack

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.plasmatrack.ui.theme.PlasmaTrackTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.view.WindowCompat

class ClockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Remettre la navigation bar en blanc et demander des icônes sombres
        try {
            window.navigationBarColor = android.graphics.Color.WHITE
            WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightNavigationBars = true
        } catch (_: Exception) {
            // ignore on older devices
        }

        setContent {
            PlasmaTrackTheme {
                // Forcer la Surface principale en blanc
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    ClockScreen()
                }
            }
        }
    }
}

@Composable
fun ClockScreen() {
    val context = LocalContext.current
    val activity = context as? Activity

    // reactive state list holding the labels (avoids MutableState<List> warning)
    val labels = remember { mutableStateListOf<ProductStorage.LabelRecord>().apply { addAll(ProductStorage.loadLabels(context)) } }
    // search query for Saved labels
    var query by remember { mutableStateOf(TextFieldValue("")) }

    // compute filtered labels based on query (trigger recompute when query.text or labels.size changes)
    val filteredLabels = remember(query.text, labels.size) {
        val q = query.text.trim().lowercase()
        if (q.isEmpty()) labels.toList() else {
            val tokens = q.split(Regex("\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
            labels.filter { item ->
                tokens.all { token ->
                    item.endoscope.lowercase().contains(token) ||
                    item.serial.lowercase().contains(token) ||
                    item.dateStr.lowercase().contains(token) ||
                    item.operator.lowercase().contains(token)
                }
            }
        }
    }

    var takingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var endoscope by remember { mutableStateOf(TextFieldValue("")) }
    var serial by remember { mutableStateOf(TextFieldValue("")) }
    var dateStr by remember { mutableStateOf(TextFieldValue(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))) }
    var operator by remember { mutableStateOf(TextFieldValue("")) }

    // camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok: Boolean ->
        if (ok && takingPhotoUri != null) {
            photoPath = takingPhotoUri.toString()
            Toast.makeText(context, "Photo captured", Toast.LENGTH_SHORT).show()
        }
    }

    // helper: parse user-entered date strings into epoch millis using several common patterns
    // returns Pair(millis, hasTime) where hasTime==true means the parsed pattern included time information
    fun parseDateStrToMillis(s: String?): Pair<Long, Boolean>? {
        if (s == null) return null
        val clean = s.trim().trimStart('\uFEFF')
        if (clean.isEmpty()) return null
        // patterns and whether they include a time component
        val patterns = listOf(
            Pair("yyyy-MM-dd HH:mm", true),
            Pair("yyyy-MM-dd", false),
            Pair("dd/MM/yyyy HH:mm", true),
            Pair("dd/MM/yyyy", false),
            Pair("yyyy/MM/dd HH:mm", true),
            Pair("yyyy/MM/dd", false),
            Pair("yyyy-MM-dd'T'HH:mm:ss'Z'", true),
            Pair("yyyy-MM-dd'T'HH:mm:ss", true)
        )
        for ((p, hasTime) in patterns) {
            try {
                val fmt = SimpleDateFormat(p, Locale.getDefault())
                fmt.isLenient = false
                val d = fmt.parse(clean)
                if (d != null) return Pair(d.time, hasTime)
            } catch (_: Exception) {
            }
        }
        // try interpret as millis number
        val asNum = clean.toLongOrNull()
        if (asNum != null) return Pair(asNum, true)
        return null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // forcer le fond du scaffold en blanc
        containerColor = Color.White,
        bottomBar = {
            // Remplacé : utiliser la même barre d'icônes que dans FavoritesActivity
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
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
                    IconButton(onClick = { activity?.startActivity(Intent(activity, HomeActivity::class.java)) }, modifier = Modifier.size(48.dp)) {
                        Icon(painter = painterResource(id = R.drawable.iconhomeb), contentDescription = "home", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                    }

                    IconButton(onClick = {
                        val intent = Intent(activity, ClockActivity::class.java)
                        try { activity?.startActivity(intent) } catch (_: Exception) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                    }, modifier = Modifier.size(48.dp)) {
                        Icon(painter = painterResource(id = R.drawable.blackiconclockb), contentDescription = "clock", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
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
    ) { innerPadding ->
        // compute nav bar bottom inset to ensure last item isn't hidden by system UI or bottom bar
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        LazyColumn(
             modifier = Modifier
                 .fillMaxSize()
                 .background(Color.White),
              contentPadding = PaddingValues(
                  start = 16.dp,
                  end = 16.dp,
                  // reduce top padding so the "Capture label" header is closer to the status bar
                  top = innerPadding.calculateTopPadding() + 18.dp,
                  // increase bottom padding so last list item isn't obscured by bottomBar or system nav
                  bottom = innerPadding.calculateBottomPadding() + navBarBottom + 96.dp
              ),
              verticalArrangement = Arrangement.spacedBy(0.dp)
          ) {
            // header + form as a single item
            item {
                // Header with back button (same pattern as other screens)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() }) {
                        Icon(painter = painterResource(id = R.drawable.arrowleft), contentDescription = "Retour", tint = Color.Black)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Capture label", style = MaterialTheme.typography.titleLarge)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Photo preview + capture
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (photoPath != null) {
                        Image(
                            painter = painterResource(id = R.drawable.draw2), // placeholder
                            contentDescription = "label photo",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEFEFEF)), contentAlignment = Alignment.Center) {
                            Text(text = "No photo")
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Button(onClick = {
                            val f = File(context.cacheDir, "label_${System.currentTimeMillis()}.jpg")
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                            takingPhotoUri = uri
                            cameraLauncher.launch(uri)
                        }) { Text(text = "Take photo") }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(onClick = {
                            takingPhotoUri = null
                            photoPath = null
                        }) { Text(text = "Clear") }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Form
                OutlinedTextField(value = endoscope, onValueChange = { endoscope = it }, label = { Text("Endoscope") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = serial, onValueChange = { serial = it }, label = { Text("Serial") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("Date (yyyy-MM-dd HH:mm)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = operator, onValueChange = { operator = it }, label = { Text("Operator") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    val e = endoscope.text.trim(); val s = serial.text.trim(); val d = dateStr.text.trim(); val o = operator.text.trim()
                    if (e.isEmpty() || s.isEmpty()) {
                        Toast.makeText(context, "Endoscope and Serial required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val rec = ProductStorage.LabelRecord(endoscope = e, serial = s, dateStr = d, operator = o, photoPath = photoPath)
                    labels.add(rec)
                    labels.sortBy { try { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(it.dateStr)?.time ?: it.savedAt } catch (_: Exception) { it.savedAt } }
                    ProductStorage.saveLabels(context, labels)
                    endoscope = TextFieldValue("")
                    serial = TextFieldValue("")
                    dateStr = TextFieldValue(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
                    operator = TextFieldValue("")
                    photoPath = null
                    takingPhotoUri = null
                    Toast.makeText(context, "Label saved", Toast.LENGTH_SHORT).show()
                }) { Text(text = "Save label") }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Saved labels (oldest → newest)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                // Search field for saved labels
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(text = "Search saved labels")
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // labels as list items (filtered by search)
            items(filteredLabels) { lbl ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFDDDFE6)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F3F7)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        // compute parsed/derived times at row scope so both columns can access
                        val parsedPair = parseDateStrToMillis(lbl.dateStr)
                        val parsedDateMillis = parsedPair?.first
                        val parsedHasTime = parsedPair?.second ?: false
                        val now = System.currentTimeMillis()
                        val dayMs = 24L * 3600L * 1000L
                        val due = if (parsedDateMillis != null) {
                            if (!parsedHasTime) {
                                val endOfDay = parsedDateMillis + dayMs - 1L
                                if (endOfDay > now) endOfDay else endOfDay + 7L * dayMs
                            } else {
                                if (parsedDateMillis > now) parsedDateMillis else parsedDateMillis + 7L * dayMs
                            }
                        } else {
                            lbl.savedAt + 7L * dayMs
                        }
                        val remaining = due - now
                        val daysLeftCeil = if (remaining <= 0L) 0L else ((remaining + dayMs - 1L) / dayMs)

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            // Affiche endoscope, serial, date (formatée si possible) et nombre de jours restants
                            Text(
                                text = lbl.endoscope,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Serial: ${lbl.serial}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            val displayDate = parsedDateMillis?.let { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it)) } ?: lbl.dateStr
                            Text(text = displayDate, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            // compact days indicator
                            Text(text = "${daysLeftCeil}j", style = MaterialTheme.typography.titleLarge)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            // Countdown badge: use the computed due
                            CountdownBadge(targetMillis = due, size = 24.dp, modifier = Modifier.padding(bottom = 8.dp))

                            Button(onClick = {
                                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                val intent = Intent(context, LabelAlarmReceiver::class.java)
                                intent.putExtra("labelId", lbl.id)
                                intent.putExtra("endoscope", lbl.endoscope)
                                val pi = PendingIntent.getBroadcast(context, lbl.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                                // set alarm for the computed due time
                                val alarmTime = due
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pi)
                                Toast.makeText(context, "Alarm set for label", Toast.LENGTH_SHORT).show()
                            }) { Text(text = "Set alarm") }

                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(onClick = {
                                labels.removeIf { r -> r.id == lbl.id }
                                ProductStorage.saveLabels(context, labels)
                            }) { Text(text = "Delete") }
                        }
                    }
                }
            }
        }
    }
}
