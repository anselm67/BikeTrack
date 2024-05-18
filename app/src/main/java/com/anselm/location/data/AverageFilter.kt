package com.anselm.location.data

abstract class AverageFilter(
    size: Int
) : DataFilter {
    private val data = DoubleArray(size) { 0.0 }

    abstract fun output(sample: Sample, value: Double)
    override fun update(sample: Sample) {
        if ( sample.seqno < data.size) {
            data[sample.seqno] = sample.location.altitude
        } else {
            data.copyInto(data, 0, 1, data.size - 1)
            data[data.size - 1] = sample.location.altitude
            output(sample, data.average())
        }
    }
}