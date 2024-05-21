package com.anselm.location.data

import android.location.Location
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.anselm.location.AutoPause
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
        val isRecording = mutableStateOf(false)
        val isAutoPause = mutableStateOf(false)

        var lastSample: Sample? = null

        fun reset() {
            lastSample = null
            filters.forEach { it.reset() }
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
        val lastSample = context.lastSample!!
        val nextSample = defaultSample.copy(
            seqno = lastSample.seqno + 1,
            location = location,
            elapsedTime = lastSample.elapsedTime + location.time - lastSample.location.time,
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
            if (shouldRun && context.isAutoPause.value) {
                context.isAutoPause.value = false
            } else if (!shouldRun && !context.isAutoPause.value) {
                Log.d(TAG, "Entering auto pause.")
                context.isAutoPause.value = true
            }
        }
        if ( shouldRun ) {
            if ( context.isRecording.value ) {
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