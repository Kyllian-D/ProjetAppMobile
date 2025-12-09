package com.example.plasmatrack

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.plasmatrack.ui.theme.PlasmaTrackTheme
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import java.io.File
import androidx.compose.ui.draw.clip
import androidx.core.view.WindowCompat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import android.app.Activity
import android.content.Intent

class ProductDetailActivityUser : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            @Suppress("DEPRECATION")
            run {
                window.navigationBarColor = android.graphics.Color.WHITE
            }
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
        } catch (_: Exception) { }
        val productId = intent.getLongExtra("product_id", -1L)
        val product = ProductStorage.loadProducts(this).firstOrNull { it.id == productId }

        setContent {
            PlasmaTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ProductDetailUserScreen(product)
                }
            }
        }
    }
}

@Composable
fun ProductDetailUserScreen(product: Product?) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .padding(16.dp)) {

            IconButton(onClick = { /* back */ (ctx as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() }) {
                Icon(painter = painterResource(id = R.drawable.arrowleft), contentDescription = "Back")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Image preview : white rounded card with elevation (no rectangle16products drawable)
            Box(modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
            ) {
                Card(
                    modifier = Modifier.size(width = 374.dp, height = 460.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp), contentAlignment = Alignment.Center) {
                        val imageShown = product?.imagePath?.let { path ->
                            val file = File(path)
                            if (file.exists()) Uri.fromFile(file) else null
                        }

                        if (imageShown != null) {
                            AndroidView(factory = { c ->
                                ImageView(c).apply {
                                    adjustViewBounds = true
                                    scaleType = ImageView.ScaleType.CENTER_CROP
                                    setImageURI(imageShown)
                                }
                            }, modifier = Modifier
                                .size(width = 350.dp, height = 420.dp)
                                .clip(RoundedCornerShape(16.dp)))
                        } else {
                            // default image inside white card
                            Image(
                                painter = painterResource(id = R.drawable.imagedefaultproducts),
                                contentDescription = "Default product",
                                modifier = Modifier
                                    .size(width = 350.dp, height = 420.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(text = product?.name ?: "Unknown product", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Overview", style = MaterialTheme.typography.titleMedium)
            Text(text = product?.description ?: "No overview available.")
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Details", style = MaterialTheme.typography.titleMedium)
            Text(text = product?.description ?: "No details available.")

            Spacer(modifier = Modifier.height(96.dp))
        }

        // Bottom bar (consistent with other screens)
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
                IconButton(onClick = { activity?.startActivity(Intent(activity, HomeActivity::class.java)) }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.iconhomeb), contentDescription = "home", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }

                IconButton(onClick = {
                    val intent = Intent(activity, ClockActivity::class.java)
                    try { activity?.startActivity(intent) } catch (_: Exception) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(intent) }
                }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.iconclockb), contentDescription = "clock", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }

                IconButton(onClick = {
                    val intent = Intent(activity, FavoritesActivity::class.java)
                    try { activity?.startActivity(intent) } catch (_: Exception) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(intent) }
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
