package com.example.plasmatrack

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.core.content.edit
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import com.example.plasmatrack.ui.theme.PlasmaTrackTheme
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextOverflow


class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Pré-créer un compte de test uniquement si aucun compte n'existe encore,
        // afin de ne pas écraser un compte créé par l'utilisateur au redémarrage.
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        // One-time cleanup: handle leftover inconsistent superadmin keys BEFORE auto-login
        try {
            val cleanupDone = prefs.getBoolean("cleanup_superadmin_done", false)
            val storedUser = prefs.getString("saved_user", null)
            val storedPass = prefs.getString("saved_pass", null)
            val storedRole = prefs.getString("saved_role", null)
            val rememberUser = prefs.getString("remember_user", null)

            // If stored_user == 'superadmin' but not the hardcoded account, wipe the corrupt keys once.
            if (!cleanupDone && storedUser != null && storedUser.equals("superadmin", ignoreCase = true) && storedPass != "888") {
                prefs.edit {
                    remove("saved_user")
                    remove("saved_pass")
                    remove("saved_email")
                    remove("saved_first")
                    remove("saved_last")
                    remove("saved_role")
                    putBoolean("cleanup_superadmin_done", true)
                }
            }

            // If saved_role == 'superadmin' but the stored user is NOT the hardcoded superadmin account, remove the saved_role once.
            val cleanupRoleDone = prefs.getBoolean("cleanup_superadmin_role_done", false)
            if (!cleanupRoleDone && storedRole != null && storedRole.equals("superadmin", ignoreCase = true)) {
                if (!(storedUser != null && storedUser.equals("superadmin", ignoreCase = true) && storedPass == "888")) {
                    prefs.edit {
                        remove("saved_role")
                        putBoolean("cleanup_superadmin_role_done", true)
                    }
                } else {
                    prefs.edit { putBoolean("cleanup_superadmin_role_done", true) }
                }
            }

            // If remember_user is set to superadmin but the hardcoded superadmin credentials are not persisted, clear the remember flags
            if (!rememberUser.isNullOrBlank() && rememberUser.equals("superadmin", ignoreCase = true)) {
                // If the stored_pass is not the hardcoded superadmin pass, or saved_user is not the hardcoded, clear remember
                if (!(storedUser != null && storedUser.equals("superadmin", ignoreCase = true) && storedPass == "888")) {
                    prefs.edit {
                        remove("remember")
                        remove("remember_user")
                    }
                }
            }
        } catch (_: Exception) {
            // ignore errors during cleanup
        }

        // Ensure there is a default test account if none exists
        if (prefs.getString("saved_user", null).isNullOrBlank()) {
            prefs.edit {
                putString("saved_user", "kyllian")
                putString("saved_pass", "123")
                // ne pas définir de rôle par défaut ici (évite d'écraser saved_role)
            }
        }

        // If user previously asked to be remembered, skip login and go straight to Home
        try {
            val remember = prefs.getBoolean("remember", false)
            if (remember) {
                // If remember_user exists and equals superadmin but credentials are not the hardcoded ones, avoid auto-login
                val rememberUser = prefs.getString("remember_user", null)
                if (rememberUser != null && rememberUser.equals("superadmin", ignoreCase = true)) {
                    // Do not auto-login as superadmin
                } else {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    return
                }
            }
        } catch (_: Exception) { /* ignore */ }

        setContent {
            PlasmaTrackTheme {
                // Use theme background instead of hard-coded white so all screens match
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LoginScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Log In, 1 = Sign Up

    // Common fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    // Sign up specific fields
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    val roles = listOf("Hospital staff", "Technician", "Sales")
    var role by remember { mutableStateOf(roles.first()) }
    var roleExpanded by remember { mutableStateOf(false) }
    var passwordConfirm by remember { mutableStateOf("") }

    // Reset fields when switching tabs to avoid leftover values
    LaunchedEffect(selectedTab) {
        // When switching tabs, clear inputs (keeps behavior simple)
        email = ""
        password = ""
        passwordConfirm = ""
        firstName = ""
        lastName = ""
        role = roles.first()
        roleExpanded = false
        rememberMe = false
        passwordVisible = false
    }

    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Foreground content (login form) on white background
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Header principal (titre + sous-texte) au-dessus du formulaire
            // Replaced by the image getstartednow

            Image(
                painter = painterResource(id = R.drawable.getstartednow),
                contentDescription = "Get started",
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                contentScale = ContentScale.Fit,
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Create an account or log in", color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            // Message field (simple placeholder as in the mock)
           /* OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                singleLine = true,
                placeholder = { Text(text = "Message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            )*/

            Spacer(modifier = Modifier.height(8.dp))

            // Rounded blue box that contains the TabRow and the white form card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color(0xFF0F4C81), shape = RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Tabs inside the blue box
                    TabRow(selectedTabIndex = selectedTab, modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp), containerColor = Color.Transparent) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                            Box(modifier = Modifier
                                .padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Text(text = "Log In", color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.LightGray)
                            }
                        }
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                            Box(modifier = Modifier
                                .padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Text(text = "Sign Up", color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.LightGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Card-like column for fields (unchanged, placed inside the blue box)
                    // make the white card scrollable when Sign Up is selected so the Sign Up button is reachable
                    val formScrollState = rememberScrollState()
                    val cardModifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
                        .padding(16.dp)
                        .heightIn(max = 420.dp)
                        .then(if (selectedTab == 1) Modifier.verticalScroll(formScrollState) else Modifier)

                    Column(modifier = cardModifier) {

                        if (selectedTab == 0) {
                            // --- LOGIN FORM ---
                            Text(text = "Email", color = Color.Gray)
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 12.dp),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Text(text = "Password", color = Color.Gray)
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(36.dp)) {
                                        Icon(
                                            // Utiliser les images raster fournies
                                            painter = painterResource(id = if (passwordVisible) R.drawable.eyeinvisible else R.drawable.eyevisible),
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 12.dp),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Checkbox + label on one row (full width)
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Remember me",
                                    modifier = Modifier
                                        .clickable { rememberMe = !rememberMe }
                                        .padding(end = 4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    softWrap = true
                                )
                            }

                            // Forgot Password placed below, aligned to the start (left) of the card
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                TextButton(onClick = { /* TODO: navigate to forgot-password */ }) {
                                    Text(text = "Forgot Password ?", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Log In button - validate credentials and navigate to HomeActivity on success
                            Button(
                                onClick = {
                                    val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                                    val savedUser = prefs.getString("saved_user", null)
                                    val savedPass = prefs.getString("saved_pass", null)
                                    val savedEmail = prefs.getString("saved_email", null)

                                    val normalizedInputUser = email.trim()

                                    // Cas spécial : session superadmin codée en dur
                                    if (normalizedInputUser.equals("superadmin", ignoreCase = true) && password == "888") {
                                        // Do NOT persist any superadmin info into SharedPreferences.
                                        // Keep only the optional "remember" flag if the user asked for it
                                        // (remember_user will store the literal login name but that's
                                        // fine for a temporary remembered session separate from saved_user).
                                        if (rememberMe) {
                                            // Only remember that the app should auto-open next time,
                                            // do NOT store the login username when it's the hardcoded superadmin.
                                            prefs.edit { putBoolean("remember", true) }
                                        } else {
                                            prefs.edit { remove("remember"); remove("remember_user") }
                                        }
                                        // Set in-memory session for this run and start HomeActivity.
                                        SessionManager.setSession("superadmin", "Admin")
                                        val intent = Intent(context, HomeActivity::class.java).apply {
                                            putExtra("session_role", "superadmin")
                                            putExtra("session_first", "Admin")
                                        }
                                        context.startActivity(intent)
                                        (context as? Activity)?.finish()
                                        return@Button
                                    }

                                    if ((normalizedInputUser == savedUser || normalizedInputUser == savedEmail || normalizedInputUser == "$savedUser@domain") && password == savedPass) {
                                         // Successful login -> set in-memory session from persisted profile and open HomeActivity
                                         val savedFirst = prefs.getString("saved_first", savedUser ?: "user")
                                         val savedRole = prefs.getString("saved_role", "")
                                         SessionManager.setSession(savedRole, savedFirst)
                                         if (rememberMe) {
                                             prefs.edit {
                                                 putBoolean("remember", true)
                                                 putString("remember_user", normalizedInputUser)
                                             }
                                         } else {
                                             prefs.edit {
                                                 remove("remember")
                                                 remove("remember_user")
                                             }
                                         }
                                         context.startActivity(Intent(context, HomeActivity::class.java))
                                         (context as? Activity)?.finish()
                                     } else {
                                         Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show()
                                     }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(text = "Log In", color = Color.White)
                                }
                            }

                        } else {
                            // --- SIGN UP FORM ---
                            // Last name
                            Text(text = "Last name", color = Color.Gray)
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 12.dp),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // First name
                            Text(text = "First name", color = Color.Gray)
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 12.dp),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Email
                            Text(text = "Email", color = Color.Gray)
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 12.dp),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Role dropdown
                            Text(text = "Role", color = Color.Gray)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = role,
                                    onValueChange = { /* read-only */ },
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = { roleExpanded = !roleExpanded }) {
                                            Icon(painter = painterResource(id = R.drawable.ic_settings), contentDescription = "Open role")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { roleExpanded = true }
                                        .padding(top = 8.dp, bottom = 12.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                androidx.compose.material3.DropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                                    roles.forEach { r ->
                                        androidx.compose.material3.DropdownMenuItem(text = { Text(r) }, onClick = {
                                            role = r
                                            roleExpanded = false
                                        })
                                    }
                                }
                            }

                            // Password
                            Text(text = "Password", color = Color.Gray)
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            painter = painterResource(id = if (passwordVisible) R.drawable.eyeinvisible else R.drawable.eyevisible),
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 12.dp),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Confirm password
                            Text(text = "Confirm password", color = Color.Gray)
                            OutlinedTextField(
                                value = passwordConfirm,
                                onValueChange = { passwordConfirm = it },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(36.dp)) {
                                        Icon(
                                            painter = painterResource(id = if (passwordVisible) R.drawable.eyeinvisible else R.drawable.eyevisible),
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                            modifier = Modifier.size(20.dp)

                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 12.dp),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Sign Up button
                            Button(
                                onClick = {
                                    // Validation simple
                                    if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank() || passwordConfirm.isBlank()) {
                                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (password != passwordConfirm) {
                                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    // Empêcher la création d'un compte 'superadmin' depuis l'application
                                    val username = email.substringBefore('@').ifBlank { email }
                                    if (username.equals("superadmin", ignoreCase = true) || email.equals("superadmin", ignoreCase = true) || email.startsWith("superadmin@", ignoreCase = true)) {
                                        Toast.makeText(context, "Cannot create account with reserved name 'superadmin'", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }

                                    // Sauvegarder le compte dans SharedPreferences
                                    val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                                    prefs.edit {
                                        putString("saved_user", username)
                                        putString("saved_email", email)
                                        putString("saved_pass", password)
                                        putString("saved_first", firstName)
                                        putString("saved_last", lastName)
                                        putString("saved_role", role)
                                    }

                                    // Set in-memory session for the newly created user (not superadmin)
                                    SessionManager.setSession(role, firstName)

                                    // Auto-login -> ouvrir HomeActivity
                                    Toast.makeText(context, "Account created", Toast.LENGTH_SHORT).show()
                                    context.startActivity(Intent(context, HomeActivity::class.java))
                                    (context as? Activity)?.finish()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(text = "Sign Up", color = Color.White)
                                }
                            }

                        }

                    }

                }
            }

            // Area for the company logo (centered under the form)
            // Placed inside the Column so it appears after the form (avoids overlap)
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Logo principal
                    Image(
                        // Use the direct raster drawable (PNG) to avoid painterResource XML/vector issues
                        painter = painterResource(id = R.drawable.plasmabioticslogo),
                        contentDescription = "Company logo",
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(100.dp)
                            .padding(horizontal = 8.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Image draw.png placée sous le logo, recadrée et arrondie
                    Image(
                        painter = painterResource(id = R.drawable.draw),
                        contentDescription = "Image draw",
                        modifier = Modifier
                            .fillMaxWidth(0.5f)              // largeur relative réduite
                            .heightIn(max = 120.dp)          // limite la hauteur sans forcer le recadrage
                             .clip(RoundedCornerShape(12.dp))
                             .padding(horizontal = 8.dp),
                         contentScale = ContentScale.Fit,     // affiche l'image entière (pas de découpe)
                         alignment = Alignment.Center
                     )
                 }
             }

         }

     }
 }

 @Preview(showBackground = true)
 @Composable
 fun LoginPreview() {
     PlasmaTrackTheme {
         LoginScreen()
     }
 }
