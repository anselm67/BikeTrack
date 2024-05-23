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
            val jsonText = buffer.joinToString(",\n") {
                Json.encodeToString(it)
            }
            if ( ! doRecordingProlog ) {
                recordingFile?.appendText(", \n")
            }
            doRecordingProlog = false
            recordingFile?.appendText(jsonText)
            buffer.clear()
        }
    }
    fun stop(): Entry {
        assert(recordingFile != null)
        val recordingFile = recordingFile!!
        Log.d(TAG, "stopRecording")
        flush()
        // Add a new entry to the catalog for this ride.
        val entry = Entry(
            id = recordingFile.name,
            title = "Enter a title",
            time = System.currentTimeMillis(),
            description = "Enter a description",
            lastSample = defaultSample,
        )
        addEntry(entry)
        doRecordingProlog = true
        this.recordingFile = null
        return entry
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
            return Recording(
                entry,
                app.dataManager.process(
                    Json.decodeFromString<List<LocationStub>>(jsonText)
                )
            )
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

    private val catalog by lazy { loadCatalog() }

    private fun rebuildCatalog(): MutableList<Entry> {
        Log.d(TAG, "rebuildCatalog")
        val catalog = mutableListOf<Entry>()
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
                catalog.add(entry.apply {
                    title = id
                    time = recording.time
                    lastSample = recording.lastSample()
                })
            }
        }
        // TODO We should save this, right now it's recomputed on each launch until
        // saved through adding/modifying a recording
        return catalog
    }

    private fun loadCatalog(): MutableList<Entry> {
        if ( catalogFile.exists() ) {
            // TODO Handle errors.
            with ( catalogFile ) {
                val jsonText = this.readText()
                return Json.decodeFromString<MutableList<Entry>>(jsonText)
            }
        } else {
            return rebuildCatalog()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun saveCatalog() {
        Log.d(TAG, "saveCatalog: ${catalog.size} entries.")
        val prettyJson = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
        with ( catalogFile ) {
            this.writeText(prettyJson.encodeToString(catalog))
        }
    }

    private fun addEntry(entry: Entry) {
        catalog.add(entry)
        saveCatalog()
    }

    fun removeEntry(entry: Entry) {
        if ( catalog.remove(entry) ) {
            File(home, entry.id).delete()
            saveCatalog()
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
