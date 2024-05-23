package com.anselm.location.data

import android.util.Log
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.TAG
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File
import java.time.format.DateTimeFormatter

private const val CATALOG_FILENAME = "catalog.json"

class RecordingManager() {
    private var recordingFile: File? = null
    private lateinit var home: File
    private lateinit var catalogFile: File
    private val buffer = mutableListOf<LocationStub>()
    private var doRecordingProlog = true
    constructor(recordingDirectory: File) : this() {
        home = File(recordingDirectory, "recordings")
        catalogFile = File(home, CATALOG_FILENAME)
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

    fun record(location: LocationStub) {
        buffer.add(location)
        if ( buffer.size > 50 ) {
            flush()
        }
    }

    fun load(recordingId: String): Recording {
        val entry = catalog?.find { it.id == recordingId }
        if ( entry == null ) {
            error("No such recording $recordingId")
        }
        return load(entry)
    }

    fun load(entry: Entry): Recording {
        Log.d(TAG, "load ${entry.id}")
        with ( File(home, entry.id) ) {
            val jsonText = "[" + this.readText() + "]"
            return Recording(entry, Json.decodeFromString<JsonArray>(jsonText))
        }
    }

    fun lastRecording(): Recording? {
        return list().lastOrNull()?.load()
    }

    companion object {
        private var instance: RecordingManager? = null
        fun getInstance(recordingDirectory: File): RecordingManager {
            return instance ?: synchronized(this) {
                instance ?: RecordingManager(recordingDirectory).also { instance = it }
            }
        }
    }

    private fun fromJson(obj: JsonObject): Entry {
        return Entry(
            id = obj["id"]?.jsonPrimitive?.content ?: "",
            title = obj["title"]?.jsonPrimitive?.content ?: "",
            time = obj["time"]?.jsonPrimitive?.long ?: 0,
            description = obj["description"]?.jsonPrimitive?.content ?: "",
            // We cache the lastSample for efficient listing of recordings.
            lastSample = obj["lastSample"]?.toSample() ?: defaultSample
        )
    }

    private var catalog : MutableList<Entry>? = null

    private fun rebuildCatalog() {
        Log.d(TAG, "rebuildCatalog")
        val newCatalog = mutableListOf<Entry>()
        home.list()?.forEach { id ->
            if ( id != "catalog.json") {
                val entry = Entry(
                    id = id,
                    title = "",
                    time = 0,
                    description = "",
                    lastSample = defaultSample
                )
                val recording = load(entry)
                newCatalog.add(entry.apply {
                    title = id
                    time = recording.time
                    lastSample = recording.lastSample()
                })
            }
        }
        catalog = newCatalog
        saveCatalog()
    }

    private fun loadCatalog() {
        if ( catalogFile.exists() ) {
            // TODO Handle errors.
            with ( catalogFile ) {
                val jsonText = this.readText()
                catalog = Json.decodeFromString<MutableList<Entry>>(jsonText)
            }
        } else {
            rebuildCatalog()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun saveCatalog() {
        assert( catalog != null )
        Log.d(TAG, "saveCatalog: ${catalog!!.size} entries.")
        val prettyJson = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
        with ( catalogFile ) {
            this.writeText(prettyJson.encodeToString(catalog))
        }
    }

    fun list(): List<Entry> {
        synchronized (this ) {
            if (catalog == null) {
                loadCatalog()
            }
        }
        return catalog!!.toList()
    }
}

@Serializable
class Entry (
    val id: String,
    var title: String,
    var time: Long,
    var description: String,
    var lastSample: Sample,
) {
    @Contextual val recordingManager = app.recordingManager

    fun load(): Recording {
        return recordingManager.load(this)
    }

    fun save() {
        recordingManager.saveCatalog()
    }
}
