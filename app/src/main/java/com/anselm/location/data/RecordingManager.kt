package com.anselm.location.data

import android.util.Log
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.TAG
import com.anselm.location.UPDATE_PERIOD_MILLISECONDS
import com.anselm.location.startOfMonth
import com.anselm.location.startOfWeek
import com.anselm.location.startOfYear
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.format.DateTimeFormatter

private const val CATALOG_FILENAME = "catalog.json"
/* Should be greater than FIRST_FLUSH_LENGTH */
private const val MIN_RECORDING_LENGTH = 5
private const val FIRST_FLUSH_LENGTH = 3

class RecordingManager() {
    private var recordingFile: File? = null
    private lateinit var home: File
    private lateinit var catalogFile: File
    private val buffer = mutableListOf<LocationStub>()
    private var doRecordingProlog = true

    init {
        // This is needed to ensure the recording file is created.
        assert(MIN_RECORDING_LENGTH > FIRST_FLUSH_LENGTH)
    }

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
        app.onRecordingChanged(true)
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
    fun stop(lastSample: Sample?): Entry? {
        app.onRecordingChanged(false)
        // Don't record small rides.
        if ( lastSample == null || lastSample.seqno <  MIN_RECORDING_LENGTH ) {
            return null
        } else {
            assert(recordingFile != null)
            val recordingFile = recordingFile!!
            Log.d(TAG, "stopRecording")
            flush()
            // Add a new entry to the catalog for this ride.
            val entry = Entry(
                id = recordingFile.name,
                title = "",
                time = System.currentTimeMillis(),
                description = "",
                lastSample = lastSample,
                tags = mutableListOf(),
            )
            addEntry(entry)
            doRecordingProlog = true
            this.recordingFile = null
            return entry
        }
    }

    // We want to flush once quickly so we record the ride, and then every minute.
    private var nextFlush = FIRST_FLUSH_LENGTH

    fun record(location: LocationStub) {
        buffer.add(location)
        if ( buffer.size > nextFlush ) {
            flush()
            nextFlush = (60000L / UPDATE_PERIOD_MILLISECONDS).toInt().coerceAtLeast(1)
        }
    }

    fun load(recordingId: String): Recording? {
        catalog.rides.find { it.id == recordingId }?.let {
            return@load load(it)
        }
        return null
    }

    private fun load(entry: Entry): Recording? {
        Log.d(TAG, "load ${entry.id}")
        try {
            with ( File(home, entry.id) ) {
                val jsonText = "[" + this.readText() + "]"
                return Recording(
                    entry,
                    app.dataManager.process(
                        Json.decodeFromString<List<LocationStub>>(jsonText)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load file ${entry.id} - removing from catalog.", e)
            delete(entry)
        }
        return null
    }

    fun liveRecording(): Recording? {
        return recordingFile?.name?.let {
            load(Entry(
                id = it,
                title = "Current ride.",
                time = System.currentTimeMillis(),
                description = "",
                lastSample = defaultSample,      // TODO This will cause troubles.
                tags = mutableListOf()
            ))
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

    private var _catalog: Catalog? = null
    private val catalog: Catalog
        get() = _catalog ?: loadCatalog()

    fun rebuildCatalog() {
        Log.d(TAG, "rebuildCatalog")
        val newCatalog = Catalog()
        val byWeek = mutableMapOf<Long, StatsEntry>()
        val byMonth = mutableMapOf<Long, StatsEntry>()
        val byYear = mutableMapOf<Long, StatsEntry>()
        home.list()?.forEach { id ->
            if ( id != "catalog.json") {
                val entry = Entry(
                    id = id,
                    title = "",
                    time = 0,
                    description = "",
                    lastSample = defaultSample,
                    tags = mutableListOf(),
                )
                load(entry)?.let { recording ->
                    val oldEntry = _catalog?.rides?.find { it.id == id }
                    val newEntry = entry.apply {
                        title = oldEntry?.title ?: id
                        description = oldEntry?.description ?: ""
                        time = recording.time
                        lastSample = recording.lastSample()
                    }
                    newCatalog.rides.add(newEntry)
                    val week = startOfWeek(newEntry.time)
                    val month = startOfMonth(newEntry.time)
                    val year = startOfYear(newEntry.time)
                    byWeek[week] = byWeek.getOrDefault(week, StatsEntry(week))
                        .aggregate(newEntry.lastSample)
                    byMonth[month] = byMonth.getOrDefault(month, StatsEntry(month))
                        .aggregate(newEntry.lastSample)
                    byYear[year] = byYear.getOrDefault(year, StatsEntry(month))
                        .aggregate(newEntry.lastSample)
                }
            }
        }
        for ((stats, by) in listOf(
            Pair(newCatalog.weeklyStats, byWeek),
            Pair(newCatalog.monthlyStats, byMonth),
            Pair(newCatalog.annualStats, byYear))) {
            for ((_, statsEntry) in by.toSortedMap()) {
                stats.add(statsEntry)
            }
        }
        // TODO We should save this, right now it's recomputed on each launch until
        // saved through adding/modifying a recording
        _catalog = newCatalog
        try {
            saveCatalog()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save catalog.", e)
        }
    }

    private fun loadCatalog(): Catalog {
        if ( _catalog == null ) {
            synchronized(this) {
                if (catalogFile.exists()) {
                    try {
                        with(catalogFile) {
                            val jsonText = this.readText()
                            _catalog = Json.decodeFromString<Catalog>(jsonText)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load catalog - rebuilding.", e)
                    }
                }
                if ( _catalog == null ) {
                    rebuildCatalog()
                }
            }
        }
        return catalog
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun saveCatalog() {
        Log.d(TAG, "saveCatalog: ${catalog.rides.size} entries.")
        val prettyJson = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
        with ( catalogFile ) {
            this.writeText(prettyJson.encodeToString(catalog))
        }
    }

    private fun addEntry(entry: Entry) {
        catalog.rides.add(entry)
        saveCatalog()
    }

    private fun delete(entry: Entry) {
        if ( catalog.rides.remove(entry) ) {
            File(home, entry.id).delete()
            saveCatalog()
        }
    }

    fun delete(recordingId: String): Boolean {
        val entry = catalog.rides.find { it.id == recordingId }
        if ( entry != null ) {
            delete(entry)
            return true
        } else {
            return false
        }
    }

    fun list(): List<Entry> {
        return catalog.rides.toList().sortedByDescending { it.time }
    }

    fun annualStats(): List<StatsEntry> {
        return catalog.annualStats
    }

    fun monthlyStats(): List<StatsEntry> {
        return catalog.monthlyStats
    }

    fun weeklyStats(): List<StatsEntry> {
        return catalog.weeklyStats
    }
}

@Serializable
private data class Catalog(
    val rides: MutableList<Entry> = mutableListOf(),
    val weeklyStats: MutableList<StatsEntry> = mutableListOf(),
    val monthlyStats: MutableList<StatsEntry> = mutableListOf(),
    val annualStats: MutableList<StatsEntry> = mutableListOf(),
)

@Serializable
class Entry (
    val id: String,
    var title: String,
    var time: Long,
    var description: String,
    val tags: MutableList<String>,
    var lastSample: Sample,
) {
    @Contextual val recordingManager = app.recordingManager

    fun save() {
        recordingManager.saveCatalog()
    }
}

@Serializable
class StatsEntry(
    val timestamp: Long = 0L,
    var distance: Double = 0.0,
    var elapsedTime: Long = 0L,
    var climb: Double = 0.0,
    var descent: Double = 0.0
) {
    fun aggregate(sample: Sample) : StatsEntry {
        this.distance += sample.totalDistance
        this.elapsedTime += sample.elapsedTime
        this.climb += sample.climb
        this.descent += sample.descent
        return this
    }
}