package com.anselm.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.data.Entry
import com.anselm.location.data.LocationStub
import com.anselm.location.data.Sample
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


private const val CHANNEL_ID = "LocationTrackerForegroundServiceChannel"

// Update period of our geo location.
const val UPDATE_PERIOD_MILLISECONDS = 5000L

class LocationTracker: Service() {
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private val liveContext = app.dataManager.createContext(true)

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        stopLocationTracker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if ( fusedLocationClient == null ) {
            Log.d(TAG, "onStartCommand")
            startLocationTracker()
        }
        return START_STICKY
    }

    private fun startLocationTracker() {
        Log.d(TAG, "startLocationTracker")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            100,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
    }

    private var isRequestingLocations = false
    @SuppressLint("MissingPermission")
    private fun startLocationRequests() {
        // We'll track locations only when recording is on.
        synchronized(this) {
            if (!isRequestingLocations) {
                fusedLocationClient?.requestLocationUpdates(
                    LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        UPDATE_PERIOD_MILLISECONDS,
                    ).build(),
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
            isRequestingLocations = true
        }
    }

    private fun stopLocationTracker() {
        Log.d(TAG, "stopLocationTracker")
        // Stops the recording if any was ongoing.
        if ( app.isRecording.value ) {
            liveContext.stopRecording()
        }
        // Stops receiving location updates.
        stopLocationRequests()
        fusedLocationClient = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun stopLocationRequests() {
        synchronized(this) {
            isRequestingLocations = false
            fusedLocationClient?.removeLocationUpdates(locationCallback)
        }
    }

    interface SampleCallback {
        fun emit(sample: Sample)

        fun close()
    }
    private val flows = mutableListOf<SampleCallback>()

    private val locationCallback = object : LocationCallback() {

        override fun onLocationResult(locationResult: LocationResult) {
            Log.d(TAG, "Tracker => ${locationResult.lastLocation}")
            locationResult.lastLocation?.let { rawLocation ->
                // Skip fake locations
                if (rawLocation.provider == null) {
                    return
                }
                val sample = app.dataManager.onLocation(
                    liveContext,
                    LocationStub(rawLocation),
                )
                flows.forEach { it.emit(sample) }
            }
        }

        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            Log.d(TAG, "Location availability $locationAvailability")
        }
    }

    fun setupLocationFlow(): Pair<Flow<Sample>, SampleCallback?> {
        var flowCallback: SampleCallback? = null
        val callbackFlow = callbackFlow {

            val callback = object : SampleCallback {
                override fun emit(sample: Sample) {
                    trySendBlocking(sample)
                }
                override fun close() {
                    close()
                }
            }

            flowCallback = callback
            flows.add(callback)

            awaitClose {
                flows.remove(callback)
            }
        }
        return Pair(callbackFlow, flowCallback)
    }

    inner class TrackerBinder : Binder() {
        private val pair = setupLocationFlow()

        val flow: Flow<Sample>
            get() = pair.first

        fun startRecording() {
            startLocationRequests()
            liveContext.startRecording()
        }

        fun stopRecording(): Entry? {
            val entry = liveContext.stopRecording()
            stopLocationRequests()
            return entry
        }

        fun close() {
            pair.second?.close()
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind")
        startService(Intent(this, LocationTracker::class.java))
        return TrackerBinder()
    }

    private fun buildExitAction(): NotificationCompat.Action {
        val intent = Intent(this, MainActivity::class.java)
        intent.action = MainActivity.EXIT_ACTION
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Action.Builder(
            0, "Exit", pendingIntent
        ).build()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Biking")
            .setContentText("Location tracker is running")
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOnlyAlertOnce(true)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .setContentIntent(PendingIntent.getActivity(
                this,
                0,
                Intent(applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE))
            .addAction(buildExitAction())
            .build()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Location Service Channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        serviceChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

}