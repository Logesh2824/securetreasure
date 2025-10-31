package com.example.securetreasure

import android.location.Location
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

class ClueViewModel : ViewModel() {
    private val idGen = AtomicLong(1)
    private val _clues = MutableStateFlow<List<Clue>>(emptyList())
    val clues: StateFlow<List<Clue>> = _clues.asStateFlow()

    init {
        // Load persisted clues if repository was initialized
        try {
            val persisted = com.example.securetreasure.ClueRepository.loadClues()
            if (persisted.isNotEmpty()) {
                _clues.value = persisted
                // ensure id generator starts after the highest id (compute manually)
                var maxId = 0L
                for (c in persisted) {
                    if (c.id > maxId) maxId = c.id
                }
                idGen.set(maxId + 1)
            }
        } catch (e: Exception) {
            // if repository not initialized or load fails, keep empty
            e.printStackTrace()
        }
    }

    // Create and store a clue: encrypt plaintext with passphrase, compute hash and add to list
    fun createClue(
        title: String,
        plaintext: String,
        passphrase: String,
        latitude: Double? = null,
        longitude: Double? = null,
        radiusMeters: Float? = null
    ) {
        val encrypted = CryptoUtils.encrypt(plaintext, passphrase)
        val hash = CryptoUtils.sha256Hex(plaintext)
        val new = Clue(idGen.getAndIncrement(), title, encrypted, hash, latitude, longitude, radiusMeters)
        _clues.value = _clues.value + new
        // persist
        try {
            com.example.securetreasure.ClueRepository.saveClues(_clues.value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Try decrypt and verify integrity: returns Pair(plaintext or null, error message or null)
    fun tryUnlock(clue: Clue, passphrase: String): Pair<String?, String?> {
        val plain = CryptoUtils.decrypt(clue.encryptedPayload, passphrase)
        if (plain == null) {
            return Pair(null, "Decryption failed (wrong passphrase?)")
        }
        val computedHash = CryptoUtils.sha256Hex(plain)
        return if (computedHash.equals(clue.payloadHash, ignoreCase = true)) {
            Pair(plain, null)
        } else {
            Pair(null, "Integrity check failed (hash mismatch)")
        }
    }

    // New helper used by the UnlockDialog in MainActivity.kt
    // Attempts to decrypt and verify; if the clue is location-anchored, also checks the provided Location.
    // Returns decrypted plaintext on success, or null on failure (wrong passphrase / hash mismatch / location not within radius / missing location).
    fun attemptUnlock(clue: Clue, passphrase: String, location: Location?): String? {
        val plain = CryptoUtils.decrypt(clue.encryptedPayload, passphrase) ?: return null
        val computedHash = CryptoUtils.sha256Hex(plain)
        if (!computedHash.equals(clue.payloadHash, ignoreCase = true)) return null

        // If clue has location constraints, require provided location and verify proximity
        if (clue.latitude != null && clue.longitude != null && clue.radiusMeters != null) {
            if (location == null) return null
            val within = LocationUtils.isWithinRadius(location, clue.latitude, clue.longitude, clue.radiusMeters)
            if (!within) return null
        }

        return plain
    }
}
