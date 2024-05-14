package com.anselm.location

import android.location.Location
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.time.format.DateTimeFormatter

fun Location.toJson(): String {
    val obj = buildJsonObject {
        put("time", this@toJson.time)
        put("latitude", this@toJson.latitude)
        put("longitude", this@toJson.longitude)
        put("altitude", this@toJson.altitude)
        put("accuracy", this@toJson.accuracy)
        put("speed", this@toJson.speed)
        put("bearing", this@toJson.bearing)
    }
    return Json.encodeToString(obj)
}

class RecordingManager() {
    private var recordingFile: File? = null
    private lateinit var home: File
    private val buffer = mutableListOf<Location>()
    private var doRecordingProlog = true
    constructor(recordingDirectory: File) : this() {
        home = File(recordingDirectory, "recordings")
        home.mkdirs()
    }
    fun start() {
        Log.d(TAG, "startRecording")
        if (recordingFile != null ) {
            Log.d(TAG, "Recording already started to ${recordingFile?.name}")
        } else {
            val dateString = DateTimeFormatter
                .ofPattern("yyyy-MM-dd-HH-mm-ss")
                .format(java.time.LocalDateTime.now())
            recordingFile = File(home, "recording-$dateString.json")
            doRecordingProlog = true
        }
    }

    private fun flush() {
        if (buffer.size > 0) {
            val jsonText = buffer.joinToString(",\n") { it.toJson() }
            if ( ! doRecordingProlog ) {
                recordingFile?.appendText(", \n")
            }
            doRecordingProlog = false
            recordingFile?.appendText(jsonText)
            buffer.clear()
        }
    }
    fun stop() {
        Log.d(TAG, "stopRecording")
        flush()
        doRecordingProlog = true
        recordingFile = null
    }

    fun record(location: Location) {
        buffer.add(location)
        if ( buffer.size > 5 ) {
            flush()
        }
    }

    companion object {
        private var instance: RecordingManager? = null
        fun getInstance(recordingDirectory: File): RecordingManager {
            return instance ?: synchronized(this) {
                instance ?: RecordingManager(recordingDirectory).also { instance = it }
            }
        }
    }
}