package com.racelink.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wraps Fused Location Provider. Emits live GPS-speed and last-fix data.
 *
 * The race engine uses [speedMph] to detect rolling-start synchronization,
 * and integrates [Location] samples to estimate finish-line crossing.
 */
class SpeedTracker(private val appContext: Context) {

    data class Sample(
        val location: Location,
        /** Phone monotonic clock at the time we received the fix. */
        val elapsedRealtimeMillis: Long,
        /** Wallclock (System.currentTimeMillis) at the time we received the fix. */
        val wallClockMillis: Long,
    )

    private val client = LocationServices.getFusedLocationProviderClient(appContext)

    private val _sample = MutableStateFlow<Sample?>(null)
    val sample: StateFlow<Sample?> = _sample.asStateFlow()

    private val _speedMph = MutableStateFlow(0f)
    val speedMph: StateFlow<Float> = _speedMph.asStateFlow()

    private var callback: LocationCallback? = null

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        appContext, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasPermission()) return
        stop()
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 200L)
            .setMinUpdateIntervalMillis(100L)
            .setMaxUpdateDelayMillis(200L)
            .setWaitForAccurateLocation(false)
            .build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val now = System.currentTimeMillis()
                _sample.value = Sample(loc, android.os.SystemClock.elapsedRealtime(), now)
                if (loc.hasSpeed()) {
                    _speedMph.value = loc.speed * MS_TO_MPH
                }
            }
        }
        callback = cb
        client.requestLocationUpdates(req, cb, Looper.getMainLooper())
    }

    fun stop() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
    }

    companion object {
        const val MS_TO_MPH = 2.2369363f
        const val MPH_TO_FT_PER_S = 1.4666667f
    }
}
