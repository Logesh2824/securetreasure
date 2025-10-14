package com.example.securetreasure

data class Clue(
    val id: Long,
    val title: String,
    val encryptedPayload: String, // base64(salt+iv+ciphertext)
    val payloadHash: String // sha256 hex of plaintext (stored when creating)
)
