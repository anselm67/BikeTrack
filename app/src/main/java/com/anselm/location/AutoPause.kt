package com.anselm.location

import android.location.Location

class AutoPause {
    private var lastLocation: Location? = null
    fun isAutoPause(location: Location): Boolean {
        var paused = false
        if ( lastLocation != null) {
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