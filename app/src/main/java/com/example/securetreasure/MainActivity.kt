package com.example.securetreasure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    onCreate = { title, plain, pass ->
                        vm.createClue(title, plain, pass)
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
fun CreateClueScreen(onCreate: (String, String, String) -> Unit, onCancel: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var plain by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

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
        Row {
            Button(onClick = {
                if (title.isNotBlank() && plain.isNotBlank() && pass.isNotBlank())
                    onCreate(title, plain, pass)
            }) { Text("Create & Encrypt") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
fun UnlockDialog(clue: Clue, vm: ClueViewModel, onDismiss: () -> Unit) {
    var pass by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var unlockedText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    if (showSuccess) {
        UnlockSuccessScreen(unlockedText)
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlock: ${clue.title}") },
        text = {
            Column {
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation()
                )
                errorText?.let { Text(it, color = Color.Red) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val (plain, err) = vm.tryUnlock(clue, pass)
                if (plain != null) {
                    unlockedText = plain
                    showSuccess = true
                } else {
                    errorText = err ?: "Incorrect passphrase"
                }
            }) { Text("Unlock") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun UnlockSuccessScreen(text: String) {
    val infinite = rememberInfiniteTransition()
    val scale by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFEDF7ED)),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier.scale(scale).padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉 Clue Unlocked!", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Text(text, fontSize = 18.sp)
            }
        }
    }
}
