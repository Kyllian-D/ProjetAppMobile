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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.plasmatrack.ui.theme.PlasmaTrackTheme
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Exception
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class ProductDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Si on ouvre l'écran en mode création pour superadmin, activer le mode plein écran (masquer status/navigation bars)
        val isSuperadminMode = intent.getBooleanExtra("is_superadmin", false)
        if (isSuperadminMode) {
            // allow drawing behind system bars
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            // autoriser l'affichage temporaire par swipe
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

        val productId = intent.getLongExtra("product_id", -1L)
        // Load product if exists
        val product = ProductStorage.loadProducts(this).firstOrNull { it.id == productId }

        setContent {
            PlasmaTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ProductDetailEditor(productId = productId, initialProduct = product)
                }
            }
        }
    }
}

private fun copyUriToInternalFile(context: Context, uri: Uri): String? {
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

// Sections loader (same key as used in ShopActivity)
private fun loadSectionsLocal(context: Context): MutableList<String> {
    val sp = context.getSharedPreferences("shop_prefs", Context.MODE_PRIVATE)
    val joined = sp.getString("product_sections", null)
    return if (joined.isNullOrEmpty()) {
        mutableListOf("ENDOSCOPE")
    } else {
        joined.split("||").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    }
}

@Composable
fun ProductDetailEditor(productId: Long, initialProduct: Product?) {
    val context = LocalContext.current
    // determine if editing existing or creating
    val isCreate = productId == -1L || initialProduct == null

    // Name should start empty by default to force user input
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var description by remember { mutableStateOf(initialProduct?.description ?: "") }
    var overview by remember { mutableStateOf(initialProduct?.description ?: "") }
    var existingImagePath by remember { mutableStateOf(initialProduct?.imagePath) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }

    // image picker
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            // persist permission
            try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            pickedUri = it
        }
    }

    // Sections state and selected section
    val sectionsState = remember { mutableStateListOf<String>().apply { addAll(loadSectionsLocal(context)) } }
    var section by remember { mutableStateOf(initialProduct?.section ?: (sectionsState.firstOrNull() ?: "ENDOSCOPE")) }
    var sectionExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .statusBarsPadding()
        .padding(16.dp)) {

        IconButton(onClick = { (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() }) {
            Icon(painter = painterResource(id = R.drawable.arrowleft), contentDescription = "Back")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Image area card
        Box(modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)) {
            Card(
                modifier = Modifier.size(width = 374.dp, height = 460.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp), contentAlignment = Alignment.Center) {

                    // determine image uri to display
                    val imageUriToShow = pickedUri ?: existingImagePath?.let { p -> File(p).takeIf { it.exists() }?.let { Uri.fromFile(it) } }

                    if (imageUriToShow != null) {
                        AndroidView(factory = { ctx ->
                            ImageView(ctx).apply {
                                adjustViewBounds = true
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                setImageURI(imageUriToShow)
                                clipToOutline = true
                            }
                        }, modifier = Modifier.size(width = 350.dp, height = 420.dp).clip(RoundedCornerShape(16.dp)))
                    } else {
                        // placeholder with clickable to add image
                        Box(modifier = Modifier
                            .size(width = 350.dp, height = 420.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .clickable { picker.launch(arrayOf("image/*")) }, contentAlignment = Alignment.Center) {
                            Text(text = "Add image", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section selector
        OutlinedTextField(
            value = section,
            onValueChange = { /* read-only */ },
            readOnly = true,
            label = { Text("Section") },
            trailingIcon = {
                IconButton(onClick = { sectionExpanded = !sectionExpanded }) { Icon(painter = painterResource(id = R.drawable.ic_settings), contentDescription = "Open sections") }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { sectionExpanded = true }
        )
        DropdownMenu(expanded = sectionExpanded, onDismissRequest = { sectionExpanded = false }) {
            sectionsState.forEach { s ->
                DropdownMenuItem(text = { Text(s) }, onClick = { section = s; sectionExpanded = false })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Editable fields for name, overview and details
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = overview, onValueChange = { overview = it }, label = { Text("Overview") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Details") }, modifier = Modifier.fillMaxWidth(), maxLines = 6)

        Spacer(modifier = Modifier.height(12.dp))

        // Buttons Save / Cancel
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                // save logic: if new -> add product, else update
                if (name.isBlank()) {
                    Toast.makeText(context, "Name is required", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val products = ProductStorage.loadProducts(context)
                var finalImagePath: String? = existingImagePath
                if (pickedUri != null) {
                    copyUriToInternalFile(context, pickedUri!!)?.let { newPath ->
                        // delete old file if different
                        existingImagePath?.let { old -> if (old != newPath) try { File(old).delete() } catch (_: Exception) {} }
                        finalImagePath = newPath
                    }
                }
                if (isCreate) {
                    val newProd = Product(id = System.currentTimeMillis(), name = name, description = description, section = section, imagePath = finalImagePath)
                    products.add(0, newProd)
                } else {
                    // in this branch initialProduct is guaranteed non-null, use direct property access
                    val targetId = initialProduct.id
                    val idx = products.indexOfFirst { it.id == targetId }
                     if (idx >= 0) {
                        val updated = products[idx].copy(name = name, description = description, imagePath = finalImagePath, section = section)
                        products[idx] = updated
                    }
                }
                // Persist sections in case user selected/created a new one via other flows
                // (no change here) -- sections are managed from Shop screen
                ProductStorage.saveProducts(context, products)
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                // return OK so caller can refresh
                (context as? Activity)?.setResult(Activity.RESULT_OK)
                (context as? Activity)?.finish()
             }) {
                 Text(text = "Save")
             }

            Button(onClick = { (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() }) {
                Text(text = "Cancel")
            }
        }
    }
}
