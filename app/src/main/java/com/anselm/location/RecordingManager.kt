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
    private var recordingFile: File? = null;
    private lateinit var home: File;
    private val buffer = mutableListOf<Location>();

    constructor(recordingDirectory: File) : this() {
        home = File(recordingDirectory, "recordings")
        home.mkdirs()
    }
    fun start() {
        Log.d(TAG, "startRecording")
        if ( this.recordingFile != null ) {
            Log.d(TAG, "Recording already started to ${recordingFile?.name}")
        } else {
            val dateString = DateTimeFormatter
                .ofPattern("yyyy-MM-dd-HH-mm-ss")
                .format(java.time.LocalDateTime.now())
            this.recordingFile = File(home, "recording-$dateString.json")
        }
    }

    private fun flush() {
        if (buffer.size > 0) {
            val jsonText = buffer.joinToString(",\n") { it.toJson() };
            recordingFile?.appendText(jsonText)
            buffer.clear()
        }
    }
    fun stop() {
        Log.d(TAG, "stopRecording")
        flush()
        recordingFile = null;
    }

    fun record(location: Location) {
        buffer.add(location);
        if ( buffer.size > 50 ) {
            flush();
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