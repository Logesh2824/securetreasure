package com.example.securetreasure.models

data class Hunt(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val totalClues: Int = 0,
    val estimatedTime: String = "",
    val difficulty: String = "Medium",
    val startingPoint: String = ""
)

data class ClueData(
    val huntId: String = "",
    val stepNumber: Int = 0,
    val title: String = "",
    val encryptedCluePayload: String = "",
    val qrCodeContentHash: String = "",
    val locationLat: Double = 0.0,
    val locationLng: Double = 0.0,
    val locationRadius: Double = 20.0,
    val hintText: String = ""
)

sealed class HuntState {
    object Navigating : HuntState()
    object ReadyToScan : HuntState()
    object Scanning : HuntState()
    data class Revealing(val decryptedText: String) : HuntState()
    object Loading : HuntState()
    data class Error(val message: String) : HuntState()
    object Complete : HuntState()
}

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float
)