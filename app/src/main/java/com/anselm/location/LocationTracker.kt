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
import com.anselm.location.data.DataManager
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
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


private const val CHANNEL_ID = "LocationTrackerForegroundServiceChannel"

class LocationTracker: Service() {
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private val liveContext = app.dataManager.createContext(false /* TODO */)

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

    @SuppressLint("MissingPermission")
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

    private fun stopLocationTracker() {
        Log.d(TAG, "stopLocationTracker")
        fusedLocationClient = null
        locationFlow = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
    inner class TrackerBinder : Binder() {

        val flow: Flow<Sample>
            get() = this@LocationTracker.locationFlow!!

        val liveContext: DataManager.Context
            get() = this@LocationTracker.liveContext
    }

    private var locationFlow: Flow<Sample>? = null

    @SuppressLint("MissingPermission")
    private fun setupLocationFlow() {
        locationFlow = callbackFlow {
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    Log.d(TAG, "Tracker => ${locationResult.lastLocation}")
                    locationResult.lastLocation?.let { rawLocation ->
                        // Skip fake locations
                        if ( rawLocation.provider == null ) {
                            return
                        }
                        val sample = app.dataManager.onLocation(
                            liveContext,
                            LocationStub(rawLocation),
                        )
                        trySendBlocking(sample)
                            .onFailure { e -> Log.e(TAG, "Failed to send location", e) }
                    }
                }
                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    Log.d(TAG, "Location availability $locationAvailability")
                }
            }

            fusedLocationClient?.requestLocationUpdates(
                LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    5000
                ).build(),
                locationCallback,
                Looper.getMainLooper()
            )

            awaitClose {
                Log.d(TAG, "awaitClose - location flow.")
                fusedLocationClient?.removeLocationUpdates(locationCallback)
            }
        }
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

    override fun onBind(intent: Intent?): IBinder {
        if ( locationFlow == null ) {
            setupLocationFlow()
        }
        return TrackerBinder()
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