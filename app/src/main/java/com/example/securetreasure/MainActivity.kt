package com.example.securetreasure

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel


class MainActivity : ComponentActivity() {
    private lateinit var authLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize repository before any ViewModel or UI that may access it
        try {
            ClueRepository.initialize(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Register the Activity Result launcher for the confirm-device-credential intent
        authLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                showAppUI()
            } else {
                finish()
            }
        }

        val keyguard = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
        val isSecure = keyguard?.isKeyguardSecure == true

        if (isSecure) {
            val credIntent: Intent? = keyguard.createConfirmDeviceCredentialIntent(
                "Unlock Secure Treasure",
                "Confirm device credential to access clues"
            )
            if (credIntent != null) {
                authLauncher.launch(credIntent)
            } else {
                // fallback: show UI directly
                showAppUI()
            }
        } else {
            // Device has no secure lock screen; proceed but warn in logs
            showAppUI()
        }
    }

    private fun showAppUI() {
        setContent {
            SecureTreasureApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureTreasureApp(vm: ClueViewModel = viewModel()) {
    val clues by vm.clues.collectAsState()
    var screen by remember { mutableStateOf("list") } // "list" or "create"

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Secure Treasure - Phase 1") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { screen = "create" }) {
                Text("+")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (screen) {
                "list" -> ClueListScreen(clues = clues, vm = vm)
                "create" -> CreateClueScreen(
                    onCreate = { title, plain, pass, lat, lon, radius ->
                        vm.createClue(title, plain, pass, lat, lon, radius)
                        screen = "list"
                    },
                    onCancel = { screen = "list" }
                )
            }
        }
    }
}

@Composable
fun ClueListScreen(clues: List<Clue>, vm: ClueViewModel) {
    var selectedClue by remember { mutableStateOf<Clue?>(null) } // Track which clue is unlocking

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Encrypted Clues", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        if (clues.isEmpty()) {
            Text("No clues yet. Tap + to create one.")
        } else {
            LazyColumn {
                items(clues.size) { idx ->
                    val c = clues[idx]
                    Card(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.title, style = MaterialTheme.typography.titleMedium)
                                Text(c.encryptedPayload.take(32) + "...", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = { selectedClue = c }) {
                                Text("Unlock")
                            }
                        }
                    }
                }
            }
        }
    }

    // Conditionally show unlock dialog
    selectedClue?.let { clue ->
        UnlockDialog(clue = clue, vm = vm, onDismiss = { selectedClue = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClueScreen(onCreate: (String, String, String, Double?, Double?, Float?) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var plain by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    var attachLocation by remember { mutableStateOf(false) }
    var radiusStr by remember { mutableStateOf("50") }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var attachedLat by remember { mutableStateOf<Double?>(null) }
    var attachedLon by remember { mutableStateOf<Double?>(null) }

    // Permission launcher for ACCESS_FINE_LOCATION
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val loc = LocationUtils.getLastKnownLocation(context)
            if (loc != null) {
                attachedLat = loc.latitude
                attachedLon = loc.longitude
                statusMsg = "Location attached (lat=${loc.latitude}, lon=${loc.longitude})"
                attachLocation = true
            } else {
                statusMsg = "Couldn't obtain location. Try again later."
                attachLocation = false
            }
        } else {
            statusMsg = "Location permission denied"
            attachLocation = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Create Clue", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(value = title, onValueChange = { title = it },
            label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = plain, onValueChange = { plain = it },
            label = { Text("Clue Text (plaintext)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = pass, onValueChange = { pass = it },
            label = { Text("Passphrase (for AES)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Attach current location")
            Spacer(Modifier.width(8.dp))
            Switch(checked = attachLocation, onCheckedChange = { checked ->
                if (checked) {
                    // Request permission and try to attach
                    val permission = Manifest.permission.ACCESS_FINE_LOCATION
                    val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        val loc = LocationUtils.getLastKnownLocation(context)
                        if (loc != null) {
                            attachedLat = loc.latitude
                            attachedLon = loc.longitude
                            statusMsg = "Location attached (lat=${loc.latitude}, lon=${loc.longitude})"
                        } else {
                            statusMsg = "Couldn't obtain location. Try again later."
                        }
                    } else {
                        // Request the permission
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                } else {
                    // Detach location
                    attachedLat = null
                    attachedLon = null
                    statusMsg = "Location detached"
                }
                attachLocation = checked
            })
        }

        if (attachLocation) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Latitude: ${attachedLat?.let { "%.4f".format(it) } ?: "N/A"}")
                Text("Longitude: ${attachedLon?.let { "%.4f".format(it) } ?: "N/A"}")
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(value = radiusStr, onValueChange = { radiusStr = it },
            label = { Text("Radius (meters, optional)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        // Show status messages
        statusMsg?.let { msg ->
            Text(msg, color = if (msg.startsWith("Couldn't")) Color.Red else Color.Green)
        }

        Spacer(Modifier.height(12.dp))

        Row {
            Button(onClick = {
                val radius = radiusStr.toFloatOrNull()
                onCreate(title, plain, pass, attachedLat, attachedLon, radius)
            }, modifier = Modifier.weight(1f)) {
                Text("Create Clue")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Add this if you use ExperimentalMaterial3Api components like OutlinedTextField
@Composable
fun UnlockDialog(clue: Clue, vm: ClueViewModel, onDismiss: () -> Unit) {
    var passwordInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var decryptedContent by remember { mutableStateOf<String?>(null) } // To hold decrypted text
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlock Clue: ${clue.title}") },
        text = {
            Column {
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Enter Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                // Display encrypted clue snippet for context
                Text("Encrypted: ${clue.encryptedPayload.take(32)}...")

                errorMessage?.let { msg ->
                    Text(msg, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
                decryptedContent?.let { content ->
                    Spacer(Modifier.height(8.dp))
                    Text("Decrypted Clue:", style = MaterialTheme.typography.titleSmall)
                    Text(content, style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                errorMessage = null // Clear previous errors
                decryptedContent = null // Clear previous decrypted content

                if (passwordInput.isBlank()) {
                    errorMessage = "Passphrase cannot be empty."
                } else {
                    val location = LocationUtils.getLastKnownLocation(context)
                    val decrypted = vm.attemptUnlock(clue, passwordInput, location)
                    if (decrypted != null) {
                        errorMessage = null // Clear error on successful unlock
                        decryptedContent = decrypted
                    } else {
                        errorMessage = "Incorrect passphrase or location requirements not met."
                    }
                }
            }) {
                Text(if (decryptedContent == null) "Unlock" else "Re-verify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}