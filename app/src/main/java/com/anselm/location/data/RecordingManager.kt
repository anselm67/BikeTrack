package com.anselm.location.data

import android.net.Uri
import android.os.FileUtils.copy
import android.util.Log
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.TAG
import com.anselm.location.UPDATE_PERIOD_MILLISECONDS
import com.anselm.location.startOfMonth
import com.anselm.location.startOfWeek
import com.anselm.location.startOfYear
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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

    fun rebuildCatalog(progress: ((Float) -> Unit)? = null) {
        Log.d(TAG, "rebuildCatalog")
        val newCatalog = Catalog()
        var done = 0
        val rides = home.list()
        rides?.forEach { id ->
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
                        tags = oldEntry?.tags?.toMutableList() ?: mutableListOf()
                        lastSample = recording.lastSample()
                    }
                    newCatalog.rides.add(newEntry)
                    progress?.let { progress(done++.toFloat() / rides.size) }
                }
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
        catalog.recomputeStatistics()
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

    class Query(
        private var rangeInMeters: ClosedFloatingPointRange<Float> = 0f..MAX_DISTANCE,
        val tags: Set<String> = emptySet(),
        val tagPrefix: String = "",
    ) {
        fun match(e: Entry): Boolean {
            return e.lastSample.totalDistance in rangeInMeters &&
                    tags.all { tag -> e.tags.contains(tag) }
        }
        var rangeInKilometers: ClosedFloatingPointRange<Float>
            get() {
                return rangeInMeters.start / 1000f .. rangeInMeters.endInclusive / 1000f
            }
            set(value) {
                rangeInMeters = value.start * 1000f .. value.endInclusive * 1000f
            }

        companion object {
            private const val MAX_DISTANCE = 250 * 1000f
            val default =  Query(0f .. MAX_DISTANCE, emptySet())
        }
    }

    fun list(query: Query? = null): List<Entry> {
        val results = mutableListOf<Entry>()
        for (entry in catalog.rides.toList().sortedByDescending { it.time }) {
            if ( query == null || query == Query.default ) {
                results.add(entry)
            } else if ( query.match(entry) ) {
                results.add(entry)
            }

        }
        return results
    }

    fun histo(query: Query? = null): List<Pair<String,Int>> {
        val counts = list(query).flatMap { entry ->
            if ( query == null || query.tagPrefix.isEmpty() ) {
                entry.tags.distinct()
            } else {
                entry.tags.distinct().filter { it.startsWith(query.tagPrefix, ignoreCase = true) }
            }
        }.groupingBy { it }.eachCount()
        return counts.entries.sortedWith(
            compareByDescending<Map.Entry<String, Int>> {  query?.tags?.contains(it.key) ?: false }.thenByDescending { it.value }
        ).map {
            Pair(it.key, it.value)
        }
    }

    fun annualStats(): List<StatsEntry> {
        catalog.annualStats.lastOrNull()?.let { lastEntry ->
            val previous = catalog.prevYearStats()
            if ( ! previous.isEmpty() ) {
                lastEntry.previous = previous
            }
        }
        return catalog.annualStats
    }

    fun monthlyStats(): List<StatsEntry> {
        catalog.monthlyStats.lastOrNull()?.let { lastEntry ->
            val previous = catalog.prevMonthStats()
            if ( ! previous.isEmpty() ) {
                lastEntry.previous = previous
            }
        }
        return catalog.monthlyStats
    }

    fun weeklyStats(): List<StatsEntry> {
        catalog.weeklyStats.lastOrNull()?.let { lastEntry ->
            val previous = catalog.prevWeekStats()
            if ( ! previous.isEmpty() ) {
                lastEntry.previous = previous
            }
        }
        return catalog.weeklyStats
    }

    fun checkTags(howMany: Int = 25, observer: ((Int) -> Unit)? = null) {
        var done = 0
        for (entry in list()) {
            if ( done >= howMany ) {
                return
            }
            if ( entry.tags.isEmpty() ) {
                load(entry)?.let {
                    if ( done++ < howMany ) {
                        RecordingTagger(it).tag {
                            observer?.let { it(done) }
                            it.save()
                        }
                    }
                }
            }
        }
    }

    private fun exportZipFile(zipFile: File, dest: Uri, progress: ((Float) -> Unit)? = null) {
        val zipOut = ZipOutputStream(zipFile.outputStream())
        val rides = home.list()
        var done = 0
        zipOut.use { zip ->
            rides?.forEach { file ->
                zip.putNextEntry(ZipEntry(file))
                FileInputStream(File(home, file)).use { inputStream -> inputStream.copyTo(zip) }
                progress?.let { it(done++.toFloat() / rides.size) }
            }
        }
        // Copies the temp file into the provided destination.
        app.contentResolver.openFileDescriptor(dest, "w").use { fd ->
            FileOutputStream(fd?.fileDescriptor).use {
                FileInputStream(zipFile).copyTo(it)
            }
        }
    }

    fun exportFiles(dest: Uri, progress: ((Float) -> Unit)? = null) {
        val zipFile = File(app.cacheDir, "rides.zip")
        try {
            return exportZipFile(zipFile, dest, progress)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to export rides as zip file.", e)
        } finally {
            zipFile.delete()
        }
    }

    private fun countEntries(uri: Uri): Int {
        var count = 0
        app.contentResolver.openInputStream(uri)?.use { input ->
            input.buffered(128 * 1024).use { zipInputStream ->
                ZipInputStream(zipInputStream).use {
                    var entry: ZipEntry? = it.nextEntry
                    while (entry != null) {
                        entry = it.nextEntry
                        count++
                    }
                }
            }
        }
        return count
    }

    private suspend fun importZipInputStream(
        zipInputStream: ZipInputStream,
        entryCount: Int,
        progress: ((Float) -> Unit)? = null
    ) {
        var entry: ZipEntry? = zipInputStream.nextEntry
        var done = 0
        while (entry != null) {
            val file = File(home, entry.name)
            if (entry.isDirectory) {
                file.mkdirs()
            } else {
                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { out ->
                        copy(zipInputStream, out)
                        progress?.let { it(done++.toFloat() / entryCount) }
                    }
                }
            }
            entry = zipInputStream.nextEntry
        }
    }

    suspend fun importZipFile(zipUri: Uri, progress: ((Float) -> Unit)? = null) {
        val backupHome = File(home.parentFile, "recordings.backup")
        try {
            if ( home.exists() && ! home.renameTo(backupHome) ) {
                throw IOException("Failed to rename $home to $backupHome")
            }
            home.mkdirs()
            val entryCount = countEntries(zipUri)
            app.contentResolver.openInputStream(zipUri)?.use { input ->
                input.buffered(128 * 1024).use { zipInputStream ->
                    ZipInputStream(zipInputStream).use {
                        importZipInputStream(it, entryCount, progress)
                    }
                }
            }
            loadCatalog()
        } catch (e: Exception) {
            Log.d(TAG, "Failed to import zip file $zipUri", e)
        } finally {
            backupHome.deleteRecursively()
        }
    }
}

@Serializable
private data class Catalog(
    val rides: MutableList<Entry> = mutableListOf(),
    val weeklyStats: MutableList<StatsEntry> = mutableListOf(),
    val monthlyStats: MutableList<StatsEntry> = mutableListOf(),
    val annualStats: MutableList<StatsEntry> = mutableListOf(),
) {
    private fun lastYear(): Pair<Long, Long> {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.todayIn(timeZone)
        return Pair(
            LocalDate(now.year - 1, Month.JANUARY, 1)
                .atStartOfDayIn(timeZone).toEpochMilliseconds(),
            LocalDate(now.year -1, now.month, now.dayOfMonth)
                .plus(1, DateTimeUnit.DAY)
                .atStartOfDayIn(timeZone).toEpochMilliseconds()
        )
    }

    private fun lastMonth(): Pair<Long, Long> {
        val timeZone = TimeZone.currentSystemDefault()
        val prevMonth = Clock.System.todayIn(timeZone).minus(1, DateTimeUnit.MONTH)
        return Pair(
            LocalDate(prevMonth.year, prevMonth.month, 1)
                .atStartOfDayIn(timeZone).toEpochMilliseconds(),
            LocalDate(prevMonth.year, prevMonth.month, prevMonth.dayOfMonth)
                .plus(1, DateTimeUnit.DAY)
                .atStartOfDayIn(timeZone).toEpochMilliseconds()
        )
    }

    private fun lastWeek() : Pair<Long, Long> {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.todayIn(timeZone).minus(1, DateTimeUnit.WEEK)
        return Pair(
            now.minus(now.dayOfWeek.isoDayNumber, DateTimeUnit.DAY)
                .atStartOfDayIn(timeZone).toEpochMilliseconds(),
            now.plus(1, DateTimeUnit.DAY)
                .atStartOfDayIn(timeZone).toEpochMilliseconds()
        )
    }

    fun recomputeStatistics() {
        val byWeek = mutableMapOf<Long, StatsEntry>()
        val byMonth = mutableMapOf<Long, StatsEntry>()
        val byYear = mutableMapOf<Long, StatsEntry>()

        for (entry in rides) {
            val week = startOfWeek(entry.time)
            val month = startOfMonth(entry.time)
            val year = startOfYear(entry.time)
            byWeek[week] = byWeek.getOrDefault(week, StatsEntry(week))
                .aggregate(entry.lastSample)
            byMonth[month] = byMonth.getOrDefault(month, StatsEntry(month))
                .aggregate(entry.lastSample)
            byYear[year] = byYear.getOrDefault(year, StatsEntry(month))
                .aggregate(entry.lastSample)
        }
        for ((stats, by) in listOf(
            Pair(weeklyStats, byWeek),
            Pair(monthlyStats, byMonth),
            Pair(annualStats, byYear))) {
            stats.clear()
            for ((_, statsEntry) in by.toSortedMap()) {
                stats.add(statsEntry)
            }
        }
    }

    fun withinStats(interval: Pair<Long, Long>): StatsEntry {
        val stats = StatsEntry()
        for (ride in rides) {
            if ( ride.time in interval.first until interval.second) {
                stats.aggregate(ride.lastSample)
            }
        }
        return stats
    }

    fun prevYearStats(): StatsEntry {
        return withinStats(lastYear())
    }

    fun prevMonthStats(): StatsEntry {
        return withinStats(lastMonth())
    }

    fun prevWeekStats(): StatsEntry {
        return withinStats(lastWeek())
    }

}


@Serializable
class Entry (
    val id: String,
    var title: String,
    var time: Long,
    var description: String,
    var tags: MutableList<String>,
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
    var descent: Double = 0.0,
    var previous: StatsEntry? = null,
) {
    fun aggregate(sample: Sample) : StatsEntry {
        this.distance += sample.totalDistance
        this.elapsedTime += sample.elapsedTime
        this.climb += sample.climb
        this.descent += sample.descent
        return this
    }

    fun isEmpty() : Boolean {
        return distance == 0.0 && elapsedTime == 0L
    }
}