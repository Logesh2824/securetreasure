package com.example.securetreasure

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val AES_ALGO = "AES/CBC/PKCS5Padding"
    private const val PBKDF2_ALGO = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 65536
    private const val KEY_SIZE = 256 // bits
    private const val IV_SIZE = 16 // bytes
    private val secureRandom = SecureRandom()

    // Derive AES key from passphrase + salt
    fun deriveKey(passphrase: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_SIZE)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGO)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    // AES encrypt (returns base64(iv + ciphertext))
    fun encrypt(plainText: String, passphrase: String): String {
        val salt = ByteArray(16).also { secureRandom.nextBytes(it) } // salt for key derivation
        val key = deriveKey(passphrase, salt)
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Store: salt + iv + ciphertext (all base64)
        val combined = ByteArray(salt.size + iv.size + cipherBytes.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(cipherBytes, 0, combined, salt.size + iv.size, cipherBytes.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    // AES decrypt (expects base64(salt+iv+ciphertext))
    fun decrypt(base64Combined: String, passphrase: String): String? {
        try {
            val combined = Base64.decode(base64Combined, Base64.NO_WRAP)
            if (combined.size < 16 + IV_SIZE) return null
            val salt = combined.copyOfRange(0, 16)
            val iv = combined.copyOfRange(16, 16 + IV_SIZE)
            val cipherBytes = combined.copyOfRange(16 + IV_SIZE, combined.size)
            val key = deriveKey(passphrase, salt)
            val cipher = Cipher.getInstance(AES_ALGO)
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
            val plain = cipher.doFinal(cipherBytes)
            return String(plain, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // SHA-256 hashing (hex)
    fun sha256Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
