package com.anselm.location.data

// Altitude filter:
// - altitude
// - avgAltitude
// - climb
// - descent

class AltitudeFilter: AverageFilter(3) {
    private var lastSample: Sample? = null
    private var sumAltitude = 0.0

    override fun output(sample: Sample, value: Double) {
        sample.altitude = value
    }

    override fun update(sample: Sample) {
        super.update(sample)
        sumAltitude += sample.altitude
        sample.avgAltitude =
            if ( sample.seqno > 0 )
                sumAltitude / sample.seqno
            else
                0.0
        if ( lastSample != null ) {
            val lastSample = lastSample!!
            val verticalDistance = sample.altitude - lastSample.altitude
            if ( verticalDistance > 0.0) {
                sample.climb = lastSample.climb + verticalDistance
                sample.descent = lastSample.descent
            } else {
                sample.climb = lastSample.climb
                sample.descent = lastSample.descent - verticalDistance
            }
            sample.verticalDistance = verticalDistance
        }
        lastSample = sample
    }

    override fun reset() {
        super.reset()
        sumAltitude = 0.0
        lastSample = null
    }
}