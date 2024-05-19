package com.anselm.location.data

import android.location.Location
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.TAG
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.io.Closeable
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

fun LocationStub.toJson(): String {
    val obj = buildJsonObject {
        put("time", this@toJson.time)
        put("latitude", this@toJson.latitude)
        put("longitude", this@toJson.longitude)
        put("altitude", this@toJson.altitude)
        put("accuracy", this@toJson.accuracy)
        put("speed", this@toJson.speed)
        put("bearing", this@toJson.bearing)
    }
    return Json.encodeToString(obj)
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
    var grade: Double,
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
    grade = 0.0
)

interface DataFilter {

    fun update(sample: Sample)

}

class DataManager {
    inner class Context: Closeable {
        val isRecording = mutableStateOf(false)
        val isAutoPause = mutableStateOf(false)

        var lastSample: Sample? = null
        var sumSpeed = 0.0
        var sumAltitude = 0.0

        fun onDestroy() {
            stopRecording()
        }
        fun onLocation(location: LocationStub): Sample {
            return this@DataManager.onLocation(this@Context, location)
        }

        fun reset() {
            lastSample = null
            sumSpeed = 0.0
            sumAltitude = 0.0
        }

        fun startRecording() {
            reset()
            isRecording.value = true
            app.recordingManager.start()
        }

        fun stopRecording() {
            isRecording.value = false
            app.recordingManager.stop()
        }

        override fun close() {
            onDestroy()
        }
    }

    fun createContext(): Context {
        return Context()
    }

    private val filters = mutableListOf<DataFilter>()
    fun addFilter(filter: DataFilter): DataManager {
        filters.add(filter)
        return this
    }

    private fun update(context: Context, location: LocationStub): Sample {
        // This really can't happen.
        if ( context.lastSample == null ) {
            throw Exception("No sample available")
        }
        val lastSample = context.lastSample!!
        context.sumSpeed += location.speed
        context.sumAltitude += location.altitude
        val verticalDistance = location.altitude - lastSample.location.altitude
        val distance = location.distanceTo(lastSample.location).toDouble()
        val nextSample = Sample(
            seqno = lastSample.seqno + 1,
            elapsedTime = lastSample.elapsedTime + location.time - lastSample.location.time,
            location = location,
            avgSpeed = context.sumSpeed / (lastSample.seqno + 1),
            maxSpeed = max(location.speed.toDouble(), lastSample.maxSpeed),
            totalDistance = lastSample.totalDistance + distance,
            distance = distance,
            grade = lastSample.grade,
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
        context.lastSample = nextSample
        return nextSample
    }

    private fun firstSample(context: Context, location: LocationStub): Sample {
        context.sumSpeed = 0.0
        context.sumAltitude = 0.0
        context.lastSample = defaultSample.copy(location = location)
        return context.lastSample!!
    }

    fun onLocation(context: Context, location: LocationStub): Sample {
        val shouldRun = true//! AutoPause.get().isAutoPause(location)
        if ( shouldRun && context.isAutoPause.value ) {
            context.isAutoPause.value = false
        } else if ( ! shouldRun && ! context.isAutoPause.value ) {
            Log.d(TAG, "Entering auto pause.")
            context.isAutoPause.value = true
        }
        if ( ! context.isAutoPause.value ) {
            if ( context.isRecording.value ) {
                app.recordingManager.record(location)
            }
            val nextSample = if (context.lastSample == null)
                firstSample(context, location)
            else
                update(context, location)
            filters.forEach { it.update(nextSample) }
            return nextSample
        } else {
            return context.lastSample ?: firstSample(context, location)
        }
    }
    fun process(input: List<LocationStub>): List<Sample> {
        createContext().use { context ->
            return input.map { onLocation(context, it) }
        }
    }
}