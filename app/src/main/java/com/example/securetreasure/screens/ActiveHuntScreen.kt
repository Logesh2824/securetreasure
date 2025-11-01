package com.example.securetreasure.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.securetreasure.models.HuntState
import com.example.securetreasure.scanner.QRScannerScreen
import com.example.securetreasure.services.LocationService
import com.example.securetreasure.services.NotificationService
import com.example.securetreasure.services.ShakeDetector
import com.example.securetreasure.viewmodels.HuntViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveHuntScreen(
    huntId: String,
    clueNumber: Int,
    viewModel: HuntViewModel,
    locationService: LocationService,
    notificationService: NotificationService,
    shakeDetector: ShakeDetector,
    onNextClue: (String, Int) -> Unit,
    onHuntComplete: () -> Unit,
    onBack: () -> Unit
) {
    val huntState by viewModel.huntState.collectAsState()
    val currentClue by viewModel.currentClue.collectAsState()
    val distanceToTarget by viewModel.distanceToTarget.collectAsState()
    val currentHunt by viewModel.currentHunt.collectAsState()
    val location by locationService.currentLocation.collectAsState()

    var showHintDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load clue on first composition
    LaunchedEffect(huntId, clueNumber) {
        viewModel.loadClue(huntId, clueNumber)
        viewModel.loadHunt(huntId)
    }

    // Update location
    LaunchedEffect(location) {
        location?.let { viewModel.updateLocation(it) }
    }

    // Listen for shake events
    LaunchedEffect(Unit) {
        shakeDetector.shakeEvent.collect {
            showHintDialog = true
        }
    }

    // Hint dialog
    if (showHintDialog) {
        AlertDialog(
            onDismissRequest = { showHintDialog = false },
            title = { Text("💡 Hint") },
            text = { Text(viewModel.showHint()) },
            confirmButton = {
                TextButton(onClick = { showHintDialog = false }) {
                    Text("Got it!")
                }
            }
        )
    }

    when (huntState) {
        is HuntState.Scanning -> {
            QRScannerScreen(
                onQRCodeScanned = { qrContent ->
                    viewModel.onScanQRCode(qrContent)
                },
                onCancel = {
                    viewModel.cancelScanning()
                }
            )
        }
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("Clue $clueNumber of ${currentHunt?.totalClues ?: "..."}")
                                Text(
                                    currentClue?.title ?: "Loading...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    when (huntState) {
                        is HuntState.Navigating -> {
                            NavigatingView(
                                clueTitle = currentClue?.title ?: "",
                                distance = distanceToTarget,
                                onShowHint = { showHintDialog = true }
                            )
                        }
                        is HuntState.ReadyToScan -> {
                            ReadyToScanView(
                                onScanQR = { viewModel.startScanning() },
                                onShowHint = { showHintDialog = true }
                            )
                        }
                        is HuntState.Revealing -> {
                            RevealingView(
                                clueText = (huntState as HuntState.Revealing).decryptedText,
                                onNext = {
                                    val nextClue = clueNumber + 1
                                    if (nextClue > (currentHunt?.totalClues ?: 0)) {
                                        viewModel.completeHunt()
                                    } else {
                                        onNextClue(huntId, nextClue)
                                    }
                                }
                            )
                        }
                        is HuntState.Loading -> {
                            LoadingView()
                        }
                        is HuntState.Error -> {
                            ErrorView(
                                message = (huntState as HuntState.Error).message,
                                onDismiss = { viewModel.acknowledgeError() }
                            )
                        }
                        is HuntState.Complete -> {
                            CompleteView(onFinish = onHuntComplete)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun NavigatingView(clueTitle: String, distance: Double?, onShowHint: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🧭",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Navigate to:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Text(
            text = clueTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        DistanceMeter(distance = distance)

        Spacer(Modifier.height(32.dp))

        OutlinedButton(
            onClick = onShowHint,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("💡 Show Hint (or shake phone)")
        }
    }
}

@Composable
fun DistanceMeter(distance: Double?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Distance to Target",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (distance != null) {
                    if (distance < 1000) "${distance.toInt()}m"
                    else "${String.format("%.1f", distance / 1000)}km"
                } else {
                    "Locating..."
                },
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ReadyToScanView(onScanQR: () -> Unit, onShowHint: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📍",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.alpha(alpha)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "You're Here!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "You've arrived at the location.\nScan the QR code to reveal the clue.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onScanQR,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "📱 Scan QR Code",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onShowHint,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("💡 Show Hint")
        }
    }
}

@Composable
fun RevealingView(clueText: String, onNext: () -> Unit) {
    var showAnimation by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000)
        showAnimation = false
    }

    if (showAnimation) {
        TreasureChestAnimation()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A237E),
                            Color(0xFF0D47A1)
                        )
                    )
                )
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🎉",
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Clue Unlocked!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Text(
                    text = clueText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(20.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107)
                )
            ) {
                Text(
                    text = "➡️ Next Clue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun TreasureChestAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🎁",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = MaterialTheme.typography.displayLarge.fontSize * scale
            )
        )
    }
}

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Loading next clue...")
        }
    }
}

@Composable
fun ErrorView(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("⚠️") },
        title = { Text("Oops!") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
fun CompleteView(onFinish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B5E20),
                        Color(0xFF388E3C)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🏆",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Congratulations!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "You've completed the treasure hunt!",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White
            )
        ) {
            Text(
                text = "Finish",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
        }
    }
}