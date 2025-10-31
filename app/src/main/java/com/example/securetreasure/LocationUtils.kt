package com.example.securetreasure

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

object LocationUtils {
    // Return last known location from available providers, or null if permission missing or no location.
    fun getLastKnownLocation(context: Context): Location? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        var best: Location? = null
        for (p in providers) {
            try {
                val l = lm.getLastKnownLocation(p)
                if (l != null) {
                    if (best == null || l.time > best.time) best = l
                }
            } catch (_: Exception) {
                // ignore provider errors
            }
        }
        return best
    }

    // Check whether `loc` is within `radiusMeters` of (lat, lon)
    fun isWithinRadius(loc: Location, lat: Double, lon: Double, radiusMeters: Float): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(loc.latitude, loc.longitude, lat, lon, results)
        return results[0] <= radiusMeters
    }
}

