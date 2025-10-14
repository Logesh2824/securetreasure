package com.example.securetreasure

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

class ClueViewModel : ViewModel() {
    private val idGen = AtomicLong(1)
    private val _clues = MutableStateFlow<List<Clue>>(emptyList())
    val clues: StateFlow<List<Clue>> = _clues.asStateFlow()

    // Create and store a clue: encrypt plaintext with passphrase, compute hash and add to list
    fun createClue(title: String, plaintext: String, passphrase: String) {
        val encrypted = CryptoUtils.encrypt(plaintext, passphrase)
        val hash = CryptoUtils.sha256Hex(plaintext)
        val new = Clue(idGen.getAndIncrement(), title, encrypted, hash)
        _clues.value = _clues.value + new
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
}
