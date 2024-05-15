package com.anselm.location

import android.location.Location
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

data class LocationStub(
    val time: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Double,
    val speed: Double,
    val bearing: Double,
)

fun fromJson(jsonObject: JsonObject): LocationStub {
    return LocationStub(
        time = jsonObject["time"]?.jsonPrimitive?.long ?: 0,
        latitude = jsonObject["latitude"]?.jsonPrimitive?.double ?: 0.0,
        longitude = jsonObject["longitude"]?.jsonPrimitive?.double ?: 0.0,
        altitude = jsonObject["altitude"]?.jsonPrimitive?.double ?: 0.0,
        accuracy = jsonObject["accuracy"]?.jsonPrimitive?.double ?: 0.0,
        speed = jsonObject["speed"]?.jsonPrimitive?.double ?: 0.0,
        bearing = jsonObject["bearing"]?.jsonPrimitive?.double ?: 0.0,
    )
}

class Recording(jsonArray: JsonArray) {
    private var dataPoints: List<LocationStub> = jsonArray.map { fromJson(it.jsonObject) }

    val size: Int
        get() = dataPoints.size

    fun extractTimes(): List<Long> {
        val startTime = dataPoints[0].time
        return dataPoints.map {  it.time - startTime }
    }

    fun extractAltitude(): List<Double> = dataPoints.map { it.altitude }

    fun extractDistances(): List<Double> {
        val latitudes = dataPoints.map { it.latitude }
        val longitudes = dataPoints.map { it.longitude }
        val size = dataPoints.size
        val c1 = latitudes.subList(1, size).zip(longitudes.subList(1, size))
        val c0 = latitudes.subList(0, size - 1).zip(longitudes.subList(0, size - 1))
        val results = FloatArray(1)
        val chunks = c1.zip(c0).map { (to, from) ->
            Location.distanceBetween(to.first, to.second, from.first, from.second, results)
            results[0].toDouble() / 1000.0
        }
        var distance = 0.0
        return chunks.map { distance += it; distance }

    }
}