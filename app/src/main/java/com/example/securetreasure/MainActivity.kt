package com.example.securetreasure

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.securetreasure.ui.theme.SecuretreasureTheme
import com.example.securetreasure.navigation.AppNavigation
import com.example.securetreasure.services.LocationService
import com.example.securetreasure.services.NotificationService
import com.example.securetreasure.services.ShakeDetector
import com.example.securetreasure.services.HintTimerService
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    private lateinit var locationService: LocationService
    private lateinit var notificationService: NotificationService
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var hintTimerService: HintTimerService

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            locationService.startLocationUpdates()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize services
        locationService = LocationService(this)
        notificationService = NotificationService(this)
        shakeDetector = ShakeDetector(this)
        hintTimerService = HintTimerService(this)

        // Request permissions
        requestLocationPermissions()
        requestNotificationPermission()
        notificationService.createNotificationChannel()

        setContent {
            SecuretreasureTheme(darkTheme = true) {
                AppNavigation(
                    locationService = locationService,
                    notificationService = notificationService,
                    shakeDetector = shakeDetector,
                    hintTimerService = hintTimerService
                )
            }
        }
    }

    private fun requestLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                locationService.startLocationUpdates()
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationService.stopLocationUpdates()
        shakeDetector.stop()
        hintTimerService.cleanup()
    }
}