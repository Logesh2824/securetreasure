package com.example.securetreasure.services

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HintTimerService(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    private val _timeElapsed = MutableStateFlow(0L)
    val timeElapsed: StateFlow<Long> = _timeElapsed

    companion object {
        const val HINT_DELAY_MILLIS = 1 * 60 * 1000L // 15 minutes
    }

    fun startTimer(hintText: String, notificationService: NotificationService) {
        stopTimer() // Cancel any existing timer

        timerJob = scope.launch {
            var elapsed = 0L
            _timeElapsed.value = 0L

            while (elapsed < HINT_DELAY_MILLIS && isActive) {
                delay(1000L) // Update every second
                elapsed += 1000L
                _timeElapsed.value = elapsed
            }

            // Send hint notification after 15 minutes
            if (isActive) {
                withContext(Dispatchers.Main) {
                    notificationService.sendHintNotification(hintText)
                }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _timeElapsed.value = 0L
    }

    fun cleanup() {
        stopTimer()
        scope.cancel()
    }
}