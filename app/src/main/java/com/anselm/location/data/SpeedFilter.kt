package com.anselm.location.data

class SpeedFilter : DataFilter {
    private var lastSample: Sample? = null
    private var sumSpeed = 0.0
    override fun update(sample: Sample) {
        if ( lastSample != null ) {
            val distance = sample.location.distanceTo(lastSample!!.location).toDouble()
            sample.distance = distance
            sample.totalDistance += distance
            sumSpeed += sample.location.speed
            sample.avgSpeed = sumSpeed / sample.totalDistance
            if ( sample.location.speed > sample.maxSpeed ) {
                sample.maxSpeed = sample.location.speed.toDouble()
            }
        }
        lastSample = sample
    }

    override fun reset() {
        lastSample = null
        sumSpeed = 0.0
    }
}