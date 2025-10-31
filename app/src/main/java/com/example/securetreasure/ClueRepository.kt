package com.example.securetreasure

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object ClueRepository {
    private const val PREFS = "secure_treasure_prefs"
    private const val KEY_CLUES = "clues_blob"
    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun saveClues(clues: List<Clue>) {
        if (!::prefs.isInitialized) throw IllegalStateException("ClueRepository not initialized")
        val ja = JSONArray()
        for (c in clues) {
            val jo = JSONObject()
            jo.put("id", c.id)
            jo.put("title", c.title)
            jo.put("encryptedPayload", c.encryptedPayload)
            jo.put("payloadHash", c.payloadHash)
            // optional location fields
            c.latitude?.let { jo.put("latitude", it) }
            c.longitude?.let { jo.put("longitude", it) }
            c.radiusMeters?.let { jo.put("radiusMeters", it.toDouble()) }
            ja.put(jo)
        }
        val json = ja.toString()
        val encrypted = KeyStoreCrypto.encrypt(json.toByteArray(Charsets.UTF_8))
        prefs.edit().putString(KEY_CLUES, encrypted).apply()
    }

    fun loadClues(): List<Clue> {
        if (!::prefs.isInitialized) throw IllegalStateException("ClueRepository not initialized")
        val blob = prefs.getString(KEY_CLUES, null) ?: return emptyList()
        val decrypted = KeyStoreCrypto.decrypt(blob) ?: return emptyList()
        val json = String(decrypted, Charsets.UTF_8)
        return try {
            val ja = JSONArray(json)
            val list = mutableListOf<Clue>()
            for (i in 0 until ja.length()) {
                val jo = ja.getJSONObject(i)
                val id = jo.getLong("id")
                val title = jo.getString("title")
                val encryptedPayload = jo.getString("encryptedPayload")
                val payloadHash = jo.getString("payloadHash")
                val latitude = if (jo.has("latitude")) jo.getDouble("latitude") else null
                val longitude = if (jo.has("longitude")) jo.getDouble("longitude") else null
                val radiusMeters = if (jo.has("radiusMeters")) jo.getDouble("radiusMeters").toFloat() else null
                list.add(Clue(id, title, encryptedPayload, payloadHash, latitude, longitude, radiusMeters))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
