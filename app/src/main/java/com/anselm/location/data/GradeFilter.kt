package com.anselm.location.data

import com.anselm.location.shift

private const val  MIN_DISTANCE_IN_METERS = 10.0

class GradeFilter : DataFilter {
    private var lastSample: Sample? = null
    private var cursor = -1
    private var pendingDistances = DoubleArray(16) { 0.0 }
    private var pendingAltitudes = DoubleArray(16) { 0.0 }

    override fun update(sample: Sample) {
        if ( cursor < 0 ) {
            if ( sample.distance > MIN_DISTANCE_IN_METERS ) {
                sample.grade = 100.0 * sample.verticalDistance / sample.distance
            } else {
                cursor = 1
                pendingDistances[0] = sample.distance
                pendingAltitudes[0] = sample.verticalDistance
                sample.grade = lastSample?.grade ?: 0.0            }
        } else {
            if ( cursor < pendingDistances.size ) {
                pendingDistances[cursor] = sample.distance
                pendingAltitudes[cursor] = sample.verticalDistance
                cursor++
            } else {
                pendingDistances.shift(-1)
                pendingAltitudes.shift(-1)
                pendingDistances[cursor - 1] = sample.distance
                pendingAltitudes[cursor - 1] = sample.verticalDistance
            }
            if ( pendingDistances.sum() > MIN_DISTANCE_IN_METERS ) {
                sample.grade = 100.0 * pendingAltitudes.sum() / pendingDistances.sum()
                do {
                    pendingDistances.shift(1)
                    pendingAltitudes.shift(1)
                    cursor--
                    pendingDistances[cursor] = 0.0
                    pendingAltitudes[cursor] = 0.0
                } while ((cursor > 0) && (pendingDistances.sum() >= MIN_DISTANCE_IN_METERS))
                if ( cursor == 0 ) {
                    cursor = -1
                }
            } else {
                sample.grade = lastSample?.grade ?: 0.0
            }
        }
        lastSample = sample
    }

    override fun reset() {
        pendingDistances.fill(0.0)
        pendingAltitudes.fill(0.0)
        lastSample = null
        cursor = -1
    }
}