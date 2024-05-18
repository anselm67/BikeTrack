package com.anselm.location.data

import android.location.Location
import android.util.Log
import com.anselm.location.TAG
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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
        if ( buffer.size > 50 ) {
            flush()
        }
    }

    fun load(filename: String): Recording {
        Log.d(TAG, "load $filename")
        with(File(home, filename)) {
            val jsonText = "[" + this.readText() + "]"
            return Recording(Json.decodeFromString<JsonArray>(jsonText))
        }
    }

    fun lastRecording(): Recording? {
        return home.listFiles()?.let { files ->
            files.maxByOrNull { it.lastModified() }?.let { load(it.name) }
        }
    }

    companion object {
        private var instance: RecordingManager? = null
        fun getInstance(recordingDirectory: File): RecordingManager {
            return instance ?: synchronized(this) {
                instance ?: RecordingManager(recordingDirectory).also { instance = it }
            }
        }

        fun get(): RecordingManager {
            return instance ?: throw IllegalStateException("RecordingManager not initialized")
        }
    }
}