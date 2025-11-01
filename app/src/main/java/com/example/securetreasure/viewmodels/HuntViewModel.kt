package com.example.securetreasure.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securetreasure.CryptoUtils
import com.example.securetreasure.models.*
import com.example.securetreasure.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class HuntViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _hunts = MutableStateFlow<List<Hunt>>(emptyList())
    val hunts: StateFlow<List<Hunt>> = _hunts.asStateFlow()

    private val _currentHunt = MutableStateFlow<Hunt?>(null)
    val currentHunt: StateFlow<Hunt?> = _currentHunt.asStateFlow()

    private val _currentClue = MutableStateFlow<ClueData?>(null)
    val currentClue: StateFlow<ClueData?> = _currentClue.asStateFlow()

    private val _huntState = MutableStateFlow<HuntState>(HuntState.Loading)
    val huntState: StateFlow<HuntState> = _huntState.asStateFlow()

    private val _distanceToTarget = MutableStateFlow<Double?>(null)
    val distanceToTarget: StateFlow<Double?> = _distanceToTarget.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadHunts()
    }

    fun loadHunts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getHunts { huntsList ->
                    _hunts.value = huntsList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _huntState.value = HuntState.Error("Failed to load hunts: ${e.message}")
            }
        }
    }

    fun loadHunt(huntId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getHunt(huntId) { hunt ->
                    _currentHunt.value = hunt
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _huntState.value = HuntState.Error("Failed to load hunt: ${e.message}")
            }
        }
    }

    fun loadClue(huntId: String, clueNumber: Int) {
        viewModelScope.launch {
            _huntState.value = HuntState.Loading
            try {
                repository.getClue(huntId, clueNumber) { clue ->
                    _currentClue.value = clue
                    _huntState.value = HuntState.Navigating
                }
            } catch (e: Exception) {
                _huntState.value = HuntState.Error("Failed to load clue: ${e.message}")
            }
        }
    }

    fun updateLocation(location: LocationData) {
        val clue = _currentClue.value ?: return
        val distance = calculateDistance(
            location.latitude,
            location.longitude,
            clue.locationLat,
            clue.locationLng
        )
        _distanceToTarget.value = distance

        // Check if within geofence
        if (distance <= clue.locationRadius && _huntState.value is HuntState.Navigating) {
            onGeofenceEntered()
        }
    }

    private fun onGeofenceEntered() {
        _huntState.value = HuntState.ReadyToScan
    }

    fun onScanQRCode(qrContent: String) {
        val clue = _currentClue.value ?: return

        // Hash the QR content
        val scannedHash = CryptoUtils.sha256Hex(qrContent)

        // Verify hash matches
        if (!scannedHash.equals(clue.qrCodeContentHash, ignoreCase = true)) {
            _huntState.value = HuntState.Error("Wrong QR Code! Try scanning the correct code.")
            return
        }

        // Try to decrypt using QR content as passphrase
        val decrypted = CryptoUtils.decrypt(clue.encryptedCluePayload, qrContent)

        if (decrypted != null) {
            onClueUnlockSuccess(decrypted)
        } else {
            onClueUnlockFail("Decryption failed")
        }
    }

    private fun onClueUnlockSuccess(plaintext: String) {
        _huntState.value = HuntState.Revealing(plaintext)
    }

    private fun onClueUnlockFail(error: String) {
        _huntState.value = HuntState.Error(error)
    }

    fun startScanning() {
        _huntState.value = HuntState.Scanning
    }

    fun cancelScanning() {
        _huntState.value = HuntState.ReadyToScan
    }

    fun acknowledgeError() {
        val currentState = _huntState.value
        if (currentState is HuntState.Error) {
            // Return to appropriate state based on distance
            val distance = _distanceToTarget.value
            val clue = _currentClue.value
            if (distance != null && clue != null && distance <= clue.locationRadius) {
                _huntState.value = HuntState.ReadyToScan
            } else {
                _huntState.value = HuntState.Navigating
            }
        }
    }

    fun completeHunt() {
        _huntState.value = HuntState.Complete
    }

    fun showHint(): String {
        return _currentClue.value?.hintText ?: "No hint available"
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}