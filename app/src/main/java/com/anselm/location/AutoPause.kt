package com.anselm.location

import android.location.Location
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class AutoPause {
    private var lastLocation: Location? = null
    fun isAutoPause(location: Location): Boolean {
        var paused = false
        if ( lastLocation != null) {
            Log.d(TAG, "AutoPause? ${lastLocation?.speed} ${location.speed}")
            paused = (
                // 0.25f is about 1km / h
                // We moved less than 20m in between samples (5 seconds)
                (location.speed + lastLocation!!.speed < 0.5f) &&
                location.distanceTo(lastLocation!!) < 20f
            )
        }
        lastLocation = location
        return paused
    }

    companion object {
        private var instance: AutoPause? = null
        fun get(): AutoPause {
            return instance ?: synchronized(this) {
                instance ?: AutoPause().also { instance = it }
            }
        }
    }
}