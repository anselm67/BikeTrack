package com.anselm.location.data

import android.location.Location
import com.anselm.location.LocationApplication.Companion.app
import kotlinx.serialization.json.JsonArray

class Recording(
    val id: String,
    jsonArray: JsonArray
) {
    private var dataPoints: List<Sample> =
        app.dataManager.process(jsonArray.map { it.toLocationStub() })

    val size: Int
        get() = dataPoints.size

    fun lastSample(): Sample = dataPoints.last()

    fun extractTime(): List<Float> {
        return dataPoints.map {  it.elapsedTime / (1000f * 60) }
    }

    fun extractAltitude(): List<Float> = dataPoints.map {
        it.altitude.toFloat()
    }

    fun extractDistances(): List<Float> {
        val latitudes = dataPoints.map { it.location.latitude }
        val longitudes = dataPoints.map { it.location.longitude }
        val size = dataPoints.size
        val c1 = latitudes.subList(1, size).zip(longitudes.subList(1, size))
        val c0 = latitudes.subList(0, size - 1).zip(longitudes.subList(0, size - 1))
        val results = FloatArray(1)
        val chunks = c1.zip(c0).map { (to, from) ->
            Location.distanceBetween(to.first, to.second, from.first, from.second, results)
            results[0].toDouble() / 1000.0
        }
        var distance = 0.0
        return chunks.map { distance += it; distance.toFloat() }
    }
    fun extractSpeed(): List<Float> = dataPoints.map { 3.6f * it.location.speed }
}