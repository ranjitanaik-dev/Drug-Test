package com.ncb.drugtestcompanion.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class LocationInfo(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val status: String = "UNAVAILABLE"
)

/**
 * LocationProvider abstraction for obtaining GPS status & reverse-geocoded address line.
 * Retrieves real GPS coordinates and human-readable address line with pincode if available.
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getCurrentLocation(): LocationInfo {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return LocationInfo(latitude = null, longitude = null, address = null, status = "UNAVAILABLE")
        }

        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return LocationInfo(latitude = null, longitude = null, address = null, status = "UNAVAILABLE")

            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.time > bestLocation.time) {
                    bestLocation = loc
                }
            }

            // If no last known location, attempt a fast location request
            if (bestLocation == null && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
                val provider = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    LocationManager.NETWORK_PROVIDER
                } else {
                    LocationManager.GPS_PROVIDER
                }

                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        bestLocation = location
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                try {
                    locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                    Thread.sleep(800) // Brief window to receive location fix
                    locationManager.removeUpdates(listener)

                    if (bestLocation == null) {
                        for (p in providers) {
                            val l = locationManager.getLastKnownLocation(p) ?: continue
                            if (bestLocation == null || l.time > bestLocation.time) {
                                bestLocation = l
                            }
                        }
                    }
                } catch (_: Throwable) {
                    locationManager.removeUpdates(listener)
                }
            }

            if (bestLocation != null) {
                val resolvedAddress = try {
                    if (Geocoder.isPresent()) {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(bestLocation.latitude, bestLocation.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val line = addr.getAddressLine(0)
                            if (!line.isNullOrBlank()) {
                                line
                            } else {
                                val locality = addr.locality ?: addr.subLocality ?: ""
                                val thoroughfare = addr.thoroughfare ?: addr.subThoroughfare ?: ""
                                val postal = addr.postalCode ?: ""
                                listOf(locality, thoroughfare, postal).filter { it.isNotBlank() }.joinToString(", ")
                            }
                        } else null
                    } else null
                } catch (_: Throwable) {
                    null
                }

                LocationInfo(
                    latitude = bestLocation.latitude,
                    longitude = bestLocation.longitude,
                    address = resolvedAddress,
                    status = "AVAILABLE"
                )
            } else {
                LocationInfo(latitude = null, longitude = null, address = null, status = "UNAVAILABLE")
            }
        } catch (_: Throwable) {
            LocationInfo(latitude = null, longitude = null, address = null, status = "UNAVAILABLE")
        }
    }
}
