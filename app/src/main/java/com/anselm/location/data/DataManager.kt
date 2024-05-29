package com.anselm.location.data

import android.location.Location
import android.util.Log
import com.anselm.location.AutoPause
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.TAG
import com.anselm.location.UPDATE_PERIOD_MILLISECONDS
import kotlinx.serialization.Serializable
import java.io.Closeable

@Serializable
data class LocationStub(
    val time: Long,
    val latitude: Double,
    val longitude: Double,
    var altitude: Double,
    val accuracy: Float,
    val speed: Float,
    val bearing: Float,
) {

    constructor(location: Location) : this(
        location.time,
        location.latitude,
        location.longitude,
        location.altitude,
        location.accuracy,
        location.speed,
        location.bearing,
    )

    constructor() : this(
        time = 0,
        latitude = 0.0,
        longitude = 0.0,
        altitude = 0.0,
        accuracy = 0f,
        speed = 0f,
        bearing = 0f
    )

    fun distanceTo(location: LocationStub): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            this.latitude,
            this.longitude,
            location.latitude,
            location.longitude,
            result,
        )
        return result[0]
    }
}

@Serializable
data class Sample(
    val seqno: Int,
    val location: LocationStub,
    // All remaining values are computed / updated through filters.
    var elapsedTime: Long,
    var distance: Double,
    var totalDistance: Double,
    var avgSpeed:  Double,
    var maxSpeed:  Double,
    var altitude: Double,
    var avgAltitude: Double,
    var verticalDistance: Double,
    var climb: Double,
    var descent: Double,
    var grade: Double,
)

val defaultSample = Sample(
    seqno = 0,
    location = LocationStub(),
    // All remaining values are computed / updated through filters.
    elapsedTime = 0L,
    distance = 0.0,
    totalDistance = 0.0,
    avgSpeed = 0.0,
    maxSpeed = 0.0,
    altitude = 0.0,
    avgAltitude = 0.0,
    verticalDistance = 0.0,
    climb = 0.0,
    descent = 0.0,
    grade = 0.0
)

interface DataFilter {

    fun update(sample: Sample)

    fun reset()

}

class DataManager {
    inner class Context(
        val canAutoPause: Boolean = true
    ): Closeable {
        val filters = mutableListOf(
            SpeedFilter(),
            AltitudeFilter(),
            GradeFilter(),
        )

        var lastSample: Sample? = null

        private fun reset() {
            lastSample = null
            filters.forEach { it.reset() }
        }

        fun startRecording() {
            reset()
            app.recordingManager.start()
        }

        fun stopRecording(): Entry? {
            return app.recordingManager.stop(lastSample)
        }

        override fun close() { }
    }

    fun createContext(canAutoPause: Boolean = true): Context {
        return Context(canAutoPause)
    }

    private fun update(context: Context, location: LocationStub): Sample {
        // This really can't happen.
        if ( context.lastSample == null ) {
            throw Exception("No sample available")
        }
        // If the amount of time elapsed since the last sample is (much) greater than the update
        // period, we were in pause. So we don't count that time as active/elapsed time.
        val lastSample = context.lastSample!!
        val lastElapsed = location.time - lastSample.location.time
        val nextSample = defaultSample.copy(
            seqno = lastSample.seqno + 1,
            location = location,
            elapsedTime = lastSample.elapsedTime +
                    if ( lastElapsed < 2 * UPDATE_PERIOD_MILLISECONDS ) lastElapsed else 0,
        )
        context.lastSample = nextSample
        return nextSample
    }

    private fun firstSample(context: Context, location: LocationStub): Sample {
        context.lastSample = defaultSample.copy(location = location)
        return context.lastSample!!
    }

    fun onLocation(context: Context, location: LocationStub): Sample {
        var shouldRun = true
        if ( context.canAutoPause ) {
            shouldRun = !AutoPause.get().isAutoPause(location)
            if (shouldRun && app.isAutoPaused.value) {
                app.onAutoPausedChanged(false)
            } else if (!shouldRun && !app.isAutoPaused.value) {
                Log.d(TAG, "Entering auto pause.")
                app.onAutoPausedChanged(true)
            }
        }
        if ( shouldRun ) {
            if ( app.isRecording.value ) {
                app.recordingManager.record(location)
            }
            val nextSample = if (context.lastSample == null)
                firstSample(context, location)
            else
                update(context, location)
            context.filters.forEach { it.update(nextSample) }
            return nextSample
        } else {
            return context.lastSample ?: firstSample(context, location)
        }
    }
    fun process(input: List<LocationStub>): List<Sample> {
        createContext(false).use { context ->
            return input.map { onLocation(context, it) }
        }
    }
}