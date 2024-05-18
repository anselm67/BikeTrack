package com.anselm.location.data

import android.location.Location
import android.util.Log
import com.anselm.location.AutoPause
import com.anselm.location.TAG
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.math.max

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

fun JsonElement.toLocationStub(): LocationStub {
    if ( this !is JsonObject ) {
        throw Exception("Expected JSON object")
    }
    val obj = this.jsonObject
    return LocationStub(
        time = obj["time"]?.jsonPrimitive?.long ?: 0,
        latitude = obj["latitude"]?.jsonPrimitive?.double ?: 0.0,
        longitude = obj["longitude"]?.jsonPrimitive?.double ?: 0.0,
        altitude = obj["altitude"]?.jsonPrimitive?.double ?: 0.0,
        accuracy = obj["accuracy"]?.jsonPrimitive?.float ?: 0.0f,
        speed = obj["speed"]?.jsonPrimitive?.float ?: 0.0f,
        bearing = obj["bearing"]?.jsonPrimitive?.float ?: 0.0f,
    )
}

data class Sample(
    val seqno: Int,
    val location: LocationStub,
    // All remaining values are computed / updated through filters.
    val elapsedTime: Long,
    val avgSpeed:  Double,
    val maxSpeed:  Double,
    val totalDistance: Double,
    val distance: Double,
    val verticalDistance: Double,
    val climb: Double,
    val descent: Double,
)

val defaultSample = Sample(
    seqno = 0,
    elapsedTime = 0L,
    location = LocationStub(),
    avgSpeed = 0.0,
    maxSpeed = 0.0,
    totalDistance = 0.0,
    distance = 0.0,
    climb = 0.0,
    descent = 0.0,
    verticalDistance = 0.0,
)

interface DataFilter {

    fun update(sample: Sample)

}

interface AutoPauseListener {

    fun onAutoPause(onOff: Boolean)
}
class DataManager(
    private val recordingManager: RecordingManager,
) {
    private var lastSample: Sample? = null
    private var isRunning = true
    private var sumSpeed = 0.0
    private var sumAltitude = 0.0


    private val autoPauseListeners = mutableListOf<AutoPauseListener>()
    fun addAutoPauseListener(autoPauseListener: AutoPauseListener): DataManager {
        autoPauseListeners.add(autoPauseListener)
        return this
    }

    fun removeAutoPauseListener(autoPauseListener: AutoPauseListener): Boolean {
        return autoPauseListeners.remove(autoPauseListener)
    }

    private val filters = mutableListOf<DataFilter>()
    fun addFilter(filter: DataFilter): DataManager {
        filters.add(filter)
        return this
    }

    fun reset() {
        lastSample = null
    }

    private fun update(rawLocation: Location): Sample {
        // This really can't happen.
        if ( lastSample == null ) {
            throw Exception("No sample available")
        }
        val location = LocationStub(rawLocation)
        val lastSample = lastSample!!
        this.sumSpeed += location.speed
        this.sumAltitude += location.altitude
        val verticalDistance = location.altitude - lastSample.location.altitude
        val distance = location.distanceTo(lastSample.location).toDouble()
        val nextSample = Sample(
            seqno = lastSample.seqno + 1,
            elapsedTime = lastSample.elapsedTime + location.time - lastSample.location.time,
            location = location,
            avgSpeed = sumSpeed / (lastSample.seqno + 1),
            maxSpeed = max(location.speed.toDouble(), lastSample.maxSpeed),
            totalDistance = lastSample.totalDistance + distance,
            distance = distance,
            climb = if (verticalDistance > 0)
                lastSample.climb + verticalDistance
            else
                lastSample.climb,
            descent = if (verticalDistance < 0)
                lastSample.descent - verticalDistance
            else
                lastSample.descent,
            verticalDistance = verticalDistance,
        )
        this.lastSample = nextSample
        return nextSample
    }

    private fun firstSample(location: Location): Sample {
        if ( location.provider == null) {
            return defaultSample
        }
        this.sumSpeed = 0.0
        this.sumAltitude = 0.0
        this.lastSample = defaultSample.copy(location = LocationStub(location))
        return this.lastSample!!
    }

    fun onLocation(location: Location): Sample {
        val shouldRun = ! AutoPause.get().isAutoPause(location)
        if ( shouldRun && ! isRunning ) {
            isRunning = true
            autoPauseListeners.forEach {
                it.onAutoPause(false)
            }
        } else if ( ! shouldRun && isRunning) {
            Log.d(TAG, "Entering auto pause.")
            isRunning = false
            autoPauseListeners.forEach {
                it.onAutoPause(true)
            }
        }
        if ( isRunning ) {
            recordingManager.record(location)
            val nextSample = if (lastSample == null) firstSample(location) else update(location)
            filters.forEach { it.update(nextSample) }
            return nextSample
        } else {
            return lastSample ?: firstSample(location)
        }
    }

}