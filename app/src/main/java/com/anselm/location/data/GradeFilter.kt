package com.anselm.location.data

private const val  MIN_DISTANCE_IN_METERS = 5.0

// TODO Keep an array of pendingDistance / pendingAltitude
class GradeFilter : DataFilter {
    private var pendingDistance = -1.0
    private var pendingAltitude = 0.0

    override fun update(sample: Sample) {
        // grade defaults to previous value, unless we update it here.
        if ( pendingDistance >= 0.0) {
            pendingDistance += sample.distance
            pendingAltitude += sample.verticalDistance
            if ( pendingDistance > MIN_DISTANCE_IN_METERS ) {
                sample.grade = 100.0 * pendingAltitude / pendingDistance
                pendingDistance = -1.0
                pendingAltitude = 0.0
            }
        } else {
            if ( sample.distance > MIN_DISTANCE_IN_METERS) {
                sample.grade = 100.0 * sample.verticalDistance / sample.distance
            } else {
                pendingDistance = sample.distance
                pendingAltitude = sample.verticalDistance
            }
        }
    }

    override fun reset() {
        pendingDistance = -1.0
        pendingAltitude = 0.0
    }
}