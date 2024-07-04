package com.anselm.location.data

class SpeedFilter : DataFilter {
    private var lastSample: Sample? = null
    override fun update(sample: Sample) {
        if ( lastSample != null ) {
            val lastSample = lastSample!!
            val distance = sample.location.distanceTo(lastSample.location).toDouble()
            sample.distance = distance
            sample.totalDistance = lastSample.totalDistance + distance
            sample.avgSpeed = sample.totalDistance / (sample.elapsedTime / 1000.0)
            if ( sample.location.speed > lastSample.maxSpeed ) {
                sample.maxSpeed = sample.location.speed.toDouble()
            } else {
                sample.maxSpeed = lastSample.maxSpeed
            }
        }
        lastSample = sample
    }

    override fun reset() {
        lastSample = null
    }
}