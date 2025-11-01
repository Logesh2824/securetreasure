package com.example.securetreasure.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.securetreasure.screens.*
import com.example.securetreasure.services.LocationService
import com.example.securetreasure.services.NotificationService
import com.example.securetreasure.services.ShakeDetector
import com.example.securetreasure.viewmodels.HuntViewModel

sealed class Screen {
    object HuntList : Screen()
    data class HuntDetail(val huntId: String) : Screen()
    data class ActiveHunt(val huntId: String, val clueNumber: Int) : Screen()
}

@Composable
fun AppNavigation(
    locationService: LocationService,
    notificationService: NotificationService,
    shakeDetector: ShakeDetector
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.HuntList) }
    val huntViewModel: HuntViewModel = viewModel()

    when (val screen = currentScreen) {
        is Screen.HuntList -> {
            HuntListScreen(
                viewModel = huntViewModel,
                onHuntSelected = { huntId ->
                    currentScreen = Screen.HuntDetail(huntId)
                }
            )
        }
        is Screen.HuntDetail -> {
            HuntDetailScreen(
                huntId = screen.huntId,
                viewModel = huntViewModel,
                onStartHunt = { huntId ->
                    currentScreen = Screen.ActiveHunt(huntId, 1)
                },
                onBack = {
                    currentScreen = Screen.HuntList
                }
            )
        }
        is Screen.ActiveHunt -> {
            ActiveHuntScreen(
                huntId = screen.huntId,
                clueNumber = screen.clueNumber,
                viewModel = huntViewModel,
                locationService = locationService,
                notificationService = notificationService,
                shakeDetector = shakeDetector,
                onNextClue = { huntId, nextClueNumber ->
                    currentScreen = Screen.ActiveHunt(huntId, nextClueNumber)
                },
                onHuntComplete = {
                    currentScreen = Screen.HuntList
                },
                onBack = {
                    currentScreen = Screen.HuntList
                }
            )
        }
    }
}