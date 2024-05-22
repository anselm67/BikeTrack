package com.anselm.location.data

class SpeedFilter : DataFilter {
    private var lastSample: Sample? = null
    private var sumSpeed = 0.0
    override fun update(sample: Sample) {
        if ( lastSample != null ) {
            val lastSample = lastSample!!
            val distance = sample.location.distanceTo(lastSample.location).toDouble()
            sample.distance = distance
            sample.totalDistance = lastSample.totalDistance + distance
            sumSpeed += sample.location.speed
            sample.avgSpeed = sumSpeed / sample.seqno
            if ( sample.location.speed > lastSample.maxSpeed ) {
                sample.maxSpeed = sample.location.speed.toDouble()
            } else {
                sample.maxSpeed = lastSample.maxSpeed
            }
        }
        lastSample = sample
//        Log.d(TAG, "speed: ${sample.location.speed} avgSpeed: ${sample.avgSpeed} maxSpeed: ${sample.maxSpeed} " +
//                "distance ${sample.distance}  ${sample.totalDistance}")
    }

    override fun reset() {
        lastSample = null
        sumSpeed = 0.0
    }
}