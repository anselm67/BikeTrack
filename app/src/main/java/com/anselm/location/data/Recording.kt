package com.anselm.location.data

import android.location.Location

class Recording(
    private val entry: Entry,
    private val dataPoints: List<Sample>
) {
    private var updated = false

    var title: String
        get() = entry.title
        set(value) {
            entry.title = value
            updated = true
        }

    var time: Long = dataPoints.last().location.time

    var description: String
        get() = entry.description
        set(value) {
            entry.description = value
            updated = true
        }

    val size: Int
        get() = dataPoints.size

    fun save() {
        if ( updated ) {
            entry.save()
        }
    }

    fun delete() {
        entry.delete()
    }

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